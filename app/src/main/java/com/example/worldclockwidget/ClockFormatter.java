package com.example.worldclockwidget;

import com.ibm.icu.text.DateFormat;
import com.ibm.icu.util.TimeZone;
import java.util.Date;
import java.util.Locale;

public final class ClockFormatter {
    private ClockFormatter() {}

    public static String time(CountryCatalog.Country c, long millis) {
        return time(c, millis, false);
    }

    public static String time(CountryCatalog.Country c, long millis, boolean force24Hour) {
        if (force24Hour) {
            com.ibm.icu.text.SimpleDateFormat f = new com.ibm.icu.text.SimpleDateFormat("HH:mm", Locale.forLanguageTag(c.localeTag));
            f.setTimeZone(TimeZone.getTimeZone(c.zone));
            return f.format(new Date(millis));
        }
        DateFormat f = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.forLanguageTag(c.localeTag));
        f.setTimeZone(TimeZone.getTimeZone(c.zone));
        return f.format(new Date(millis));
    }

    public static String date(CountryCatalog.Country c, long millis) {
        Locale local = Locale.forLanguageTag(c.localeTag);
        if ("persian".equals(c.calendar)) local = new Locale.Builder().setLanguageTag(c.localeTag + "-u-ca-persian").build();
        else if ("buddhist".equals(c.calendar)) local = new Locale.Builder().setLanguageTag(c.localeTag + "-u-ca-buddhist").build();
        else if ("roc".equals(c.calendar)) local = new Locale.Builder().setLanguageTag(c.localeTag + "-u-ca-roc").build();
        else if ("islamic".equals(c.calendar)) local = new Locale.Builder().setLanguageTag(c.localeTag + "-u-ca-islamic").build();

        DateFormat localFormat = DateFormat.getDateInstance(DateFormat.MEDIUM, local);
        localFormat.setTimeZone(TimeZone.getTimeZone(c.zone));
        String localDate = localFormat.format(new Date(millis));

        if ("gregory".equals(c.calendar)) return localDate;

        Locale gregorian = new Locale.Builder().setLanguageTag(c.localeTag + "-u-ca-gregory").build();
        DateFormat gregorianFormat = DateFormat.getDateInstance(DateFormat.MEDIUM, gregorian);
        gregorianFormat.setTimeZone(TimeZone.getTimeZone(c.zone));
        return localDate + "  |  " + gregorianFormat.format(new Date(millis));
    }


    /** Compact date in the selected city's local calendar and language. */
    public static String widgetDate(CountryCatalog.Country c, long millis) {
        Locale locale;
        String pattern;
        boolean uppercase = false;
        switch (c.id) {
            case "tehran":
                locale = new Locale.Builder().setLanguageTag("fa-IR-u-ca-persian").build();
                pattern = "d MMMM";
                break;
            case "istanbul":
                locale = Locale.forLanguageTag("tr-TR");
                pattern = "d MMMM";
                break;
            case "shanghai":
                locale = Locale.US;
                pattern = "d MMM";
                uppercase = true;
                break;
            default:
                locale = Locale.forLanguageTag(c.localeTag);
                pattern = "d MMM";
                break;
        }
        com.ibm.icu.text.SimpleDateFormat f =
                new com.ibm.icu.text.SimpleDateFormat(pattern, locale);
        f.setTimeZone(TimeZone.getTimeZone(c.zone));
        String value = f.format(new Date(millis));
        return uppercase ? value.toUpperCase(Locale.US) : value;
    }

    public static String offset(CountryCatalog.Country c, long millis) {
        TimeZone z = TimeZone.getTimeZone(c.zone);
        int total = z.getOffset(millis) / 60000;
        int h = Math.abs(total) / 60, m = Math.abs(total) % 60;
        return String.format(Locale.US, "UTC%s%02d:%02d", total < 0 ? "-" : "+", h, m);
    }
}
