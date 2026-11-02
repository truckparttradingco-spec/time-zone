package com.example.worldclockwidget;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
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

    public static void load(Context context, CountryCatalog.Country country, int year,
                            boolean force, Callback callback) {
        SharedPreferences cache = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String k = key(country.id, year);
        String cached = cache.getString(k, null);
        long saved = cache.getLong(k + "_saved", 0L);
        boolean fresh = cached != null && System.currentTimeMillis() - saved < MAX_AGE;
        if (cached != null) callback.onResult(cached, true, null);
        if (fresh && !force) return;

        String code = countryCode(country.id);
        EXECUTOR.execute(() -> {
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
                cache.edit().putString(k, parsed.toString())
                        .putLong(k + "_saved", System.currentTimeMillis()).apply();
                callback.onResult(parsed.toString(), false, null);
            } catch (Exception e) {
                if (cached == null) callback.onResult(null, false,
                        "فعلاً دریافت تعطیلات این کشور ممکن نیست. اتصال اینترنت را بررسی کنید.");
            } finally {
                if (connection != null) connection.disconnect();
            }
        });
    }
}
