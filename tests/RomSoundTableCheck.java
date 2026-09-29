package com.vdavid003.sm64port;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.zip.ZipFile;

/** Run without arguments for a ROM-free check, or with ROM and base.zip for parity. */
public final class RomSoundTableCheck {
    public static void main(String[] args) throws Exception {
        byte[] fixture = new byte[80];
        ByteBuffer source = ByteBuffer.wrap(fixture).order(ByteOrder.BIG_ENDIAN);
        source.putShort(0, (short) 2);
        source.putShort(2, (short) 1);
        source.putInt(4, 16);
        source.putInt(8, 64);
        for (int i = 16; i < 80; i++) fixture[i] = (byte) i;
        byte[] packed = RomSoundTable.repack(fixture, 0, fixture.length);
        ByteBuffer output = ByteBuffer.wrap(packed).order(ByteOrder.LITTLE_ENDIAN);
        if (packed.length != 128 || output.getShort(0) != 2 || output.getShort(2) != 1 ||
                output.getLong(8) != 32 || output.getInt(16) != 64 ||
                !Arrays.equals(Arrays.copyOfRange(packed, 32, 96), Arrays.copyOfRange(fixture, 16, 80)))
            throw new AssertionError("Sample table fixture mismatch");
        if (args.length == 0) { System.out.println("ROM-free sample table check passed"); return; }
        if (args.length != 2) throw new IllegalArgumentException("ROM and base.zip required");

        byte[] rom = read(new FileInputStream(args[0]));
        ZipFile zip = new ZipFile(args[1]);
        try {
            if (!Arrays.equals(RomSoundTable.build(rom),
                    read(zip.getInputStream(zip.getEntry("sound/sound_data.tbl.le.64")))))
                throw new AssertionError("Rebuilt sample table differs from private base.zip");
        } finally { zip.close(); }
        System.out.println("US ROM sample table matches private base.zip byte-for-byte");
    }

    private static byte[] read(InputStream input) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            byte[] buffer = new byte[65536];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
        } finally { input.close(); }
        return output.toByteArray();
    }
}
