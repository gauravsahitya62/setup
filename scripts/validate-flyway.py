"""Fail if Flyway versions are missing, duplicated, or not a contiguous V1..Vn sequence."""
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[1] / "backend" / "src" / "main" / "resources" / "db" / "migration"
pattern = re.compile(r"^V(\d+)__.*\.sql$")
versions = []
for path in sorted(ROOT.glob("V*.sql")):
    match = pattern.match(path.name)
    if not match:
        print(f"Invalid migration filename: {path.name}", file=sys.stderr)
        sys.exit(1)
    versions.append(int(match.group(1)))

versions.sort()

if len(versions) != len(set(versions)):
    print(f"Duplicate Flyway versions: {versions}", file=sys.stderr)
    sys.exit(1)

expected = list(range(1, max(versions) + 1))
if versions != expected:
    print(f"Flyway versions must be contiguous starting at 1. Found {versions}, expected {expected}", file=sys.stderr)
    sys.exit(1)

print(f"Flyway migrations OK: {len(versions)} files (V1..V{max(versions)})")
