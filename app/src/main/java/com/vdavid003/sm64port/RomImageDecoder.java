package com.vdavid003.sm64port;

import java.util.HashMap;
import java.util.Map;

/** Decodes the ordinary N64 textures listed in us-textures.tsv from a verified US ROM. */
final class RomImageDecoder {
    private final byte[] rom;
    private final Map<Integer, byte[]> segments = new HashMap<Integer, byte[]>();

    RomImageDecoder(byte[] rom) {
        if (rom.length != 8 * 1024 * 1024) throw new IllegalArgumentException("Expected an 8 MB US ROM");
        this.rom = rom;
    }

    byte[] rgba(int segment, int offset, int size, String format, int width, int height) {
        byte[] source = segment < 0 ? rom : segment(segment);
        int pixels = width * height;
        int bits;
        if ("rgba16".equals(format) || "ia16".equals(format)) bits = 16;
        else if ("ia8".equals(format)) bits = 8;
        else if ("ia4".equals(format)) bits = 4;
        else if ("ia1".equals(format)) bits = 1;
        else throw new IllegalArgumentException("Unsupported texture format: " + format);
        if (width <= 0 || height <= 0 || pixels > 1024 * 1024 ||
                size != (pixels * bits + 7) / 8 || offset < 0 || offset > source.length - size)
            throw new IllegalArgumentException("Invalid texture bounds");

        byte[] result = new byte[pixels * 4];
        for (int pixel = 0; pixel < pixels; pixel++) {
            int r, g, b, a;
            int index = offset + pixel * bits / 8;
            if ("rgba16".equals(format)) {
                int value = (source[index] & 255) << 8 | source[index + 1] & 255;
                r = ((value >> 11) & 31) * 255 / 31;
                g = ((value >> 6) & 31) * 255 / 31;
                b = ((value >> 1) & 31) * 255 / 31;
                a = (value & 1) != 0 ? 255 : 0;
            } else {
                int intensity;
                if ("ia16".equals(format)) {
                    intensity = source[index] & 255;
                    a = source[index + 1] & 255;
                } else if ("ia8".equals(format)) {
                    int value = source[index] & 255;
                    intensity = (value >> 4) * 17;
                    a = (value & 15) * 17;
                } else if ("ia4".equals(format)) {
                    int value = pixel % 2 == 0 ? (source[index] & 255) >> 4 : source[index] & 15;
                    intensity = (value >> 1) * 36;
                    a = (value & 1) != 0 ? 255 : 0;
                } else {
                    intensity = (source[index] & (1 << (7 - pixel % 8))) != 0 ? 255 : 0;
                    a = intensity;
                }
                r = g = b = intensity;
            }
            int target = pixel * 4;
            result[target] = (byte) r;
            result[target + 1] = (byte) g;
            result[target + 2] = (byte) b;
            result[target + 3] = (byte) a;
        }
        return result;
    }

    private byte[] segment(int start) {
        byte[] cached = segments.get(start);
        if (cached != null) return cached;
        if (start < 0 || start > rom.length - 16 || be32(rom, start) != 0x4d494f30)
            throw new IllegalArgumentException("Invalid MIO0 segment");
        int length = be32(rom, start + 4);
        int compressed = start + be32(rom, start + 8);
        int raw = start + be32(rom, start + 12);
        int control = start + 16;
        if (length <= 0 || length > rom.length || compressed < start || raw < start)
            throw new IllegalArgumentException("Invalid MIO0 size");
        byte[] output = new byte[length];
        int destination = 0, bits = 0, remaining = 0;
        while (destination < length) {
            if (remaining == 0) {
                if (control > rom.length - 4) throw new IllegalArgumentException("Truncated MIO0 control");
                bits = be32(rom, control);
                control += 4;
                remaining = 32;
            }
            if (bits < 0) {
                if (raw >= rom.length) throw new IllegalArgumentException("Truncated MIO0 raw data");
                output[destination++] = rom[raw++];
            } else {
                if (compressed > rom.length - 2) throw new IllegalArgumentException("Truncated MIO0 compressed data");
                int pair = (rom[compressed] & 255) << 8 | rom[compressed + 1] & 255;
                compressed += 2;
                int count = (pair >> 12) + 3;
                int distance = (pair & 4095) + 1;
                if (distance > destination || count > length - destination)
                    throw new IllegalArgumentException("Invalid MIO0 back-reference");
                while (count-- > 0) {
                    output[destination] = output[destination - distance];
                    destination++;
                }
            }
            bits <<= 1;
            remaining--;
        }
        segments.put(start, output);
        return output;
    }

    private static int be32(byte[] data, int offset) {
        return (data[offset] & 255) << 24 | (data[offset + 1] & 255) << 16 |
                (data[offset + 2] & 255) << 8 | data[offset + 3] & 255;
    }
}
