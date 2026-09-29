#!/usr/bin/env python3
"""Print metadata for US ROM textures; no image or ROM data is included."""

import json
from pathlib import Path


root = Path(__file__).resolve().parents[1]
assets = json.loads((root / "app/jni/src/assets.json").read_text())
formats = {"rgba16", "ia1", "ia4", "ia8", "ia16"}
for path, data in sorted(assets.items()):
    if not path.endswith(".png") or "us" not in data[-1]:
        continue
    image_format = path.split(".")[-2]
    if image_format not in formats:
        continue
    width, height, size, positions = data
    location = positions["us"]
    segment, offset = (-1, location[0]) if len(location) == 1 else location
    print("\t".join(map(str, (path, image_format, width, height, segment, offset, size))))
