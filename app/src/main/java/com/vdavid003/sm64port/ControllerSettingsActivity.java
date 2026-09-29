package com.vdavid003.sm64port;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ControllerSettingsActivity extends Activity {
    private static final int BG = 0xff111523;
    private static final int PANEL = 0xff1d2638;
    private static final int ACCENT = 0xffffc878;
    private static final int MUTED = 0xffb8c4d8;
    private static final String[] KEYS = { "key_a", "key_b", "key_z", "key_start" };
    private static final String[] ACTIONS = { "Jump", "Attack", "Crouch", "Pause" };
    private static final int[] DEFAULT_BUTTONS = { 0, 1, 26, 6 };
    private static final String[] DEFAULT_BINDINGS = {
            "0026 1000 1103", "0033 1001 1101", "0025 101a ffff", "0039 1006 ffff"
    };

    private final Button[] mappingButtons = new Button[KEYS.length];
    private TextView deviceLabel;
    private TextView message;
    private TextView deadzoneLabel;
    private TextView triggerLabel;
    private SeekBar deadzoneBar;
    private SeekBar triggerBar;
    private Button cameraButton;
    private String capturing;
    private int swallowedKey = -1;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        buildScreen();
        refresh();
    }

    private void buildScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);
        LinearLayout root = column();
        root.setPadding(dp(24), dp(8), dp(24), dp(8));
        scroll.addView(root);
        setContentView(scroll);

        root.addView(label("SM64  /  CONTROLS", 12, ACCENT, true));
        root.addView(label("Make it yours.", 23, Color.WHITE, true));
        deviceLabel = label("", 13, MUTED, false);
        root.addView(deviceLabel);
        message = label("D-pad/A select · Changes apply on the next game launch.", 12, MUTED, false);
        root.addView(message);

        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams gridParams = new LinearLayout.LayoutParams(-1, -2);
        gridParams.topMargin = dp(8);
        root.addView(grid, gridParams);
        LinearLayout left = panel();
        LinearLayout right = panel();
        grid.addView(left, new LinearLayout.LayoutParams(0, -2, 1));
        LinearLayout.LayoutParams rightParams = new LinearLayout.LayoutParams(0, -2, 1);
        rightParams.leftMargin = dp(12);
        grid.addView(right, rightParams);

        left.addView(label("BUTTONS", 16, Color.WHITE, true));
        left.addView(label("Select an action, then press the new button.", 13, MUTED, false));
        for (int i = 0; i < KEYS.length; i++) {
            final int index = i;
            mappingButtons[i] = button("", left, new View.OnClickListener() {
                @Override public void onClick(View view) {
                    capturing = KEYS[index];
                    message.setText("Press a gamepad button for " + ACTIONS[index] + ". Android Back cancels.");
                }
            });
        }

        right.addView(label("STICKS & CAMERA", 16, Color.WHITE, true));
        right.addView(label("Right stick looks around. L1 recenters; L2 crouches.", 13, MUTED, false));
        cameraButton = button("", right, new View.OnClickListener() {
            @Override public void onClick(View view) {
                if (save("bettercam_enable", String.valueOf(!cameraEnabled()))) {
                    refresh();
                    message.setText("Camera change applies on the next game launch.");
                }
            }
        });

        deadzoneLabel = label("", 13, MUTED, false);
        deadzoneLabel.setText("Stick dead zone  ·  " + intValue("stick_deadzone", 16) + " / 40");
        right.addView(deadzoneLabel);
        deadzoneBar = new SeekBar(this);
        deadzoneBar.setMax(40);
        deadzoneBar.setProgress(intValue("stick_deadzone", 16));
        deadzoneBar.setMinimumHeight(dp(48));
        right.addView(deadzoneBar);
        deadzoneBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int value, boolean fromUser) {
                deadzoneLabel.setText("Stick dead zone  ·  " + value + " / 40");
                if (fromUser) save("stick_deadzone", String.valueOf(value));
            }
            @Override public void onStartTrackingTouch(SeekBar bar) {}
            @Override public void onStopTrackingTouch(SeekBar bar) {}
        });

        triggerLabel = label("", 13, MUTED, false);
        triggerLabel.setText("Trigger press point  ·  " + intValue("trigger_threshold", 23) + "%");
        right.addView(triggerLabel);
        triggerBar = new SeekBar(this);
        triggerBar.setMax(55);
        triggerBar.setProgress(Math.max(0, Math.min(55, intValue("trigger_threshold", 23) - 5)));
        triggerBar.setMinimumHeight(dp(48));
        right.addView(triggerBar);
        triggerBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int value, boolean fromUser) {
                triggerLabel.setText("Trigger press point  ·  " + (value + 5) + "%");
                if (fromUser) save("trigger_threshold", String.valueOf(value + 5));
            }
            @Override public void onStartTrackingTouch(SeekBar bar) {}
            @Override public void onStopTrackingTouch(SeekBar bar) {}
        });

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams actionParams = new LinearLayout.LayoutParams(-1, -2);
        actionParams.topMargin = dp(8);
        root.addView(actions, actionParams);
        Button back = button("←  Back to setup", actions, new View.OnClickListener() {
            @Override public void onClick(View view) { finish(); }
        });
        Button reset = button("Reset controls", actions, new View.OnClickListener() {
            @Override public void onClick(View view) { resetDefaults(); }
        });
        back.setLayoutParams(new LinearLayout.LayoutParams(0, dp(48), 1));
        LinearLayout.LayoutParams resetParams = new LinearLayout.LayoutParams(0, dp(48), 1);
        resetParams.leftMargin = dp(12);
        reset.setLayoutParams(resetParams);

        mappingButtons[0].requestFocus();
    }

    private void refresh() {
        for (int i = 0; i < KEYS.length; i++)
            mappingButtons[i].setText(ACTIONS[i] + "  ·  " + bindingLabel(KEYS[i], DEFAULT_BUTTONS[i]));
        cameraButton.setText(cameraEnabled() ? "Better Camera  ·  ON" : "Better Camera  ·  OFF");
        deadzoneBar.setProgress(Math.max(0, Math.min(40, intValue("stick_deadzone", 16))));
        triggerBar.setProgress(Math.max(0, Math.min(55, intValue("trigger_threshold", 23) - 5)));
        deadzoneLabel.setText("Stick dead zone  ·  " + deadzoneBar.getProgress() + " / 40");
        triggerLabel.setText("Trigger press point  ·  " + (triggerBar.getProgress() + 5) + "%");
        String names = "";
        for (int id : InputDevice.getDeviceIds()) {
            InputDevice device = InputDevice.getDevice(id);
            if (device != null && (device.getSources() & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD) {
                if (!names.isEmpty()) names += ", ";
                names += device.getName();
            }
        }
        deviceLabel.setText(names.isEmpty() ? "No gamepad detected · connect one before remapping"
                : "Controller: " + names);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_UP && event.getKeyCode() == swallowedKey) {
            swallowedKey = -1;
            return true;
        }
        if (capturing != null && event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
            if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
                capturing = null;
                message.setText("Remapping cancelled.");
                swallowedKey = KeyEvent.KEYCODE_BACK;
                return true;
            }
            int button = gamepadButton(event.getKeyCode());
            if (button >= 0) {
                bindCaptured(button);
                swallowedKey = event.getKeyCode();
                return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        if (capturing != null && (event.getSource() & InputDevice.SOURCE_JOYSTICK) != 0) {
            if (event.getAxisValue(MotionEvent.AXIS_LTRIGGER) > 0.7f) {
                bindCaptured(26);
                return true;
            }
            if (event.getAxisValue(MotionEvent.AXIS_RTRIGGER) > 0.7f) {
                bindCaptured(27);
                return true;
            }
        }
        return super.onGenericMotionEvent(event);
    }

    private void bindCaptured(int button) {
        String key = capturing;
        capturing = null;
        String[] values = bindParts(readConfig(), key);
        if (save(key, String.format(Locale.US, "%s %04x %s", values[1], 0x1000 + button, values[3]))) {
            refresh();
            message.setText(buttonName(button) + " mapped to " + actionName(key) + ". Restart the game to use it.");
        }
    }

    private void resetDefaults() {
        Map<String, String> changes = new LinkedHashMap<String, String>();
        for (int i = 0; i < KEYS.length; i++) changes.put(KEYS[i], DEFAULT_BINDINGS[i]);
        changes.put("stick_deadzone", "16");
        changes.put("trigger_threshold", "23");
        changes.put("bettercam_enable", "true");
        try {
            writeConfig(changes);
            refresh();
            message.setTextColor(MUTED);
            message.setText("Handheld controls restored. Restart the game to use them.");
        } catch (RuntimeException error) {
            showError(error);
        }
    }

    private String actionName(String key) {
        for (int i = 0; i < KEYS.length; i++) if (KEYS[i].equals(key)) return ACTIONS[i];
        return key;
    }

    private String[] bindParts(List<String> lines, String key) {
        String existing = option(lines, key);
        if (existing != null) {
            String[] parts = existing.trim().split("\\s+");
            if (parts.length >= 4) return parts;
        }
        if ("key_a".equals(key)) return new String[] { key, "0026", "1000", "1103" };
        if ("key_b".equals(key)) return new String[] { key, "0033", "1001", "1101" };
        if ("key_z".equals(key)) return new String[] { key, "0025", "101a", "ffff" };
        return new String[] { key, "0039", "1006", "ffff" };
    }

    private static int gamepadButton(int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BUTTON_A: return 0;
            case KeyEvent.KEYCODE_BUTTON_B: return 1;
            case KeyEvent.KEYCODE_BUTTON_X: return 2;
            case KeyEvent.KEYCODE_BUTTON_Y: return 3;
            case KeyEvent.KEYCODE_BUTTON_SELECT: return 4;
            case KeyEvent.KEYCODE_BUTTON_START: return 6;
            case KeyEvent.KEYCODE_BUTTON_THUMBL: return 7;
            case KeyEvent.KEYCODE_BUTTON_THUMBR: return 8;
            case KeyEvent.KEYCODE_BUTTON_L1: return 9;
            case KeyEvent.KEYCODE_BUTTON_R1: return 10;
            case KeyEvent.KEYCODE_DPAD_UP: return 11;
            case KeyEvent.KEYCODE_DPAD_DOWN: return 12;
            case KeyEvent.KEYCODE_DPAD_LEFT: return 13;
            case KeyEvent.KEYCODE_DPAD_RIGHT: return 14;
            case KeyEvent.KEYCODE_BUTTON_L2: return 26;
            case KeyEvent.KEYCODE_BUTTON_R2: return 27;
            default: return -1;
        }
    }

    private static String buttonName(int button) {
        switch (button) {
            case 0: return "A";
            case 1: return "B";
            case 2: return "X";
            case 3: return "Y";
            case 4: return "Select";
            case 6: return "Start";
            case 7: return "L3";
            case 8: return "R3";
            case 9: return "L1";
            case 10: return "R1";
            case 11: return "D-pad ↑";
            case 12: return "D-pad ↓";
            case 13: return "D-pad ←";
            case 14: return "D-pad →";
            case 26: return "L2";
            case 27: return "R2";
            default: return "Button " + button;
        }
    }

    private File config() {
        return new File(new File(getExternalFilesDir(null), "user"), "sm64config.txt");
    }

    private List<String> readConfig() {
        List<String> lines = new ArrayList<String>();
        if (!config().isFile()) return lines;
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(config()), "UTF-8"));
            try {
                String line;
                while ((line = reader.readLine()) != null) lines.add(line);
            } finally {
                reader.close();
            }
        } catch (Exception error) {
            throw new IllegalStateException("Could not read game settings", error);
        }
        return lines;
    }

    private String option(List<String> lines, String key) {
        for (String line : lines) if (line.startsWith(key + " ")) return line;
        return null;
    }

    private int buttonValue(String key, int fallback) {
        String line = option(readConfig(), key);
        if (line == null) return fallback;
        String[] parts = line.trim().split("\\s+");
        if (parts.length < 3) return fallback;
        try {
            int value = Integer.parseInt(parts[2], 16) - 0x1000;
            return value >= 0 && value <= 27 ? value : fallback;
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private String bindingLabel(String key, int fallback) {
        int primary = buttonValue(key, fallback);
        String result = buttonName(primary);
        String line = option(readConfig(), key);
        if (line == null) return result;
        String[] parts = line.trim().split("\\s+");
        if (parts.length >= 4) {
            try {
                int extra = Integer.parseInt(parts[3], 16) - 0x1000;
                if (extra >= 0 && extra <= 27 && extra != primary)
                    result += " / " + buttonName(extra);
            } catch (NumberFormatException ignored) {}
        }
        return result;
    }

    private int intValue(String key, int fallback) {
        String line = option(readConfig(), key);
        if (line == null) return fallback;
        try {
            return Integer.parseInt(line.trim().split("\\s+")[1]);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private boolean cameraEnabled() {
        String line = option(readConfig(), "bettercam_enable");
        return line == null || line.endsWith("true");
    }

    private boolean save(String key, String value) {
        Map<String, String> one = new LinkedHashMap<String, String>();
        one.put(key, value);
        try {
            writeConfig(one);
            message.setTextColor(MUTED);
            return true;
        } catch (RuntimeException error) {
            showError(error);
            return false;
        }
    }

    private void showError(RuntimeException error) {
        message.setTextColor(0xffff8585);
        message.setText(error.getMessage() == null ? "Could not save controls" : error.getMessage());
    }

    private void writeConfig(Map<String, String> changes) {
        List<String> lines = readConfig();
        for (Map.Entry<String, String> change : changes.entrySet()) {
            boolean found = false;
            for (int i = 0; i < lines.size(); i++) {
                if (lines.get(i).startsWith(change.getKey() + " ")) {
                    lines.set(i, change.getKey() + " " + change.getValue());
                    found = true;
                    break;
                }
            }
            if (!found) lines.add(change.getKey() + " " + change.getValue());
        }
        File target = config();
        if (!target.getParentFile().isDirectory() && !target.getParentFile().mkdirs())
            throw new IllegalStateException("Could not create settings directory");
        File temp = new File(target.getParentFile(), "sm64config.txt.part");
        File backup = new File(target.getParentFile(), "sm64config.txt.backup");
        try {
            OutputStream output = new FileOutputStream(temp);
            try {
                for (String line : lines) output.write((line + "\n").getBytes("UTF-8"));
            } finally {
                output.close();
            }
            if (backup.exists() && !target.exists() && !backup.renameTo(target))
                throw new IllegalStateException("Could not restore game settings");
            if (backup.exists() && !backup.delete())
                throw new IllegalStateException("Could not clear old settings backup");
            boolean old = target.exists();
            if (old && !target.renameTo(backup))
                throw new IllegalStateException("Could not back up game settings");
            if (!temp.renameTo(target)) {
                if (old) backup.renameTo(target);
                throw new IllegalStateException("Could not save game settings");
            }
            if (old) backup.delete();
        } catch (Exception error) {
            throw new IllegalStateException("Could not save game settings", error);
        } finally {
            temp.delete();
        }
    }

    private LinearLayout panel() {
        LinearLayout panel = column();
        panel.setPadding(dp(16), dp(8), dp(16), dp(8));
        panel.setBackground(rounded(PANEL));
        return panel;
    }

    private LinearLayout column() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    private TextView label(String text, int sp, int color, boolean bold) {
        TextView label = new TextView(this);
        label.setText(text);
        label.setTextSize(sp);
        label.setTextColor(color);
        label.setGravity(Gravity.CENTER_VERTICAL);
        if (bold) label.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return label;
    }

    private Button button(String text, LinearLayout parent, View.OnClickListener click) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(14);
        button.setTextColor(BG);
        button.setAllCaps(false);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setMinHeight(dp(48));
        StateListDrawable backgrounds = new StateListDrawable();
        GradientDrawable focused = rounded(ACCENT);
        focused.setStroke(dp(3), Color.WHITE);
        backgrounds.addState(new int[] { android.R.attr.state_focused }, focused);
        backgrounds.addState(new int[] {}, rounded(ACCENT));
        button.setBackground(backgrounds);
        button.setOnClickListener(click);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(48));
        params.topMargin = dp(4);
        parent.addView(button, params);
        return button;
    }

    private GradientDrawable rounded(int color) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(dp(12));
        return shape;
    }

    private int dp(int value) {
        return (int) (getResources().getDisplayMetrics().density * value + 0.5f);
    }
}
