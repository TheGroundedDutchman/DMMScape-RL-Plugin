#!/usr/bin/env bash
set -euo pipefail

root=$(git rev-parse --show-toplevel)
cd "$root"

violations=0

if rg -n "import .*\\*;" src/main/java; then
	echo "Wildcard imports detected. Replace with explicit imports."
	violations=1
fi

if rg -n -U "import .*;\\n\\nimport" src/main/java; then
	echo "Import blocks contain blank lines. Use a single import block."
	violations=1
fi

if rg -n -P "^[ ]+(?!\\*)\\S" src/main/java; then
	echo "Space-indented lines detected in Java sources. Use tabs."
	violations=1
fi

missing_headers=()
while IFS= read -r file; do
	if ! rg -q "Copyright \(c\) 2026, DMMScape" "$file"; then
		missing_headers+=("$file")
	fi
done < <(rg --files -g "*.java" src/main/java)

if [ "${#missing_headers[@]}" -gt 0 ]; then
	echo "Missing RuneLite header in:"
	printf ' - %s\n' "${missing_headers[@]}"
	violations=1
fi

if [ "$violations" -ne 0 ]; then
	echo "Conventions check failed."
	exit 1
fi

echo "Conventions check passed."
