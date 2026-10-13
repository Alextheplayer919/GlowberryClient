package org.chimeramc.client.core.mods.inbuilt.overlay;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import org.chimeramc.client.R;
import org.chimeramc.client.launcher.controller.ControllerProfile;
import org.chimeramc.client.launcher.controller.ControllerProfileManager;
import org.chimeramc.client.launcher.controller.ControllerRumble;
import org.chimeramc.client.launcher.controller.ControllerType;
import org.chimeramc.client.launcher.controller.RemapLayer;
import org.chimeramc.client.launcher.controller.RumbleCurve;
import org.chimeramc.client.launcher.controller.StickCurve;
import org.chimeramc.client.launcher.controller.TriggerCurve;
import org.chimeramc.client.ui.views.CurvePreviewView;
import org.chimeramc.client.util.PersonalizationManager;
import org.chimeramc.client.ui.dialogs.CustomAlertDialog;

import java.util.List;

/**
 * The full controller profile editor, shared by the launcher's Controller tab and the in-game VIP
 * menu's controller section.
 *
 * <p>It was extracted from {@code ControllerSettingsFragment} so the two call sites cannot drift:
 * the in-game screen used to offer only a subset (profile chips + two stick sliders), which is the
 * "controller settings are limiting" report. Now both open exactly the same editor — dead zones,
 * sensitivities, vibration and rumble with a test button, anti-drift, button-map layers, and the
 * left/right stick and trigger curves with a live preview.
 *
 * <p>Builds a plain {@link View} and shows it in a {@link CustomAlertDialog}, so it needs only a
 * {@link Context} and the profile manager, not a Fragment.
 */
public final class ControllerProfileEditor {

    private final Context context;
    private final ControllerProfileManager profileManager;

    /** A capture in progress: the layer waiting for its modifier button. */
    private RemapLayer pendingLayerModifier;
    private Runnable layerModifierRefresh;
    private LinearLayout layerContainer;

    /** Notified after the profile is saved, so the caller can refresh its own view. */
    public interface SavedListener {
        void onSaved(ControllerProfile profile);
    }

    public ControllerProfileEditor(Context context, ControllerProfileManager profileManager) {
        this.context = context;
        this.profileManager = profileManager;
    }

    public boolean isCapturingModifier() {
        return pendingLayerModifier != null;
    }

    /**
     * Offers a hardware key press to a running modifier capture.
     *
     * @return true when the press was consumed by the capture (the caller must not also treat it as
     *         a normal input).
     */
    public boolean handleHardwareKey(int keyCode) {
        if (pendingLayerModifier == null) return false;
        RemapLayer layer = pendingLayerModifier;
        pendingLayerModifier = null;
        layer.setModifierKeyCode(keyCode);
        if (layerModifierRefresh != null) layerModifierRefresh.run();
        return true;
    }

    /** Opens the editor for the active profile of a controller type. */
    public void show(ControllerType type, SavedListener onSaved) {
        if (type == null) type = ControllerType.XBOX;
        ControllerProfile profile = profileManager.getActiveProfile(type);
        ControllerProfile working = profile == null ? new ControllerProfile("Default") : profile.copy();
        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(8), dp(4), dp(8), dp(4));

        content.addView(label(string(R.string.controller_editor_name_label)));
        android.widget.EditText nameInput = new android.widget.EditText(context);
        nameInput.setText(working.getName());
        content.addView(nameInput);

        content.addView(label(string(R.string.controller_editor_deadzone_label)));
        SeekBar leftDz = seekBar(working.getLeftDeadZone());
        SeekBar rightDz = seekBar(working.getRightDeadZone());
        content.addView(label(string(R.string.controller_editor_left_deadzone)));
        content.addView(leftDz);
        content.addView(label(string(R.string.controller_editor_right_deadzone)));
        content.addView(rightDz);

        content.addView(label(string(R.string.controller_editor_sensitivity_label)));
        SeekBar leftSens = seekBarSens(working.getLeftStickSensitivity());
        SeekBar rightSens = seekBarSens(working.getRightStickSensitivity());
        content.addView(label(string(R.string.controller_editor_left_sensitivity)));
        content.addView(leftSens);
        content.addView(label(string(R.string.controller_editor_right_sensitivity)));
        content.addView(rightSens);

        android.widget.Switch vibration = new android.widget.Switch(context);
        vibration.setChecked(working.isVibrationEnabled());
        content.addView(label(string(R.string.controller_editor_vibration)));
        content.addView(vibration);

        content.addView(subLabel(string(R.string.controller_editor_rumble_strength)));
        final SeekBar rumbleBar = seekBar(working.getRumbleStrength() / 100f);
        final TextView rumbleValue = subLabel(
                context.getString(R.string.controller_editor_rumble_percent, working.getRumbleStrength()));
        content.addView(rumbleBar);
        content.addView(rumbleValue);
        final android.widget.Button rumbleTest = new android.widget.Button(context);
        rumbleTest.setText(R.string.controller_editor_rumble_test);
        content.addView(rumbleTest);
        rumbleBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                rumbleValue.setText(context.getString(
                        R.string.controller_editor_rumble_percent, progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar sb) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar sb) {
            }
        });
        final ControllerType rumbleType = type;
        rumbleTest.setOnClickListener(v -> {
            int percent = vibration.isChecked() ? rumbleBar.getProgress() : 0;
            if (RumbleCurve.isOff(percent)) {
                android.widget.Toast.makeText(context, R.string.controller_editor_rumble_off,
                        android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
            ControllerRumble.play(context, rumbleType, RumbleCurve.amplitude(percent));
        });

        android.widget.Switch antiDrift = new android.widget.Switch(context);
        antiDrift.setChecked(working.isAntiDriftEnabled());
        content.addView(label(string(R.string.controller_antidrift_title)));
        content.addView(subLabel(string(R.string.controller_antidrift_summary)));
        content.addView(antiDrift);

        content.addView(label(string(R.string.controller_layers_title)));
        content.addView(subLabel(string(R.string.controller_layers_summary)));
        layerContainer = new LinearLayout(context);
        layerContainer.setOrientation(LinearLayout.VERTICAL);
        content.addView(layerContainer);
        layerModifierRefresh = () -> rebuildLayerRows(working, layerContainer);
        rebuildLayerRows(working, layerContainer);

        content.addView(label(string(R.string.controller_editor_stick_curve_label)));
        CurvePreviewView preview = new CurvePreviewView(context);
        preview.setAccentColor(new PersonalizationManager(context).getAccentColor());
        preview.setProfile(working);
        content.addView(preview, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(140)));

        content.addView(label(string(R.string.controller_editor_left_curve)));
        StickCurve leftCurve = working.getLeftCurve();
        Spinner leftStickPreset = stickPresetSpinner(leftCurve);
        Spinner leftStickKind = stickKindSpinner(leftCurve.getKind());
        SeekBar leftStickExp = exponentBar(leftCurve.getExponent(),
                StickCurve.MIN_EXPONENT, StickCurve.MAX_EXPONENT);
        content.addView(subLabel(string(R.string.controller_editor_curve_preset)));
        content.addView(leftStickPreset);
        content.addView(subLabel(string(R.string.controller_editor_curve_kind)));
        content.addView(leftStickKind);
        content.addView(subLabel(string(R.string.controller_editor_curve_exponent)));
        content.addView(leftStickExp);

        content.addView(label(string(R.string.controller_editor_right_curve)));
        StickCurve rightCurve = working.getRightCurve();
        Spinner rightStickPreset = stickPresetSpinner(rightCurve);
        Spinner rightStickKind = stickKindSpinner(rightCurve.getKind());
        SeekBar rightStickExp = exponentBar(rightCurve.getExponent(),
                StickCurve.MIN_EXPONENT, StickCurve.MAX_EXPONENT);
        content.addView(subLabel(string(R.string.controller_editor_curve_preset)));
        content.addView(rightStickPreset);
        content.addView(subLabel(string(R.string.controller_editor_curve_kind)));
        content.addView(rightStickKind);
        content.addView(subLabel(string(R.string.controller_editor_curve_exponent)));
        content.addView(rightStickExp);

        content.addView(label(string(R.string.controller_editor_trigger_label)));
        content.addView(label(string(R.string.controller_editor_left_trigger)));
        TriggerCurve leftTrigger = working.getLeftTriggerCurve();
        Spinner leftTriggerPreset = triggerPresetSpinner(leftTrigger);
        SeekBar leftTriggerDz = deadZoneBar(leftTrigger.getDeadZone());
        SeekBar leftTriggerExp = exponentBar(leftTrigger.getExponent(),
                TriggerCurve.MIN_EXPONENT, TriggerCurve.MAX_EXPONENT);
        content.addView(subLabel(string(R.string.controller_editor_trigger_preset)));
        content.addView(leftTriggerPreset);
        content.addView(subLabel(string(R.string.controller_editor_trigger_deadzone)));
        content.addView(leftTriggerDz);
        content.addView(subLabel(string(R.string.controller_editor_trigger_exponent)));
        content.addView(leftTriggerExp);

        content.addView(label(string(R.string.controller_editor_right_trigger)));
        TriggerCurve rightTrigger = working.getRightTriggerCurve();
        Spinner rightTriggerPreset = triggerPresetSpinner(rightTrigger);
        SeekBar rightTriggerDz = deadZoneBar(rightTrigger.getDeadZone());
        SeekBar rightTriggerExp = exponentBar(rightTrigger.getExponent(),
                TriggerCurve.MIN_EXPONENT, TriggerCurve.MAX_EXPONENT);
        content.addView(subLabel(string(R.string.controller_editor_trigger_preset)));
        content.addView(rightTriggerPreset);
        content.addView(subLabel(string(R.string.controller_editor_trigger_deadzone)));
        content.addView(rightTriggerDz);
        content.addView(subLabel(string(R.string.controller_editor_trigger_exponent)));
        content.addView(rightTriggerExp);

        Runnable pushToPreview = () -> {
            working.setLeftCurve(new StickCurve(
                    StickCurve.Kind.values()[leftStickKind.getSelectedItemPosition()],
                    exponentFrom(leftStickExp, StickCurve.MIN_EXPONENT, StickCurve.MAX_EXPONENT)));
            working.setRightCurve(new StickCurve(
                    StickCurve.Kind.values()[rightStickKind.getSelectedItemPosition()],
                    exponentFrom(rightStickExp, StickCurve.MIN_EXPONENT, StickCurve.MAX_EXPONENT)));
            working.setLeftTriggerCurve(new TriggerCurve(
                    deadZoneFrom(leftTriggerDz),
                    exponentFrom(leftTriggerExp, TriggerCurve.MIN_EXPONENT, TriggerCurve.MAX_EXPONENT)));
            working.setRightTriggerCurve(new TriggerCurve(
                    deadZoneFrom(rightTriggerDz),
                    exponentFrom(rightTriggerExp, TriggerCurve.MIN_EXPONENT, TriggerCurve.MAX_EXPONENT)));
            preview.setProfile(working);
        };
        leftStickPreset.setOnItemSelectedListener(primedPresetListener(
                presetListener(leftStickKind, leftStickExp, pushToPreview)));
        rightStickPreset.setOnItemSelectedListener(primedPresetListener(
                presetListener(rightStickKind, rightStickExp, pushToPreview)));
        leftTriggerPreset.setOnItemSelectedListener(primedPresetListener(
                triggerPresetListener(leftTriggerDz, leftTriggerExp, pushToPreview)));
        rightTriggerPreset.setOnItemSelectedListener(primedPresetListener(
                triggerPresetListener(rightTriggerDz, rightTriggerExp, pushToPreview)));
        for (SeekBar bar : new SeekBar[]{leftStickExp, rightStickExp, leftTriggerDz, leftTriggerExp,
                rightTriggerDz, rightTriggerExp}) {
            bar.setOnSeekBarChangeListener(changeListener(pushToPreview));
        }
        for (Spinner spinner : new Spinner[]{leftStickKind, rightStickKind}) {
            spinner.setOnItemSelectedListener(selectionListener(pushToPreview));
        }

        android.widget.ScrollView scroller = new android.widget.ScrollView(context);
        scroller.addView(content);
        int maxHeight = (int) (context.getResources().getDisplayMetrics().heightPixels * 0.65f);
        scroller.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Math.min(maxHeight, dp(560))));

        final ControllerType saveType = type;
        new CustomAlertDialog(context)
                .setTitleText(string(R.string.controller_edit))
                .setCustomView(scroller)
                .setPositiveButton(string(R.string.controller_save), v -> {
                    working.setName(nameInput.getText().toString());
                    working.setLeftDeadZone(leftDz.getProgress() / 100f);
                    working.setRightDeadZone(rightDz.getProgress() / 100f);
                    working.setLeftStickSensitivity(0.25f + leftSens.getProgress() / 100f * 2.75f);
                    working.setRightStickSensitivity(0.25f + rightSens.getProgress() / 100f * 2.75f);
                    working.setVibrationEnabled(vibration.isChecked());
                    working.setRumbleStrength(rumbleBar.getProgress());
                    working.setAntiDriftEnabled(antiDrift.isChecked());
                    pushToPreview.run();
                    int active = profileManager.getActiveSlot(saveType);
                    List<ControllerProfile> profiles = profileManager.getProfiles(saveType);
                    if (active >= 0 && active < profiles.size()) {
                        profiles.set(active, working);
                        profileManager.saveProfiles(saveType, profiles);
                        org.chimeramc.client.launcher.controller.ControllerInputProcessor
                                .setActiveProfile(saveType, working);
                    }
                    org.chimeramc.client.ui.animation.UiTouchFeedback.confirm(context);
                    if (onSaved != null) onSaved.onSaved(working);
                })
                .setNegativeButton(string(R.string.controller_cancel), v -> {
                })
                .show();
    }

    // --- Helpers (ported from ControllerSettingsFragment) ------------------------------------

    private void rebuildLayerRows(ControllerProfile working, LinearLayout container) {
        if (container == null) return;
        container.removeAllViews();

        for (int i = 0; i < working.getRemapLayers().size(); i++) {
            final int index = i;
            RemapLayer layer = working.getRemapLayers().get(i);
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);

            TextView name = new TextView(context);
            name.setText(context.getString(R.string.controller_layer_row, layer.getName(),
                    modifierLabel(layer.getModifierKeyCode())));
            name.setTextSize(12f);
            name.setTextColor(context.getColor(R.color.on_surface));
            row.addView(name, new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            android.widget.Button set = new android.widget.Button(context);
            set.setText(R.string.controller_layer_set);
            set.setTextSize(11f);
            set.setOnClickListener(v -> startLayerModifierCapture(layer));
            row.addView(set);

            android.widget.Button clear = new android.widget.Button(context);
            clear.setText(R.string.controller_layer_clear);
            clear.setTextSize(11f);
            clear.setOnClickListener(v -> {
                layer.setModifierKeyCode(RemapLayer.NO_MODIFIER);
                rebuildLayerRows(working, container);
            });
            row.addView(clear);

            android.widget.Button remove = new android.widget.Button(context);
            remove.setText(R.string.controller_layer_delete);
            remove.setTextSize(11f);
            remove.setOnClickListener(v -> {
                working.removeRemapLayer(index);
                rebuildLayerRows(working, container);
            });
            row.addView(remove);

            container.addView(row);
        }

        android.widget.Button add = new android.widget.Button(context);
        add.setText(R.string.controller_layer_add);
        add.setOnClickListener(v -> {
            RemapLayer layer = new RemapLayer(context.getString(
                    R.string.controller_layer_default_name, working.getRemapLayers().size() + 1));
            layer.getRemaps().putAll(working.getButtonRemaps());
            working.addRemapLayer(layer);
            rebuildLayerRows(working, container);
        });
        container.addView(add);
    }

    private void startLayerModifierCapture(RemapLayer layer) {
        pendingLayerModifier = layer;
        android.widget.Toast.makeText(context, R.string.controller_layer_press_modifier,
                android.widget.Toast.LENGTH_SHORT).show();
    }

    private String modifierLabel(int keyCode) {
        if (keyCode == RemapLayer.NO_MODIFIER) {
            return context.getString(R.string.controller_layer_no_modifier);
        }
        String name = android.view.KeyEvent.keyCodeToString(keyCode);
        return name.startsWith("KEYCODE_") ? name.substring(8).replace('_', ' ') : name;
    }

    private AdapterView.OnItemSelectedListener presetListener(Spinner kindSpinner,
                                                              SeekBar exponentBar,
                                                              Runnable onChange) {
        return new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                StickCurve.Preset preset = StickCurve.Preset.values()[position];
                kindSpinner.setSelection(preset.toCurve().getKind().ordinal());
                exponentBar.setProgress(progressFor(preset.toCurve().getExponent(),
                        StickCurve.MIN_EXPONENT, StickCurve.MAX_EXPONENT));
                onChange.run();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        };
    }

    private AdapterView.OnItemSelectedListener triggerPresetListener(SeekBar deadZoneBar,
                                                                     SeekBar exponentBar,
                                                                     Runnable onChange) {
        return new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                TriggerCurve.Preset preset = TriggerCurve.Preset.values()[position];
                TriggerCurve curve = preset.toCurve();
                deadZoneBar.setProgress((int) (curve.getDeadZone() * 100f));
                exponentBar.setProgress(progressFor(curve.getExponent(),
                        TriggerCurve.MIN_EXPONENT, TriggerCurve.MAX_EXPONENT));
                onChange.run();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        };
    }

    /**
     * Swallows the preset spinner's first callback so it cannot clobber a custom curve: a Spinner
     * always fires once after layout at whatever position is selected, and a custom curve sits on
     * position 0. The controls are initialised from the curve first, so dropping it loses nothing.
     */
    private AdapterView.OnItemSelectedListener primedPresetListener(
            AdapterView.OnItemSelectedListener delegate) {
        return new AdapterView.OnItemSelectedListener() {
            private boolean primed = false;

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!primed) {
                    primed = true;
                    return;
                }
                delegate.onItemSelected(parent, view, position, id);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                delegate.onNothingSelected(parent);
            }
        };
    }

    private static int progressFor(float value, float min, float max) {
        float clamped = Math.max(min, Math.min(max, value));
        return (int) (((clamped - min) / (max - min)) * 100f);
    }

    private SeekBar.OnSeekBarChangeListener changeListener(Runnable onChange) {
        return new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                onChange.run();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        };
    }

    private AdapterView.OnItemSelectedListener selectionListener(Runnable onChange) {
        return new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                onChange.run();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        };
    }

    private TextView label(String text) {
        TextView tv = new TextView(context);
        tv.setText(text);
        tv.setTextSize(13f);
        tv.setTextColor(context.getColor(R.color.on_surface));
        tv.setPadding(0, dp(6), 0, dp(2));
        return tv;
    }

    private TextView subLabel(String text) {
        TextView tv = new TextView(context);
        tv.setText(text);
        tv.setTextSize(12f);
        tv.setTextColor(context.getColor(R.color.text_secondary));
        tv.setPadding(0, dp(4), 0, 0);
        return tv;
    }

    private SeekBar seekBar(float value) {
        SeekBar sb = new SeekBar(context);
        sb.setMax(100);
        sb.setProgress((int) (value * 100f));
        return sb;
    }

    private SeekBar seekBarSens(float value) {
        SeekBar sb = new SeekBar(context);
        sb.setMax(100);
        float scaled = (value - 0.25f) / 2.75f;
        sb.setProgress((int) (scaled * 100f));
        return sb;
    }

    private SeekBar exponentBar(float value, float min, float max) {
        SeekBar sb = new SeekBar(context);
        sb.setMax(100);
        float clamped = Math.max(min, Math.min(max, value));
        sb.setProgress((int) (((clamped - min) / (max - min)) * 100f));
        return sb;
    }

    private float exponentFrom(SeekBar bar, float min, float max) {
        return min + (bar.getProgress() / 100f) * (max - min);
    }

    private SeekBar deadZoneBar(float value) {
        SeekBar sb = new SeekBar(context);
        sb.setMax(100);
        sb.setProgress((int) (TriggerCurve.clampDeadZone(value) * 100f));
        return sb;
    }

    private float deadZoneFrom(SeekBar bar) {
        return bar.getProgress() / 100f;
    }

    private Spinner stickPresetSpinner(StickCurve curve) {
        Spinner spinner = new Spinner(context);
        StickCurve.Preset[] presets = StickCurve.Preset.values();
        String[] names = new String[presets.length];
        for (int i = 0; i < names.length; i++) names[i] = stickPresetName(presets[i]);
        spinner.setAdapter(new ArrayAdapter<>(context,
                android.R.layout.simple_spinner_dropdown_item, names));
        StickCurve.Preset match = curve == null ? null : curve.matchingPreset();
        if (match != null) spinner.setSelection(match.ordinal());
        return spinner;
    }

    private Spinner stickKindSpinner(StickCurve.Kind kind) {
        Spinner spinner = new Spinner(context);
        StickCurve.Kind[] kinds = StickCurve.Kind.values();
        String[] names = new String[kinds.length];
        for (int i = 0; i < names.length; i++) names[i] = stickKindName(kinds[i]);
        spinner.setAdapter(new ArrayAdapter<>(context,
                android.R.layout.simple_spinner_dropdown_item, names));
        spinner.setSelection(kind == null ? 0 : kind.ordinal());
        return spinner;
    }

    private Spinner triggerPresetSpinner(TriggerCurve curve) {
        Spinner spinner = new Spinner(context);
        TriggerCurve.Preset[] presets = TriggerCurve.Preset.values();
        String[] names = new String[presets.length];
        for (int i = 0; i < names.length; i++) names[i] = triggerPresetName(presets[i]);
        spinner.setAdapter(new ArrayAdapter<>(context,
                android.R.layout.simple_spinner_dropdown_item, names));
        TriggerCurve.Preset match = curve == null ? null : curve.matchingPreset();
        if (match != null) spinner.setSelection(match.ordinal());
        return spinner;
    }

    private String stickPresetName(StickCurve.Preset preset) {
        switch (preset) {
            case PRECISION: return string(R.string.controller_preset_precision);
            case BALANCED: return string(R.string.controller_preset_balanced);
            case AGGRESSIVE: return string(R.string.controller_preset_aggressive);
            case INSTANT: return string(R.string.controller_preset_instant);
            case LINEAR:
            default: return string(R.string.controller_preset_linear);
        }
    }

    private String stickKindName(StickCurve.Kind kind) {
        switch (kind) {
            case EXPONENTIAL: return string(R.string.controller_curve_kind_exponential);
            case EASE_OUT: return string(R.string.controller_curve_kind_ease_out);
            case SIGMOID: return string(R.string.controller_curve_kind_sigmoid);
            case LINEAR:
            default: return string(R.string.controller_curve_kind_linear);
        }
    }

    private String triggerPresetName(TriggerCurve.Preset preset) {
        switch (preset) {
            case SOFT: return string(R.string.controller_trigger_preset_soft);
            case HAIR_TRIGGER: return string(R.string.controller_trigger_preset_hair);
            case DELIBERATE: return string(R.string.controller_trigger_preset_deliberate);
            case LINEAR:
            default: return string(R.string.controller_trigger_preset_linear);
        }
    }

    private String string(int res) {
        return context.getString(res);
    }

    private int dp(int value) {
        return (int) (value * context.getResources().getDisplayMetrics().density + 0.5f);
    }
}
