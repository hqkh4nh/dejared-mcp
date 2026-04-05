"use strict";

const { spawn, execFileSync } = require("node:child_process");
const fs = require("node:fs");
const https = require("node:https");
const path = require("node:path");
const os = require("node:os");

const PACKAGE = require(path.join(__dirname, "..", "package.json"));
const JAR_VERSION = PACKAGE.version;
const JAR_NAME = `dejared-mcp-${JAR_VERSION}.jar`;
const CACHE_DIR = path.join(os.homedir(), ".dejared-mcp");
const JAR_PATH = path.join(CACHE_DIR, JAR_NAME);
const DOWNLOAD_URL = `https://github.com/HuynhKhanh1402/dejared-mcp/releases/download/v${JAR_VERSION}/${JAR_NAME}`;

function log(msg) {
  process.stderr.write(`[dejared-mcp] ${msg}\n`);
}

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

async function run() {
  const javaPath = resolveJava();
  await ensureJar();

  const child = spawn(javaPath, ["-jar", JAR_PATH], {
    stdio: "inherit",
  });

  child.on("error", (err) => {
    log(`Failed to start Java: ${err.message}`);
    process.exit(1);
  });

  child.on("exit", (code) => {
    process.exit(code ?? 1);
  });
}

module.exports = { run };
