package com.vdavid003.sm64port;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.zip.ZipFile;

public final class RomSoundControlCheck {
    public static void main(String[] args) throws Exception {
        fixture();
        if (args.length == 0) return;
        if (args.length != 2) throw new IllegalArgumentException("US ROM and private base.zip required");
        byte[] actual = RomSoundControl.build(Files.readAllBytes(Path.of(args[0])));
        try (ZipFile zip = new ZipFile(args[1])) {
            byte[] expected = zip.getInputStream(zip.getEntry("sound/sound_data.ctl.le.64")).readAllBytes();
            if (!Arrays.equals(actual, expected)) throw new AssertionError("Sound control differs");
            System.out.println("All 38 control banks match the private build");
        }
    }

    private static void fixture() {
        byte[] source = new byte[320 + 192 + 37 * 32];
        ByteBuffer be = ByteBuffer.wrap(source).order(ByteOrder.BIG_ENDIAN);
        be.putShort(0, (short) 1);
        be.putShort(2, (short) 38);
        for (int i = 0; i < 38; i++) {
            be.putInt(4 + i * 8, 320 + 192 + (i - 1) * 32);
            be.putInt(8 + i * 8, i == 0 ? 192 : 32);
        }
        be.putInt(4, 320);
        be.putInt(320, 1);
        be.putInt(332, 0x19960214);
        be.putInt(340, 16);
        be.put(320 + 16 + 16 + 2, (byte) 127);
        be.putInt(320 + 16 + 16 + 4, 48);
        be.putInt(320 + 16 + 16 + 16, 64);
        be.putFloat(320 + 16 + 16 + 20, 1.0f);
        be.putShort(320 + 16 + 48, (short) 65535);
        be.putInt(320 + 16 + 64 + 8, 84);
        be.putInt(320 + 16 + 64 + 12, 100);
        be.putInt(320 + 16 + 64 + 16, 9);
        be.putInt(320 + 16 + 84 + 4, 17);
        be.putInt(320 + 16 + 100, 2);
        be.putInt(320 + 16 + 104, 2);
        byte[] actual = RomSoundControl.repack(source, 0, source.length);
        ByteBuffer le = ByteBuffer.wrap(actual).order(ByteOrder.LITTLE_ENDIAN);
        if (le.getShort(0) != 1 || le.getShort(2) != 38 || le.getLong(8) != 624)
            throw new AssertionError("Bad control table");
        if (le.getInt(624) != 1 || le.getInt(636) != 0x19960214)
            throw new AssertionError("Bad control bank");
        if (le.getInt(16) <= 0) throw new AssertionError("Empty bank");
        System.out.println("ROM-free control fixture passed");
    }
}
