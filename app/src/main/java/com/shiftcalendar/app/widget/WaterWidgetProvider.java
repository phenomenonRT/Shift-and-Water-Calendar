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
import com.shiftcalendar.app.ui.MainActivity;

import java.time.LocalDate;

public class WaterWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int id : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, id);
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
            onUpdate(context, manager, ids);
        }
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int id) {
        PrefsRepository prefs = new PrefsRepository(context);
        ShiftConfig config = prefs.getConfig();
        LocalDate today = LocalDate.now();

        int daysToWater = -1;
        for (int i = 0; i <= 60; i++) {
            DayStatus s = ShiftCalculator.getStatus(config, today.plusDays(i));
            if (s.isWaterDay()) {
                daysToWater = i;
                break;
            }
        }

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_water_only);
        String text = (daysToWater == 0) ? "Вода СЕГОДНЯ!" :
                (daysToWater == 1) ? "Вода завтра" :
                        (daysToWater == -1) ? "Нет данных" :
                                "Вода через " + daysToWater + " " + getDayString(daysToWater);

        views.setTextViewText(R.id.widgetNext, text);

        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widgetRoot, pendingIntent);

        appWidgetManager.updateAppWidget(id, views);
    }

    private static String getDayString(int n) {
        if (n % 10 == 1 && n % 100 != 11) return "день";
        if (n % 10 >= 2 && n % 10 <= 4 && (n % 100 < 10 || n % 100 >= 20)) return "дня";
        return "дней";
    }
}
