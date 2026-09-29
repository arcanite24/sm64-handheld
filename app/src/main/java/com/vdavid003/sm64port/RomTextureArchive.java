package com.vdavid003.sm64port;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.zip.CRC32;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Adds ordinary US ROM textures to a private base.zip being built on-device. */
final class RomTextureArchive {
    private static final Charset UTF8 = Charset.forName("UTF-8");

    static int append(byte[] rom, InputStream manifest, ZipOutputStream zip) throws Exception {
        RomImageDecoder decoder = new RomImageDecoder(rom);
        BufferedReader lines = new BufferedReader(new InputStreamReader(manifest, UTF8));
        int count = 0;
        String line;
        while ((line = lines.readLine()) != null) {
            String[] fields = line.split("\t");
            if (fields.length != 7) throw new IllegalArgumentException("Invalid texture map");
            String path = fields[0];
            if (!(path.startsWith("actors/") || path.startsWith("levels/") || path.startsWith("textures/")) ||
                    path.contains("..") || !path.endsWith(".png"))
                throw new IllegalArgumentException("Invalid texture path");
            int width = Integer.parseInt(fields[2]), height = Integer.parseInt(fields[3]);
            byte[] rgba = decoder.rgba(Integer.parseInt(fields[4]), Integer.parseInt(fields[5]),
                    Integer.parseInt(fields[6]), fields[1], width, height);
            zip.putNextEntry(new ZipEntry("gfx/" + path));
            writePng(zip, rgba, width, height);
            zip.closeEntry();
            count++;
        }
        return count;
    }

    private static void writePng(ZipOutputStream zip, byte[] rgba, int width, int height) throws Exception {
        DataOutputStream data = new DataOutputStream(zip);
        data.write(new byte[] {(byte) 137, 80, 78, 71, 13, 10, 26, 10});
        ByteArrayOutputStream header = new ByteArrayOutputStream(13);
        DataOutputStream dimensions = new DataOutputStream(header);
        dimensions.writeInt(width);
        dimensions.writeInt(height);
        dimensions.write(new byte[] {8, 6, 0, 0, 0});
        chunk(data, "IHDR", header.toByteArray());

        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        DeflaterOutputStream deflate = new DeflaterOutputStream(compressed);
        for (int row = 0; row < height; row++) {
            deflate.write(0);
            deflate.write(rgba, row * width * 4, width * 4);
        }
        deflate.close();
        chunk(data, "IDAT", compressed.toByteArray());
        chunk(data, "IEND", new byte[0]);
    }

    private static void chunk(DataOutputStream output, String name, byte[] payload) throws Exception {
        byte[] type = name.getBytes(UTF8);
        CRC32 crc = new CRC32();
        crc.update(type);
        crc.update(payload);
        output.writeInt(payload.length);
        output.write(type);
        output.write(payload);
        output.writeInt((int) crc.getValue());
    }
}
