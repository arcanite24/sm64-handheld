package com.vdavid003.sm64port;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/** Rebuilds the Android audio control banks from the user's US ROM. */
final class RomSoundControl {
    private static final int US_START = 5748512;
    private static final int US_SIZE = 97856;

    static byte[] build(byte[] rom) {
        if (rom.length != 8 * 1024 * 1024) throw new IllegalArgumentException("Expected an 8 MB US ROM");
        return repack(rom, US_START, US_SIZE);
    }

    static byte[] repack(byte[] rom, int start, int size) {
        if (start < 0 || size < 16 || start > rom.length - size)
            throw new IllegalArgumentException("Invalid sound control bounds");
        Reader ctl = new Reader(rom, start, size);
        int count = ctl.u16(2);
        if (ctl.u16(0) != 1 || count != 38) throw new IllegalArgumentException("Invalid sound control header");
        List<byte[]> banks = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            int offset = ctl.i32(4 + i * 8);
            int length = ctl.i32(8 + i * 8);
            if (offset < 0 || length < 16 || offset > size - length)
                throw new IllegalArgumentException("Invalid sound bank entry");
            banks.add(repackBank(new Reader(rom, start + offset, length)));
        }
        int dataStart = align(8 + count * 16, 16);
        int total = dataStart;
        for (byte[] bank : banks) total += bank.length;
        byte[] result = new byte[align(total + 1, 64)];
        ByteBuffer out = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
        out.putShort(0, (short) 1);
        out.putShort(2, (short) count);
        int pos = dataStart;
        for (int i = 0; i < count; i++) {
            byte[] bank = banks.get(i);
            out.putLong(8 + i * 16, pos);
            out.putInt(16 + i * 16, bank.length);
            System.arraycopy(bank, 0, result, pos, bank.length);
            pos += bank.length;
        }
        return result;
    }

    private static byte[] repackBank(Reader input) {
        int instCount = input.i32(0), drumCount = input.i32(4);
        if (instCount < 0 || instCount > 256 || drumCount < 0 || drumCount > 256)
            throw new IllegalArgumentException("Invalid sound bank counts");
        Reader data = input.slice(16, input.size - 16);
        int drumsArray = data.i32(0);
        int[] insts = new int[instCount], drums = new int[drumCount];
        for (int i = 0; i < instCount; i++) insts[i] = data.i32(4 + i * 4);
        for (int i = 0; i < drumCount; i++) drums[i] = data.i32(drumsArray + i * 4);
        TreeSet<Integer> sortedInsts = new TreeSet<Integer>();
        for (int inst : insts) if (inst != 0) sortedInsts.add(inst);

        LinkedHashSet<Integer> samples = new LinkedHashSet<>();
        TreeMap<Integer, Integer> envelopes = new TreeMap<>();
        boolean drumsDone = false;
        int firstDrumSample = drumCount == 0 ? Integer.MAX_VALUE : data.i32(drums[0] + 4);
        for (int inst : sortedInsts) {
            if (!drumsDone && drumCount > 0 && laterSample(data, inst, firstDrumSample)) {
                addDrums(data, drums, samples, envelopes);
                drumsDone = true;
            }
            envelopes.put(data.i32(inst + 4), 0);
            for (int j = 0; j < 3; j++) addSample(samples, data.i32(inst + 8 + j * 8));
        }
        if (!drumsDone) addDrums(data, drums, samples, envelopes);

        // The source bank's unused envelope stubs are retained by the PC builder.
        TreeMap<Integer, Integer> usedWords = new TreeMap<>();
        for (int addr : envelopes.keySet()) markEnvelope(data, addr, usedWords);
        if (!usedWords.isEmpty()) {
            int first = usedWords.firstKey(), last = usedWords.lastKey();
            for (int addr = first + 4; addr < last; addr += 4) {
                if (!usedWords.containsKey(addr)) {
                    envelopes.put(addr, 0);
                    markEnvelope(data, addr, usedWords);
                }
            }
        }

        Writer out = new Writer();
        out.i32(instCount);
        out.i32(drumCount);
        out.i32(input.i32(8));
        out.i32(input.i32(12));
        int base = out.pos();
        int drumPointer = out.reserve(8);
        int instPointers = out.reserve(instCount * 8);
        out.align(16);

        TreeMap<Integer, Integer> samplePositions = new TreeMap<>();
        for (int sample : samples) {
            samplePositions.put(sample, out.pos() - base);
            out.i64(0);
            out.i64(data.i32(sample + 4));
            int loopPointer = out.reserve(8), bookPointer = out.reserve(8);
            int size = data.i32(sample + 16);
            out.i32(size);
            out.align(16);

            int book = data.i32(sample + 12);
            out.patch64(bookPointer, out.pos() - base);
            int order = data.i32(book), predictors = data.i32(book + 4);
            if (order <= 0 || predictors <= 0 || order * predictors > 128)
                throw new IllegalArgumentException("Invalid sound codebook");
            out.i32(order);
            out.i32(predictors);
            for (int j = 0; j < 8 * order * predictors; j++) out.i16(data.u16(book + 8 + j * 2));
            out.align(16);

            int loop = data.i32(sample + 8);
            out.patch64(loopPointer, out.pos() - base);
            int count = data.i32(loop + 8);
            out.i32(data.i32(loop));
            out.i32(count == 0 ? ((size - (size % 9 == 1 ? 1 : 0)) / 9 * 16 + (size % 2) + (size % 9 == 1 ? 1 : 0)) : data.i32(loop + 4));
            out.i32(count);
            out.i32(0);
            if (count != 0) for (int j = 0; j < 16; j++) out.i16(data.u16(loop + 16 + j * 2));
            out.align(16);
        }

        TreeMap<Integer, Integer> envelopePositions = new TreeMap<>();
        for (int addr : envelopes.keySet()) {
            envelopePositions.put(addr, out.pos() - base);
            int words = envelopeWords(data, addr);
            for (int j = 0; j < words * 4; j++) out.u8(data.u8(addr + j));
            out.align(16);
        }

        TreeMap<Integer, Integer> instPositions = new TreeMap<>();
        for (int inst : sortedInsts) {
            instPositions.put(inst, out.pos() - base);
            out.u8(0);
            out.u8(data.u8(inst + 1));
            out.u8(data.u8(inst + 2));
            out.u8(data.u8(inst + 3));
            out.i32(0);
            out.i64(envelopePositions.get(data.i32(inst + 4)));
            for (int j = 0; j < 3; j++) writeSound(out, data, inst + 8 + j * 8, samplePositions);
        }
        out.align(16);
        for (int i = 0; i < instCount; i++)
            out.patch64(instPointers + i * 8, insts[i] == 0 ? 0 : instPositions.get(insts[i]));

        if (drumCount > 0) {
            int[] positions = new int[drumCount];
            for (int i = 0; i < drumCount; i++) {
                int drum = drums[i];
                positions[i] = out.pos() - base;
                for (int j = 0; j < 4; j++) out.u8(data.u8(drum + j));
                out.i32(0);
                writeSound(out, data, drum + 4, samplePositions);
                out.i64(envelopePositions.get(data.i32(drum + 12)));
            }
            out.align(16);
            out.patch64(drumPointer, out.pos() - base);
            for (int position : positions) out.i64(position);
            out.align(16);
        }
        out.align(16);
        return out.bytes();
    }

    private static boolean laterSample(Reader data, int inst, int drumSample) {
        for (int j = 0; j < 3; j++) {
            int sample = data.i32(inst + 8 + j * 8);
            if (sample != 0 && sample > drumSample) return true;
        }
        return false;
    }

    private static void addDrums(Reader data, int[] drums, LinkedHashSet<Integer> samples,
                                 TreeMap<Integer, Integer> envelopes) {
        for (int drum : drums) {
            addSample(samples, data.i32(drum + 4));
            envelopes.put(data.i32(drum + 12), 0);
        }
    }

    private static void addSample(LinkedHashSet<Integer> samples, int sample) {
        if (sample != 0) samples.add(sample);
    }

    private static int envelopeWords(Reader data, int addr) {
        for (int i = 0; i < 256; i++) {
            int delay = data.u16(addr + i * 4);
            if (delay >= 65533) return i + 1;
        }
        throw new IllegalArgumentException("Unterminated sound envelope");
    }

    private static void markEnvelope(Reader data, int addr, TreeMap<Integer, Integer> used) {
        int words = align(envelopeWords(data, addr), 4);
        for (int i = 0; i < words; i++) used.put(addr + i * 4, 0);
    }

    private static void writeSound(Writer out, Reader data, int addr,
                                   Map<Integer, Integer> positions) {
        int sample = data.i32(addr);
        out.i64(sample == 0 ? 0 : positions.get(sample));
        out.i32(data.i32(addr + 4));
        out.i32(0);
    }

    private static int align(int value, int multiple) { return (value + multiple - 1) & -multiple; }

    private static final class Reader {
        final byte[] bytes;
        final int start, size;
        Reader(byte[] bytes, int start, int size) { this.bytes = bytes; this.start = start; this.size = size; }
        Reader slice(int offset, int length) { check(offset, length); return new Reader(bytes, start + offset, length); }
        void check(int offset, int length) {
            if (offset < 0 || length < 0 || offset > size - length)
                throw new IllegalArgumentException("Sound control data is truncated");
        }
        int u8(int offset) { check(offset, 1); return bytes[start + offset] & 255; }
        int u16(int offset) { check(offset, 2); return u8(offset) << 8 | u8(offset + 1); }
        int i32(int offset) { check(offset, 4); return u8(offset) << 24 | u8(offset + 1) << 16 | u8(offset + 2) << 8 | u8(offset + 3); }
    }

    private static final class Writer {
        private byte[] bytes = new byte[32768];
        private int pos;
        int pos() { return pos; }
        int reserve(int count) { int at = pos; ensure(count); pos += count; return at; }
        void ensure(int count) {
            if (pos + count > bytes.length) throw new IllegalArgumentException("Sound bank is too large");
        }
        void u8(int value) { ensure(1); bytes[pos++] = (byte) value; }
        void i16(int value) { u8(value); u8(value >> 8); }
        void i32(int value) { for (int i = 0; i < 4; i++) u8(value >> (i * 8)); }
        void i64(long value) { for (int i = 0; i < 8; i++) u8((int) (value >> (i * 8))); }
        void patch64(int at, long value) { for (int i = 0; i < 8; i++) bytes[at + i] = (byte) (value >> (i * 8)); }
        void align(int multiple) { reserve(RomSoundControl.align(pos, multiple) - pos); }
        byte[] bytes() { byte[] result = new byte[pos]; System.arraycopy(bytes, 0, result, 0, pos); return result; }
    }
}
