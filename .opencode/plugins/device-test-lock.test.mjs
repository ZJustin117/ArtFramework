import assert from "node:assert/strict"
import { createHash } from "node:crypto"
import { mkdir, mkdtemp, readFile, rm, stat, writeFile } from "node:fs/promises"
import { tmpdir } from "node:os"
import { join } from "node:path"
import { spawn } from "node:child_process"

const wrapper = new URL("../../scripts/with-d1-lock", import.meta.url)

function digest(serial) {
  return createHash("sha256").update(serial).digest("hex").slice(0, 24)
}

function holdLock(path) {
  const child = spawn("flock", [path, "sh", "-c", "printf acquired; cat >/dev/null"], {
    stdio: ["pipe", "pipe", "pipe"],
  })
  return new Promise((resolve, reject) => {
    child.stdout.once("data", () => resolve(child))
    child.once("error", reject)
    child.once("exit", (code) => reject(new Error(`lock holder exited ${code}`)))
  })
}

function tryLock(path) {
  return new Promise((resolve, reject) => {
    const child = spawn("flock", ["-n", path, "true"])
    child.once("error", reject)
    child.once("exit", (code) => resolve(code))
  })
}

function run(command, args, env) {
  return new Promise((resolve, reject) => {
    const child = spawn(command, args, { env, stdio: ["ignore", "pipe", "pipe"] })
    let stderr = ""
    child.stderr.on("data", (chunk) => { stderr += String(chunk) })
    child.once("error", reject)
    child.once("exit", (code) => resolve({ code, stderr }))
  })
}

function start(command, args, env) {
  const child = spawn(command, args, { env, stdio: ["ignore", "pipe", "pipe"] })
  return new Promise((resolve, reject) => {
    child.once("error", reject)
    child.once("exit", (code) => reject(new Error(`lease exited ${code}`)))
    resolve({ child })
  })
}

async function loadPlugin() {
  return (await import("./device-test-lock.ts")).default
}

async function waitForFile(path) {
  for (let attempt = 0; attempt < 20; attempt += 1) {
    try {
      await stat(path)
      return
    } catch {
      await new Promise((resolve) => setTimeout(resolve, 25))
    }
  }
  throw new Error(`timed out waiting for ${path}`)
}

const runtime = await mkdtemp(join(tmpdir(), "artframework-device-lock-test-"))
try {
  const lockDirectory = join(runtime, "artframework-device-locks")
  await mkdir(lockDirectory)
  const first = join(lockDirectory, `d1-${digest("test-device-a")}.lock`)
  const second = join(lockDirectory, `d1-${digest("test-device-b")}.lock`)
  assert.notEqual(first, second, "device lock keys must not collide")
  assert.ok(!first.includes("test-device-a"), "lock path must not disclose the serial")

  const holder = await holdLock(first)
  assert.equal(await tryLock(first), 1, "same device must block a second holder")
  assert.equal(await tryLock(second), 0, "different devices may run concurrently")

  holder.stdin.end()
  await new Promise((resolve) => holder.once("exit", resolve))
  assert.equal(await tryLock(first), 0, "closing the owner releases the lock")

  const pluginFactory = await loadPlugin()
  const plugin = await pluginFactory({ directory: runtime })
  const blocked = await holdLock(first)
  await writeFile(`${first}.info`, "label=blocked verification\npid=1234\n")
  const previousSerial = process.env.ART_D1_SERIAL
  const previousTimeout = process.env.ART_DEVICE_LOCK_TIMEOUT_SECONDS
  process.env.ART_D1_SERIAL = "test-device-a"
  process.env.ART_DEVICE_LOCK_TIMEOUT_SECONDS = "0"
  try {
    await assert.rejects(
      plugin["tool.execute.before"](
        { tool: "bash", sessionID: "ses_test", callID: "call_blocked" },
        { args: { command: "python3 tools/art-verify/run.py demo.yaml --device" } },
      ),
      /D1 device lock unavailable after 0s:.*lock=.*holder=label=blocked verification pid=1234/,
      "a busy device must fail immediately with holder diagnostics",
    )
  } finally {
    if (previousSerial === undefined) delete process.env.ART_D1_SERIAL
    else process.env.ART_D1_SERIAL = previousSerial
    if (previousTimeout === undefined) delete process.env.ART_DEVICE_LOCK_TIMEOUT_SECONDS
    else process.env.ART_DEVICE_LOCK_TIMEOUT_SECONDS = previousTimeout
    blocked.stdin.end()
    await new Promise((resolve) => blocked.once("exit", resolve))
    await rm(`${first}.info`, { force: true })
    await plugin.dispose()
  }

  const leaseEnv = { ...process.env, ART_D1_SERIAL: "test-device-a", XDG_RUNTIME_DIR: runtime }
  const active = await start(wrapper.pathname, ["--ttl", "10s", "--label", "metadata check", "--", "sh", "-c", "sleep 2"], leaseEnv)
  await waitForFile(`${first}.info`)
  const metadata = await readFile(`${first}.info`, "utf8")
  assert.match(metadata, /label=metadata check/, "an active lease identifies its operation")
  assert.match(metadata, /deadline_at=\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}Z/, "an active lease identifies its deadline")
  active.child.kill("SIGTERM")
  await new Promise((resolve) => active.child.once("exit", resolve))
  await assert.rejects(readFile(`${first}.info`, "utf8"), "a released lease removes its metadata")

  const expired = await run(wrapper.pathname, ["--ttl", "1s", "--", "sh", "-c", "sleep 2"], leaseEnv)
  assert.notEqual(expired.code, 0, "a debug lease must terminate at its TTL")
  assert.equal(await tryLock(first), 0, "an expired debug lease releases the device lock")
  console.log("device-test-lock tests: PASS")
} finally {
  await rm(runtime, { recursive: true, force: true })
}
