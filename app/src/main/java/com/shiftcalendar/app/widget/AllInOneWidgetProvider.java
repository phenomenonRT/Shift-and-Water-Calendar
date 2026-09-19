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

public class AllInOneWidgetProvider extends AppWidgetProvider {

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
                Intent.ACTION_TIMEZONE_CHANGED.equals(action) ||
                Intent.ACTION_USER_PRESENT.equals(action) ||
                Intent.ACTION_BOOT_COMPLETED.equals(action) ||
                "android.intent.action.QUICKBOOT_POWERON".equals(action)) {
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            int[] ids = manager.getAppWidgetIds(new ComponentName(context, getClass()));
            onUpdate(context, manager, ids);
        }
    }

    static void updateWidget(Context context, AppWidgetManager appWidgetManager, int id) {
        PrefsRepository prefs = new PrefsRepository(context);
        ShiftConfig config = prefs.getConfig();
        LocalDate today = LocalDate.now();
        DayStatus todayStatus = ShiftCalculator.getStatus(config, today);
        ShiftType todayType = todayStatus.getShiftType();

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_all_in_one);

        // Статус смены
        String shiftText = todayType.isWork() ? todayType.getIcon() + " " + todayType.getName() : "💤 Отдых";
        views.setTextViewText(R.id.widgetShiftStatus, shiftText);

        // График воды
        int daysToWater = -1;
        for (int i = 0; i <= 60; i++) {
            DayStatus s = ShiftCalculator.getStatus(config, today.plusDays(i));
            if (s.isWaterDay()) {
                daysToWater = i;
                break;
            }
        }
        
        String scheduleText = (daysToWater == 0) ? "💧 Вода СЕГОДНЯ!" :
                (daysToWater == 1) ? "💧 Вода завтра" :
                        (daysToWater == -1) ? "💧 Нет данных" :
                                "💧 Вода через " + daysToWater + " дн.";
        views.setTextViewText(R.id.widgetWaterSchedule, scheduleText);

        // Клик по фону
        Intent mainIntent = new Intent(context, MainActivity.class);
        PendingIntent mainPi = PendingIntent.getActivity(context, 111, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widgetRoot, mainPi);

        appWidgetManager.updateAppWidget(id, views);
    }
}
