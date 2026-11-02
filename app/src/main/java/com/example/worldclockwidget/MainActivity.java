package com.example.worldclockwidget;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class MainActivity extends Activity {
    private static final String PREFS = "world_clock_settings";
    private final Handler handler = new Handler();
    private final TextView[] times = new TextView[3];
    private final TextView[] dates = new TextView[3];
    private final TextView[] offsets = new TextView[3];
    private final String[] keys = {"slot_1","slot_2","slot_3"};
    private final String[] defaults = {"tehran","istanbul","shanghai"};
    private SharedPreferences prefs;
    private Spinner holidayCountry;
    private TextView holidayStatus, holidayList, holidayYearLabel;
    private int holidayYear = Calendar.getInstance().get(Calendar.YEAR);
    private String selectedHolidayCountry = "tehran";

    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            long now = System.currentTimeMillis();
            for (int i=0;i<3;i++) {
                CountryCatalog.Country c = CountryCatalog.byId(prefs.getString(keys[i], defaults[i]));
                times[i].setText(ClockFormatter.time(c, now));
                dates[i].setText(ClockFormatter.date(c, now));
                offsets[i].setText(ClockFormatter.offset(c, now));
            }
            handler.postDelayed(this, 15000);
        }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        selectedHolidayCountry = prefs.getString(keys[0], defaults[0]);
        buildScreen();
    }
    private int dp(float v) { return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }
    private TextView text(String value, int size, int color) {
        TextView t = new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color);
        t.setGravity(Gravity.CENTER); t.setPadding(dp(6),dp(5),dp(6),dp(5)); return t;
    }
    private LinearLayout card(LinearLayout parent) {
        LinearLayout c = new LinearLayout(this); c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(10),dp(10),dp(10),dp(10)); c.setBackgroundColor(Color.rgb(35,43,59));
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1,-2);
        cp.setMargins(0,dp(12),0,0); parent.addView(c,cp); return c;
    }
    private void buildScreen() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16),dp(18),dp(16),dp(20)); root.setBackgroundColor(Color.rgb(18,24,38));
        scroll.addView(root);
        TextView title=text("TIME · ساعت جهانی",25,Color.WHITE); title.setTypeface(null,Typeface.BOLD); root.addView(title);
        TextView subtitle=text("سه شهر دلخواه · ساعت و تقویم محلی",14,Color.LTGRAY); root.addView(subtitle);
        TextView local=text("منطقه زمانی دستگاه: " + ZoneId.systemDefault().getId(),13,Color.rgb(230,190,110));
        root.addView(local);
        for(int i=0;i<3;i++) {
            final int slot=i;
            LinearLayout countryCard=card(root);
            TextView heading=text("جایگاه " + (i+1) + " · انتخاب کشور / شهر",15,Color.WHITE);
            heading.setTypeface(null,Typeface.BOLD); countryCard.addView(heading);
            Spinner spinner=new Spinner(this);
            List<String> labels=new ArrayList<>(); for(CountryCatalog.Country c:CountryCatalog.ALL) labels.add(c.toString());
            ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,labels);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); spinner.setAdapter(adapter);
            spinner.setSelection(CountryCatalog.indexOf(prefs.getString(keys[i],defaults[i])));
            countryCard.addView(spinner);
            times[i]=text("--:--",32,Color.WHITE); times[i].setTypeface(null,Typeface.BOLD); countryCard.addView(times[i]);
            dates[i]=text("",15,Color.rgb(232,193,119)); countryCard.addView(dates[i]);
            offsets[i]=text("",12,Color.LTGRAY); countryCard.addView(offsets[i]);
            spinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                boolean first=true;
                @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                    String selected=CountryCatalog.ALL.get(position).id;
                    if(first) { first=false; return; }
                    prefs.edit().putString(keys[slot],selected).apply();
                    refreshWidget(); ticker.run();
                    if (slot == 0 && holidayCountry != null) {
                        selectedHolidayCountry = selected;
                        holidayCountry.setSelection(position);
                    }
                }
                @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
            });
        }

        buildHolidayCalendar(root);

        TextView note=text("تقویم‌های محلی: شمسی برای ایران، هجری قمری برای عراق و کشورهای خلیج فارس، بودایی برای تایلند، جمهوری چین برای تایوان و میلادی محلی برای سایر کشورها. ساعت تابستانی با پایگاه منطقه زمانی دستگاه اعمال می‌شود.",13,Color.LTGRAY);
        LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(-1,-2); np.setMargins(0,dp(16),0,0); root.addView(note,np);
        TextView refresh=text("به‌روزرسانی ساعت‌ها",15,Color.WHITE); refresh.setBackgroundColor(Color.rgb(30,100,180));
        refresh.setOnClickListener(v->{ticker.run(); refreshWidget();});
        LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,-2); rp.setMargins(0,dp(12),0,0); root.addView(refresh,rp);
        setContentView(scroll);
    }

    private void buildHolidayCalendar(LinearLayout root) {
        LinearLayout section = card(root);
        TextView heading = text("تقویم سالانه · مناسبت‌ها و تعطیلات",19,Color.WHITE);
        heading.setTypeface(null,Typeface.BOLD); section.addView(heading);
        TextView hint = text("کشور را انتخاب کن تا تعطیلات رسمی همان سال نمایش داده شود.",13,Color.LTGRAY);
        section.addView(hint);
        holidayCountry = new Spinner(this);
        List<String> labels = new ArrayList<>();
        for (CountryCatalog.Country c : CountryCatalog.ALL) labels.add(c.toString());
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        holidayCountry.setAdapter(adapter);
        holidayCountry.setSelection(CountryCatalog.indexOf(selectedHolidayCountry));
        section.addView(holidayCountry);
        holidayCountry.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                selectedHolidayCountry = CountryCatalog.ALL.get(position).id;
                loadHolidays(false);
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
        LinearLayout yearRow = new LinearLayout(this); yearRow.setGravity(Gravity.CENTER);
        TextView previous = text("سال قبل",14,Color.WHITE);
        previous.setBackgroundColor(Color.rgb(55,68,88));
        holidayYearLabel = text(String.valueOf(holidayYear),18,Color.rgb(232,193,119));
        TextView next = text("سال بعد",14,Color.WHITE);
        next.setBackgroundColor(Color.rgb(55,68,88));
        yearRow.addView(previous,new LinearLayout.LayoutParams(0,-2,1));
        yearRow.addView(holidayYearLabel,new LinearLayout.LayoutParams(0,-2,1));
        yearRow.addView(next,new LinearLayout.LayoutParams(0,-2,1));
        section.addView(yearRow);
        previous.setOnClickListener(v->{holidayYear--; holidayYearLabel.setText(String.valueOf(holidayYear)); loadHolidays(false);});
        next.setOnClickListener(v->{holidayYear++; holidayYearLabel.setText(String.valueOf(holidayYear)); loadHolidays(false);});
        holidayStatus = text("در حال بررسی تقویم…",12,Color.LTGRAY); section.addView(holidayStatus);
        holidayList = text("",14,Color.WHITE); holidayList.setGravity(Gravity.RIGHT);
        holidayList.setPadding(dp(8),dp(10),dp(8),dp(10)); section.addView(holidayList);
        TextView force = text("دریافت تازه‌ترین تعطیلات",13,Color.rgb(232,193,119));
        force.setOnClickListener(v->loadHolidays(true)); section.addView(force);
    }

    private void loadHolidays(boolean force) {
        if (holidayStatus == null) return;
        CountryCatalog.Country country = CountryCatalog.byId(selectedHolidayCountry);
        holidayStatus.setText("تقویم " + country.name + " · در حال بررسی اطلاعات ذخیره‌شده…");
        HolidayCalendar.load(this, country, holidayYear, force, (json, fromCache, error) ->
                runOnUiThread(() -> {
                    if (!selectedHolidayCountry.equals(country.id) || holidayYear != Integer.parseInt(holidayYearLabel.getText().toString())) return;
                    if (json == null) {
                        holidayStatus.setText(error == null ? "اطلاعات تقویم در دسترس نیست." : error);
                        return;
                    }
                    try {
                        JSONArray items = new JSONArray(json);
                        StringBuilder out = new StringBuilder();
                        for (int i=0;i<items.length();i++) {
                            JSONObject item = items.getJSONObject(i);
                            String date = item.optString("date","");
                            String name = item.optString("localName", item.optString("name",""));
                            String english = item.optString("name","");
                            out.append("• ").append(date).append("\n  ").append(name);
                            if (!english.isEmpty() && !english.equals(name)) out.append(" · ").append(english);
                            out.append("\n\n");
                        }
                        holidayList.setText(out.length()==0 ? "برای این سال موردی از منبع عمومی دریافت نشد." : out.toString());
                        holidayStatus.setText("تعداد مناسبت‌های رسمی: " + items.length()
                                + (fromCache ? " · نمایش از حافظهٔ دستگاه" : " · اطلاعات تازه دریافت شد")
                                + " · ذخیره برای استفادهٔ آفلاین");
                    } catch (Exception e) {
                        holidayStatus.setText("خواندن اطلاعات تقویم با خطا روبه‌رو شد.");
                    }
                }));
    }

    private void refreshWidget() { IntentHelper.update(this); }
    @Override protected void onResume() { super.onResume(); if(prefs!=null) { ticker.run(); if(holidayStatus!=null) loadHolidays(false); } }
    @Override protected void onPause() { super.onPause(); handler.removeCallbacks(ticker); }
}
