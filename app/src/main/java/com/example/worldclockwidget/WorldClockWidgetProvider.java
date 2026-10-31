package com.example.worldclockwidget;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.widget.RemoteViews;

import com.ibm.icu.util.PersianCalendar;
import com.ibm.icu.util.TimeZone;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class WorldClockWidgetProvider extends AppWidgetProvider {

    private static final String ACTION_UPDATE =
            "com.example.worldclockwidget.UPDATE_CLOCKS";

    private static final String[] PERSIAN_MONTHS = {
            "فروردین", "اردیبهشت", "خرداد", "تیر",
            "مرداد", "شهریور", "مهر", "آبان",
            "آذر", "دی", "بهمن", "اسفند"
    };

    @Override
    public void onUpdate(
            Context context,
            AppWidgetManager manager,
            int[] ids) {

        updateAll(context);
        schedule(context);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        if (ACTION_UPDATE.equals(intent.getAction())) {
            updateAll(context);
            schedule(context);
        }
    }

    private void updateAll(Context context) {
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

        if (alarmManager == null) return;

        long now = System.currentTimeMillis();

        // زمان اجرای بعدی: ابتدای دقیقه بعد
        long nextMinute = now - (now % 60000L) + 60000L;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && !alarmManager.canScheduleExactAlarms()) {

            alarmManager.setInexactRepeating(
                    AlarmManager.RTC_WAKEUP,
                    nextMinute,
                    60000L,
                    pendingIntent);

        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    nextMinute,
                    pendingIntent);

        } else {

            alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    nextMinute,
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

        // همه شهرها از یک لحظه زمانی استفاده می‌کنند
        long now = System.currentTimeMillis();

        setTehran(views, now);
        setIstanbul(views, now);
        setShanghai(views, now);

        manager.updateAppWidget(id, views);
    }

    private String formatTime(long now, String zone) {
        SimpleDateFormat format =
                new SimpleDateFormat("HH:mm", Locale.US);

        format.setTimeZone(
                java.util.TimeZone.getTimeZone(zone));

        return format.format(new Date(now));
    }

    private void setTehran(RemoteViews views, long now) {
        String zone = "Asia/Tehran";

        TimeZone icuZone = TimeZone.getTimeZone(zone);

        PersianCalendar calendar =
                new PersianCalendar(icuZone, new Locale("fa", "IR"));

        calendar.setTimeInMillis(now);

        int day = calendar.get(PersianCalendar.DAY_OF_MONTH);
        int month = calendar.get(PersianCalendar.MONTH);
        int year = calendar.get(PersianCalendar.YEAR);

        String date = toPersianDigits(
                day + " " + PERSIAN_MONTHS[month] + " " + year);

        views.setTextViewText(
                R.id.time_tehran, formatTime(now, zone));

        views.setTextViewText(R.id.greg_tehran, "");
        views.setTextViewText(R.id.jalali_tehran, date);
    }

    private void setIstanbul(RemoteViews views, long now) {
        String zone = "Europe/Istanbul";

        SimpleDateFormat dateFormat =
                new SimpleDateFormat("d MMMM yyyy",
                        new Locale("tr", "TR"));

        dateFormat.setTimeZone(
                java.util.TimeZone.getTimeZone(zone));

        views.setTextViewText(
                R.id.time_istanbul, formatTime(now, zone));

        views.setTextViewText(
                R.id.greg_istanbul,
                dateFormat.format(new Date(now)));

        views.setTextViewText(R.id.jalali_istanbul, "");
    }

    private void setShanghai(RemoteViews views, long now) {
        String zone = "Asia/Shanghai";

        SimpleDateFormat dateFormat =
                new SimpleDateFormat("MMM d, yyyy", Locale.US);

        dateFormat.setTimeZone(
                java.util.TimeZone.getTimeZone(zone));

        views.setTextViewText(
                R.id.time_shanghai, formatTime(now, zone));

        views.setTextViewText(
                R.id.greg_shanghai,
                dateFormat.format(new Date(now)));

        views.setTextViewText(R.id.jalali_shanghai, "");
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
