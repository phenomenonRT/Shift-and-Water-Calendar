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

public class WorkWidgetProvider extends AppWidgetProvider {

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
                Intent.ACTION_TIMEZONE_CHANGED.equals(action) ||
                Intent.ACTION_USER_PRESENT.equals(action) ||
                Intent.ACTION_BOOT_COMPLETED.equals(action) ||
                "android.intent.action.QUICKBOOT_POWERON".equals(action)) {

            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            ComponentName name = new ComponentName(context, getClass());
            int[] ids = manager.getAppWidgetIds(name);
            if (ids != null && ids.length > 0) {
                onUpdate(context, manager, ids);
            }
        }
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int id) {
        PrefsRepository prefs = new PrefsRepository(context);
        ShiftConfig config = prefs.getConfig();
        LocalDate today = LocalDate.now();

        DayStatus todayStatus = ShiftCalculator.getStatus(config, today);
        ShiftType todayType = todayStatus.getShiftType();

        int daysToWork = -1;
        if (!todayType.isWork()) {
            for (int i = 1; i <= 60; i++) {
                DayStatus s = ShiftCalculator.getStatus(config, today.plusDays(i));
                if (s.getShiftType().isWork()) {
                    daysToWork = i;
                    break;
                }
            }
        }

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_shift_only);
        String text = todayType.isWork() ? "Смена сегодня" :
                (daysToWork == 1) ? "Смена завтра" :
                        (daysToWork == -1) ? "Нет смен" :
                                "Смена через " + daysToWork + " " + getDayString(daysToWork);

        views.setTextViewText(R.id.tvIcon, todayType.isWork() ? todayType.getIcon() : "⚒️");
        views.setTextViewText(R.id.widgetNext, text);

        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 103, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widgetRoot, pendingIntent);

        appWidgetManager.updateAppWidget(id, views);
    }

    private static String getDayString(int n) {
        if (n % 10 == 1 && n % 100 != 11) return "день";
        if (n % 10 >= 2 && n % 10 <= 4 && (n % 100 < 10 || n % 100 >= 20)) return "дня";
        return "дней";
    }
}
