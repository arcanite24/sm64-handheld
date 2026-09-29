package com.vdavid003.sm64port;

import java.io.File;
import java.io.IOException;

final class FileSwap {
    private FileSwap() {}

    static void recover(File target) throws IOException {
        File backup = new File(target.getParentFile(), target.getName() + ".backup");
        if (!backup.exists()) return;
        if (!target.exists()) {
            if (!backup.renameTo(target)) throw new IOException("Could not restore " + target.getName());
        } else if (!backup.delete()) {
            throw new IOException("Could not clear backup for " + target.getName());
        }
    }

    static void replace(File source, File target) throws IOException {
        recover(target);
        File backup = new File(target.getParentFile(), target.getName() + ".backup");
        boolean old = target.exists();
        if (old && !target.renameTo(backup))
            throw new IOException("Could not back up " + target.getName());
        if (!source.renameTo(target)) {
            if (old && !backup.renameTo(target))
                throw new IOException("Could not restore " + target.getName() + "; old file remains at " + backup.getName());
            throw new IOException("Could not save " + target.getName());
        }
        if (old && !backup.delete())
            throw new IOException("Could not clear backup for " + target.getName());
    }
}
