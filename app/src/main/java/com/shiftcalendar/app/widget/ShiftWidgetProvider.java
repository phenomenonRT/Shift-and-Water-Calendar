package com.shiftcalendar.app.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.RemoteViews;

import com.shiftcalendar.app.R;
import com.shiftcalendar.app.data.PrefsRepository;
import com.shiftcalendar.app.logic.ShiftCalculator;
import com.shiftcalendar.app.model.DayStatus;
import com.shiftcalendar.app.model.ShiftConfig;
import com.shiftcalendar.app.model.ShiftType;
import com.shiftcalendar.app.ui.MainActivity;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class ShiftWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
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
            ComponentName name = new ComponentName(context, getClass());
            int[] ids = manager.getAppWidgetIds(name);
            if (ids != null && ids.length > 0) {
                onUpdate(context, manager, ids);
            }
        }
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        PrefsRepository prefs = new PrefsRepository(context);
        ShiftConfig config = prefs.getConfig();
        LocalDate today = LocalDate.now();
        DayStatus todayStatus = ShiftCalculator.getStatus(config, today);
        ShiftType todayType = todayStatus.getShiftType();

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_shift);

        // Дата
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("d MMMM", new Locale("ru"));
        views.setTextViewText(R.id.widgetDate, today.format(dtf));

        // Статус
        String icon = todayType.getIcon();
        String name = todayType.getName();
        
        // Убираем домик и название "Выходной" по просьбе пользователя, если это отдых
        if ("rest".equals(todayType.getId())) {
            icon = "";
            name = "—";
        }
        
        String statusText = "Сегодня: " + (icon.isEmpty() ? "" : icon + " ") + name;
        views.setTextViewText(R.id.widgetStatus, statusText);

        // Вода
        boolean isWater = todayStatus.isWaterDay();
        views.setViewVisibility(R.id.widgetWater, isWater ? View.VISIBLE : View.GONE);

        // Расчёт следующей смены и следующей воды
        int daysToWork = -1;
        int daysToWater = -1;

        for (int i = 1; i <= 60; i++) {
            DayStatus s = ShiftCalculator.getStatus(config, today.plusDays(i));
            if (daysToWork == -1 && s.getShiftType().isWork()) {
                daysToWork = i;
            }
            if (daysToWater == -1 && s.isWaterDay()) {
                daysToWater = i;
            }
            if (daysToWork != -1 && daysToWater != -1) break;
        }

        StringBuilder nextText = new StringBuilder();
        if (todayType.isWork()) {
            nextText.append("Сегодня работа. ");
        } else if (daysToWork != -1) {
            if (daysToWork == 1) {
                nextText.append("Смена завтра. ");
            } else {
                nextText.append("Смена через ").append(daysToWork).append(" ").append(getDayString(daysToWork)).append(". ");
            }
        }

        if (!isWater && daysToWater != -1) {
            if (daysToWater == 1) {
                nextText.append("Вода завтра");
            } else {
                nextText.append("До воды: ").append(daysToWater).append(" ").append(getDayString(daysToWater));
            }
        }

        views.setTextViewText(R.id.widgetNext, nextText.toString().trim());

        // Клик по виджету открывает приложение
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 100, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widgetRoot, pendingIntent);
        
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    private static String getDayString(int n) {
        if (n % 10 == 1 && n % 100 != 11) return "день";
        if (n % 10 >= 2 && n % 10 <= 4 && (n % 100 < 10 || n % 100 >= 20)) return "дня";
        return "дней";
    }
}
