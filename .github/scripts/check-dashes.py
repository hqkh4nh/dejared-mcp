import subprocess, sys
SKIP = {"java-mcp/gradlew", "java-mcp/gradlew.bat"}
BAD = {"\u2014": "em dash", "\u2013": "en dash"}
files = [f for f in subprocess.run(["git","ls-files"],capture_output=True,text=True).stdout.split("\n") if f.strip()]
hits = []
for f in files:
    if f in SKIP: continue
    try: lines = open(f, encoding="utf-8").read().split("\n")
    except Exception: continue
    for n, line in enumerate(lines, 1):
        for ch, name in BAD.items():
            if ch in line:
                hits.append(f"{f}:{n}: {name} found")
for h in hits: print(f"::error::{h}")
if hits:
    print(f"{len(hits)} line(s) use a typographic dash. Use ASCII punctuation instead.")
    sys.exit(1)
print("No em or en dashes found.")
