package com.example.worldclockwidget;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ibm.icu.text.DateFormatSymbols;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.TimeZone;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class CalendarRenderer {
    private static final int NAVY = Color.rgb(8, 18, 31);
    private static final int GOLD = Color.rgb(245, 199, 107);
    private static final int MUTED = Color.rgb(165, 180, 198);

    private CalendarRenderer() {}

    public static Locale locale(CountryCatalog.Country country) {
        String tag = country.localeTag;
        if ("persian".equals(country.calendar)) tag += "-u-ca-persian";
        else if ("islamic".equals(country.calendar)) tag += "-u-ca-islamic";
        else if ("buddhist".equals(country.calendar)) tag += "-u-ca-buddhist";
        else if ("roc".equals(country.calendar)) tag += "-u-ca-roc";
        return Locale.forLanguageTag(tag);
    }

    public static int currentYear(CountryCatalog.Country country) {
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone(country.zone), locale(country));
        return c.get(Calendar.YEAR);
    }

    public static void render(Activity activity, LinearLayout target,
                              CountryCatalog.Country country, int year, JSONArray holidays) {
        render(activity, target, country, year, holidays, 1);
    }

    public static void render(Activity activity, LinearLayout target,
                              CountryCatalog.Country country, int year, JSONArray holidays, int mode) {
        target.removeAllViews();
        Locale locale = locale(country);
        TimeZone zone = TimeZone.getTimeZone(country.zone);
        Map<Integer, Map<Integer, List<String>>> byMonth = new HashMap<>();

        if (holidays != null) {
            for (int i = 0; i < holidays.length(); i++) {
                try {
                    JSONObject item = holidays.getJSONObject(i);
                    org.json.JSONArray types = item.optJSONArray("types");
                    if (types != null && types.length() > 0) {
                        boolean official = false;
                        for (int typeIndex = 0; typeIndex < types.length(); typeIndex++) {
                            String type = types.optString(typeIndex, "");
                            if ("Public".equalsIgnoreCase(type) || "Bank".equalsIgnoreCase(type)
                                    || "Authorities".equalsIgnoreCase(type)) official = true;
                        }
                        if (!official) continue;
                    }
                    LocalDate date = LocalDate.parse(item.optString("date"));
                    java.util.GregorianCalendar civil = new java.util.GregorianCalendar(
                            java.util.TimeZone.getTimeZone(country.zone), Locale.US);
                    civil.clear();
                    civil.set(date.getYear(), date.getMonthValue() - 1, date.getDayOfMonth(), 12, 0, 0);
                    Calendar local = Calendar.getInstance(zone, locale);
                    local.setTimeInMillis(civil.getTimeInMillis());
                    if (local.get(Calendar.YEAR) != year) continue;
                    int month = local.get(Calendar.MONTH);
                    int day = local.get(Calendar.DAY_OF_MONTH);
                    String name = item.optString("localName", item.optString("name", "Holiday"));
                    String english = item.optString("name", "");
                    String display = dayNumber(day, locale) + "  " + name;
                    if (!english.isEmpty() && !english.equals(name)) display += " · " + english;
                    byMonth.computeIfAbsent(month, k -> new HashMap<>())
                            .computeIfAbsent(day, k -> new ArrayList<>()).add(display);
                } catch (Exception ignored) {}
            }
        }

        Calendar month = Calendar.getInstance(zone, locale);
        month.clear();
        month.set(Calendar.YEAR, year);
        month.set(Calendar.MONTH, 0);
        month.set(Calendar.DAY_OF_MONTH, 1);
        month.set(Calendar.HOUR_OF_DAY, 12);

        int firstMonth = mode == 0 ? Calendar.getInstance(zone, locale).get(Calendar.MONTH) : 0;
        int lastMonth = mode == 0 ? firstMonth + 1 : 12;
        for (int m = firstMonth; m < lastMonth; m++) {
            month.set(Calendar.MONTH, m);
            month.set(Calendar.DAY_OF_MONTH, 1);
            LinearLayout card = new LinearLayout(activity);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(activity, 13), dp(activity, 11), dp(activity, 13), dp(activity, 11));
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(NAVY);
            bg.setCornerRadius(dp(activity, 15));
            bg.setStroke(dp(activity, 1), Color.rgb(35, 68, 99));
            card.setBackground(bg);
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            cardParams.setMargins(0, dp(activity, 7), 0, dp(activity, 4));
            target.addView(card, cardParams);

            TextView title = label(activity,
                    new SimpleDateFormat("MMMM", locale).format(month.getTime()) + "  ·  " + year,
                    17, GOLD, true);
            title.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
            card.addView(title);

            if (mode != 2) {
            int firstDay = month.getFirstDayOfWeek();
            int actualFirst = month.get(Calendar.DAY_OF_WEEK);
            int leading = (actualFirst - firstDay + 7) % 7;
            int days = month.getActualMaximum(Calendar.DAY_OF_MONTH);
            String[] weekdays = new DateFormatSymbols(locale).getShortWeekdays();

            LinearLayout weekHeader = new LinearLayout(activity);
            weekHeader.setOrientation(LinearLayout.HORIZONTAL);
            weekHeader.setLayoutDirection(android.view.View.LAYOUT_DIRECTION_LTR);
            for (int col = 0; col < 7; col++) {
                int weekday = ((firstDay - 1 + col) % 7) + 1;
                String name = weekdays[weekday];
                TextView cell = label(activity, name == null ? "" : name, 10, MUTED, false);
                weekHeader.addView(cell, new LinearLayout.LayoutParams(0, dp(activity, 25), 1));
            }
            card.addView(weekHeader);

            LinearLayout week = null;
            Map<Integer, List<String>> monthHolidays = byMonth.getOrDefault(m, new HashMap<>());
            for (int cellIndex = 0; cellIndex < leading + days; cellIndex++) {
                if (cellIndex % 7 == 0) {
                    week = new LinearLayout(activity);
                    week.setOrientation(LinearLayout.HORIZONTAL);
                    week.setLayoutDirection(android.view.View.LAYOUT_DIRECTION_LTR);
                    card.addView(week);
                }
                int day = cellIndex - leading + 1;
                TextView cell;
                if (day < 1 || day > days) {
                    cell = label(activity, "", 12, Color.WHITE, false);
                } else {
                    boolean holiday = monthHolidays.containsKey(day);
                    cell = label(activity, dayNumber(day, locale), 12,
                            holiday ? GOLD : Color.WHITE, holiday);
                    if (holiday) {
                        GradientDrawable mark = new GradientDrawable();
                        mark.setColor(Color.rgb(75, 54, 20));
                        mark.setCornerRadius(dp(activity, 8));
                        cell.setBackground(mark);
                    }
                }
                week.addView(cell, new LinearLayout.LayoutParams(0, dp(activity, 29), 1));
            }
            if (week != null) {
                int used = (leading + days) % 7;
                if (used != 0) for (int col = used; col < 7; col++) {
                    week.addView(label(activity, "", 12, Color.WHITE, false),
                            new LinearLayout.LayoutParams(0, dp(activity, 29), 1));
                }
            }

            }

            ViewDivider.add(activity, card);
            if (monthHolidays.isEmpty()) {
                TextView none = label(activity, "تعطیلی ثبت‌شده‌ای برای این ماه در منبع موجود نیست.", 11, MUTED, false);
                none.setGravity(Gravity.RIGHT);
                card.addView(none);
            } else {
                for (int day = 1; day <= days; day++) {
                    List<String> entries = monthHolidays.get(day);
                    if (entries == null) continue;
                    for (String entry : entries) {
                        TextView holiday = label(activity, "•  " + entry, 12, GOLD, false);
                        holiday.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
                        holiday.setPadding(dp(activity, 4), dp(activity, 4), dp(activity, 4), dp(activity, 4));
                        card.addView(holiday);
                    }
                }
            }
        }
    }

    private static String dayNumber(int day, Locale locale) {
        return NumberFormat.getIntegerInstance(locale).format(day);
    }

    private static TextView label(Activity activity, String value, int size, int color, boolean bold) {
        TextView t = new TextView(activity);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER);
        t.setIncludeFontPadding(false);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private static int dp(Activity a, float value) {
        return (int) (value * a.getResources().getDisplayMetrics().density + 0.5f);
    }

    private static final class ViewDivider {
        static void add(Activity activity, LinearLayout parent) {
            TextView line = new TextView(activity);
            line.setBackgroundColor(Color.rgb(35, 68, 99));
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(activity, 1));
            p.topMargin = dp(activity, 8);
            p.bottomMargin = dp(activity, 6);
            parent.addView(line, p);
        }
    }
}
