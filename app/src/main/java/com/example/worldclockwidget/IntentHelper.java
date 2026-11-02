package com.example.worldclockwidget;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;

public final class IntentHelper {
    private IntentHelper() {}
    public static void update(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] ids = manager.getAppWidgetIds(new ComponentName(context, WorldClockWidgetProvider.class));
        WorldClockWidgetProvider.updateWidgets(context, manager, ids);
    }
}
