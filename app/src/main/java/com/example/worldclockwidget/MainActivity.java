package com.example.worldclockwidget;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
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
import java.util.Locale;

public class MainActivity extends Activity {
    private static final String PREFS = "world_clock_settings";
    private final Handler handler = new Handler();
    private final TextView[] times = new TextView[3], dates = new TextView[3], offsets = new TextView[3];
    private final String[] keys = {"slot_1","slot_2","slot_3"};
    private final String[] defaults = {"germany","istanbul","shanghai"};
    private SharedPreferences prefs;
    private LinearLayout root, content, nav;
    private TextView holidayStatus, holidayYearLabel;
    private LinearLayout holidayMonths;
    private int holidayYear = Calendar.getInstance().get(Calendar.YEAR);
    private String selectedHolidayCountry = "germany";
    private int currentTab = 0;
    private int selectedSlot = 0;
    private String countryQuery = "";

    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            if (prefs == null || currentTab != 0) return;
            long now = System.currentTimeMillis();
            for (int i=0;i<3;i++) {
                CountryCatalog.Country c = CountryCatalog.byId(prefs.getString(keys[i], defaults[i]));
                times[i].setText(ClockFormatter.time(c, now, prefs.getBoolean("use_24_hour", true)));
                dates[i].setText(ClockFormatter.date(c, now));
                offsets[i].setText(ClockFormatter.offset(c, now));
            }
            handler.postDelayed(this, 1000);
        }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        selectedHolidayCountry = prefs.getString("holiday_country", prefs.getString(keys[0], defaults[0]));
        holidayYear = CalendarRenderer.currentYear(CountryCatalog.byId(selectedHolidayCountry));
        buildShell();
        showTab(0);
    }

    private int dp(float v) { return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }
    private TextView text(String value, int size, int color) {
        TextView t = new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color);
        t.setGravity(Gravity.CENTER); t.setPadding(dp(7),dp(7),dp(7),dp(7)); return t;
    }
    private GradientDrawable panel(int color, int stroke) {
        GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(16)); d.setStroke(dp(1),stroke); return d;
    }
    private LinearLayout vertical() { LinearLayout l=new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l; }
    private LinearLayout horizontal() { LinearLayout l=new LinearLayout(this); l.setOrientation(LinearLayout.HORIZONTAL); l.setGravity(Gravity.CENTER_VERTICAL); return l; }
    private LinearLayout card(LinearLayout parent) {
        LinearLayout c=vertical(); c.setPadding(dp(12),dp(10),dp(12),dp(10)); c.setBackground(panel(Color.rgb(8,17,29),Color.rgb(38,76,112)));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2); p.setMargins(0,dp(9),0,0); parent.addView(c,p); return c;
    }
    private Button button(String label, Runnable action) {
        Button b=new Button(this); b.setText(label); b.setTextColor(Color.WHITE); b.setTextSize(12);
        b.setAllCaps(false); b.setBackground(panel(Color.rgb(24,47,72),Color.rgb(62,105,145)));
        b.setOnClickListener(v->action.run()); return b;
    }
    private void buildShell() {
        ScrollView scroll=new ScrollView(this);
        root=vertical(); root.setPadding(dp(15),dp(12),dp(15),dp(16));
        root.setBackgroundColor(Color.BLACK); scroll.addView(root);
        LinearLayout header=horizontal();
        TextView logo=text("TIME",22,Color.WHITE); logo.setTypeface(Typeface.SERIF,Typeface.BOLD);
        logo.setBackground(panel(Color.rgb(190,20,30),Color.rgb(255,90,90)));
        header.addView(logo);
        TextView tagline=text("  WORLD CLOCK",15,Color.rgb(245,199,107)); tagline.setTypeface(null,Typeface.BOLD);
        header.addView(tagline); root.addView(header);
        content=vertical(); root.addView(content,new LinearLayout.LayoutParams(-1,-2));
        nav=horizontal(); nav.setGravity(Gravity.CENTER); nav.setPadding(0,dp(8),0,0);
        String[] labels={"خانه","کشورها","تقویم","تنظیمات"};
        for(int i=0;i<labels.length;i++) {
            final int tab=i;
            Button b=button(labels[i],()->showTab(tab));
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1); p.setMargins(dp(2),0,dp(2),0); nav.addView(b,p);
        }
        root.addView(nav);
        setContentView(scroll);
    }
    private void showTab(int tab) {
        currentTab=tab; content.removeAllViews();
        String[] names={"خانه","کشورها","تقویم","تنظیمات"};
        TextView heading=text(names[tab],23,Color.WHITE); heading.setTypeface(null,Typeface.BOLD);
        content.addView(heading);
        if(tab==0) buildHome();
        else if(tab==1) buildCountries();
        else if(tab==2) buildCalendar();
        else buildSettings();
        for(int i=0;i<nav.getChildCount();i++) {
            Button b=(Button)nav.getChildAt(i);
            b.setBackground(panel(i==tab?Color.rgb(150,25,35):Color.rgb(24,47,72),i==tab?Color.rgb(255,100,100):Color.rgb(62,105,145)));
        }
    }

    private void buildHome() {
        TextView subtitle=text("ساعت شهرهای انتخابی شما · به‌روزرسانی خودکار هر ثانیه",13,Color.LTGRAY);
        content.addView(subtitle);
        TextView local=text("منطقه زمانی دستگاه: "+ZoneId.systemDefault().getId(),12,Color.rgb(245,199,107)); content.addView(local);
        for(int i=0;i<3;i++) {
            final int slot=i;
            LinearLayout c=card(content);
            TextView title=text("جایگاه "+(i+1)+" · انتخاب شهر",14,Color.rgb(245,199,107)); title.setTypeface(null,Typeface.BOLD); c.addView(title);
            Spinner spinner=new Spinner(this);
            List<String> labels=new ArrayList<>(); for(CountryCatalog.Country country:CountryCatalog.ALL) labels.add(country.toString());
            ArrayAdapter<String> adapter=new ArrayAdapter<>(this,R.layout.spinner_item,labels);
            adapter.setDropDownViewResource(R.layout.spinner_dropdown_item); spinner.setAdapter(adapter);
            spinner.setSelection(CountryCatalog.indexOf(prefs.getString(keys[i],defaults[i]))); c.addView(spinner);
            times[i]=text("--:--",38,Color.WHITE); times[i].setTypeface(Typeface.DEFAULT,Typeface.BOLD); c.addView(times[i]);
            dates[i]=text("",14,Color.LTGRAY); c.addView(dates[i]);
            offsets[i]=text("",12,Color.rgb(245,199,107)); c.addView(offsets[i]);
            spinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                boolean first=true;
                @Override public void onItemSelected(android.widget.AdapterView<?> p,View v,int position,long id) {
                    if(first){first=false;return;}
                    prefs.edit().putString(keys[slot],CountryCatalog.ALL.get(position).id).apply();
                    if(slot==0) { selectedHolidayCountry=CountryCatalog.ALL.get(position).id; prefs.edit().putString("holiday_country",selectedHolidayCountry).apply(); }
                    refreshWidget(); ticker.run();
                }
                @Override public void onNothingSelected(android.widget.AdapterView<?> p) {}
            });
        }
        LinearLayout actions=horizontal();
        actions.addView(button("مدیریت شهرها",()->showTab(1)),new LinearLayout.LayoutParams(0,-2,1));
        actions.addView(button("تنظیمات",()->showTab(3)),new LinearLayout.LayoutParams(0,-2,1));
        content.addView(actions);
        ticker.run();
    }

    private void buildCountries() {
        TextView intro=text("شهر دلخواه را جست‌وجو و با یک لمس در یکی از سه جایگاه ساعت قرار بده.",13,Color.LTGRAY); content.addView(intro);
        EditText search=new EditText(this); search.setSingleLine(true); search.setTextColor(Color.WHITE);
        search.setHintTextColor(Color.GRAY); search.setHint("جست‌وجوی کشور یا شهر…"); search.setText(countryQuery);
        search.setBackground(panel(Color.rgb(12,24,39),Color.rgb(62,105,145))); content.addView(search);
        TextView slotLabel=text("انتخاب جایگاه مقصد",13,Color.rgb(245,199,107)); content.addView(slotLabel);
        String[] slotNames={"جایگاه ۱","جایگاه ۲","جایگاه ۳"};
        Spinner slotSpinner=new Spinner(this);
        slotSpinner.setAdapter(new ArrayAdapter<>(this,R.layout.spinner_item,slotNames));
        ArrayAdapter<String> slotAdapter=new ArrayAdapter<>(this,R.layout.spinner_item,slotNames);
        slotAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        slotSpinner.setAdapter(slotAdapter);
        slotSpinner.setSelection(selectedSlot);
        slotSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){
            @Override public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){selectedSlot=pos;}
            @Override public void onNothingSelected(android.widget.AdapterView<?> p){}
        });
        content.addView(slotSpinner);
        LinearLayout list=vertical(); content.addView(list);
        Runnable render=()->{
            list.removeAllViews();
            String q=countryQuery.trim().toLowerCase(new Locale("fa"));
            for(CountryCatalog.Country c:CountryCatalog.ALL) {
                if(!q.isEmpty()&&!c.name.toLowerCase(new Locale("fa")).contains(q)&&!c.id.contains(q)&&!c.zone.toLowerCase(Locale.ROOT).contains(q)) continue;
                LinearLayout row=card(list);
                TextView name=text(c.toString(),15,Color.WHITE); name.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL); row.addView(name);
                TextView meta=text(c.zone+"  ·  "+c.calendar,11,Color.LTGRAY); meta.setGravity(Gravity.RIGHT); row.addView(meta);
                row.addView(button("افزودن به "+slotNames[selectedSlot],()->{
                    prefs.edit().putString(keys[selectedSlot],c.id).apply();
                    refreshWidget(); showTab(0);
                }));
            }
            if(list.getChildCount()==0) list.addView(text("موردی پیدا نشد.",14,Color.LTGRAY));
        };
        search.addTextChangedListener(new TextWatcher(){
            @Override public void beforeTextChanged(CharSequence s,int st,int count,int after){}
            @Override public void onTextChanged(CharSequence s,int st,int before,int count){countryQuery=s.toString();render.run();}
            @Override public void afterTextChanged(Editable e){}
        });
        render.run();
    }

    private void buildCalendar() {
        LinearLayout section=card(content);
        TextView hint=text("تقویم سالانه کشور · ماه‌ها و روزها بر اساس تقویم و زبان همان کشور · تعطیلات رسمی زیر هر ماه",13,Color.LTGRAY);
        section.addView(hint);
        Spinner country=new Spinner(this);
        List<String> labels=new ArrayList<>(); for(CountryCatalog.Country c:CountryCatalog.ALL) labels.add(c.toString());
        ArrayAdapter<String> adapter=new ArrayAdapter<>(this,R.layout.spinner_item,labels);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item); country.setAdapter(adapter);
        country.setSelection(CountryCatalog.indexOf(selectedHolidayCountry)); section.addView(country);
        country.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){
            @Override public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){
                selectedHolidayCountry=CountryCatalog.ALL.get(pos).id;
                prefs.edit().putString("holiday_country",selectedHolidayCountry).apply();
                holidayYear=CalendarRenderer.currentYear(CountryCatalog.ALL.get(pos));
                if (holidayYearLabel != null) holidayYearLabel.setText(String.valueOf(holidayYear));
                loadHolidays(false);
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> p){}
        });
        LinearLayout yearRow=horizontal();
        yearRow.addView(button("سال قبل",()->{holidayYear--;holidayYearLabel.setText(String.valueOf(holidayYear));loadHolidays(false);}),new LinearLayout.LayoutParams(0,-2,1));
        holidayYearLabel=text(String.valueOf(holidayYear),19,Color.rgb(245,199,107));
        yearRow.addView(holidayYearLabel,new LinearLayout.LayoutParams(0,-2,1));
        yearRow.addView(button("سال بعد",()->{holidayYear++;holidayYearLabel.setText(String.valueOf(holidayYear));loadHolidays(false);}),new LinearLayout.LayoutParams(0,-2,1));
        section.addView(yearRow);
        holidayStatus=text("در حال دریافت تقویم…",12,Color.LTGRAY); section.addView(holidayStatus);
        holidayMonths=vertical();
        content.addView(holidayMonths,new LinearLayout.LayoutParams(-1,-2));
        section.addView(button("دریافت مجدد اطلاعات",()->loadHolidays(true)));
        loadHolidays(false);
    }

    private void loadHolidays(boolean force) {
        if(holidayStatus==null||holidayYearLabel==null||holidayMonths==null) return;
        CountryCatalog.Country country=CountryCatalog.byId(selectedHolidayCountry);
        int requestedYear=holidayYear;
        holidayStatus.setText("تقویم "+country.name+" · در حال دریافت تعطیلات…");
        HolidayCalendar.load(this,country,requestedYear,force,(json,fromCache,error)->runOnUiThread(()->{
            if(currentTab!=2||!selectedHolidayCountry.equals(country.id)||holidayYear!=requestedYear)return;
            if(json==null){
                holidayStatus.setText(error==null?"اطلاعات تعطیلات در دسترس نیست.":error);
                CalendarRenderer.render(this,holidayMonths,country,requestedYear,new JSONArray());
                return;
            }
            try {
                JSONArray items=new JSONArray(json);
                CalendarRenderer.render(this,holidayMonths,country,requestedYear,items);
                holidayStatus.setText("تعطیلات ثبت‌شده در منبع: "+items.length()+
                        (fromCache?" · حافظه دستگاه":" · دریافت تازه")+
                        " · منبع: Nager.Date · برخی تعطیلات قمری وابسته به رؤیت هلال ممکن است درج نشده باشند.");
            }catch(Exception e){
                holidayStatus.setText("ساخت تقویم ناموفق بود: "+e.getMessage());
            }
        }));
    }

    private void buildSettings() {
        LinearLayout c=card(content);
        TextView info=text("تنظیمات در حافظه دستگاه ذخیره می‌شوند و پس از بستن برنامه باقی می‌مانند.",13,Color.LTGRAY); c.addView(info);
        android.widget.Switch clockSwitch=new android.widget.Switch(this);
        clockSwitch.setText("نمایش ساعت ۲۴ ساعته"); clockSwitch.setTextColor(Color.WHITE);
        clockSwitch.setChecked(prefs.getBoolean("use_24_hour",false)); c.addView(clockSwitch);
        clockSwitch.setOnCheckedChangeListener((button,checked)->{
            prefs.edit().putBoolean("use_24_hour",checked).apply(); refreshWidget(); if(currentTab==0){ticker.run();}
        });
        TextView refresh=text("بازه به‌روزرسانی ساعت: ۱ ثانیه",14,Color.WHITE); c.addView(refresh);
        TextView detail=text("ساعت‌های داخل اپ هر ثانیه تازه می‌شوند. ویجت با ساعت سیستمی Android مستقل از باز بودن اپ کار می‌کند.",12,Color.LTGRAY); c.addView(detail);
        c.addView(button("بازگردانی شهرهای پیش‌فرض",()->{
            prefs.edit().putString(keys[0],"germany").putString(keys[1],"istanbul").putString(keys[2],"shanghai").apply();
            refreshWidget(); showTab(0);
        }));
        c.addView(button("به‌روزرسانی ویجت",()->refreshWidget()));
        c.addView(button("پاک‌سازی حافظه تقویم",()->{
            getSharedPreferences("holiday_calendar_cache",MODE_PRIVATE).edit().clear().apply();
            if(currentTab==3) {
                TextView done=text("حافظه موقت تقویم پاک شد؛ اطلاعات هنگام ورود به بخش تقویم دوباره دریافت می‌شود.",12,Color.rgb(245,199,107)); c.addView(done);
            }
        }));
        c.addView(button("درباره TIME",()->{
            c.addView(text("TIME · ساعت جهانی و تقویم کشورها\nنسخه 6.4\nاطلاعات تعطیلات از Nager.Date دریافت می‌شود.",13,Color.LTGRAY));
        }));
    }

    private void refreshWidget(){IntentHelper.update(this);}
    @Override protected void onResume(){super.onResume();if(prefs!=null&&times[0]!=null&&currentTab==0)ticker.run();}
    @Override protected void onPause(){super.onPause();handler.removeCallbacks(ticker);}
}
