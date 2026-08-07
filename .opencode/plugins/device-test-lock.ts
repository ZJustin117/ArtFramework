import { createHash } from "node:crypto"
import { mkdirSync, readFileSync } from "node:fs"
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
  if (!value) return 10 * 60 * 1000
  if (!/^\d+$/.test(value)) return 10 * 60 * 1000
  return Number(value) * 1000
}

function requiresDeviceLock(tool: string, args: unknown): boolean {
  if (tool !== "bash") return false
  const command = (args as { command?: unknown })?.command
  return typeof command === "string" && DEVICE_COMMAND_PATTERNS.some((pattern) => pattern.test(command))
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
      reject(new Error(`timed out waiting for D1 device lock after ${timeout / 1000}s`))
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
      if (code !== 0) reject(new Error(stderr.trim() || `D1 device lock unavailable (flock exited ${code})`))
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
    holder.child.once("exit", () => resolve())
    setTimeout(resolve, 1000).unref()
  })
}

export default async ({ directory }: { directory: string }) => {
  const holders = new Map<string, Promise<LockHolder>>()

  async function releaseSession(sessionID: string): Promise<void> {
    const pending = holders.get(sessionID)
    holders.delete(sessionID)
    if (!pending) return
    try {
      await release(await pending)
    } catch {
      // A failed acquisition owns no lock and must not affect session cleanup.
    }
  }

  return {
    "tool.execute.before": async (
      input: { tool: string; sessionID: string },
      output: { args: unknown },
    ) => {
      if (!requiresDeviceLock(input.tool, output.args)) return
      const serial = serialFromEnvironment(directory)
      if (!serial) {
        throw new Error("device test lock: missing environment key ART_D1_SERIAL")
      }
      if (!holders.has(input.sessionID)) {
        holders.set(input.sessionID, acquire(serial))
      }
      const holder = await holders.get(input.sessionID)!
      console.error(`device test lock: session ${input.sessionID} holds D1 ${holder.serialDigest}`)
    },
    event: async ({ event }: { event: { type: string; properties: { sessionID?: string; info?: { id?: string } } } }) => {
      if (event.type === "session.idle") {
        await releaseSession(event.properties.sessionID || "")
      }
      if (event.type === "session.deleted") {
        await releaseSession(event.properties.info?.id || "")
      }
    },
    dispose: async () => {
      await Promise.all([...holders.keys()].map(releaseSession))
    },
  }
}
