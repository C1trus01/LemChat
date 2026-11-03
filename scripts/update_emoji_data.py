#!/usr/bin/env python3
"""
Downloads emoji data from iamcal/emoji-data and generates a stripped-down
JSON resource for DankChat's emoji shortcode suggestions.

Output: app/src/main/res/raw/emoji_data.json
Source: https://github.com/iamcal/emoji-data (Emoji 17.0 / Unicode 17.0)

Usage: python3 scripts/update_emoji_data.py
"""

import json
import os
import urllib.request

# Using Nerixyz fork until upstream PR is merged: https://github.com/iamcal/emoji-data/pull/255
# Switch back to iamcal/emoji-data/master once merged.
EMOJI_DATA_URL = "https://raw.githubusercontent.com/Nerixyz/emoji-data/feat/17-0/emoji.json"
OUTPUT_PATH = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "res", "raw", "emoji_data.json")


def main():
    print(f"Downloading emoji data from {EMOJI_DATA_URL}...")
    with urllib.request.urlopen(EMOJI_DATA_URL) as response:
        data = json.loads(response.read())

    print(f"Loaded {len(data)} emoji entries")

    entries = []
    for emoji in data:
        category = emoji.get("category", "")
        if category == "Component":
            continue

        codes = emoji["unified"].split("-")
        unicode_char = "".join(chr(int(c, 16)) for c in codes)

        for shortcode in emoji.get("short_names", []):
            entries.append({"code": shortcode, "unicode": unicode_char})

    entries.sort(key=lambda e: e["code"])

    output_path = os.path.normpath(OUTPUT_PATH)
    with open(output_path, "w", encoding="utf-8") as f:
        json.dump(entries, f, ensure_ascii=False, separators=(",", ":"))

    size_kb = os.path.getsize(output_path) / 1024
    print(f"Written {len(entries)} entries to {output_path} ({size_kb:.1f} KB)")


if __name__ == "__main__":
    main()
