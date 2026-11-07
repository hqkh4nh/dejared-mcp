"use strict";

const assert = require("node:assert/strict");
const fs = require("node:fs");
const os = require("node:os");
const path = require("node:path");
const { spawnSync } = require("node:child_process");
const { test } = require("node:test");

test("detects drift without writes, syncs all fields, and remains idempotent", (t) => {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), "dejared-version-test-"));
  t.after(() => fs.rmSync(root, { recursive: true, force: true }));
  const files = {
    "package.json": { version: "1.2.3-beta.1" },
    "server.json": { version: "old", packages: [{ version: "old", identifier: "keep" }] },
    ".claude-plugin/plugin.json": { version: "old" },
    ".claude-plugin/marketplace.json": { plugins: [{ version: "old" }] },
    ".codex-plugin/plugin.json": { version: "old" },
    ".agents/plugins/marketplace.json": { plugins: [{ version: "old" }] },
  };
  for (const [file, data] of Object.entries(files)) {
    const target = path.join(root, file);
    fs.mkdirSync(path.dirname(target), { recursive: true });
    fs.writeFileSync(target, JSON.stringify(data));
  }
  fs.mkdirSync(path.join(root, "scripts"));
  const script = path.join(root, "scripts/sync-version.cjs");
  fs.copyFileSync(path.join(__dirname, "sync-version.cjs"), script);
  const run = (...args) => spawnSync(process.execPath, [script, ...args], { encoding: "utf8" });
  assert.equal(run("--check").status, 1);
  for (const [file, data] of Object.entries(files)) {
    assert.equal(fs.readFileSync(path.join(root, file), "utf8"), JSON.stringify(data));
  }
  assert.equal(run().status, 0);
  assert.equal(run("--check").status, 0);
  const server = JSON.parse(fs.readFileSync(path.join(root, "server.json"), "utf8"));
  assert.equal(server.version, "1.2.3-beta.1");
  assert.equal(server.packages[0].version, server.version);
  assert.equal(server.packages[0].identifier, "keep");
  const before = Object.keys(files).map((file) => fs.readFileSync(path.join(root, file), "utf8"));
  assert.equal(run().status, 0);
  assert.deepEqual(Object.keys(files).map((file) => fs.readFileSync(path.join(root, file), "utf8")), before);
  assert.notEqual(run("--unknown").status, 0);
});
