package com.vdavid003.sm64port;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Builds the private ARM64 game archive from a verified user-owned ROM. */
final class RomBaseArchive {
    interface ManifestSource { InputStream open(String name) throws Exception; }

    static void write(byte[] rom, ManifestSource assets, OutputStream output) throws Exception {
        try (ZipOutputStream zip = new ZipOutputStream(output);
             InputStream textures = assets.open("us-textures.tsv");
             InputStream sequences = assets.open("us-sound-sequences.tsv")) {
            if (RomTextureArchive.append(rom, textures, zip) != 1893)
                throw new IllegalArgumentException("Incomplete US texture map");
            byte[][] music = RomSoundSequences.build(rom, sequences);
            add(zip, "sound/sequences.bin.le.64", music[0]);
            add(zip, "sound/bank_sets.le.64", music[1]);
            add(zip, "sound/sound_data.ctl.le.64", RomSoundControl.build(rom));
            add(zip, "sound/sound_data.tbl.le.64", RomSoundTable.build(rom));
        }
    }

    private static void add(ZipOutputStream zip, String path, byte[] data) throws Exception {
        zip.putNextEntry(new ZipEntry(path));
        zip.write(data);
        zip.closeEntry();
    }
}
