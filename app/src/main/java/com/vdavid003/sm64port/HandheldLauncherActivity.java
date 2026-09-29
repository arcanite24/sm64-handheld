package com.vdavid003.sm64port;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipFile;

public final class HandheldLauncherActivity extends Activity {
    private static final int PICK_RESOURCES = 1;
    private static final String MODEL_URL =
            "https://github.com/Render96/ModelPack/releases/download/3.25/Render96_DynOs_v3.25.7z";
    private static final String MODEL_SHA256 =
            "22eea6dafcc0a87659d60513eb6a6b0f4ea0acbc6645a6b1b445de9e5812e6df";
    private static final String TEXTURE_URL =
            "https://github.com/pokeheadroom/RENDER96-HD-TEXTURE-PACK/releases/download/1.3.26.7.7/Render96.HD.Texture.Pack.1.3.26.7.7.7z";
    private static final String TEXTURE_SHA256 =
            "a9ea999c8a4a2bd68954b0f9eecee62db481ef31f6189cf362cc72c9edc4191c";
    private static final String TEXTURE_ROOT = "Render96 HD Texture Pack 1.3.26.7.7/";
    private static final String TEXTURE_MARKER = ".sm64-handheld-render96-hd";
    private static final String WORLD_PACK = "Render96_DynOS_v3.25";
    private static final String MARIO_PACK = "Render96 Mario v3.25";
    private static final String WORLD_KEY = "dynos_pack_422ABEBD";
    private static final String MARIO_KEY = "dynos_pack_2E96906A";
    private static final int BACKGROUND = 0xff111523;
    private static final int PANEL = 0xff1d2638;
    private static final int ACCENT = 0xffffc878;
    private static final int MUTED = 0xffb8c4d8;

    private Button playButton;
    private Button resourcesButton;
    private Button modelsButton;
    private Button presetButton;
    private Button texturesButton;
    private Button texturePresetButton;
    private TextView resourcesStatus;
    private TextView modelsStatus;
    private TextView texturesStatus;
    private TextView message;
    private boolean working;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BACKGROUND);
        getWindow().setNavigationBarColor(BACKGROUND);
        buildScreen();
        refresh();
    }

    private void buildScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BACKGROUND);
        LinearLayout root = column();
        root.setPadding(dp(24), dp(12), dp(24), dp(12));
        scroll.addView(root);
        setContentView(scroll);

        TextView eyebrow = label("SM64  /  HANDHELD EDITION", 12, ACCENT, true);
        root.addView(eyebrow);
        TextView title = label("Your next star awaits.", 25, Color.WHITE, true);
        root.addView(title);
        TextView subtitle = label("Set up once, then play with your handheld controls.", 14, MUTED, false);
        root.addView(subtitle);

        LinearLayout cards = new LinearLayout(this);
        cards.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams cardRow = new LinearLayout.LayoutParams(-1, -2);
        cardRow.topMargin = dp(12);
        root.addView(cards, cardRow);

        LinearLayout first = card("01  GAME FILES", "Import your own privately built base.zip.");
        resourcesStatus = label("", 13, MUTED, false);
        first.addView(resourcesStatus);
        resourcesButton = button("Choose base.zip", first, new View.OnClickListener() {
            @Override public void onClick(View v) { chooseResources(); }
        });
        cards.addView(first, new LinearLayout.LayoutParams(0, -2, 1));

        LinearLayout second = card("02  THE LOOK", "Render96 models, downloaded from their official release.");
        modelsStatus = label("", 13, MUTED, false);
        second.addView(modelsStatus);
        modelsButton = button("Install Render96", second, new View.OnClickListener() {
            @Override public void onClick(View v) { installModels(); }
        });
        presetButton = button("Use Classic models", second, new View.OnClickListener() {
            @Override public void onClick(View v) {
                try {
                    setRender96(!render96Enabled());
                    refresh();
                    message.setText("Model choice saved. It takes effect on the next game launch.");
                } catch (Exception e) {
                    showError(e);
                }
            }
        });
        LinearLayout.LayoutParams secondParams = new LinearLayout.LayoutParams(0, -2, 1);
        secondParams.leftMargin = dp(12);
        cards.addView(second, secondParams);

        LinearLayout textures = card("03  SHARPER TEXTURES",
                "Optional Render96 HD pack from its official release · 392 MB download.");
        LinearLayout.LayoutParams textureParams = new LinearLayout.LayoutParams(0, -2, 1);
        textureParams.leftMargin = dp(12);
        cards.addView(textures, textureParams);
        texturesStatus = label("", 13, MUTED, false);
        textures.addView(texturesStatus);
        LinearLayout textureActions = new LinearLayout(this);
        textureActions.setOrientation(LinearLayout.HORIZONTAL);
        textures.addView(textureActions);
        texturesButton = button("Download HD textures", textureActions, new View.OnClickListener() {
            @Override public void onClick(View v) { installTextures(); }
        });
        texturePresetButton = button("Use Classic textures", textureActions, new View.OnClickListener() {
            @Override public void onClick(View v) {
                try {
                    setTexturesEnabled(!texturesEnabled());
                    refresh();
                    message.setText("Texture choice saved. It takes effect on the next game launch.");
                } catch (Exception e) {
                    showError(e);
                }
            }
        });
        texturesButton.setLayoutParams(new LinearLayout.LayoutParams(0, dp(48), 1));
        LinearLayout.LayoutParams textureToggleParams = new LinearLayout.LayoutParams(0, dp(48), 1);
        textureToggleParams.leftMargin = dp(12);
        texturePresetButton.setLayoutParams(textureToggleParams);

        LinearLayout launchActions = new LinearLayout(this);
        launchActions.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams launchParams = new LinearLayout.LayoutParams(-1, -2);
        launchParams.topMargin = dp(12);
        root.addView(launchActions, launchParams);
        playButton = button("Play Super Mario 64  →", launchActions, new View.OnClickListener() {
            @Override public void onClick(View v) {
                startActivity(new Intent(HandheldLauncherActivity.this, sm64portActivity.class));
            }
        });
        Button controlsButton = button("Controls & camera", launchActions, new View.OnClickListener() {
            @Override public void onClick(View v) {
                startActivity(new Intent(HandheldLauncherActivity.this, ControllerSettingsActivity.class));
            }
        });
        playButton.setLayoutParams(new LinearLayout.LayoutParams(0, dp(48), 2));
        LinearLayout.LayoutParams controlsButtonParams = new LinearLayout.LayoutParams(0, dp(48), 1);
        controlsButtonParams.leftMargin = dp(12);
        controlsButton.setLayoutParams(controlsButtonParams);
        message = label("", 14, MUTED, false);
        root.addView(message);
        TextView controls = label("LEFT STICK  Move     RIGHT STICK  Camera     L1  Recenter     Open Controls to see your buttons.", 12, MUTED, false);
        LinearLayout.LayoutParams controlsParams = new LinearLayout.LayoutParams(-1, -2);
        controlsParams.topMargin = dp(8);
        root.addView(controls, controlsParams);
        playButton.requestFocus();
    }

    private LinearLayout card(String heading, String description) {
        LinearLayout card = column();
        card.setPadding(dp(16), dp(12), dp(16), dp(12));
        card.setBackground(rounded(PANEL, 16));
        card.addView(label(heading, 16, Color.WHITE, true));
        card.addView(label(description, 13, MUTED, false));
        return card;
    }

    private Button button(String title, LinearLayout parent, View.OnClickListener click) {
        Button button = new Button(this);
        button.setText(title);
        button.setTextSize(14);
        button.setTextColor(new ColorStateList(
                new int[][] { new int[] { -android.R.attr.state_enabled }, new int[] {} },
                new int[] { MUTED, BACKGROUND }));
        button.setAllCaps(false);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setMinHeight(dp(48));
        StateListDrawable backgrounds = new StateListDrawable();
        GradientDrawable focused = rounded(ACCENT, 12);
        focused.setStroke(dp(3), Color.WHITE);
        backgrounds.addState(new int[] { -android.R.attr.state_enabled }, rounded(0xff39445a, 12));
        backgrounds.addState(new int[] { android.R.attr.state_focused }, focused);
        backgrounds.addState(new int[] {}, rounded(ACCENT, 12));
        button.setBackground(backgrounds);
        button.setOnClickListener(click);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(48));
        params.topMargin = dp(8);
        parent.addView(button, params);
        return button;
    }

    private TextView label(String content, int sp, int color, boolean bold) {
        TextView label = new TextView(this);
        label.setText(content);
        label.setTextSize(sp);
        label.setTextColor(color);
        label.setGravity(Gravity.CENTER_VERTICAL);
        if (bold) label.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return label;
    }

    private LinearLayout column() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    private GradientDrawable rounded(int color, int radius) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(dp(radius));
        return shape;
    }

    private int dp(int value) {
        return (int) (getResources().getDisplayMetrics().density * value + 0.5f);
    }

    private File files() {
        File root = getExternalFilesDir(null);
        if (root == null) throw new IllegalStateException("App storage is unavailable");
        return root;
    }

    private File resources() {
        return new File(new File(files(), "res"), "base.zip");
    }

    private File packs() {
        return new File(new File(files(), "dynos"), "packs");
    }

    private File activeTextures() {
        return new File(new File(files(), "res"), "gfx");
    }

    private File inactiveTextures() {
        return new File(new File(files(), "visuals"), "gfx");
    }

    private boolean texturesEnabled() {
        return new File(activeTextures(), TEXTURE_MARKER).isFile();
    }

    private boolean texturesInstalled() {
        return texturesEnabled() || new File(inactiveTextures(), TEXTURE_MARKER).isFile();
    }

    private boolean modelsInstalled() {
        return new File(packs(), WORLD_PACK + "/bowser_geo.bin").isFile()
                && new File(packs(), MARIO_PACK + "/mario_geo.bin").isFile();
    }

    private void refresh() {
        boolean ready = resources().isFile();
        boolean models = modelsInstalled();
        resourcesStatus.setText(ready ? "Ready to play" : "Needed before you can play");
        modelsStatus.setText(models ? "Installed" : "Optional · Classic models are ready");
        texturesStatus.setText(texturesInstalled() ?
                (texturesEnabled() ? "HD textures active" : "Installed · Classic textures active") :
                "Optional · uses about 406 MB after installation");
        playButton.setEnabled(ready && !working);
        resourcesButton.setEnabled(!working);
        modelsButton.setEnabled(!working && !models);
        modelsButton.setText(models ? "Render96 installed" : "Install Render96");
        presetButton.setEnabled(!working && models);
        presetButton.setText(render96Enabled() ? "Use Classic models" : "Use Render96 models");
        texturesButton.setEnabled(!working && !texturesInstalled());
        texturesButton.setVisibility(texturesInstalled() ? View.GONE : View.VISIBLE);
        texturesButton.setText(texturesInstalled() ? "HD textures installed" : "Download HD textures");
        texturePresetButton.setEnabled(!working && texturesInstalled());
        texturePresetButton.setVisibility(texturesInstalled() ? View.VISIBLE : View.GONE);
        texturePresetButton.setText(texturesEnabled() ? "Use Classic textures" : "Use HD textures");
        message.setTextColor(MUTED);
        message.setText(working ? "Working… keep the app open." :
                "Use the D-pad and A to select. Android Back closes this screen.");
    }

    private void chooseResources() {
        Intent choose = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        choose.setType("*/*");
        choose.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(choose, PICK_RESOURCES);
    }

    @Override
    protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request == PICK_RESOURCES && result == RESULT_OK && data != null) {
            final Uri uri = data.getData();
            if (uri != null) work(new Job() {
                @Override public void run() throws Exception { importResources(uri); }
            }, "Game files imported.");
        }
    }

    private void importResources(Uri uri) throws Exception {
        File target = resources();
        if (!target.getParentFile().isDirectory() && !target.getParentFile().mkdirs())
            throw new IllegalStateException("Could not create game storage");
        File temp = new File(target.getParentFile(), "base.zip.part");
        try {
            InputStream input = getContentResolver().openInputStream(uri);
            if (input == null) throw new IllegalArgumentException("Could not read selected file");
            try {
                OutputStream output = new FileOutputStream(temp);
                try {
                    copy(input, output, 128L * 1024 * 1024);
                } finally {
                    output.close();
                }
            } finally {
                input.close();
            }
            ZipFile zip = new ZipFile(temp);
            try {
                if (zip.getEntry("sound/sound_data.ctl.le.64") == null ||
                    zip.getEntry("gfx/textures/skybox_tiles/bits.22.rgba16.png") == null)
                    throw new IllegalArgumentException("This is not a compatible SM64 base.zip");
            } finally {
                zip.close();
            }
            replace(temp, target);
        } finally {
            temp.delete();
        }
    }

    private void installModels() {
        work(new Job() {
            @Override public void run() throws Exception { downloadAndExtractModels(); }
        }, "Render96 installed. Start the game to see it.");
    }

    private void downloadAndExtractModels() throws Exception {
        File archive = new File(getCacheDir(), "render96-v3.25.7z");
        File staging = new File(files(), "dynos/.render96-install");
        deleteTree(staging);
        if (!staging.mkdirs()) throw new IllegalStateException("Could not create model storage");
        try {
            downloadArchive(MODEL_URL, MODEL_SHA256, 120L * 1024 * 1024,
                    archive, "Render96");
            progress("Installing Render96 models…");
            extractModels(archive, staging);
            if (!new File(staging, WORLD_PACK + "/bowser_geo.bin").isFile() ||
                !new File(staging, MARIO_PACK + "/mario_geo.bin").isFile())
                throw new IllegalArgumentException("Render96 archive is missing required models");
            if (!packs().isDirectory() && !packs().mkdirs())
                throw new IllegalStateException("Could not create model pack directory");
            for (String name : new String[] { WORLD_PACK, MARIO_PACK }) {
                File destination = new File(packs(), name);
                if (destination.exists()) continue;
                if (!new File(staging, name).renameTo(destination))
                    throw new IllegalStateException("Could not install " + name);
            }
        } finally {
            archive.delete();
            deleteTree(staging);
        }
    }

    private void extractModels(File archive, File staging) throws Exception {
        SevenZFile seven = new SevenZFile(archive);
        try {
            SevenZArchiveEntry entry;
            long total = 0;
            while ((entry = seven.getNextEntry()) != null) {
                String name = entry.getName();
                if (!(name.startsWith(WORLD_PACK + "/") || name.startsWith(MARIO_PACK + "/")))
                    continue;
                if (name.contains("../") || name.contains("\\") || name.startsWith("/"))
                    throw new IllegalArgumentException("Unsafe model path");
                if (entry.isDirectory()) continue;
                long size = entry.getSize();
                if (size < 0 || size > 64L * 1024 * 1024 ||
                    (total += size) > 256L * 1024 * 1024)
                    throw new IllegalArgumentException("Model archive is larger than expected");
                File outputFile = new File(staging, name);
                if (!outputFile.getParentFile().isDirectory() && !outputFile.getParentFile().mkdirs())
                    throw new IllegalStateException("Could not create model folder");
                OutputStream output = new FileOutputStream(outputFile);
                try {
                    byte[] buffer = new byte[65536];
                    long remaining = size;
                    while (remaining > 0) {
                        int read = seven.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                        if (read < 0) throw new IllegalArgumentException("Truncated model archive");
                        output.write(buffer, 0, read);
                        remaining -= read;
                    }
                } finally {
                    output.close();
                }
            }
        } finally {
            seven.close();
        }
    }

    private void installTextures() {
        work(new Job() {
            @Override public void run() throws Exception { downloadAndExtractTextures(); }
        }, "HD textures installed. Start the game to see them.");
    }

    private void downloadAndExtractTextures() throws Exception {
        if (activeTextures().exists() || inactiveTextures().exists())
            throw new IllegalStateException("An existing texture folder needs to be moved first");
        File archive = new File(getCacheDir(), "render96-hd.7z");
        File staging = new File(new File(files(), "visuals"), ".render96-hd-install");
        deleteTree(staging);
        if (!staging.mkdirs()) throw new IllegalStateException("Could not create texture storage");
        try {
            downloadArchive(TEXTURE_URL, TEXTURE_SHA256, 420L * 1024 * 1024,
                    archive, "HD textures");
            progress("Installing HD textures…");
            extractTextures(archive, staging);
            File extracted = new File(staging, "gfx");
            if (!new File(extracted, "actors/amp/amp_body.rgba16.png").isFile())
                throw new IllegalArgumentException("Texture archive is missing required files");
            if (!new File(extracted, TEXTURE_MARKER).createNewFile())
                throw new IllegalStateException("Could not mark installed textures");
            if (!extracted.renameTo(activeTextures()))
                throw new IllegalStateException("Could not enable HD textures");
        } finally {
            archive.delete();
            deleteTree(staging);
        }
    }

    private void extractTextures(File archive, File staging) throws Exception {
        SevenZFile seven = new SevenZFile(archive);
        try {
            SevenZArchiveEntry entry;
            long total = 0;
            int count = 0;
            while ((entry = seven.getNextEntry()) != null) {
                String name = entry.getName();
                if (!name.startsWith(TEXTURE_ROOT) || entry.isDirectory()) continue;
                String relative = name.substring(TEXTURE_ROOT.length());
                if (!(relative.startsWith("actors/") || relative.startsWith("levels/") ||
                      relative.startsWith("textures/")) || !relative.endsWith(".png")) continue;
                if (relative.startsWith("/") || relative.contains("../") || relative.contains("/./") ||
                    relative.contains("\\"))
                    throw new IllegalArgumentException("Unsafe texture path");
                long size = entry.getSize();
                if (size < 0 || size > 16L * 1024 * 1024 ||
                    (total += size) > 512L * 1024 * 1024 || ++count > 5000)
                    throw new IllegalArgumentException("Texture archive is larger than expected");
                if (count % 100 == 0) progress("Installing HD textures · " + count + " files");
                File outputFile = new File(new File(staging, "gfx"), relative);
                if (!outputFile.getParentFile().isDirectory() && !outputFile.getParentFile().mkdirs())
                    throw new IllegalStateException("Could not create texture folder");
                OutputStream output = new FileOutputStream(outputFile);
                try {
                    byte[] buffer = new byte[65536];
                    long remaining = size;
                    while (remaining > 0) {
                        int read = seven.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                        if (read < 0) throw new IllegalArgumentException("Truncated texture archive");
                        output.write(buffer, 0, read);
                        remaining -= read;
                    }
                } finally {
                    output.close();
                }
            }
        } finally {
            seven.close();
        }
    }

    private void setTexturesEnabled(boolean enabled) throws Exception {
        File source = enabled ? inactiveTextures() : activeTextures();
        File target = enabled ? activeTextures() : inactiveTextures();
        if (!new File(source, TEXTURE_MARKER).isFile() || target.exists())
            throw new IllegalStateException("Texture folder changed; restart setup");
        if (!target.getParentFile().isDirectory() && !target.getParentFile().mkdirs())
            throw new IllegalStateException("Could not create texture storage");
        if (!source.renameTo(target)) throw new IllegalStateException("Could not switch textures");
    }

    private void downloadArchive(String url, String sha256, long maxBytes,
                                 File archive, String label) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(20000);
        connection.setReadTimeout(30000);
        connection.setInstanceFollowRedirects(true);
        try {
            if (connection.getResponseCode() != 200)
                throw new IllegalStateException(label + " download returned HTTP " + connection.getResponseCode());
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            InputStream input = connection.getInputStream();
            try {
                OutputStream output = new FileOutputStream(archive);
                try {
                    byte[] buffer = new byte[65536];
                    long total = 0;
                    long shownAt = 0;
                    long expected = connection.getContentLengthLong();
                    int size;
                    while ((size = input.read(buffer)) != -1) {
                        total += size;
                        if (total > maxBytes)
                            throw new IllegalArgumentException(label + " download is larger than expected");
                        digest.update(buffer, 0, size);
                        output.write(buffer, 0, size);
                        if (total - shownAt >= 4L * 1024 * 1024) {
                            shownAt = total;
                            progress(expected > 0 ?
                                    "Downloading " + label + " · " + (total * 100 / expected) + "%" :
                                    "Downloading " + label + " · " + (total / 1024 / 1024) + " MB");
                        }
                    }
                } finally {
                    output.close();
                }
            } finally {
                input.close();
            }
            if (!sha256.equals(hex(digest.digest())))
                throw new IllegalArgumentException(label + " download failed its integrity check");
        } finally {
            connection.disconnect();
        }
    }

    private boolean render96Enabled() {
        File config = new File(new File(files(), "user"), "DynOS.1.0.config.txt");
        if (!config.isFile()) return true;
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(config), "UTF-8"));
            try {
                boolean world = true;
                boolean mario = true;
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith(WORLD_KEY + "=")) world = line.endsWith("=1");
                    else if (line.startsWith(MARIO_KEY + "=")) mario = line.endsWith("=1");
                }
                return world && mario;
            } finally {
                reader.close();
            }
        } catch (Exception ignored) {
            // The engine's default is Render96 when a model pack exists.
        }
        return true;
    }

    private void setRender96(boolean enabled) throws Exception {
        File config = new File(new File(files(), "user"), "DynOS.1.0.config.txt");
        if (!config.getParentFile().isDirectory() && !config.getParentFile().mkdirs())
            throw new IllegalStateException("Could not create settings directory");
        List<String> lines = new ArrayList<String>();
        if (config.isFile()) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(config), "UTF-8"));
            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.startsWith(WORLD_KEY + "=") && !line.startsWith(MARIO_KEY + "="))
                        lines.add(line);
                }
            } finally {
                reader.close();
            }
        }
        lines.add(WORLD_KEY + "=" + (enabled ? "1" : "0"));
        lines.add(MARIO_KEY + "=" + (enabled ? "1" : "0"));
        File temp = new File(config.getParentFile(), config.getName() + ".part");
        try {
            OutputStream output = new FileOutputStream(temp);
            try {
                for (String line : lines) output.write((line + "\n").getBytes("UTF-8"));
            } finally {
                output.close();
            }
            replace(temp, config);
        } finally {
            temp.delete();
        }
    }

    private void progress(final String status) {
        runOnUiThread(new Runnable() {
            @Override public void run() {
                if (working) message.setText(status);
            }
        });
    }

    private static void replace(File source, File destination) throws Exception {
        File backup = new File(destination.getParentFile(), destination.getName() + ".backup");
        if (backup.exists() && !destination.exists() && !backup.renameTo(destination))
            throw new IllegalStateException("Could not restore previous file");
        if (backup.exists() && !backup.delete())
            throw new IllegalStateException("Could not clear previous backup");
        boolean hadOriginal = destination.exists();
        if (hadOriginal && !destination.renameTo(backup))
            throw new IllegalStateException("Could not back up current file");
        if (!source.renameTo(destination)) {
            if (hadOriginal) backup.renameTo(destination);
            throw new IllegalStateException("Could not save new file");
        }
        if (hadOriginal) backup.delete();
    }

    private static void copy(InputStream input, OutputStream output, long limit) throws Exception {
        byte[] buffer = new byte[65536];
        long total = 0;
        int size;
        while ((size = input.read(buffer)) != -1) {
            total += size;
            if (total > limit) throw new IllegalArgumentException("Selected file is too large");
            output.write(buffer, 0, size);
        }
    }

    private static String hex(byte[] bytes) {
        StringBuilder result = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) result.append(String.format(Locale.US, "%02x", value & 0xff));
        return result.toString();
    }

    private static void deleteTree(File path) {
        if (path.isDirectory()) {
            File[] children = path.listFiles();
            if (children != null) for (File child : children) deleteTree(child);
        }
        path.delete();
    }

    private interface Job { void run() throws Exception; }

    private void work(final Job job, final String success) {
        if (working) return;
        working = true;
        refresh();
        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    job.run();
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            working = false;
                            refresh();
                            message.setText(success);
                        }
                    });
                } catch (final Throwable error) {
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            working = false;
                            refresh();
                            showError(error);
                        }
                    });
                }
            }
        }, "handheld-setup").start();
    }

    private void showError(Throwable error) {
        message.setText(error.getMessage() == null ?
                "Setup failed: " + error.getClass().getSimpleName() : error.getMessage());
        message.setTextColor(0xffff8585);
    }
}
