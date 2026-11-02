package com.example.worldclockwidget;

import com.ibm.icu.text.DateFormat;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.TimeZone;
import java.util.Date;
import java.util.Locale;

public final class ClockFormatter {
    private ClockFormatter() {}
    public static String time(CountryCatalog.Country c, long millis) {
        DateFormat f = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.forLanguageTag(c.localeTag));
        f.setTimeZone(TimeZone.getTimeZone(c.zone));
        return f.format(new Date(millis));
    }
    public static String date(CountryCatalog.Country c, long millis) {
        Locale locale = Locale.forLanguageTag(c.localeTag);
        String tag = c.localeTag;
        if ("persian".equals(c.calendar)) locale = new Locale.Builder().setLanguageTag(tag + "-u-ca-persian").build();
        else if ("buddhist".equals(c.calendar)) locale = new Locale.Builder().setLanguageTag(tag + "-u-ca-buddhist").build();
        else if ("roc".equals(c.calendar)) locale = new Locale.Builder().setLanguageTag(tag + "-u-ca-roc").build();
        else if ("islamic".equals(c.calendar)) locale = new Locale.Builder().setLanguageTag(tag + "-u-ca-islamic").build();
        DateFormat f = DateFormat.getDateInstance(DateFormat.MEDIUM, locale);
        f.setTimeZone(TimeZone.getTimeZone(c.zone));
        return f.format(new Date(millis));
    }
    public static String offset(CountryCatalog.Country c, long millis) {
        TimeZone z = TimeZone.getTimeZone(c.zone);
        int total = z.getOffset(millis) / 60000;
        int h = Math.abs(total) / 60, m = Math.abs(total) % 60;
        return String.format(Locale.US, "UTC%s%02d:%02d", total < 0 ? "-" : "+", h, m);
    }
}
