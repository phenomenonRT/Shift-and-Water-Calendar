package com.shiftcalendar.app.widget;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import com.shiftcalendar.app.data.PrefsRepository;

public class ActionReceiver extends BroadcastReceiver {
    public static final String ACTION_ADD_WATER = "com.shiftcalendar.app.ACTION_ADD_WATER";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (ACTION_ADD_WATER.equals(intent.getAction())) {
            PrefsRepository prefs = new PrefsRepository(context);
            prefs.addWater(250);

            // Обновляем все виджеты
            updateAllWidgets(context);
        }
    }

    private void updateAllWidgets(Context context) {
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

        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        for (Class<?> cls : providers) {
            int[] ids = manager.getAppWidgetIds(new ComponentName(context, cls));
            if (ids.length > 0) {
                Intent updateIntent = new Intent(context, cls);
                updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
                context.sendBroadcast(updateIntent);
            }
        }
    }
}
