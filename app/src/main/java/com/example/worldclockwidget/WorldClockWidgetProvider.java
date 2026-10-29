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
import android.icu.util.PersianCalendar;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class WorldClockWidgetProvider extends AppWidgetProvider {
    private static final String ACTION_UPDATE = "com.example.worldclockwidget.UPDATE_CLOCKS";

    @Override public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        for (int id : ids) updateOne(context, manager, id);
        schedule(context);
    }

    @Override public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (ACTION_UPDATE.equals(intent.getAction())) {
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            int[] ids = manager.getAppWidgetIds(new ComponentName(context, WorldClockWidgetProvider.class));
            for (int id : ids) updateOne(context, manager, id);
        }
    }

    private void schedule(Context context) {
        Intent i = new Intent(context, WorldClockWidgetProvider.class).setAction(ACTION_UPDATE);
        PendingIntent pi = PendingIntent.getBroadcast(context, 71, i,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 60000, 60000, pi);
    }

    private void updateOne(Context c, AppWidgetManager m, int id) {
        RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.world_clock_widget);
        setCity(v, "Asia/Tehran", R.id.time_tehran, R.id.greg_tehran, R.id.jalali_tehran);
        setCity(v, "Europe/Istanbul", R.id.time_istanbul, R.id.greg_istanbul, R.id.jalali_istanbul);
        setCity(v, "Asia/Shanghai", R.id.time_shanghai, R.id.greg_shanghai, R.id.jalali_shanghai);
        m.updateAppWidget(id, v);
    }

    private void setCity(RemoteViews v, String zone, int timeId, int gregId, int jalaliId) {
        TimeZone tz = TimeZone.getTimeZone(zone);
        Date now = new Date();
        SimpleDateFormat tf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        tf.setTimeZone(tz);
        SimpleDateFormat gf = new SimpleDateFormat("yyyy/MM/dd", Locale.US);
        gf.setTimeZone(tz);
        PersianCalendar pc = new PersianCalendar(tz, Locale.getDefault());
        pc.setTimeInMillis(now.getTime());
        String jalali = String.format(Locale.US, "%04d/%02d/%02d",
            pc.get(PersianCalendar.YEAR), pc.get(PersianCalendar.MONTH) + 1, pc.get(PersianCalendar.DAY_OF_MONTH));
        v.setTextViewText(timeId, tf.format(now));
        v.setTextViewText(gregId, gf.format(now));
        v.setTextViewText(jalaliId, toPersianDigits(jalali));
    }

    private String toPersianDigits(String s) {
        return s.replace('0','۰').replace('1','۱').replace('2','۲').replace('3','۳')
            .replace('4','۴').replace('5','۵').replace('6','۶').replace('7','۷')
            .replace('8','۸').replace('9','۹');
    }
}
