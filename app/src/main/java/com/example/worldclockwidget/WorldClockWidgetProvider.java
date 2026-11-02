package com.example.worldclockwidget;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import java.util.Calendar;
import java.util.TimeZone;
import android.widget.RemoteViews;

import java.util.Locale;

public class WorldClockWidgetProvider extends AppWidgetProvider {
    private static final String PREFS = "world_clock_settings";
    private static final String[] KEYS = {"slot_1", "slot_2", "slot_3"};
    private static final String[] DEFAULTS = {"germany", "istanbul", "shanghai"};
    private static final String ACTION_DATE_REFRESH = "com.example.worldclockwidget.ACTION_DATE_REFRESH";

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
        if (ACTION_DATE_REFRESH.equals(action)) {
            updateAll(context);
        }
        if (Intent.ACTION_TIME_CHANGED.equals(action)
                || Intent.ACTION_TIMEZONE_CHANGED.equals(action)
                || Intent.ACTION_DATE_CHANGED.equals(action)) {
            updateAll(context);
        }
    }

    private static void updateAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] ids = manager.getAppWidgetIds(
                new ComponentName(context, WorldClockWidgetProvider.class));
        updateWidgets(context, manager, ids);
    }

    private static String cityName(CountryCatalog.Country country) {
        switch (country.id) {
            case "tehran": return "Tehran";
            case "istanbul": return "Istanbul";
            case "shanghai": return "Shanghai";
            case "germany": return "Berlin";
            case "france": return "Paris";
            case "usa": return "New York";
            case "uk": return "London";
            case "russia": return "Moscow";
            case "iraq": return "Baghdad";
            case "uae": return "Dubai";
            case "qatar": return "Doha";
            case "malaysia": return "Kuala Lumpur";
            case "thailand": return "Bangkok";
            case "vietnam": return "Hanoi";
            case "taiwan": return "Taipei";
            case "india": return "New Delhi";
            case "indonesia": return "Jakarta";
            default: return country.name;
        }
    }

    static void updateWidgets(Context context, AppWidgetManager manager, int[] ids) {
        if (ids == null || ids.length == 0) return;

        long now = System.currentTimeMillis();
        android.content.SharedPreferences prefs =
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        int[] names = {R.id.city_name_1, R.id.city_name_2, R.id.city_name_3};
        int[] times = {R.id.city_time_1, R.id.city_time_2, R.id.city_time_3};
        int[] dates = {R.id.city_date_1, R.id.city_date_2, R.id.city_date_3};
        int[] zones = {R.id.city_zone_1, R.id.city_zone_2, R.id.city_zone_3};

        boolean use24Hour = prefs.getBoolean("use_24_hour", true);

        for (int id : ids) {
            RemoteViews views =
                    new RemoteViews(context.getPackageName(), R.layout.world_clock_widget);

            for (int i = 0; i < 3; i++) {
                CountryCatalog.Country country =
                        CountryCatalog.byId(prefs.getString(KEYS[i], DEFAULTS[i]));

                views.setTextViewText(names[i], country.flag + "  " + cityName(country));

                // TextClock is hosted by the launcher. Each clock uses its own IANA time zone
                // and follows Android's system clock without requiring the app to stay open.
                // Time zones and formats are declared directly in XML for RemoteViews safety.

                views.setTextViewText(dates[i], ClockFormatter.widgetDate(country, now));
                views.setTextViewText(zones[i], ClockFormatter.offset(country, now));
            }

            Intent open = new Intent(context, MainActivity.class);
            open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, id, open,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent);
            manager.updateAppWidget(id, views);
        }
        scheduleNextDateRefresh(context, prefs);
    }

    private static void scheduleNextDateRefresh(Context context,
            android.content.SharedPreferences prefs) {
        long next = Long.MAX_VALUE;
        for (int i = 0; i < 3; i++) {
            CountryCatalog.Country country =
                    CountryCatalog.byId(prefs.getString(KEYS[i], DEFAULTS[i]));
            Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone(country.zone));
            calendar.add(Calendar.DAY_OF_YEAR, 1);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 2);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            next = Math.min(next, calendar.getTimeInMillis());
        }
        Intent refresh = new Intent(context, WorldClockWidgetProvider.class);
        refresh.setAction(ACTION_DATE_REFRESH);
        PendingIntent pending = PendingIntent.getBroadcast(context, 7319, refresh,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarms != null) alarms.setAndAllowWhileIdle(AlarmManager.RTC, next, pending);
    }
}
