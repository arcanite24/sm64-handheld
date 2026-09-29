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

for name in ("bbh", "bidw", "bitfs", "bits", "ccm", "cloud_floor", "clouds", "ssl", "water", "wdw"):
    size, positions = assets[f"textures/skyboxes/{name}.png"]
    segment, offset = positions["us"]
    assert (size - 320) % 2048 == 0
    # The last bitfs tile duplicates another one and skyconv omits it.
    count = (size - 320) // 2048 - (name == "bitfs")
    for tile in range(count):
        path = f"textures/skybox_tiles/{name}.{tile}.rgba16.png"
        print("\t".join(map(str, (path, "rgba16", 32, 32, segment, offset + tile * 2048, 2048))))

size, positions = assets["levels/ending/cake.png"]
segment, offset = positions["us"]
assert size % 3200 == 0
for tile in range(size // 3200):
    path = f"textures/skybox_tiles/cake.{tile}.rgba16.png"
    print("\t".join(map(str, (path, "rgba16", 80, 20, segment, offset + tile * 3200, 3200))))
