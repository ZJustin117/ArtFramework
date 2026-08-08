import { createHash } from "node:crypto"
import { existsSync, mkdirSync, readFileSync } from "node:fs"
import { spawn } from "node:child_process"

const DEVICE_COMMAND_PATTERNS = [
  /(?:^|\s)(?:\.\/)?scripts\/art-lab(?:\s|$)/,
  /(?:^|\s)(?:\.\/)?scripts\/ensure-art-enabled-mods\.sh(?:\s|$)/,
  /(?:^|\s)tools\/art-verify\/run\.py\b[\s\S]*\s--device(?:\s|$)/,
  /(?:^|\s)(?:python3|python)\s+(?:\.\/)?run\.py\b[\s\S]*\s--device(?:\s|$)/,
  /(?:^|\s)(?:python3|python)\s+[^\n]*sts-harness(?:\s|$)/,
  /(?:^|\s)(?:python3|python)\s+[^\n]*scripts\.tools\.arthas(?:\s|$)/,
  /(?:^|\s)adb\s+-s\s+\S+(?:\s|$)/,
]

type LockHolder = {
  child: ReturnType<typeof spawn>
  released: boolean
  serialDigest: string
}

function valueFromDotEnv(content: string, key: string): string | undefined {
  for (const raw of content.split(/\r?\n/)) {
    const line = raw.trim()
    if (!line || line.startsWith("#")) continue
    const match = line.match(/^([A-Za-z_][A-Za-z0-9_]*)=(.*)$/)
    if (!match || match[1] !== key) continue
    let value = match[2].trim()
    if (
      (value.startsWith('"') && value.endsWith('"')) ||
      (value.startsWith("'") && value.endsWith("'"))
    ) {
      value = value.slice(1, -1)
    }
    return value || undefined
  }
  return undefined
}

function serialFromEnvironment(directory: string): string | undefined {
  const processValue = process.env.ART_D1_SERIAL?.trim()
  if (processValue) return processValue
  try {
    return valueFromDotEnv(readFileSync(`${directory}/.env.local`, "utf8"), "ART_D1_SERIAL")
  } catch {
    return undefined
  }
}

function lockPath(serial: string): string {
  const digest = createHash("sha256").update(serial).digest("hex").slice(0, 24)
  const runtimeDir = process.env.XDG_RUNTIME_DIR || "/tmp"
  const dir = `${runtimeDir}/artframework-device-locks`
  mkdirSync(dir, { recursive: true, mode: 0o700 })
  return `${dir}/d1-${digest}.lock`
}

function timeoutMilliseconds(): number {
  const value = process.env.ART_DEVICE_LOCK_TIMEOUT_SECONDS?.trim()
  if (!value) return 30 * 1000
  if (!/^\d+$/.test(value)) return 30 * 1000
  return Number(value) * 1000
}

function lockFailure(path: string, timeout: number, code: number | null, stderr: string): Error {
  let holder = ""
  const infoPath = `${path}.info`
  if (existsSync(infoPath)) {
    try {
      holder = readFileSync(infoPath, "utf8").trim()
    } catch {
      holder = "unreadable"
    }
  }
  const reason = stderr.trim() || (code === null ? "lock wait timed out" : `flock exited ${code}`)
  const details = holder ? ` holder=${holder.replace(/\s+/g, " ")}` : " holder=unknown"
  return new Error(
    `D1 device lock unavailable after ${timeout / 1000}s: ${reason}; lock=${path};${details}`,
  )
}

function requiresDeviceLock(tool: string, args: unknown): boolean {
  if (tool !== "bash") return false
  const command = (args as { command?: unknown })?.command
  if (typeof command !== "string") return false
  // The wrapper owns a lease for its child command. Taking a second flock here
  // would make a wrapped art-lab/adb command wait on its own transaction.
  if (/(?:^|\s)(?:\.\/)?scripts\/with-d1-lock(?:\s|$)/.test(command)) return false
  return DEVICE_COMMAND_PATTERNS.some((pattern) => pattern.test(command))
}

async function acquire(serial: string): Promise<LockHolder> {
  const path = lockPath(serial)
  const timeout = timeoutMilliseconds()
  const child = spawn("flock", ["-w", String(timeout / 1000), path, "sh", "-c", "printf acquired; cat >/dev/null"], {
    stdio: ["pipe", "pipe", "pipe"],
  })
  const acquired = await new Promise<boolean>((resolve, reject) => {
    let stderr = ""
    const timer = setTimeout(() => {
      child.kill("SIGTERM")
      reject(lockFailure(path, timeout, null, ""))
    }, timeout + 1000)
    child.stdout.once("data", (chunk) => {
      clearTimeout(timer)
      resolve(String(chunk).includes("acquired"))
    })
    child.stderr.on("data", (chunk) => {
      stderr += String(chunk)
    })
    child.once("error", (error) => {
      clearTimeout(timer)
      reject(error)
    })
    child.once("exit", (code) => {
      clearTimeout(timer)
      if (code !== 0) reject(lockFailure(path, timeout, code, stderr))
    })
  })
  if (!acquired) throw new Error("D1 device lock unavailable")
  return {
    child,
    released: false,
    serialDigest: createHash("sha256").update(serial).digest("hex").slice(0, 12),
  }
}

async function release(holder: LockHolder | undefined): Promise<void> {
  if (!holder || holder.released) return
  holder.released = true
  holder.child.stdin.end()
  await new Promise<void>((resolve) => {
    let forceTimer: ReturnType<typeof setTimeout> | undefined
    const done = () => {
      if (forceTimer) clearTimeout(forceTimer)
      resolve()
    }
    holder.child.once("exit", done)
    setTimeout(() => {
      holder.child.kill("SIGTERM")
      forceTimer = setTimeout(done, 1000)
      forceTimer.unref()
    }, 1000).unref()
  })
}

export default async ({ directory }: { directory: string }) => {
  const holders = new Map<string, Promise<LockHolder>>()

  async function releaseCall(callID: string): Promise<void> {
    const pending = holders.get(callID)
    holders.delete(callID)
    if (!pending) return
    try {
      await release(await pending)
    } catch {
      // A failed acquisition owns no lock and must not affect session cleanup.
    }
  }

  return {
    "tool.execute.before": async (
      input: { tool: string; sessionID: string; callID: string },
      output: { args: unknown },
    ) => {
      if (!requiresDeviceLock(input.tool, output.args)) return
      const serial = serialFromEnvironment(directory)
      if (!serial) {
        throw new Error("device test lock: missing environment key ART_D1_SERIAL")
      }
      if (!holders.has(input.callID)) {
        const pending = acquire(serial)
        holders.set(input.callID, pending)
        // A failed acquisition must not poison retries in this session.
        pending.catch(() => {
          if (holders.get(input.callID) === pending) holders.delete(input.callID)
        })
      }
      const holder = await holders.get(input.callID)!
      console.error(`device test lock: call ${input.callID} holds D1 ${holder.serialDigest}`)
    },
    "tool.execute.after": async (input: { callID: string }) => {
      await releaseCall(input.callID)
    },
    dispose: async () => {
      await Promise.all([...holders.keys()].map(releaseCall))
    },
  }
}
