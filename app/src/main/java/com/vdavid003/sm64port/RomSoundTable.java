package com.vdavid003.sm64port;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/** Widens the US ROM sample table for Android's 64-bit, little-endian audio loader. */
final class RomSoundTable {
    private static final int US_START = 5846368;
    private static final int US_SIZE = 2216704;

    static byte[] build(byte[] rom) {
        if (rom.length != 8 * 1024 * 1024) throw new IllegalArgumentException("Expected an 8 MB US ROM");
        return repack(rom, US_START, US_SIZE);
    }

    static byte[] repack(byte[] rom, int start, int size) {
        if (start < 0 || size < 16 || start > rom.length - size)
            throw new IllegalArgumentException("Invalid sample table bounds");
        ByteBuffer source = ByteBuffer.wrap(rom).order(ByteOrder.BIG_ENDIAN);
        int count = source.getShort(start + 2) & 65535;
        if ((source.getShort(start) & 65535) != 2 || count == 0 || count > 255)
            throw new IllegalArgumentException("Invalid sample table header");
        int oldData = align(4 + count * 8, 16);
        int newData = align(8 + count * 16, 16);
        if (oldData > size) throw new IllegalArgumentException("Truncated sample table");
        int extra = newData - oldData;
        byte[] result = new byte[align(size + extra + 1, 64)];
        ByteBuffer target = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
        target.putShort(0, (short) 2);
        target.putShort(2, (short) count);
        for (int i = 0; i < count; i++) {
            int offset = source.getInt(start + 4 + i * 8);
            int length = source.getInt(start + 8 + i * 8);
            if (offset < oldData || length < 0 || offset > size - length)
                throw new IllegalArgumentException("Invalid sample bank entry");
            target.putLong(8 + i * 16, offset + extra);
            target.putInt(16 + i * 16, length);
        }
        System.arraycopy(rom, start + oldData, result, newData, size - oldData);
        return result;
    }

    private static int align(int value, int multiple) {
        return (value + multiple - 1) & -multiple;
    }
}
