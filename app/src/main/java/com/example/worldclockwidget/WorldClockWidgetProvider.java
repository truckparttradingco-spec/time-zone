package com.example.worldclockwidget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

public class WorldClockWidgetProvider extends AppWidgetProvider {
    private static final String PREFS = "world_clock_settings";
    private static final String[] KEYS = {"slot_1","slot_2","slot_3"};
    private static final String[] DEFAULTS = {"germany","istanbul","shanghai"};

    @Override public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        updateWidgets(context, manager, ids);
    }

    @Override public void onEnabled(Context context) {
        super.onEnabled(context);
        updateAll(context);
    }

    @Override public void onAppWidgetOptionsChanged(Context context, AppWidgetManager manager,
                                                     int appWidgetId, android.os.Bundle options) {
        super.onAppWidgetOptionsChanged(context, manager, appWidgetId, options);
        updateWidgets(context, manager, new int[]{appWidgetId});
    }

    @Override public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        String action = intent.getAction();
        if (Intent.ACTION_TIME_CHANGED.equals(action)
                || Intent.ACTION_TIMEZONE_CHANGED.equals(action)
                || Intent.ACTION_DATE_CHANGED.equals(action)) {
            updateAll(context);
        }
    }

    private static void updateAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] ids = manager.getAppWidgetIds(new ComponentName(context, WorldClockWidgetProvider.class));
        updateWidgets(context, manager, ids);
    }

    static void updateWidgets(Context context, AppWidgetManager manager, int[] ids) {
        if (ids == null || ids.length == 0) return;
        long now = System.currentTimeMillis();
        android.content.SharedPreferences prefs =
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        int[] names = {R.id.city_name_1,R.id.city_name_2,R.id.city_name_3};
        int[] times = {R.id.city_time_1,R.id.city_time_2,R.id.city_time_3};
        int[] dates = {R.id.city_date_1,R.id.city_date_2,R.id.city_date_3};
        int[] zones = {R.id.city_zone_1,R.id.city_zone_2,R.id.city_zone_3};

        for (int id : ids) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.world_clock_widget);
            for (int i=0;i<3;i++) {
                CountryCatalog.Country c = CountryCatalog.byId(prefs.getString(KEYS[i],DEFAULTS[i]));
                views.setTextViewText(names[i], c.toString());
                views.setTextViewText(times[i], ClockFormatter.time(c,now,prefs.getBoolean("use_24_hour",false)));
                views.setTextViewText(dates[i], ClockFormatter.date(c,now));
                views.setTextViewText(zones[i], ClockFormatter.offset(c,now));
            }
            Intent open = new Intent(context, MainActivity.class);
            open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent pi = PendingIntent.getActivity(context, id, open,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.widget_root, pi);
            manager.updateAppWidget(id, views);
        }
    }
}
