package com.example.worldclockwidget;

import android.content.Context;
import android.content.SharedPreferences;

import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.TimeZone;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class HolidayCalendar {
    private static final String PREFS = "holiday_calendar_cache";
    private static final long MAX_AGE = 7L * 24L * 60L * 60L * 1000L;
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private HolidayCalendar() {}

    public interface Callback {
        void onResult(String json, boolean fromCache, String error);
    }

    public static String countryCode(String id) {
        switch (id) {
            case "tehran": return "IR";
            case "istanbul": return "TR";
            case "shanghai": return "CN";
            case "germany": return "DE";
            case "france": return "FR";
            case "usa": return "US";
            case "uk": return "GB";
            case "russia": return "RU";
            case "iraq": return "IQ";
            case "uae": return "AE";
            case "qatar": return "QA";
            case "malaysia": return "MY";
            case "thailand": return "TH";
            case "vietnam": return "VN";
            case "taiwan": return "TW";
            case "india": return "IN";
            case "indonesia": return "ID";
            default: return "";
        }
    }

    private static String key(String id, int year) { return id + "_" + year; }

    private static int[] gregorianYears(CountryCatalog.Country country, int localYear) {
        Calendar start = Calendar.getInstance(TimeZone.getTimeZone(country.zone),
                CalendarRenderer.locale(country));
        start.clear();
        start.set(Calendar.YEAR, localYear);
        start.set(Calendar.MONTH, 0);
        start.set(Calendar.DAY_OF_MONTH, 1);
        start.set(Calendar.HOUR_OF_DAY, 0);
        long startMillis = start.getTimeInMillis();

        Calendar end = (Calendar) start.clone();
        end.add(Calendar.YEAR, 1);
        long endMillis = end.getTimeInMillis() - 1L;

        java.util.Calendar gregorian = java.util.Calendar.getInstance(
                java.util.TimeZone.getTimeZone("UTC"), Locale.US);
        gregorian.setTimeInMillis(startMillis);
        int first = gregorian.get(java.util.Calendar.YEAR);
        gregorian.setTimeInMillis(endMillis);
        int last = gregorian.get(java.util.Calendar.YEAR);
        return new int[]{first, last};
    }

    public static void load(Context context, CountryCatalog.Country country, int localYear,
                            boolean force, Callback callback) {
        SharedPreferences cache = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String k = key(country.id, localYear);
        String cached = cache.getString(k, null);
        long saved = cache.getLong(k + "_saved", 0L);
        boolean fresh = cached != null && System.currentTimeMillis() - saved < MAX_AGE;
        if (cached != null) callback.onResult(cached, true, null);
        if (fresh && !force) return;

        String code = countryCode(country.id);
        if (code.isEmpty()) {
            if (cached == null) callback.onResult(null, false, "کد کشور برای دریافت تعطیلات تعریف نشده است.");
            return;
        }

        int[] years = gregorianYears(country, localYear);
        EXECUTOR.execute(() -> {
            Map<String, JSONObject> merged = new LinkedHashMap<>();
            boolean anySuccess = false;
            String lastError = null;
            for (int year = years[0]; year <= years[1]; year++) {
                HttpURLConnection connection = null;
                try {
                    URL url = new URL("https://date.nager.at/api/v3/PublicHolidays/" + year + "/" + code);
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setConnectTimeout(8000);
                    connection.setReadTimeout(10000);
                    connection.setRequestProperty("Accept", "application/json");
                    int status = connection.getResponseCode();
                    if (status != 200) throw new Exception("HTTP " + status);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(
                            connection.getInputStream(), StandardCharsets.UTF_8));
                    StringBuilder body = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) body.append(line);
                    reader.close();
                    JSONArray parsed = new JSONArray(body.toString());
                    for (int i = 0; i < parsed.length(); i++) {
                        JSONObject item = parsed.getJSONObject(i);
                        String identity = item.optString("date") + "|" + item.optString("name");
                        merged.put(identity, item);
                    }
                    anySuccess = true;
                } catch (Exception e) {
                    lastError = e.getMessage();
                } finally {
                    if (connection != null) connection.disconnect();
                }
            }

            if (anySuccess) {
                JSONArray result = new JSONArray();
                for (JSONObject item : merged.values()) result.put(item);
                String json = result.toString();
                cache.edit().putString(k, json)
                        .putLong(k + "_saved", System.currentTimeMillis()).apply();
                callback.onResult(json, false, lastError);
            } else if (cached == null) {
                callback.onResult(null, false,
                        "تعطیلات این کشور در منبع فعلی پوشش داده نشده یا اتصال برقرار نیست. (" +
                                (lastError == null ? "نامشخص" : lastError) + ")");
            }
        });
    }
}
