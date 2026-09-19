package com.shiftcalendar.app.ui;

import android.app.DatePickerDialog;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.shiftcalendar.app.R;
import com.shiftcalendar.app.data.PrefsRepository;
import com.shiftcalendar.app.logic.ShiftCalculator;
import com.shiftcalendar.app.model.DayStatus;
import com.shiftcalendar.app.model.ShiftConfig;
import com.shiftcalendar.app.model.ShiftType;
import com.shiftcalendar.app.widget.AllInOneWidgetProvider;
import com.shiftcalendar.app.widget.ClockWaterWidget4x1Provider;
import com.shiftcalendar.app.widget.ClockWaterWidgetProvider;
import com.shiftcalendar.app.widget.ClockWidgetProvider;
import com.shiftcalendar.app.widget.ClockWorkWidget4x1Provider;
import com.shiftcalendar.app.widget.ShiftWidgetProvider;
import com.shiftcalendar.app.widget.WaterMiniWidgetProvider;
import com.shiftcalendar.app.widget.WaterWidget2x1Provider;
import com.shiftcalendar.app.widget.WaterWidgetProvider;
import com.shiftcalendar.app.widget.WorkEfficiencyWidgetProvider;
import com.shiftcalendar.app.widget.WorkWidget2x1Provider;
import com.shiftcalendar.app.widget.WorkWidgetProvider;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Единственный экран приложения: месяц-календарь с индикаторами смены/воды,
 * карточки "События"/"Заметки" и настройка графика через FAB.
 */
public class MainActivity extends AppCompatActivity {

    private PrefsRepository prefsRepository;
    private CalendarAdapter adapter;

    private YearMonth currentMonth;
    private LocalDate selectedDate;

    private TextView tvMonthYear;
    private TextView tvEventsContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefsRepository = new PrefsRepository(this);

        selectedDate = LocalDate.now();
        currentMonth = YearMonth.from(selectedDate);

        tvMonthYear = findViewById(R.id.tvMonthYear);
        tvEventsContent = findViewById(R.id.tvEventsContent);

        buildWeekdayHeader();

        RecyclerView rvCalendar = findViewById(R.id.rvCalendar);
        rvCalendar.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CalendarAdapter(this::onDaySelected);
        rvCalendar.setAdapter(adapter);

        setupSwipeGestures(rvCalendar);

        findViewById(R.id.btnPrevMonth).setOnClickListener(v -> {
            currentMonth = currentMonth.minusMonths(1);
            refreshCalendar();
        });
        findViewById(R.id.btnNextMonth).setOnClickListener(v -> {
            currentMonth = currentMonth.plusMonths(1);
            refreshCalendar();
        });

        FloatingActionButton fabSettings = findViewById(R.id.fabSettings);
        fabSettings.setOnClickListener(v -> showSettingsDialog());

        refreshCalendar();
        refreshCards();
        updateWidget();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateWidget();
    }

    private void buildWeekdayHeader() {
        LinearLayout header = findViewById(R.id.weekdayHeader);
        header.removeAllViews();
        DayOfWeek[] order = {
                DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY
        };
        for (DayOfWeek dow : order) {
            TextView tv = new TextView(this);
            tv.setText(dow.getDisplayName(TextStyle.SHORT, new Locale("ru")));
            tv.setGravity(Gravity.CENTER);
            tv.setTextSize(12f);
            tv.setPadding(0, 4, 0, 12);
            tv.setTextColor(getResources().getColor(
                    dow == DayOfWeek.SUNDAY ? R.color.text_red_sunday : R.color.text_dim));

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            tv.setLayoutParams(lp);
            header.addView(tv);
        }
    }

    private void setupSwipeGestures(View view) {
        GestureDetector gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (Math.abs(velocityX) > Math.abs(velocityY)) {
                    if (velocityX > 0) {
                        // Swipe Right -> Prev Month
                        currentMonth = currentMonth.minusMonths(1);
                    } else {
                        // Swipe Left -> Next Month
                        currentMonth = currentMonth.plusMonths(1);
                    }
                    refreshCalendar();
                    return true;
                }
                return false;
            }
        });

        view.setOnTouchListener((v, event) -> gestureDetector.onTouchEvent(event));
    }

    private void onDaySelected(LocalDate date) {
        selectedDate = date;
        adapter.setSelectedDate(date);
        refreshCards();
    }

    private void refreshCalendar() {
        String monthName = currentMonth.getMonth()
                .getDisplayName(TextStyle.FULL, new Locale("ru"));
        monthName = monthName.substring(0, 1).toUpperCase(new Locale("ru")) + monthName.substring(1);
        tvMonthYear.setText(monthName + " " + currentMonth.getYear());

        ShiftConfig config = prefsRepository.getConfig();
        adapter.submit(config, currentMonth, selectedDate);
    }

    private void refreshCards() {
        ShiftConfig config = prefsRepository.getConfig();
        LocalDate today = LocalDate.now();
        
        int daysToWork = -1;
        int daysToWater = -1;
        
        for (int i = 0; i <= 60; i++) {
            DayStatus s = ShiftCalculator.getStatus(config, today.plusDays(i));
            if (daysToWork == -1 && s.getShiftType().isWork()) {
                daysToWork = i;
            }
            if (daysToWater == -1 && s.isWaterDay()) {
                daysToWater = i;
            }
            if (daysToWork != -1 && daysToWater != -1) break;
        }

        StringBuilder sb = new StringBuilder();
        if (daysToWork == 0) sb.append("Сегодня рабочая смена. ");
        else if (daysToWork == 1) sb.append("Завтра выходить на работу. ");
        else if (daysToWork > 1) sb.append("Ближайшая смена через ").append(daysToWork).append(" дн. ");

        if (daysToWater == 0) sb.append("Сегодня ВОДА!");
        else if (daysToWater == 1) sb.append("Завтра вода.");
        else if (daysToWater > 1) sb.append("Вода будет через ").append(daysToWater).append(" дн.");

        String text = sb.toString().trim();
        tvEventsContent.setText(text.isEmpty() ? "Ближайших событий не найдено" : text);
    }

    private void showSettingsDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_settings, null);

        EditText etCycle = dialogView.findViewById(R.id.etCycle);
        TextView tvStartDate = dialogView.findViewById(R.id.tvStartDate);
        View btnPickDate = dialogView.findViewById(R.id.btnPickDate);

        ShiftConfig current = prefsRepository.getConfig();
        
        // Преобразуем текущий цикл в строку для отображения
        StringBuilder cycleStr = new StringBuilder();
        List<ShiftType> cycle = current.getCycle();
        List<Boolean> waterDays = current.getWaterDays();
        
        for (int i = 0; i < cycle.size(); i++) {
            ShiftType type = cycle.get(i);
            boolean hasWater = waterDays != null && i < waterDays.size() && waterDays.get(i);
            
            if (hasWater) cycleStr.append("П");
            
            String id = type.getId();
            if (id.equals("work")) cycleStr.append("У");
            else if (id.equals("night")) cycleStr.append("Н");
            else if (id.equals("day")) cycleStr.append("Д");
            else if (id.equals("rest")) {
                if (!hasWater) cycleStr.append("_");
            } else {
                cycleStr.append("_");
            }
            cycleStr.append(",");
        }
        if (cycleStr.length() > 0) cycleStr.setLength(cycleStr.length() - 1);
        
        etCycle.setText(cycleStr.toString());

        final LocalDate[] pickedStartDate = {current.getStartDate()};
        tvStartDate.setText(getString(R.string.start_date_label, pickedStartDate[0].toString()));

        btnPickDate.setOnClickListener(v -> {
            LocalDate d = pickedStartDate[0];
            new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                pickedStartDate[0] = LocalDate.of(year, month + 1, dayOfMonth);
                tvStartDate.setText(getString(R.string.start_date_label, pickedStartDate[0].toString()));
            }, d.getYear(), d.getMonthValue() - 1, d.getDayOfMonth()).show();
        });

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.settings_title)
                .setView(dialogView)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    String rawCycle = etCycle.getText().toString().toUpperCase().trim();
                    String[] tokens = rawCycle.split(",");
                    List<String> ids = new ArrayList<>();
                    
                    for (String t : tokens) {
                        String token = t.trim();
                        boolean hasP = token.contains("П");
                        String shiftId = "rest";
                        
                        if (token.contains("У")) shiftId = "work";
                        else if (token.contains("Н")) shiftId = "night";
                        else if (token.contains("Д")) shiftId = "day";
                        
                        if (hasP) {
                            ids.add(shiftId + "|water");
                        } else {
                            ids.add(shiftId);
                        }
                    }
                    
                    if (ids.isEmpty()) ids.add("rest");

                    prefsRepository.saveStartDate(pickedStartDate[0]);
                    prefsRepository.saveCycle(ids);
                    
                    // Убедимся, что все типы смен существуют
                    List<ShiftType> types = new ArrayList<>();
                    types.add(new ShiftType("work", "Утро", "☀️", 0xFF1DE9B6, true));
                    types.add(new ShiftType("night", "Ночь", "🌙", 0xFF2979FF, true));
                    types.add(new ShiftType("rest", "—", "", 0xFFB0B0B0, false));
                    types.add(new ShiftType("day", "День", "🌞", 0xFFFFD600, true));
                    types.add(new ShiftType("water", "Вода", "💧", 0xFF40C4FF, false));
                    prefsRepository.saveShiftTypes(types);

                    updateWidget();
                    refreshCalendar();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void updateWidget() {
        Class<?>[] providers = {
                ShiftWidgetProvider.class,
                WaterWidgetProvider.class,
                WorkWidgetProvider.class,
                WaterWidget2x1Provider.class,
                WorkWidget2x1Provider.class,
                ClockWidgetProvider.class,
                ClockWaterWidgetProvider.class,
                ClockWaterWidget4x1Provider.class,
                ClockWorkWidget4x1Provider.class,
                AllInOneWidgetProvider.class,
                WorkEfficiencyWidgetProvider.class
        };
        
        for (Class<?> cls : providers) {
            Intent intent = new Intent(this, cls);
            intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            int[] ids = AppWidgetManager.getInstance(getApplication())
                    .getAppWidgetIds(new ComponentName(getApplication(), cls));
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
            sendBroadcast(intent);
        }
    }

    private int parseIntOrDefault(String value, int defaultValue) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
