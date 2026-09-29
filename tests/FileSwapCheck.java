package com.vdavid003.sm64port;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class FileSwapCheck {
    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("sm64-file-swap-");
        File target = dir.resolve("settings.txt").toFile();
        File backup = dir.resolve("settings.txt.backup").toFile();
        File temp = dir.resolve("settings.txt.part").toFile();
        try {
            Files.write(target.toPath(), "old".getBytes(StandardCharsets.UTF_8));
            check(target.renameTo(backup), "simulate interrupted replacement");
            FileSwap.recover(target);
            check("old".equals(read(target)) && !backup.exists(), "restore interrupted replacement");

            Files.write(temp.toPath(), "new".getBytes(StandardCharsets.UTF_8));
            FileSwap.replace(temp, target);
            check("new".equals(read(target)) && !backup.exists(), "replace settings");

            Files.write(backup.toPath(), "old".getBytes(StandardCharsets.UTF_8));
            FileSwap.recover(target);
            check("new".equals(read(target)) && !backup.exists(), "keep committed replacement");

            try {
                FileSwap.replace(dir.resolve("missing").toFile(), target);
                throw new AssertionError("missing replacement unexpectedly succeeded");
            } catch (java.io.IOException expected) {
                check("new".equals(read(target)) && !backup.exists(), "restore after failed replacement");
            }
        } finally {
            Files.deleteIfExists(temp.toPath());
            Files.deleteIfExists(backup.toPath());
            Files.deleteIfExists(target.toPath());
            Files.delete(dir);
        }
    }

    private static String read(File file) throws Exception {
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
