package com.vdavid003.sm64port;

public final class SaveDataCheck {
    private static void sign(byte[] data, int offset, int size, int magic) {
        int signature = offset + size - 4;
        data[signature] = (byte) (magic >> 8);
        data[signature + 1] = (byte) magic;
        int sum = 0;
        for (int i = offset; i < offset + size - 2; i++) sum = (sum + (data[i] & 0xff)) & 0xffff;
        data[signature + 2] = (byte) (sum >> 8);
        data[signature + 3] = (byte) sum;
    }

    public static void main(String[] args) throws Exception {
        byte[] data = new byte[512];
        for (int file = 0; file < 4; file++) sign(data, file * 112, 56, 0x4441);
        sign(data, 448, 32, 0x4849);
        if (!SaveData.valid(data)) throw new AssertionError("valid backup rejected");
        data[0]++;
        if (SaveData.valid(data)) throw new AssertionError("damaged save accepted");
        sign(data, 56, 56, 0x4441);
        if (!SaveData.valid(data)) throw new AssertionError("redundant slot rejected");
        if (SaveData.valid(new byte[511])) throw new AssertionError("wrong size accepted");
        if (args.length == 1 && !SaveData.valid(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(args[0]))))
            throw new AssertionError("device backup rejected");
    }
}
