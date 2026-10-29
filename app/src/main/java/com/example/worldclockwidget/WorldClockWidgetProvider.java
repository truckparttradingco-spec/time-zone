package com.example.worldclockwidget;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.widget.RemoteViews;

import com.ibm.icu.util.PersianCalendar;
import com.ibm.icu.util.TimeZone;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class WorldClockWidgetProvider extends AppWidgetProvider {

    private static final String ACTION_UPDATE =
            "com.example.worldclockwidget.UPDATE_CLOCKS";

    @Override
    public void onUpdate(
            Context context,
            AppWidgetManager manager,
            int[] ids) {

        for (int id : ids) {
            updateOne(context, manager, id);
        }

        schedule(context);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        if (ACTION_UPDATE.equals(intent.getAction())) {
            AppWidgetManager manager =
                    AppWidgetManager.getInstance(context);

            int[] ids = manager.getAppWidgetIds(
                    new ComponentName(
                            context,
                            WorldClockWidgetProvider.class));

            for (int id : ids) {
                updateOne(context, manager, id);
            }
        }
    }

    private void schedule(Context context) {
        Intent intent = new Intent(
                context,
                WorldClockWidgetProvider.class);

        intent.setAction(ACTION_UPDATE);

        PendingIntent pendingIntent =
                PendingIntent.getBroadcast(
                        context,
                        71,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                                | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(
                        Context.ALARM_SERVICE);

        if (alarmManager != null) {
            alarmManager.setInexactRepeating(
                    AlarmManager.ELAPSED_REALTIME,
                    SystemClock.elapsedRealtime() + 60000,
                    60000,
                    pendingIntent);
        }
    }

    private void updateOne(
            Context context,
            AppWidgetManager manager,
            int id) {

        RemoteViews views = new RemoteViews(
                context.getPackageName(),
                R.layout.world_clock_widget);

        setCity(
                views, "Asia/Tehran",
                R.id.time_tehran,
                R.id.greg_tehran,
                R.id.jalali_tehran);

        setCity(
                views, "Europe/Istanbul",
                R.id.time_istanbul,
                R.id.greg_istanbul,
                R.id.jalali_istanbul);

        setCity(
                views, "Asia/Shanghai",
                R.id.time_shanghai,
                R.id.greg_shanghai,
                R.id.jalali_shanghai);

        manager.updateAppWidget(id, views);
    }

    private void setCity(
            RemoteViews views,
            String zone,
            int timeId,
            int gregId,
            int jalaliId) {

        Date now = new Date();

        java.util.TimeZone javaZone =
                java.util.TimeZone.getTimeZone(zone);

        SimpleDateFormat timeFormat =
                new SimpleDateFormat("HH:mm", Locale.US);
        timeFormat.setTimeZone(javaZone);

        SimpleDateFormat gregorianFormat =
                new SimpleDateFormat("yyyy/MM/dd", Locale.US);
        gregorianFormat.setTimeZone(javaZone);

        TimeZone icuZone = TimeZone.getTimeZone(zone);

        PersianCalendar persianCalendar =
                new PersianCalendar(icuZone, Locale.getDefault());

        persianCalendar.setTimeInMillis(now.getTime());

        String jalali = String.format(
                Locale.US,
                "%04d/%02d/%02d",
                persianCalendar.get(PersianCalendar.YEAR),
                persianCalendar.get(PersianCalendar.MONTH) + 1,
                persianCalendar.get(PersianCalendar.DAY_OF_MONTH));

        views.setTextViewText(timeId, timeFormat.format(now));
        views.setTextViewText(
                gregId, gregorianFormat.format(now));
        views.setTextViewText(
                jalaliId, toPersianDigits(jalali));
    }

    private String toPersianDigits(String text) {
        return text.replace('0', '۰')
                .replace('1', '۱')
                .replace('2', '۲')
                .replace('3', '۳')
                .replace('4', '۴')
                .replace('5', '۵')
                .replace('6', '۶')
                .replace('7', '۷')
                .replace('8', '۸')
                .replace('9', '۹');
    }
}
