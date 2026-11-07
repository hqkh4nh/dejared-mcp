"use strict";

const fs = require("node:fs");
const path = require("node:path");
const root = path.resolve(__dirname, "..");
const args = process.argv.slice(2);
if (args.some((arg) => arg !== "--check")) {
  throw new Error("Usage: node scripts/sync-version.cjs [--check]");
}
const check = args.includes("--check");
const version = JSON.parse(fs.readFileSync(path.join(root, "package.json"), "utf8")).version;
const targets = [
  ["server.json", [["version"], ["packages", 0, "version"]]],
  [".claude-plugin/plugin.json", [["version"]]],
  [".claude-plugin/marketplace.json", [["plugins", 0, "version"]]],
  [".codex-plugin/plugin.json", [["version"]]],
  [".agents/plugins/marketplace.json", [["plugins", 0, "version"]]],
];

// Validate all inputs before changing any manifest.
const updates = targets.map(([file, fields]) => {
  const filename = path.join(root, file);
  const original = fs.readFileSync(filename, "utf8");
  const data = JSON.parse(original);
  let changed = false;
  for (const keys of fields) {
    const parent = keys.slice(0, -1).reduce((value, key) => value[key], data);
    const key = keys.at(-1);
    if (typeof parent[key] !== "string") throw new Error(`Missing version in ${file}`);
    if (parent[key] !== version) {
      changed = true;
      parent[key] = version;
    }
  }
  const newline = original.includes("\r\n") ? "\r\n" : "\n";
  return { file, filename, changed, contents: (JSON.stringify(data, null, 2) + "\n").replace(/\n/g, newline) };
});

for (const update of updates.filter((entry) => entry.changed)) {
  if (check) {
    console.error(`${update.file}: version differs from package.json (${version})`);
    process.exitCode = 1;
  } else {
    fs.writeFileSync(update.filename, update.contents);
    console.log(`Updated ${update.file} to ${version}`);
  }
}
if (!process.exitCode) console.log(`Versions match: ${version}`);
