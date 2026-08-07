import assert from "node:assert/strict"
import { createHash } from "node:crypto"
import { mkdtemp, rm } from "node:fs/promises"
import { tmpdir } from "node:os"
import { join } from "node:path"
import { spawn } from "node:child_process"

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

const runtime = await mkdtemp(join(tmpdir(), "artframework-device-lock-test-"))
try {
  const first = join(runtime, `d1-${digest("test-device-a")}.lock`)
  const second = join(runtime, `d1-${digest("test-device-b")}.lock`)
  assert.notEqual(first, second, "device lock keys must not collide")
  assert.ok(!first.includes("test-device-a"), "lock path must not disclose the serial")

  const holder = await holdLock(first)
  assert.equal(await tryLock(first), 1, "same device must block a second holder")
  assert.equal(await tryLock(second), 0, "different devices may run concurrently")

  holder.stdin.end()
  await new Promise((resolve) => holder.once("exit", resolve))
  assert.equal(await tryLock(first), 0, "closing the owner releases the lock")
  console.log("device-test-lock tests: PASS")
} finally {
  await rm(runtime, { recursive: true, force: true })
}
