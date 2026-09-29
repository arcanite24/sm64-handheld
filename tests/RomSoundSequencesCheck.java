package com.vdavid003.sm64port;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.zip.ZipFile;

/** Run without arguments for a ROM-free check, or with ROM, base.zip, sequence map for parity. */
public final class RomSoundSequencesCheck {
    public static void main(String[] args) throws Exception {
        byte[] fixture = new byte[8 * 1024 * 1024];
        for (int i = 0; i < 32; i++) fixture[64 + i] = (byte) (i + 1);
        byte[][] files = RomSoundSequences.build(fixture, new ByteArrayInputStream(
                "0\t64\t16\t2,1\n1\t80\t16\t3\n".getBytes("UTF-8")));
        ByteBuffer header = ByteBuffer.wrap(files[0]).order(ByteOrder.LITTLE_ENDIAN);
        if (files[0].length != 128 || header.getShort(0) != 3 || header.getShort(2) != 2 ||
                header.getLong(8) != 48 || header.getInt(16) != 16 ||
                header.getLong(24) != 64 || header.getInt(32) != 16 ||
                !Arrays.equals(Arrays.copyOfRange(files[0], 48, 80), Arrays.copyOfRange(fixture, 64, 96)))
            throw new AssertionError("Sequence bundle fixture mismatch");
        if (files[1].length != 16 || files[1][0] != 4 || files[1][2] != 7 ||
                files[1][4] != 2 || files[1][5] != 2 || files[1][6] != 1 ||
                files[1][7] != 1 || files[1][8] != 3)
            throw new AssertionError("Bank-set fixture mismatch");
        if (args.length == 0) { System.out.println("ROM-free sound sequence check passed"); return; }
        if (args.length != 3) throw new IllegalArgumentException("ROM, base.zip, sequence map required");

        byte[] rom = read(new FileInputStream(args[0]));
        InputStream map = new FileInputStream(args[2]);
        try { files = RomSoundSequences.build(rom, map); }
        finally { map.close(); }
        ZipFile zip = new ZipFile(args[1]);
        try {
            if (!Arrays.equals(files[0], read(zip.getInputStream(zip.getEntry("sound/sequences.bin.le.64")))) ||
                    !Arrays.equals(files[1], read(zip.getInputStream(zip.getEntry("sound/bank_sets.le.64")))))
                throw new AssertionError("Rebuilt sound files differ from private base.zip");
        } finally { zip.close(); }
        System.out.println("US ROM sequence and bank-set files match private base.zip byte-for-byte");
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
