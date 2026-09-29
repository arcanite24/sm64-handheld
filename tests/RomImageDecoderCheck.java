package com.vdavid003.sm64port;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import javax.imageio.ImageIO;

/** Run without arguments for a ROM-free check, or with ROM, base.zip, texture map for parity. */
public final class RomImageDecoderCheck {
    public static void main(String[] args) throws Exception {
        byte[] fixture = new byte[8 * 1024 * 1024];
        fixture[64] = 'M'; fixture[65] = 'I'; fixture[66] = 'O'; fixture[67] = '0';
        fixture[71] = 2; fixture[75] = 20; fixture[79] = 20;
        fixture[80] = (byte) 0xc0;
        fixture[84] = (byte) 0xf8; fixture[85] = 1;
        RomImageDecoder decoder = new RomImageDecoder(fixture);
        equal(decoder.rgba(64, 0, 2, "rgba16", 1, 1), 255, 0, 0, 255);
        fixture[0] = (byte) 0xf1;
        equal(decoder.rgba(-1, 0, 1, "ia4", 2, 1), 252, 252, 252, 255, 0, 0, 0, 255);
        fixture[0] = (byte) 0x80;
        equal(decoder.rgba(-1, 0, 1, "ia1", 2, 1), 255, 255, 255, 255, 0, 0, 0, 0);
        if (args.length == 0) { System.out.println("ROM-free decoder check passed"); return; }
        if (args.length != 3) throw new IllegalArgumentException("ROM, base.zip, texture map required");

        byte[] rom = read(new File(args[0]));
        decoder = new RomImageDecoder(rom);
        ZipFile zip = new ZipFile(args[1]);
        File generated = File.createTempFile("rom-textures-", ".zip");
        InputStream textureMap = new FileInputStream(args[2]);
        ZipOutputStream output = new ZipOutputStream(new FileOutputStream(generated));
        int written;
        try { written = RomTextureArchive.append(rom, textureMap, output); }
        finally { textureMap.close(); output.close(); }
        ZipFile rebuilt = new ZipFile(generated);
        BufferedReader map = new BufferedReader(new FileReader(args[2]));
        int checked = 0;
        try {
            String line;
            while ((line = map.readLine()) != null) {
                String[] item = line.split("\t");
                if (item.length != 7) throw new AssertionError("Invalid texture map entry");
                int width = Integer.parseInt(item[2]), height = Integer.parseInt(item[3]);
                byte[] rgba = decoder.rgba(Integer.parseInt(item[4]), Integer.parseInt(item[5]),
                        Integer.parseInt(item[6]), item[1], width, height);
                ZipEntry entry = zip.getEntry("gfx/" + item[0]);
                if (entry == null) throw new AssertionError("Missing " + item[0]);
                InputStream input = zip.getInputStream(entry);
                BufferedImage expected;
                try { expected = ImageIO.read(input); }
                finally { input.close(); }
                if (expected.getWidth() != width || expected.getHeight() != height)
                    throw new AssertionError("Wrong size: " + item[0]);
                ZipEntry rebuiltEntry = rebuilt.getEntry("gfx/" + item[0]);
                if (rebuiltEntry == null) throw new AssertionError("Missing rebuilt " + item[0]);
                InputStream rebuiltInput = rebuilt.getInputStream(rebuiltEntry);
                BufferedImage rebuiltImage;
                try { rebuiltImage = ImageIO.read(rebuiltInput); }
                finally { rebuiltInput.close(); }
                if (rebuiltImage.getWidth() != width || rebuiltImage.getHeight() != height)
                    throw new AssertionError("Wrong rebuilt size: " + item[0]);
                Raster raster = expected.getRaster();
                Raster rebuiltRaster = rebuiltImage.getRaster();
                int bands = raster.getNumBands();
                if (bands != 2 && bands != 4) throw new AssertionError("Unexpected PNG channels: " + item[0]);
                int[] samples = new int[bands];
                int[] rebuiltSamples = new int[4];
                for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
                    int pixel = (y * width + x) * 4;
                    raster.getPixel(x, y, samples);
                    rebuiltRaster.getPixel(x, y, rebuiltSamples);
                    for (int channel = 0; channel < 4; channel++)
                        if ((rgba[pixel + channel] & 255) != rebuiltSamples[channel])
                            throw new AssertionError("Rebuilt pixel mismatch: " + item[0] + " at " + x + "," + y);
                    if (bands == 2) {
                        if ((rgba[pixel] & 255) != samples[0] || (rgba[pixel + 3] & 255) != samples[1])
                            throw new AssertionError("Pixel mismatch: " + item[0] + " at " + x + "," + y);
                    } else for (int channel = 0; channel < 4; channel++)
                        if ((rgba[pixel + channel] & 255) != samples[channel])
                            throw new AssertionError("Pixel mismatch: " + item[0] + " at " + x + "," + y);
                }
                checked++;
            }
        } finally { map.close(); zip.close(); rebuilt.close(); generated.delete(); }
        if (written != checked) throw new AssertionError("Incomplete rebuilt texture archive");
        System.out.println(checked + " ROM textures match private base.zip pixels");
    }

    private static void equal(byte[] actual, int... expected) {
        byte[] bytes = new byte[expected.length];
        for (int i = 0; i < expected.length; i++) bytes[i] = (byte) expected[i];
        if (!Arrays.equals(actual, bytes)) throw new AssertionError("Decoder fixture mismatch");
    }

    private static byte[] read(File file) throws Exception {
        byte[] bytes = new byte[(int) file.length()];
        InputStream input = new FileInputStream(file);
        try {
            int position = 0, count;
            while (position < bytes.length && (count = input.read(bytes, position, bytes.length - position)) > 0)
                position += count;
            if (position != bytes.length) throw new IllegalArgumentException("Incomplete ROM");
        } finally { input.close(); }
        return bytes;
    }
}
