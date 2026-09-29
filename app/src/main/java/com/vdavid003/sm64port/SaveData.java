package com.vdavid003.sm64port;

final class SaveData {
    private SaveData() {}

    static boolean valid(byte[] data) {
        if (data == null || data.length != 512) return false;
        for (int file = 0; file < 4; file++) {
            int offset = file * 112;
            if (!validBlock(data, offset, 56, 0x4441) &&
                    !validBlock(data, offset + 56, 56, 0x4441)) return false;
        }
        return validBlock(data, 448, 32, 0x4849) ||
                validBlock(data, 480, 32, 0x4849);
    }

    private static boolean validBlock(byte[] data, int offset, int size, int magic) {
        int signature = offset + size - 4;
        if (word(data, signature) != magic) return false;
        int sum = 0;
        for (int i = offset; i < offset + size - 2; i++)
            sum = (sum + (data[i] & 0xff)) & 0xffff;
        return word(data, signature + 2) == sum;
    }

    private static int word(byte[] data, int offset) {
        return ((data[offset] & 0xff) << 8) | (data[offset + 1] & 0xff);
    }
}
