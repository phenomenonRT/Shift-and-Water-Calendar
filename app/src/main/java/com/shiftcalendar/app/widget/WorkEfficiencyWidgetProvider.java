package com.shiftcalendar.app.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.shiftcalendar.app.R;
import com.shiftcalendar.app.data.PrefsRepository;
import com.shiftcalendar.app.logic.ShiftCalculator;
import com.shiftcalendar.app.model.DayStatus;
import com.shiftcalendar.app.model.ShiftConfig;
import com.shiftcalendar.app.model.ShiftType;
import com.shiftcalendar.app.ui.MainActivity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Duration;

public class WorkEfficiencyWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int id : appWidgetIds) {
            updateWidget(context, appWidgetManager, id);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        String action = intent.getAction();
        if (Intent.ACTION_DATE_CHANGED.equals(action) ||
                Intent.ACTION_TIME_CHANGED.equals(action) ||
                Intent.ACTION_TIMEZONE_CHANGED.equals(action)) {
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            int[] ids = manager.getAppWidgetIds(new ComponentName(context, getClass()));
            onUpdate(context, manager, ids);
        }
    }

    static void updateWidget(Context context, AppWidgetManager appWidgetManager, int id) {
        PrefsRepository prefs = new PrefsRepository(context);
        ShiftConfig config = prefs.getConfig();
        LocalDateTime now = LocalDateTime.now();
        DayStatus todayStatus = ShiftCalculator.getStatus(config, now.toLocalDate());
        ShiftType todayType = todayStatus.getShiftType();

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_work_efficiency);

        if (todayType.isWork()) {
            LocalTime start = LocalTime.of(todayType.getStartHour(), 0);
            LocalTime end = LocalTime.of(todayType.getEndHour(), 0);
            
            LocalDateTime startDt = LocalDateTime.of(now.toLocalDate(), start);
            LocalDateTime endDt = LocalDateTime.of(now.toLocalDate(), end);
            
            if (now.isBefore(startDt)) {
                views.setTextViewText(R.id.widgetTimerText, "Начнется через: " + formatDuration(Duration.between(now, startDt)));
                views.setProgressBar(R.id.widgetShiftProgress, 100, 0, false);
            } else if (now.isAfter(endDt)) {
                views.setTextViewText(R.id.widgetTimerText, "Смена закончена");
                views.setProgressBar(R.id.widgetShiftProgress, 100, 100, false);
            } else {
                long total = Duration.between(startDt, endDt).toMinutes();
                long passed = Duration.between(startDt, now).toMinutes();
                int percent = (int) (passed * 100 / total);
                
                views.setTextViewText(R.id.widgetTimerText, "Осталось: " + formatDuration(Duration.between(now, endDt)));
                views.setProgressBar(R.id.widgetShiftProgress, 100, percent, false);
            }
            views.setTextViewText(R.id.tvIcon, todayType.getIcon());
        } else {
            views.setTextViewText(R.id.tvIcon, "💤");
            views.setTextViewText(R.id.widgetTimerText, "Сегодня выходной");
            views.setProgressBar(R.id.widgetShiftProgress, 100, 0, false);
        }

        Intent mainIntent = new Intent(context, MainActivity.class);
        PendingIntent mainPi = PendingIntent.getActivity(context, 2, mainIntent, PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widgetRoot, mainPi);

        appWidgetManager.updateAppWidget(id, views);
    }

    private static String formatDuration(Duration d) {
        long h = d.toHours();
        long m = d.toMinutes() % 60;
        return h + "ч " + m + "м";
    }
}
