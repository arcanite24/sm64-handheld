package com.vdavid003.sm64port;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/** Rebuilds Android's music sequence and bank-set files from a verified US ROM. */
final class RomSoundSequences {
    private static final class Entry {
        int offset, size;
        byte[] banks;
    }

    static byte[][] build(byte[] rom, InputStream manifest) throws Exception {
        if (rom.length != 8 * 1024 * 1024) throw new IllegalArgumentException("Expected an 8 MB US ROM");
        List<Entry> entries = new ArrayList<Entry>();
        BufferedReader lines = new BufferedReader(new InputStreamReader(manifest, "UTF-8"));
        String line;
        int dataSize = 0, bankSize = 0;
        while ((line = lines.readLine()) != null) {
            String[] fields = line.split("\t", -1);
            if (fields.length != 4 || Integer.parseInt(fields[0]) != entries.size())
                throw new IllegalArgumentException("Invalid sequence map");
            Entry entry = new Entry();
            entry.offset = Integer.parseInt(fields[1]);
            entry.size = Integer.parseInt(fields[2]);
            if (entry.size <= 0 || entry.size % 16 != 0 || entry.offset < 0 || entry.offset > rom.length - entry.size)
                throw new IllegalArgumentException("Invalid sequence bounds");
            String[] bankIds = fields[3].isEmpty() ? new String[0] : fields[3].split(",");
            entry.banks = new byte[bankIds.length];
            for (int i = 0; i < bankIds.length; i++) {
                int id = Integer.parseInt(bankIds[i]);
                if (id < 0 || id > 255) throw new IllegalArgumentException("Invalid bank ID");
                entry.banks[i] = (byte) id;
            }
            entries.add(entry);
            dataSize += entry.size;
            bankSize += 1 + entry.banks.length;
        }
        int count = entries.size();
        if (count == 0 || count > 255) throw new IllegalArgumentException("Invalid sequence count");

        int dataStart = align(8 + count * 16, 16);
        byte[] sequences = new byte[align(dataStart + dataSize, 64)];
        ByteBuffer sequenceHeader = ByteBuffer.wrap(sequences).order(ByteOrder.LITTLE_ENDIAN);
        sequenceHeader.putShort(0, (short) 3);
        sequenceHeader.putShort(2, (short) count);
        int cursor = dataStart;
        for (int i = 0; i < count; i++) {
            Entry entry = entries.get(i);
            sequenceHeader.putLong(8 + i * 16, cursor);
            sequenceHeader.putInt(16 + i * 16, entry.size);
            System.arraycopy(rom, entry.offset, sequences, cursor, entry.size);
            cursor += entry.size;
        }

        byte[] bankSets = new byte[align(count * 2 + bankSize, 16)];
        ByteBuffer bankHeader = ByteBuffer.wrap(bankSets).order(ByteOrder.LITTLE_ENDIAN);
        cursor = count * 2;
        for (int i = 0; i < count; i++) {
            Entry entry = entries.get(i);
            bankHeader.putShort(i * 2, (short) cursor);
            bankSets[cursor++] = (byte) entry.banks.length;
            System.arraycopy(entry.banks, 0, bankSets, cursor, entry.banks.length);
            cursor += entry.banks.length;
        }
        return new byte[][] {sequences, bankSets};
    }

    private static int align(int value, int multiple) {
        return (value + multiple - 1) & -multiple;
    }
}
