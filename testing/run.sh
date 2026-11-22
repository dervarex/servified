#!/usr/bin/env bash

cd "$(dirname "$0")"
shopt -s nullglob

if ! command -v fzf &>/dev/null; then
    echo "fzf is not installed"
    exit 1
fi

while true; do
    entries=()

    for file in *.java; do
        entries+=("$file")
    done

    for dir in */; do
        sources=("$dir"*.java)
        ((${#sources[@]})) && entries+=("${dir%/}")
    done

    choice=$(
        for entry in "${entries[@]}"; do
            if [[ -d "$entry" ]]; then
                printf '%s\t%s\n' "$entry" "[TEST] $entry/"
            else
                printf '%s\t%s\n' "$entry" "[JAVA] $entry"
            fi
        done |
        fzf \
            --prompt="Select program > " \
            --header="Java Test Runner | Enter to run | Esc to exit" \
            --delimiter='\t' \
            --with-nth=2.. \
            --accept-nth=1 \
            --preview='if [ -d {1} ]; then for f in {1}/*.java; do echo "=== $f ==="; sed -n "1,100p" "$f"; done; else sed -n "1,100p" {1}; fi' \
            --preview-window=right:50%
    ) || exit 0

    clear

    if [[ -d "$choice" ]]; then
        echo "Compiling $choice..."

        sources=("$choice"/*.java)

        if javac "${sources[@]}"; then
            echo "Running $choice..."
            java -cp "$choice" "$(basename "$choice")"
        fi
    else
        echo "Compiling $choice..."

        if javac "$choice"; then
            echo "Running $choice..."
            java -cp . "$(basename "$choice" .java)"
        fi
    fi

    echo
    read -rp "Press Enter to return to selection..."
done
