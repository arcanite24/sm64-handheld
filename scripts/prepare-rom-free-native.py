#!/usr/bin/env python3
"""Generate name-only Android native inputs without reading a game ROM."""

from collections import defaultdict
from pathlib import Path
import re
import subprocess
import sys


root = Path(__file__).resolve().parents[1]
engine = root / "app/jni/src"
build = engine / "build/us_pc"
manifest = root / "app/src/main/assets/us-textures.tsv"


def write(path, content):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content, encoding="ascii")


def name_array(path):
    data = path.encode("ascii") + b"\0"
    return ",".join(f"0x{value:X}" for value in data) + ",\n"


def converter(script, output, *args):
    output.parent.mkdir(parents=True, exist_ok=True)
    with output.open("w", encoding="ascii") as target:
        subprocess.run([sys.executable, f"tools/{script}", *args], cwd=engine,
                       stdout=target, check=True)


tiles = defaultdict(set)
for line in manifest.read_text(encoding="ascii").splitlines():
    path = line.split("\t", 1)[0]
    match = re.fullmatch(r"textures/skybox_tiles/([a-z_]+)\.(\d+)\.rgba16\.png", path)
    if match:
        tiles[match.group(1)].add(int(match.group(2)))
    else:
        if not path.endswith(".png") or not path.startswith(("actors/", "levels/", "textures/")):
            raise ValueError(f"Unexpected texture path: {path}")
        name = path[:-4]
        write(build / f"{name}.inc.c", name_array(name))

for name, indices in tiles.items():
    if indices != set(range(len(indices))):
        raise ValueError(f"Non-sequential tile map: {name}")
    if name == "cake":
        write(build / "levels/ending/cake.inc.c", "".join(
            f'ALIGNED8 static const u8 cake_end_texture_{i}[] = '
            f'"textures/skybox_tiles/cake.{i}.rgba16";\n\n'
            for i in range(len(indices))))
        continue
    count = len(indices)
    content = ['#include "sm64.h"\n\n#include "make_const_nonconst.h"\n\n']
    content.extend(
        f'ALIGNED8 static const u8 {name}_skybox_texture_{i:05X}[] = '
        f'"textures/skybox_tiles/{name}.{i}.rgba16";\n\n'
        for i in range(count))
    content.append(f"const u8 *const {name}_skybox_ptrlist[] = {{\n")
    content.extend(
        f"{name}_skybox_texture_{min(row * 8 + col % 8, count - 1):05X},\n"
        for row in range(8) for col in range(10))
    content.append("};\n\n")
    write(build / f"bin/{name}_skybox.c", "".join(content))

for part in ("bank_sets", "sequences.bin", "sound_data.ctl", "sound_data.tbl"):
    for endian in ("be", "le"):
        for bits in (32, 64):
            name = f"sound/{part}.{endian}.{bits}"
            write(build / f"{name}.inc.c", name_array(name))

converter("mario_anims_converter.py", build / "assets/mario_anim_data.c")
converter("demo_data_converter.py", build / "assets/demo_data.c",
          "assets/demo_data.json", "-DVERSION_US")
print("Prepared name-only native inputs from checked-in metadata")
