package com.shiftcalendar.app.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.shiftcalendar.app.model.ShiftType;
import com.shiftcalendar.app.model.ShiftConfig;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Хранит настройки графика в SharedPreferences с использованием JSON.
 */
public class PrefsRepository {

    private static final String PREFS_NAME = "shift_calendar_prefs";
    private static final String KEY_START_DATE_EPOCH_DAY = "start_date_epoch_day";
    private static final String KEY_SHIFT_TYPES_JSON = "shift_types_json";
    private static final String KEY_CYCLE_IDS_JSON = "cycle_ids_json";
    private static final String KEY_EVENTS_TEXT = "events_text";
    private static final String KEY_NOTES_TEXT = "notes_text";
    private static final String KEY_WATER_ML = "water_ml";
    private static final String KEY_WATER_GOAL = "water_goal";
    private static final String KEY_WATER_LAST_DATE = "water_last_date";

    private final SharedPreferences prefs;

    public PrefsRepository(Context context) {
        this.prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public boolean hasConfig() {
        return prefs.contains(KEY_START_DATE_EPOCH_DAY);
    }

    public List<ShiftType> getAllShiftTypes() {
        String json = prefs.getString(KEY_SHIFT_TYPES_JSON, "");
        List<ShiftType> types = new ArrayList<>();
        if (json.isEmpty()) {
            // Дефолтные типы
            types.add(new ShiftType("work", "Работа", "⚒️", 0xFF1DE9B6, true));
            types.add(new ShiftType("rest", "—", "", 0xFFB0B0B0, false));
            return types;
        }

        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                types.add(new ShiftType(
                        obj.getString("id"),
                        obj.getString("name"),
                        obj.getString("icon"),
                        obj.getInt("color"),
                        obj.getBoolean("isWork"),
                        obj.optInt("startHour", 8),
                        obj.optInt("endHour", 20)
                ));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return types;
    }

    public void saveShiftTypes(List<ShiftType> types) {
        JSONArray arr = new JSONArray();
        try {
            for (ShiftType type : types) {
                JSONObject obj = new JSONObject();
                obj.put("id", type.getId());
                obj.put("name", type.getName());
                obj.put("icon", type.getIcon());
                obj.put("color", type.getColor());
                obj.put("isWork", type.isWork());
                obj.put("startHour", type.getStartHour());
                obj.put("endHour", type.getEndHour());
                arr.put(obj);
            }
            prefs.edit().putString(KEY_SHIFT_TYPES_JSON, arr.toString()).apply();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public List<String> getCycleIds() {
        String json = prefs.getString(KEY_CYCLE_IDS_JSON, "");
        List<String> ids = new ArrayList<>();
        if (json.isEmpty()) {
            // Дефолтный цикл 2 через 2
            ids.add("work");
            ids.add("work");
            ids.add("rest");
            ids.add("rest");
            return ids;
        }

        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                ids.add(arr.getString(i));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return ids;
    }

    public void saveCycle(List<String> ids) {
        JSONArray arr = new JSONArray(ids);
        prefs.edit().putString(KEY_CYCLE_IDS_JSON, arr.toString()).apply();
    }

    public ShiftConfig getConfig() {
        long epochDay = prefs.getLong(KEY_START_DATE_EPOCH_DAY, LocalDate.now().toEpochDay());
        LocalDate startDate = LocalDate.ofEpochDay(epochDay);

        List<ShiftType> allTypes = getAllShiftTypes();
        Map<String, ShiftType> typeMap = new HashMap<>();
        for (ShiftType t : allTypes) {
            typeMap.put(t.getId(), t);
        }

        List<String> cycleIds = getCycleIds();
        List<ShiftType> cycle = new ArrayList<>();
        List<Boolean> waterDays = new ArrayList<>();
        
        ShiftType defaultRest = new ShiftType("rest", "—", "", 0xFFB0B0B0, false);

        for (String id : cycleIds) {
            // Разделяем комбинированные ID (например, "work|water")
            String[] parts = id.split("\\|");
            String shiftId = parts[0];
            boolean hasWater = parts.length > 1 && "water".equals(parts[1]);
            
            ShiftType t = typeMap.get(shiftId);
            if (t == null) t = defaultRest;
            
            cycle.add(t);
            waterDays.add(hasWater);
        }

        if (cycle.isEmpty()) {
            cycle.add(defaultRest);
            waterDays.add(false);
        }

        return new ShiftConfig(startDate, cycle, waterDays);
    }

    public void saveStartDate(LocalDate date) {
        prefs.edit().putLong(KEY_START_DATE_EPOCH_DAY, date.toEpochDay()).apply();
    }

    public String getEventsText() {
        return prefs.getString(KEY_EVENTS_TEXT, "");
    }

    public void setEventsText(String text) {
        prefs.edit().putString(KEY_EVENTS_TEXT, text).apply();
    }

    public String getNotesText() {
        return prefs.getString(KEY_NOTES_TEXT, "");
    }

    public void setNotesText(String text) {
        prefs.edit().putString(KEY_NOTES_TEXT, text).apply();
    }

    public int getWaterMl() {
        checkWaterReset();
        return prefs.getInt(KEY_WATER_ML, 0);
    }

    public void addWater(int ml) {
        int current = getWaterMl();
        prefs.edit().putInt(KEY_WATER_ML, current + ml).apply();
    }

    public int getWaterGoal() {
        return prefs.getInt(KEY_WATER_GOAL, 2000);
    }

    public void setWaterGoal(int goal) {
        prefs.edit().putInt(KEY_WATER_GOAL, goal).apply();
    }

    private void checkWaterReset() {
        String today = LocalDate.now().toString();
        String last = prefs.getString(KEY_WATER_LAST_DATE, "");
        if (!today.equals(last)) {
            prefs.edit()
                    .putInt(KEY_WATER_ML, 0)
                    .putString(KEY_WATER_LAST_DATE, today)
                    .apply();
        }
    }
}
