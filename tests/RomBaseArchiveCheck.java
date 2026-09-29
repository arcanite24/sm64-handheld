package com.vdavid003.sm64port;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

public final class RomBaseArchiveCheck {
    public static void main(String[] args) throws Exception {
        if (args.length != 3 && args.length != 4)
            throw new IllegalArgumentException("US ROM, private base.zip, asset directory, optional output path required");
        byte[] rom = Files.readAllBytes(Path.of(args[0]));
        Path assets = Path.of(args[2]);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        RomBaseArchive.write(rom, name -> Files.newInputStream(assets.resolve(name)), output);
        if (args.length == 4) Files.write(Path.of(args[3]), output.toByteArray());
        int graphics = 0, sound = 0;
        try (ZipFile expected = new ZipFile(args[1]);
             ZipInputStream generated = new ZipInputStream(new ByteArrayInputStream(output.toByteArray()))) {
            ZipEntry entry;
            while ((entry = generated.getNextEntry()) != null) {
                if (entry.getName().startsWith("gfx/")) {
                    graphics++;
                    if (expected.getEntry(entry.getName()) == null) throw new AssertionError(entry.getName());
                } else if (entry.getName().startsWith("sound/")) {
                    sound++;
                    try (InputStream original = expected.getInputStream(expected.getEntry(entry.getName()))) {
                        if (!Arrays.equals(generated.readAllBytes(), original.readAllBytes()))
                            throw new AssertionError(entry.getName());
                    }
                } else throw new AssertionError(entry.getName());
            }
        }
        if (graphics != 1893 || sound != 4) throw new AssertionError(graphics + " graphics, " + sound + " sound");
        System.out.println("On-device archive: " + graphics + " graphics + " + sound + " byte-matched sound files");
    }
}
