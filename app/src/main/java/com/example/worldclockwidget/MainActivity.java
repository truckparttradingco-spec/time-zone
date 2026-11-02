package com.example.worldclockwidget;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
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
import java.time.ZoneId;
import java.util.ArrayList;
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
        buildScreen();
    }
    private int dp(float v) { return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }
    private TextView text(String value, int size, int color) {
        TextView t = new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color);
        t.setGravity(Gravity.CENTER); t.setPadding(dp(6),dp(5),dp(6),dp(5)); return t;
    }
    private void buildScreen() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16),dp(18),dp(16),dp(20)); root.setBackgroundColor(Color.rgb(18,24,38));
        scroll.addView(root);
        TextView title=text("ساعت جهانی",25,Color.WHITE); title.setTypeface(null,Typeface.BOLD); root.addView(title);
        TextView subtitle=text("سه شهر دلخواه · ساعت و تقویم محلی",14,Color.LTGRAY); root.addView(subtitle);
        TextView local=text("منطقه زمانی دستگاه: " + ZoneId.systemDefault().getId(),13,Color.rgb(230,190,110));
        root.addView(local);
        for(int i=0;i<3;i++) {
            final int slot=i;
            LinearLayout card=new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(10),dp(10),dp(10),dp(10)); card.setBackgroundColor(Color.rgb(35,43,59));
            LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2); cp.setMargins(0,dp(12),0,0); root.addView(card,cp);
            TextView heading=text("جایگاه " + (i+1) + " · انتخاب کشور / شهر",15,Color.WHITE);
            heading.setTypeface(null,Typeface.BOLD); card.addView(heading);
            Spinner spinner=new Spinner(this);
            List<String> labels=new ArrayList<>(); for(CountryCatalog.Country c:CountryCatalog.ALL) labels.add(c.toString());
            ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,labels);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); spinner.setAdapter(adapter);
            spinner.setSelection(CountryCatalog.indexOf(prefs.getString(keys[i],defaults[i])));
            card.addView(spinner);
            times[i]=text("--:--",32,Color.WHITE); times[i].setTypeface(null,Typeface.BOLD); card.addView(times[i]);
            dates[i]=text("",15,Color.rgb(232,193,119)); card.addView(dates[i]);
            offsets[i]=text("",12,Color.LTGRAY); card.addView(offsets[i]);
            spinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                boolean first=true;
                @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                    String selected=CountryCatalog.ALL.get(position).id;
                    if(first) { first=false; return; }
                    prefs.edit().putString(keys[slot],selected).apply();
                    refreshWidget();
                    ticker.run();
                }
                @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
            });
        }
        TextView note=text("تقویم‌ها: شمسی برای ایران، هجری قمری برای عراق و کشورهای خلیج فارس، بودایی برای تایلند، جمهوری چین برای تایوان و میلادی محلی برای سایر کشورها. ساعت تابستانی با پایگاه منطقه زمانی دستگاه اعمال می‌شود.",13,Color.LTGRAY);
        LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(-1,-2); np.setMargins(0,dp(16),0,0); root.addView(note,np);
        TextView refresh=text("به‌روزرسانی ساعت‌ها",15,Color.WHITE); refresh.setBackgroundColor(Color.rgb(30,100,180));
        refresh.setOnClickListener(v->{ticker.run(); refreshWidget();});
        LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,-2); rp.setMargins(0,dp(12),0,0); root.addView(refresh,rp);
        setContentView(scroll);
    }
    private void refreshWidget() {
        IntentHelper.update(this);
    }
    @Override protected void onResume() { super.onResume(); if(prefs!=null) ticker.run(); }
    @Override protected void onPause() { super.onPause(); handler.removeCallbacks(ticker); }
}
