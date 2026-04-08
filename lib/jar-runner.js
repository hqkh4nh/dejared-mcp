/**
 * JAR runner for dejared-mcp.
 *
 * Downloads the matching JAR from GitHub Releases into a platform-specific
 * cache directory, then launches it with the locally available Java runtime.
 */
"use strict";

const { spawn, execFileSync } = require("node:child_process");
const fs = require("node:fs");
const https = require("node:https");
const path = require("node:path");
const os = require("node:os");

const PACKAGE = require(path.join(__dirname, "..", "package.json"));
const JAR_VERSION = PACKAGE.version;
const JAR_NAME = `dejared-mcp-${JAR_VERSION}.jar`;

/**
 * Returns the platform-specific cache directory for storing downloaded JARs.
 *   - Linux/BSD : $XDG_CACHE_HOME/dejared-mcp  (default ~/.cache/dejared-mcp)
 *   - macOS     : ~/Library/Caches/dejared-mcp
 *   - Windows   : %LOCALAPPDATA%\dejared-mcp
 */
function getCacheDir() {
  const env = process.env;
  switch (os.platform()) {
    case "win32":
      return path.join(
        env.LOCALAPPDATA || path.join(os.homedir(), "AppData", "Local"),
        "dejared-mcp",
      );
    case "darwin":
      return path.join(os.homedir(), "Library", "Caches", "dejared-mcp");
    default:
      return path.join(
        env.XDG_CACHE_HOME || path.join(os.homedir(), ".cache"),
        "dejared-mcp",
      );
  }
}

const CACHE_DIR = getCacheDir();
const JAR_PATH = path.join(CACHE_DIR, JAR_NAME);
const DOWNLOAD_URL = `https://github.com/HuynhKhanh1402/dejared-mcp/releases/download/v${JAR_VERSION}/${JAR_NAME}`;

function log(msg) {
  process.stderr.write(`[dejared-mcp] ${msg}\n`);
}

/**
 * Resolves the Java binary path.
 * Honours the DEJARED_JAVA_PATH env var; falls back to `java` on PATH.
 */
function resolveJava() {
  const envJava = process.env.DEJARED_JAVA_PATH;
  if (envJava) {
    if (!fs.existsSync(envJava)) {
      log(`DEJARED_JAVA_PATH points to "${envJava}" but it does not exist.`);
      process.exit(1);
    }
    return envJava;
  }
  try {
    execFileSync("java", ["-version"], { stdio: "ignore" });
    return "java";
  } catch {
    log("Java not found. Set DEJARED_JAVA_PATH or install Java 21+.");
    process.exit(1);
  }
}

/**
 * Downloads a file over HTTPS, following up to {@link redirects} redirects.
 * Writes to a temporary file first, then atomically renames to {@link dest}
 * to avoid leaving a partial/corrupt file on disk.
 */
function download(url, dest, redirects = 5) {
  return new Promise((resolve, reject) => {
    if (redirects <= 0) return reject(new Error("Too many redirects"));

    https.get(url, (res) => {
      if (res.statusCode >= 300 && res.statusCode < 400 && res.headers.location) {
        res.resume();
        return resolve(download(res.headers.location, dest, redirects - 1));
      }

      if (res.statusCode !== 200) {
        res.resume();
        return reject(new Error(`HTTP ${res.statusCode} from ${url}`));
      }

      const tmp = dest + ".tmp";
      const file = fs.createWriteStream(tmp);
      res.pipe(file);
      file.on("finish", () => {
        file.close(() => {
          const stat = fs.statSync(tmp);
          if (stat.size === 0) {
            fs.unlinkSync(tmp);
            return reject(new Error("Downloaded file is empty"));
          }
          fs.renameSync(tmp, dest);
          resolve();
        });
      });
      file.on("error", (err) => {
        fs.unlink(tmp, () => {});
        reject(err);
      });
    }).on("error", reject);
  });
}

/** Ensures the JAR exists in cache, downloading it if missing or corrupted. */
async function ensureJar() {
  if (fs.existsSync(JAR_PATH)) {
    const stat = fs.statSync(JAR_PATH);
    if (stat.size > 0) return;
    log("Cached JAR is corrupted (0 bytes). Re-downloading...");
    fs.unlinkSync(JAR_PATH);
  }

  fs.mkdirSync(CACHE_DIR, { recursive: true });
  log(`Downloading dejared-mcp v${JAR_VERSION}...`);
  try {
    await download(DOWNLOAD_URL, JAR_PATH);
    log("Download complete.");
  } catch (err) {
    log(`Failed to download JAR: ${err.message}`);
    log(`URL: ${DOWNLOAD_URL}`);
    process.exit(1);
  }
}

/** Entry point – resolves Java, ensures the JAR is cached, then spawns it. */
async function run() {
  const javaPath = resolveJava();
  await ensureJar();

  const logDir = process.env.DEJARED_LOG_DIR || path.join(CACHE_DIR, "logs");
  fs.mkdirSync(logDir, { recursive: true });

  const jvmArgs = [`-Ddejared.log.dir=${logDir}`];

  if (process.env.DEJARED_LOG_LEVEL) {
    jvmArgs.push(`-Ddejared.log.level=${process.env.DEJARED_LOG_LEVEL}`);
  }

  const child = spawn(
    javaPath,
    [...jvmArgs, "-jar", JAR_PATH, "--spring.profiles.active=file-logging"],
    { stdio: "inherit" },
  );

  child.on("error", (err) => {
    log(`Failed to start Java: ${err.message}`);
    process.exit(1);
  });

  child.on("exit", (code) => {
    process.exit(code ?? 1);
  });
}

module.exports = { run };
