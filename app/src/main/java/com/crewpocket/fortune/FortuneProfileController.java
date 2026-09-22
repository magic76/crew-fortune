package com.crewpocket.fortune;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

final class FortuneProfileController {
    private final MainActivity host;
    private final BirthPlaceSearchClient birthPlaceSearchClient =
            new BirthPlaceSearchClient();

    private EditText birthInput;
    private EditText birthTimeInput;
    private LinearLayout genderRow;
    private Button maleButton;
    private Button femaleButton;
    private String selectedGender = "";

    private LinearLayout vedicLocationSection;
    private EditText birthPlaceNameInput;
    private EditText latitudeInput;
    private EditText longitudeInput;
    private Button timeZoneButton;
    private Button geocodeButton;
    private TextView geocodeStatus;
    private LinearLayout vedicCoordinateFields;
    private String selectedTimeZoneId = "+08:00";
    private boolean geocodingBirthPlace;

    FortuneProfileController(MainActivity host) {
        this.host = host;
    }

    void addFields(LinearLayout form) {
        form.addView(host.label("基本資料"), host.marginTop(8));

        birthInput = host.input("點選生日  →");
        birthInput.setFocusable(false);
        birthInput.setClickable(true);
        birthInput.setTextColor(MainActivity.ACCENT);
        birthInput.setBackground(host.roundBorder(
                Color.rgb(31, 24, 49),
                Color.rgb(150, 116, 206),
                14,
                1));
        birthInput.setOnClickListener(v -> showDatePicker());

        birthTimeInput = host.input("點選出生時間  →");
        birthTimeInput.setFocusable(false);
        birthTimeInput.setClickable(true);
        birthTimeInput.setTextColor(MainActivity.ACCENT);
        birthTimeInput.setBackground(host.roundBorder(
                Color.rgb(31, 24, 49),
                Color.rgb(150, 116, 206),
                14,
                1));
        birthTimeInput.setOnClickListener(v -> showTimePicker());

        LinearLayout dateTimeRow = new LinearLayout(host);
        dateTimeRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams dateLp =
                new LinearLayout.LayoutParams(0, host.dp(48), 1.15f);
        dateLp.rightMargin = host.dp(6);
        dateTimeRow.addView(birthInput, dateLp);
        dateTimeRow.addView(
                birthTimeInput,
                new LinearLayout.LayoutParams(
                        0, host.dp(48), 0.85f));
        form.addView(dateTimeRow, host.marginTop(8));

        TextView sharedHint = host.text(
                "基本資料固定顯示，切換命理不會消失；不同系統只取它需要的欄位。",
                10,
                MainActivity.MUTED,
                false);
        sharedHint.setLineSpacing(host.dp(2), 1f);
        form.addView(sharedHint, host.marginTop(4));

        TextView genderLabel = host.text(
                "性別 · 八字使用",
                11,
                MainActivity.MUTED,
                true);
        form.addView(genderLabel, host.marginTop(7));

        genderRow = new LinearLayout(host);
        genderRow.setOrientation(LinearLayout.HORIZONTAL);
        maleButton = host.genderButton("男");
        femaleButton = host.genderButton("女");
        maleButton.setOnClickListener(v -> selectGender("male"));
        femaleButton.setOnClickListener(v -> selectGender("female"));

        LinearLayout.LayoutParams genderLp =
                new LinearLayout.LayoutParams(0, host.dp(44), 1f);
        genderLp.rightMargin = host.dp(6);
        genderRow.addView(maleButton, genderLp);
        genderRow.addView(
                femaleButton,
                new LinearLayout.LayoutParams(0, host.dp(44), 1f));
        form.addView(genderRow, host.marginTop(6));

        addVedicLocationFields(form);
        addPresetActions(form);
    }

    void onModeSelected(FortuneMode mode) {
        if (birthTimeInput != null) {
            birthTimeInput.setVisibility(View.VISIBLE);
        }
        if (genderRow != null) {
            genderRow.setVisibility(View.VISIBLE);
        }
        if (vedicLocationSection != null) {
            vedicLocationSection.setVisibility(View.VISIBLE);
        }
    }

    FortuneProfile buildProfile(FortuneMode mode) {
        BirthPlace birthPlace = null;
        if (mode == FortuneMode.VEDIC_ASTROLOGY) {
            String latitudeText = text(latitudeInput);
            String longitudeText = text(longitudeInput);
            String zoneText = selectedTimeZoneId == null
                    ? ""
                    : selectedTimeZoneId.trim();
            if (latitudeText.isEmpty()
                    || longitudeText.isEmpty()
                    || zoneText.isEmpty()) {
                throw new IllegalArgumentException(
                        "印度星盤請先搜尋並選擇出生城市");
            }

            final double latitude;
            final double longitude;
            try {
                latitude = Double.parseDouble(latitudeText);
                longitude = Double.parseDouble(longitudeText);
            } catch (NumberFormatException error) {
                throw new IllegalArgumentException(
                        "出生地座標格式不正確",
                        error);
            }

            birthPlace = new BirthPlace(
                    text(birthPlaceNameInput),
                    latitude,
                    longitude,
                    zoneText);
        }

        return new FortuneProfile(
                "",
                text(birthInput),
                text(birthTimeInput),
                selectedGender,
                birthPlace);
    }

    String selectedTimeZoneId() {
        return selectedTimeZoneId;
    }

    FortunePreset currentPreset(FortuneMode mode) {
        return new FortunePreset(
                "",
                text(birthInput),
                text(birthTimeInput),
                selectedGender,
                mode,
                text(birthPlaceNameInput),
                text(latitudeInput),
                text(longitudeInput),
                selectedTimeZoneId);
    }

    void saveState(Bundle outState) {
        outState.putString("state_birth_date", text(birthInput));
        outState.putString("state_birth_time", text(birthTimeInput));
        outState.putString("state_gender", selectedGender);
        outState.putString(
                "state_birth_place_name",
                text(birthPlaceNameInput));
        outState.putString("state_latitude", text(latitudeInput));
        outState.putString("state_longitude", text(longitudeInput));
        outState.putString("state_timezone", selectedTimeZoneId);
    }

    void restoreState(Bundle state) {
        if (state == null) return;
        birthInput.setText(state.getString("state_birth_date", ""));
        birthTimeInput.setText(state.getString("state_birth_time", ""));
        birthPlaceNameInput.setText(
                state.getString("state_birth_place_name", ""));
        latitudeInput.setText(state.getString("state_latitude", ""));
        longitudeInput.setText(state.getString("state_longitude", ""));
        setTimeZoneSelection(
                state.getString("state_timezone", "+08:00"));

        String gender = state.getString("state_gender", "");
        if (gender.isEmpty()) {
            clearGender();
        } else {
            selectGender(gender);
        }
    }

    void restoreLastProfile() {
        FortunePreset preset = FortunePresetStore.loadLast(host);
        if (preset != null) applyPreset(preset);
    }

    void saveCurrentPreset(FortuneMode mode) {
        FortunePreset preset = currentPreset(mode);
        if (preset.birthDate.isEmpty()) {
            Toast.makeText(
                    host,
                    "先選生日再儲存 preset",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if ((preset.mode == FortuneMode.BA_ZI
                || preset.mode == FortuneMode.VEDIC_ASTROLOGY)
                && preset.birthTime.isEmpty()) {
            Toast.makeText(
                    host,
                    preset.mode == FortuneMode.BA_ZI
                            ? "八字 preset 需要出生時間"
                            : "印度星盤 preset 需要出生時間",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (preset.mode == FortuneMode.VEDIC_ASTROLOGY
                && preset.birthPlaceOrNull() == null) {
            Toast.makeText(
                    host,
                    "印度星盤常用資料需要先選擇出生城市",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (preset.mode == FortuneMode.BA_ZI
                && preset.gender.isEmpty()) {
            Toast.makeText(
                    host,
                    "八字 preset 需要選擇性別",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        FortunePresetStore.savePreset(host, preset);
        OperationLog.add(host, "PRESET_SAVED", preset.label());
        Toast.makeText(
                host,
                "已儲存：" + preset.label(),
                Toast.LENGTH_SHORT).show();
    }

    void showPresetPicker() {
        final List<FortunePreset> presets =
                FortunePresetStore.loadPresets(host);
        if (presets.isEmpty()) {
            Toast.makeText(
                    host,
                    "目前還沒有 preset",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        String[] labels = new String[presets.size()];
        for (int i = 0; i < presets.size(); i++) {
            labels[i] = presets.get(i).label();
        }

        new AlertDialog.Builder(host)
                .setTitle("選擇常用資料")
                .setItems(labels, (dialog, which) -> {
                    OperationLog.add(
                            host,
                            "PRESET_LOADED",
                            presets.get(which).label());
                    applyPreset(presets.get(which));
                })
                .setNeutralButton("清除全部", (dialog, which) -> {
                    FortunePresetStore.clearPresets(host);
                    OperationLog.add(host, "PRESETS_CLEARED", "");
                    Toast.makeText(
                            host,
                            "已清除 presets",
                            Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    boolean needsBirthPlaceGeocoding() {
        if (text(birthPlaceNameInput).isEmpty()) return false;
        return text(latitudeInput).isEmpty()
                || text(longitudeInput).isEmpty();
    }

    void geocodeBirthPlace(boolean calculateAfterSuccess) {
        if (geocodingBirthPlace) return;

        String query = text(birthPlaceNameInput);
        if (query.length() < 2) {
            Toast.makeText(
                    host,
                    "請輸入至少 2 個字的出生城市",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        android.view.inputmethod.InputMethodManager keyboard =
                (android.view.inputmethod.InputMethodManager)
                        host.getSystemService(
                                android.content.Context.INPUT_METHOD_SERVICE);
        if (keyboard != null && birthPlaceNameInput != null) {
            keyboard.hideSoftInputFromWindow(
                    birthPlaceNameInput.getWindowToken(),
                    0);
        }

        geocodingBirthPlace = true;
        if (geocodeButton != null) {
            geocodeButton.setEnabled(false);
            geocodeButton.setText("搜尋中…");
        }
        if (geocodeStatus != null) {
            geocodeStatus.setText("正在搜尋「" + query + "」…");
            geocodeStatus.setTextColor(MainActivity.MUTED);
        }
        OperationLog.add(host, "VEDIC_CITY_SEARCH_START", query);

        new Thread(() -> {
            try {
                final List<BirthPlaceSearchClient.Result> results =
                        birthPlaceSearchClient.search(query);
                host.runOnUiThread(() -> {
                    finishBirthPlaceSearchUi();
                    if (results.isEmpty()) {
                        if (geocodeStatus != null) {
                            geocodeStatus.setText(
                                    "找不到符合的城市，請換較完整的名稱再試一次");
                            geocodeStatus.setTextColor(
                                    Color.rgb(255, 150, 150));
                        }
                        OperationLog.add(
                                host,
                                "VEDIC_CITY_SEARCH_EMPTY",
                                query);
                        return;
                    }
                    showBirthPlaceSearchResults(
                            results,
                            calculateAfterSuccess);
                });
            } catch (Exception error) {
                host.runOnUiThread(() -> {
                    finishBirthPlaceSearchUi();
                    if (geocodeStatus != null) {
                        geocodeStatus.setText(
                                "城市搜尋失敗，請稍後重試");
                        geocodeStatus.setTextColor(
                                Color.rgb(255, 150, 150));
                    }
                    OperationLog.add(
                            host,
                            "VEDIC_CITY_SEARCH_FAILED",
                            host.safeErrorMessage(error));
                    Toast.makeText(
                            host,
                            "城市搜尋暫時無法使用，請稍後重試",
                            Toast.LENGTH_LONG).show();
                });
            }
        }, "crew-fortune-city-search").start();
    }

    private void addVedicLocationFields(LinearLayout form) {
        vedicLocationSection = host.column();

        TextView cityLabel = host.text(
                "出生城市",
                12,
                MainActivity.TEXT,
                true);
        vedicLocationSection.addView(cityLabel);

        TextView vedicRule = host.text(
                "搜尋城市後，座標與時區會自動設定；不用另外輸入。",
                10,
                MainActivity.MUTED,
                false);
        vedicRule.setLineSpacing(host.dp(2), 1f);
        vedicLocationSection.addView(
                vedicRule,
                host.marginTop(2));

        birthPlaceNameInput = host.input(
                "新北市、Bangkok、Tokyo…");
        birthPlaceNameInput.setBackgroundColor(Color.TRANSPARENT);
        birthPlaceNameInput.setPadding(
                host.dp(12), 0, host.dp(8), 0);
        birthPlaceNameInput.setImeOptions(
                android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);
        birthPlaceNameInput.setOnEditorActionListener(
                (view, actionId, event) -> {
                    if (actionId
                            == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                        geocodeBirthPlace(false);
                        return true;
                    }
                    return false;
                });

        birthPlaceNameInput.addTextChangedListener(
                new android.text.TextWatcher() {
                    @Override public void beforeTextChanged(
                            CharSequence value,
                            int start,
                            int count,
                            int after) {}

                    @Override public void onTextChanged(
                            CharSequence value,
                            int start,
                            int before,
                            int count) {}

                    @Override public void afterTextChanged(
                            android.text.Editable value) {
                        if (latitudeInput != null) {
                            latitudeInput.setText("");
                        }
                        if (longitudeInput != null) {
                            longitudeInput.setText("");
                        }
                        if (geocodeStatus != null) {
                            geocodeStatus.setText(
                                    "城市有變更時請重新搜尋；座標與時區會自動更新。");
                            geocodeStatus.setTextColor(
                                    MainActivity.MUTED);
                        }
                    }
                });

        geocodeButton = host.primaryButton("搜尋");
        geocodeButton.setTextSize(13);
        geocodeButton.setMinHeight(0);
        geocodeButton.setMinimumHeight(0);
        geocodeButton.setPadding(
                host.dp(14), 0, host.dp(14), 0);
        geocodeButton.setOnClickListener(
                v -> geocodeBirthPlace(false));

        LinearLayout citySearchRow = new LinearLayout(host);
        citySearchRow.setOrientation(LinearLayout.HORIZONTAL);
        citySearchRow.setGravity(Gravity.CENTER_VERTICAL);
        citySearchRow.setPadding(
                host.dp(2), host.dp(2), host.dp(2), host.dp(2));
        citySearchRow.setBackground(host.roundBorder(
                Color.rgb(31, 24, 49),
                Color.rgb(150, 116, 206),
                15,
                1));

        LinearLayout.LayoutParams cityInputLp =
                new LinearLayout.LayoutParams(
                        0,
                        host.dp(48),
                        1f);
        citySearchRow.addView(
                birthPlaceNameInput,
                cityInputLp);

        LinearLayout.LayoutParams searchButtonLp =
                new LinearLayout.LayoutParams(
                        host.dp(82),
                        host.dp(44));
        citySearchRow.addView(
                geocodeButton,
                searchButtonLp);

        geocodeStatus = host.text(
                "印度星盤會使用；八字／塔羅可先不填。",
                10,
                MainActivity.MUTED,
                false);
        geocodeStatus.setLineSpacing(host.dp(2), 1f);

        // Coordinates/timezone are implementation details. Keep them in state only,
        // not as separate user-facing controls.
        latitudeInput = host.input("");
        longitudeInput = host.input("");
        latitudeInput.setVisibility(View.GONE);
        longitudeInput.setVisibility(View.GONE);
        vedicCoordinateFields = host.column();
        vedicCoordinateFields.setVisibility(View.GONE);

        vedicLocationSection.addView(
                citySearchRow,
                host.marginTop(6));
        vedicLocationSection.addView(
                geocodeStatus,
                host.marginTop(4));
        vedicLocationSection.setVisibility(View.VISIBLE);
        form.addView(vedicLocationSection, host.marginTop(8));
    }

    private void addPresetActions(LinearLayout form) {
        LinearLayout presetRow = new LinearLayout(host);
        presetRow.setOrientation(LinearLayout.HORIZONTAL);

        Button choosePreset =
                host.secondaryButton("常用資料  →");
        Button savePreset =
                host.secondaryButton("儲存常用資料");

        choosePreset.setOnClickListener(
                v -> showPresetPicker());
        savePreset.setOnClickListener(
                v -> saveCurrentPreset(
                        host.aiModeForController()));

        LinearLayout.LayoutParams presetLp =
                new LinearLayout.LayoutParams(0, host.dp(44), 1f);
        presetLp.rightMargin = host.dp(6);
        presetRow.addView(choosePreset, presetLp);
        presetRow.addView(
                savePreset,
                new LinearLayout.LayoutParams(
                        0,
                        host.dp(44),
                        1f));
        form.addView(presetRow, host.marginTop(6));
    }

    void applyHistoryPreset(FortunePreset preset) {
        applyPreset(preset);
    }

    private void applyPreset(FortunePreset preset) {
        if (preset == null) return;

        birthInput.setText(preset.birthDate);
        birthTimeInput.setText(preset.birthTime);
        birthPlaceNameInput.setText(preset.birthPlaceName);
        latitudeInput.setText(preset.latitude);
        longitudeInput.setText(preset.longitude);
        setTimeZoneSelection(preset.timeZoneId);

        host.selectModeFromProfileController(preset.mode);
        if (!preset.gender.isEmpty()) {
            selectGender(preset.gender);
        } else {
            clearGender();
        }
    }

    private void selectGender(String gender) {
        selectedGender = gender == null ? "" : gender;
        OperationLog.add(host, "GENDER_SELECTED", selectedGender);
        boolean male = "male".equals(selectedGender);

        maleButton.setBackground(host.roundBorder(
                male
                        ? MainActivity.ACCENT
                        : Color.rgb(31, 24, 49),
                male
                        ? MainActivity.ACCENT
                        : Color.rgb(150, 116, 206),
                14,
                male ? 2 : 1));
        femaleButton.setBackground(host.roundBorder(
                !male
                        ? MainActivity.ACCENT
                        : Color.rgb(31, 24, 49),
                !male
                        ? MainActivity.ACCENT
                        : Color.rgb(150, 116, 206),
                14,
                !male ? 2 : 1));
        maleButton.setTextColor(
                male
                        ? Color.rgb(30, 22, 46)
                        : MainActivity.ACCENT);
        femaleButton.setTextColor(
                !male
                        ? Color.rgb(30, 22, 46)
                        : MainActivity.ACCENT);
    }

    private void clearGender() {
        selectedGender = "";
        if (maleButton == null || femaleButton == null) return;
        maleButton.setBackground(host.roundBorder(
                Color.rgb(31, 24, 49),
                Color.rgb(150, 116, 206),
                14,
                1));
        femaleButton.setBackground(host.roundBorder(
                Color.rgb(31, 24, 49),
                Color.rgb(150, 116, 206),
                14,
                1));
        maleButton.setTextColor(MainActivity.ACCENT);
        femaleButton.setTextColor(MainActivity.ACCENT);
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        String raw = text(birthInput);
        if (raw.matches("\\d{4}-\\d{2}-\\d{2}")) {
            try {
                String[] parts = raw.split("-");
                calendar.set(
                        Integer.parseInt(parts[0]),
                        Integer.parseInt(parts[1]) - 1,
                        Integer.parseInt(parts[2]));
            } catch (Exception ignored) {}
        }

        DatePickerDialog dialog = new DatePickerDialog(
                host,
                (view, year, month, day) -> {
                    String value = String.format(
                            Locale.US,
                            "%04d-%02d-%02d",
                            year,
                            month + 1,
                            day);
                    birthInput.setText(value);
                    OperationLog.add(
                            host,
                            "BIRTH_DATE_SELECTED",
                            value);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        dialog.getDatePicker().setMaxDate(
                System.currentTimeMillis());
        dialog.show();
    }

    private void showTimePicker() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        String raw = text(birthTimeInput);
        if (raw.matches("\\d{2}:\\d{2}")) {
            try {
                String[] parts = raw.split(":");
                hour = Integer.parseInt(parts[0]);
                minute = Integer.parseInt(parts[1]);
            } catch (Exception ignored) {}
        }

        new TimePickerDialog(
                host,
                (view, selectedHour, selectedMinute) -> {
                    String value = String.format(
                            Locale.US,
                            "%02d:%02d",
                            selectedHour,
                            selectedMinute);
                    birthTimeInput.setText(value);
                    OperationLog.add(
                            host,
                            "BIRTH_TIME_SELECTED",
                            value);
                },
                hour,
                minute,
                true).show();
    }

    private void showBirthPlaceSearchResults(
            List<BirthPlaceSearchClient.Result> results,
            boolean calculateAfterSuccess) {
        String[] labels = new String[results.size()];
        for (int i = 0; i < results.size(); i++) {
            BirthPlaceSearchClient.Result item = results.get(i);
            labels[i] =
                    item.displayName()
                            + "\n"
                            + item.detail();
        }

        new AlertDialog.Builder(host)
                .setTitle("選擇出生城市")
                .setItems(labels, (dialog, which) -> {
                    BirthPlaceSearchClient.Result selected =
                            results.get(which);
                    applyBirthPlaceSearchResult(selected);
                    if (calculateAfterSuccess) {
                        host.calculateFromProfileController();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void finishBirthPlaceSearchUi() {
        geocodingBirthPlace = false;
        if (geocodeButton != null) {
            geocodeButton.setEnabled(true);
            geocodeButton.setText("搜尋");
        }
    }

    private void applyBirthPlaceSearchResult(
            BirthPlaceSearchClient.Result result) {
        if (result == null) return;

        birthPlaceNameInput.setText(result.displayName());
        latitudeInput.setText(formatCoordinate(result.latitude));
        longitudeInput.setText(formatCoordinate(result.longitude));
        setTimeZoneSelection(result.timezone);

        if (geocodeStatus != null) {
            geocodeStatus.setText(
                    "已選：" + result.displayName()
                            + " · 已自動設定時區");
            geocodeStatus.setTextColor(MainActivity.GOLD);
        }

        OperationLog.add(
                host,
                "VEDIC_CITY_SELECTED",
                result.displayName()
                        + " · "
                        + formatCoordinate(result.latitude)
                        + ","
                        + formatCoordinate(result.longitude)
                        + " · "
                        + result.timezone);
    }

    private void showTimeZonePicker() {
        final String[] labels = new String[] {
                "UTC+08:00（預設）",
                "Asia/Taipei（台灣）",
                "UTC+09:00",
                "Asia/Tokyo（日本）",
                "UTC+07:00",
                "Asia/Bangkok（泰國）",
                "UTC+05:30",
                "Asia/Kolkata（印度）",
                "UTC+00:00",
                "Europe/London（英國）",
                "UTC+01:00",
                "Europe/Paris（中歐）",
                "UTC-05:00",
                "America/New_York（美東）",
                "UTC-08:00",
                "America/Los_Angeles（美西）"
        };
        final String[] values = new String[] {
                "+08:00",
                "Asia/Taipei",
                "+09:00",
                "Asia/Tokyo",
                "+07:00",
                "Asia/Bangkok",
                "+05:30",
                "Asia/Kolkata",
                "UTC",
                "Europe/London",
                "+01:00",
                "Europe/Paris",
                "-05:00",
                "America/New_York",
                "-08:00",
                "America/Los_Angeles"
        };

        int selected = 0;
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(selectedTimeZoneId)) {
                selected = i;
                break;
            }
        }

        new AlertDialog.Builder(host)
                .setTitle("選擇出生地時區")
                .setSingleChoiceItems(
                        labels,
                        selected,
                        (dialog, which) -> {
                            setTimeZoneSelection(values[which]);
                            OperationLog.add(
                                    host,
                                    "VEDIC_TIMEZONE_SELECTED",
                                    values[which]);
                            dialog.dismiss();
                        })
                .setNegativeButton("取消", null)
                .show();
    }

    private void setTimeZoneSelection(String value) {
        String normalized =
                value == null ? "" : value.trim();
        if (normalized.isEmpty()) normalized = "+08:00";
        selectedTimeZoneId = normalized;

        if (timeZoneButton == null) return;

        String label = normalized;
        if ("+08:00".equals(normalized)) label = "UTC+08:00";
        else if ("+09:00".equals(normalized)) label = "UTC+09:00";
        else if ("+07:00".equals(normalized)) label = "UTC+07:00";
        else if ("+05:30".equals(normalized)) label = "UTC+05:30";
        else if ("+01:00".equals(normalized)) label = "UTC+01:00";
        else if ("-05:00".equals(normalized)) label = "UTC-05:00";
        else if ("-08:00".equals(normalized)) label = "UTC-08:00";
        else if ("UTC".equals(normalized)) label = "UTC+00:00";

        timeZoneButton.setText("時區：" + label);
    }

    private String text(EditText input) {
        return input == null
                ? ""
                : input.getText().toString().trim();
    }

    private static String formatCoordinate(double value) {
        return String.format(Locale.US, "%.6f", value);
    }
}
