#!/usr/bin/env python3
"""Print US ROM music offsets and bank IDs without including audio data."""

import json
from pathlib import Path


root = Path(__file__).resolve().parents[1] / "app/jni/src"
assets = json.loads((root / "assets.json").read_text())
sequences = json.loads((root / "sound/sequences.json").read_text())
banks = sorted(path.stem for path in (root / "sound/sound_banks").glob("*.json"))
for name, assigned in sequences.items():
    if name == "comment":
        continue
    index = int(name.split("_", 1)[0], 16)
    if isinstance(assigned, dict):
        assert "VERSION_US" in assigned["ifdef"]
        assigned = assigned["banks"]
    if index == 0:
        # The decompiled sound-player sequence is byte-identical to this ROM slice.
        offset, size = 8063360, 13452
    else:
        size, locations = assets[f"sound/sequences/us/{name}.m64"]
        offset = locations["us"][0]
    bank_ids = ",".join(str(banks.index(bank)) for bank in reversed(assigned))
    print(f"{index}\t{offset}\t{(size + 15) & ~15}\t{bank_ids}")
