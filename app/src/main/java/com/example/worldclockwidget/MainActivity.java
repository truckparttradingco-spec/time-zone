package com.example.worldclockwidget;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
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
        requestExactAlarmAccessIfNeeded();
    }

    private void requestExactAlarmAccessIfNeeded() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S) return;
        android.app.AlarmManager alarms = (android.app.AlarmManager) getSystemService(ALARM_SERVICE);
        if (alarms == null || alarms.canScheduleExactAlarms()) return;
        if (prefs.getBoolean("exact_alarm_prompted", false)) return;
        prefs.edit().putBoolean("exact_alarm_prompted", true).apply();
        try {
            Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                    Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        } catch (Exception ignored) {
            // The widget continues with Android's inexact alarm fallback.
        }
    }

    private void openExactAlarmSettings() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S) return;
        try {
            Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                    Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        } catch (Exception ignored) {}
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
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        root = vertical();
        root.setPadding(dp(16), dp(12), dp(16), dp(18));
        root.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{Color.rgb(4,10,19), Color.rgb(7,17,31), Color.rgb(3,8,16)}));
        scroll.addView(root);

        LinearLayout header = horizontal();
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView menu = text("☰", 23, Color.LTGRAY);
        menu.setOnClickListener(v -> showTab(3));
        header.addView(menu, new LinearLayout.LayoutParams(dp(42), dp(48)));
        TextView logo = text("TIME", 23, Color.WHITE);
        logo.setTypeface(Typeface.SERIF, Typeface.BOLD);
        GradientDrawable logoBg = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{Color.rgb(255,74,79), Color.rgb(165,8,24)});
        logoBg.setCornerRadius(dp(14)); logoBg.setStroke(dp(1), Color.rgb(255,130,130));
        logo.setBackground(logoBg);
        header.addView(logo, new LinearLayout.LayoutParams(dp(68), dp(48)));
        TextView brand = text("TIME", 22, Color.WHITE);
        brand.setTypeface(Typeface.SERIF, Typeface.BOLD);
        brand.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
        header.addView(brand, new LinearLayout.LayoutParams(0, dp(48), 1));
        TextView settings = text("⚙", 25, Color.WHITE);
        settings.setOnClickListener(v -> showSettings());
        header.addView(settings, new LinearLayout.LayoutParams(dp(44), dp(48)));
        root.addView(header);

        content = vertical();
        root.addView(content, new LinearLayout.LayoutParams(-1, 0, 1));

        nav = horizontal();
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(dp(4), dp(8), dp(4), dp(5));
        String[] labels = {"خانه", "تقویم", "کشورها", "مناسبت‌ها"};
        String[] icons = {"⌂", "▦", "◎", "★"};
        for (int i=0; i<labels.length; i++) {
            final int tab=i;
            LinearLayout item=vertical();
            item.setGravity(Gravity.CENTER);
            TextView ico=text(icons[i],22,Color.LTGRAY);
            TextView label=text(labels[i],11,Color.LTGRAY);
            item.addView(ico); item.addView(label);
            item.setPadding(0,dp(5),0,dp(4));
            item.setOnClickListener(v -> showTab(tab));
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);
            nav.addView(item,p);
        }
        root.addView(nav);
        setContentView(scroll);
    }
    private void showTab(int tab) {
        currentTab=tab; content.removeAllViews();
        String[] names={"خانه","تقویم","انتخاب کشور","مناسبت‌ها"};
        TextView heading=text(names[tab],22,Color.WHITE);
        heading.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        heading.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
        heading.setPadding(0,dp(10),0,dp(8));
        content.addView(heading);
        if(tab==0) buildHome();
        else if(tab==1) buildCalendar();
        else if(tab==2) buildCountries();
        else buildEvents();
        for(int i=0;i<nav.getChildCount();i++) {
            LinearLayout item=(LinearLayout)nav.getChildAt(i);
            int color=i==tab?Color.rgb(255,76,82):Color.rgb(165,180,198);
            for(int j=0;j<item.getChildCount();j++) ((TextView)item.getChildAt(j)).setTextColor(color);
            item.setBackground(i==tab?panel(Color.rgb(34,15,27),Color.rgb(120,38,51)):panel(Color.TRANSPARENT,Color.TRANSPARENT));
        }
    }

    private void showSettings() {
        currentTab=4; content.removeAllViews();
        TextView heading=text("تنظیمات",22,Color.WHITE); heading.setGravity(Gravity.RIGHT);
        content.addView(heading);
        buildSettings();
    }

    private TextView sectionTitle(String value) {
        TextView t=text(value,15,Color.rgb(245,199,107));
        t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        t.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
        t.setPadding(0,dp(12),0,dp(5));
        return t;
    }

    private LinearLayout premiumCard(LinearLayout parent) {
        LinearLayout c=vertical();
        c.setPadding(dp(14),dp(13),dp(14),dp(13));
        GradientDrawable bg=new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{Color.rgb(15,37,62),Color.rgb(7,17,31)});
        bg.setCornerRadius(dp(20)); bg.setStroke(dp(1),Color.rgb(38,75,111));
        c.setBackground(bg);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);
        p.setMargins(0,dp(7),0,dp(7)); parent.addView(c,p);
        return c;
    }

    private void buildHome() {
        TextView caption=text("ساعت جهانی · زمان محلی شهرها با منطقه زمانی واقعی",12,Color.rgb(177,192,210));
        caption.setGravity(Gravity.RIGHT); content.addView(caption);
        LinearLayout modes=horizontal();
        modes.addView(button("نمایش چند شهر در یک کارت",()->{prefs.edit().putBoolean("home_grouped",true).apply();showTab(0);}),
                new LinearLayout.LayoutParams(0,-2,1));
        modes.addView(button("کارت‌های جداگانه",()->{prefs.edit().putBoolean("home_grouped",false).apply();showTab(0);}),
                new LinearLayout.LayoutParams(0,-2,1));
        content.addView(modes);
        boolean grouped=prefs.getBoolean("home_grouped",true);
        long now=System.currentTimeMillis();
        if(grouped) {
            LinearLayout c=premiumCard(content);
            LinearLayout row=horizontal();
            for(int i=0;i<3;i++) {
                final int slot=i;
                CountryCatalog.Country city=CountryCatalog.byId(prefs.getString(keys[i],defaults[i]));
                LinearLayout col=vertical(); col.setGravity(Gravity.CENTER);
                TextView flag=text(city.flag,25,Color.WHITE); col.addView(flag);
                TextView name=text(city.name,12,Color.WHITE); name.setTypeface(null,Typeface.BOLD); col.addView(name);
                TextView time=text(ClockFormatter.time(city,now,prefs.getBoolean("use_24_hour",true)),25,Color.WHITE);
                time.setTypeface(Typeface.DEFAULT,Typeface.BOLD); times[i]=time; col.addView(time);
                TextView date=text(ClockFormatter.date(city,now),10,Color.LTGRAY); dates[i]=date; col.addView(date);
                TextView offset=text(ClockFormatter.offset(city,now),10,Color.rgb(245,199,107)); offsets[i]=offset; col.addView(offset);
                row.addView(col,new LinearLayout.LayoutParams(0,-2,1));
                if(i<2){View line=new View(this);line.setBackgroundColor(Color.rgb(48,91,128));row.addView(line,new LinearLayout.LayoutParams(dp(1),dp(118)));}
            }
            c.addView(row);
            LinearLayout dots=horizontal(); dots.setGravity(Gravity.CENTER);
            dots.addView(text("●   ○   ○",12,Color.WHITE)); c.addView(dots);
            c.addView(button("مدیریت شهرها",()->showTab(2)));
        } else {
            for(int i=0;i<3;i++) {
                CountryCatalog.Country city=CountryCatalog.byId(prefs.getString(keys[i],defaults[i]));
                LinearLayout c=premiumCard(content);
                LinearLayout top=horizontal();
                TextView flag=text(city.flag,25,Color.WHITE); top.addView(flag);
                TextView name=text(city.name,15,Color.WHITE); name.setTypeface(null,Typeface.BOLD);
                top.addView(name,new LinearLayout.LayoutParams(0,-2,1));
                top.addView(text("★",19,Color.rgb(245,199,107)));
                c.addView(top);
                TextView time=text(ClockFormatter.time(city,now,prefs.getBoolean("use_24_hour",true)),38,Color.WHITE);
                time.setTypeface(Typeface.DEFAULT,Typeface.BOLD); times[i]=time; c.addView(time);
                dates[i]=text(ClockFormatter.date(city,now),13,Color.LTGRAY); c.addView(dates[i]);
                offsets[i]=text(ClockFormatter.offset(city,now),12,Color.rgb(245,199,107)); c.addView(offsets[i]);
                c.addView(text("منطقه زمانی  "+city.zone,10,Color.rgb(160,185,210)));
            }
        }
        LinearLayout features=premiumCard(content);
        features.addView(sectionTitle("امکانات TIME"));
        String[] featureNames={"شخصی‌سازی","یادآوری","مناسبت‌ها","بروزرسانی خودکار","تقویم کامل","انتخاب کشورها"};
        String[] featureIcons={"◉","♧","★","⟳","▦","◎"};
        for(int i=0;i<featureNames.length;i++){
            final int action=i;
            TextView item=text(featureIcons[i]+"     "+featureNames[i],13,Color.rgb(245,199,107));
            item.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
            item.setPadding(dp(6),dp(7),dp(6),dp(7));
            item.setOnClickListener(v->{if(action==0)showSettings();else if(action==1||action==2)showTab(3);else if(action==4)showTab(1);else if(action==5)showTab(2);else loadHolidays(true);});
            features.addView(item);
        }
        ticker.run();
    }

    private void buildCountries() {
        TextView intro=text("کشورها و شهرهای مرجع را جست‌وجو و برای ساعت‌های صفحه خانه انتخاب کن.",12,Color.LTGRAY);
        intro.setGravity(Gravity.RIGHT); content.addView(intro);
        EditText search=new EditText(this); search.setSingleLine(true); search.setTextColor(Color.WHITE);
        search.setHintTextColor(Color.GRAY); search.setHint("جستجو کشور یا شهر...");
        search.setText(countryQuery); search.setPadding(dp(14),dp(8),dp(14),dp(8));
        search.setBackground(panel(Color.rgb(12,25,42),Color.rgb(53,91,128))); content.addView(search);
        TextView selected=text("سه جایگاه ساعت ویجت و خانه",12,Color.rgb(245,199,107));
        selected.setGravity(Gravity.RIGHT); content.addView(selected);
        LinearLayout slotPicker=horizontal();
        String[] slotNames={"شهر اول","شهر دوم","شهر سوم"};
        for(int i=0;i<3;i++){
            final int slot=i;
            Spinner sp=new Spinner(this);
            List<String> labels=new ArrayList<>();
            for(CountryCatalog.Country c:CountryCatalog.ALL)labels.add(c.toString());
            ArrayAdapter<String> a=new ArrayAdapter<>(this,R.layout.spinner_item,labels);
            a.setDropDownViewResource(R.layout.spinner_dropdown_item);sp.setAdapter(a);
            sp.setSelection(CountryCatalog.indexOf(prefs.getString(keys[i],defaults[i])));
            sp.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){
                boolean first=true;
                @Override public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){
                    if(first){first=false;return;}
                    prefs.edit().putString(keys[slot],CountryCatalog.ALL.get(pos).id).apply();
                    if(slot==0){selectedHolidayCountry=CountryCatalog.ALL.get(pos).id;prefs.edit().putString("holiday_country",selectedHolidayCountry).apply();}
                    refreshWidget();
                }
                @Override public void onNothingSelected(android.widget.AdapterView<?> p){}
            });
            slotPicker.addView(sp,new LinearLayout.LayoutParams(0,dp(48),1));
        }
        content.addView(slotPicker);
        LinearLayout list=vertical(); content.addView(list);
        Runnable render=()->{
            list.removeAllViews();
            String q=countryQuery.trim().toLowerCase(Locale.ROOT);
            for(CountryCatalog.Country c:CountryCatalog.ALL){
                String searchable=(c.name+" "+c.id+" "+c.zone+" "+c.localeTag).toLowerCase(Locale.ROOT);
                if(!q.isEmpty()&&!searchable.contains(q))continue;
                LinearLayout row=premiumCard(list);
                LinearLayout line=horizontal();
                TextView flag=text(c.flag,24,Color.WHITE);line.addView(flag);
                TextView name=text(c.name,14,Color.WHITE);name.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
                line.addView(name,new LinearLayout.LayoutParams(0,dp(42),1));
                row.addView(line);
                TextView zone=text(c.zone+"   ·   "+c.calendar,10,Color.LTGRAY);zone.setGravity(Gravity.RIGHT);row.addView(zone);
                LinearLayout actions=horizontal();
                for(int slot=0;slot<3;slot++){
                    final int target=slot;
                    actions.addView(button("انتخاب "+(slot+1),()->{
                        prefs.edit().putString(keys[target],c.id).apply();
                        if(target==0){selectedHolidayCountry=c.id;prefs.edit().putString("holiday_country",c.id).apply();}
                        refreshWidget();showTab(0);
                    }),new LinearLayout.LayoutParams(0,dp(42),1));
                }
                row.addView(actions);
            }
            if(list.getChildCount()==0)list.addView(text("کشوری پیدا نشد.",13,Color.LTGRAY));
        };
        search.addTextChangedListener(new TextWatcher(){
            @Override public void beforeTextChanged(CharSequence s,int st,int count,int after){}
            @Override public void onTextChanged(CharSequence s,int st,int before,int count){countryQuery=s.toString();render.run();}
            @Override public void afterTextChanged(Editable e){}
        });
        render.run();
    }

    private void buildCalendar() {
        LinearLayout controls=premiumCard(content);
        Spinner country=new Spinner(this);
        List<String> labels=new ArrayList<>();for(CountryCatalog.Country c:CountryCatalog.ALL)labels.add(c.toString());
        ArrayAdapter<String> adapter=new ArrayAdapter<>(this,R.layout.spinner_item,labels);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);country.setAdapter(adapter);
        country.setSelection(CountryCatalog.indexOf(selectedHolidayCountry));controls.addView(country);
        country.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){
            @Override public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){
                selectedHolidayCountry=CountryCatalog.ALL.get(pos).id;
                prefs.edit().putString("holiday_country",selectedHolidayCountry).apply();
                holidayYear=CalendarRenderer.currentYear(CountryCatalog.ALL.get(pos));
                if(holidayYearLabel!=null)holidayYearLabel.setText(String.valueOf(holidayYear));
                loadHolidays(false);
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> p){}
        });
        LinearLayout modes=horizontal();
        String[] modeNames={"ماهانه","سالانه","تعطیلات"};
        for(int i=0;i<3;i++){final int m=i;modes.addView(button(modeNames[i],()->{prefs.edit().putInt("calendar_mode",m).apply();showTab(1);}),new LinearLayout.LayoutParams(0,dp(44),1));}
        controls.addView(modes);
        LinearLayout yearRow=horizontal();
        yearRow.addView(button("‹",()->{holidayYear--;holidayYearLabel.setText(String.valueOf(holidayYear));loadHolidays(false);}),new LinearLayout.LayoutParams(dp(55),dp(44)));
        holidayYearLabel=text(String.valueOf(holidayYear),18,Color.rgb(245,199,107));
        yearRow.addView(holidayYearLabel,new LinearLayout.LayoutParams(0,dp(44),1));
        yearRow.addView(button("›",()->{holidayYear++;holidayYearLabel.setText(String.valueOf(holidayYear));loadHolidays(false);}),new LinearLayout.LayoutParams(dp(55),dp(44)));
        controls.addView(yearRow);
        holidayStatus=text("در حال دریافت مناسبت‌ها...",11,Color.LTGRAY);controls.addView(holidayStatus);
        holidayMonths=vertical();content.addView(holidayMonths,new LinearLayout.LayoutParams(-1,-2));
        controls.addView(button("⟳  دریافت بروزرسانی تعطیلات",()->loadHolidays(true)));
        loadHolidays(false);
    }

    private void loadHolidays(boolean force) {
        if(holidayStatus==null||holidayYearLabel==null||holidayMonths==null) return;
        CountryCatalog.Country country=CountryCatalog.byId(selectedHolidayCountry);
        int requestedYear=holidayYear;
        holidayStatus.setText("تقویم "+country.name+" · در حال دریافت تعطیلات…");
        HolidayCalendar.load(this,country,requestedYear,force,(json,fromCache,error)->runOnUiThread(()->{
            if(currentTab!=1||!selectedHolidayCountry.equals(country.id)||holidayYear!=requestedYear)return;
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
                        (error!=null?" · دریافت بعضی سال‌های میلادی کامل نشد":"")+
                        " · منبع: Nager.Date · برخی تعطیلات قمری وابسته به رؤیت هلال ممکن است درج نشده باشند.");
            }catch(Exception e){
                holidayStatus.setText("ساخت تقویم ناموفق بود: "+e.getMessage());
            }
        }));
    }

    private void buildSettings() {
        LinearLayout c=premiumCard(content);
        addSettingSwitch(c,"نمایش ساعت ۲۴ ساعته","use_24_hour",true);
        addSettingSwitch(c,"نمایش کارت‌های جداگانه در خانه","home_grouped",false);
        addSettingSwitch(c,"حالت تیره","dark_theme",true);
        c.addView(sectionTitle("زبان برنامه"));
        Spinner language=new Spinner(this);
        language.setAdapter(new ArrayAdapter<>(this,R.layout.spinner_item,new String[]{"فارسی","English"}));
        language.setSelection(prefs.getString("language","fa").equals("en")?1:0);
        language.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){
            boolean first=true;
            @Override public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){
                if(first){first=false;return;}prefs.edit().putString("language",pos==1?"en":"fa").apply();
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> p){}
        });
        c.addView(language);
        c.addView(sectionTitle("شهر مرجع اختلاف ساعت"));
        Spinner reference=new Spinner(this);
        List<String> names=new ArrayList<>();for(CountryCatalog.Country x:CountryCatalog.ALL)names.add(x.toString());
        reference.setAdapter(new ArrayAdapter<>(this,R.layout.spinner_item,names));
        reference.setSelection(CountryCatalog.indexOf(prefs.getString("reference_city","germany")));
        reference.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){
            boolean first=true;
            @Override public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){if(first){first=false;return;}prefs.edit().putString("reference_city",CountryCatalog.ALL.get(pos).id).apply();}
            @Override public void onNothingSelected(android.widget.AdapterView<?> p){}
        });c.addView(reference);
        c.addView(sectionTitle("مدیریت اعلان و یادآوری"));
        addSettingSwitch(c,"یادآوری مناسبت‌ها","event_reminders",false);
        addSettingSwitch(c,"اعلان بروزرسانی تعطیلات","holiday_notifications",false);
        c.addView(button("تنظیم صدای هشدار",()->new android.app.AlertDialog.Builder(this).setMessage("صدای هشدار از تنظیمات اعلان‌های Android مدیریت می‌شود.").setPositiveButton("متوجه شدم",null).show()));
        c.addView(sectionTitle("اطلاعات و نگهداری"));
        c.addView(button("بازگردانی شهرهای پیش‌فرض",()->{
            prefs.edit().putString(keys[0],"germany").putString(keys[1],"istanbul").putString(keys[2],"shanghai").apply();
            refreshWidget();showTab(0);
        }));
        c.addView(button("به‌روزرسانی ویجت",()->refreshWidget()));
        c.addView(button("پاک‌سازی حافظه تقویم",()->{
            getSharedPreferences("holiday_calendar_cache",MODE_PRIVATE).edit().clear().apply();
            holidayStatus=null;holidayMonths=null;
        }));
        c.addView(button("درباره TIME TIME",()->new android.app.AlertDialog.Builder(this)
                .setTitle("TIME TIME").setMessage("ساعت جهانی و تقویم کشورها\\nنسخه 6.6\\nتعطیلات رسمی: Nager.Date")
                .setPositiveButton("بستن",null).show()));
    }

    private void addSettingSwitch(LinearLayout parent,String label,String key,boolean defaultValue){
        android.widget.Switch sw=new android.widget.Switch(this);
        sw.setText(label);sw.setTextColor(Color.WHITE);sw.setPadding(dp(4),dp(8),dp(4),dp(8));
        sw.setChecked(prefs.getBoolean(key,defaultValue));parent.addView(sw);
        sw.setOnCheckedChangeListener((b,checked)->{
            prefs.edit().putBoolean(key,checked).apply();
            if("use_24_hour".equals(key)){refreshWidget();if(currentTab==0)ticker.run();}
            if("home_grouped".equals(key)&&currentTab==0)showTab(0);
        });
    }

    private void buildEvents() {
        LinearLayout selector=premiumCard(content);
        Spinner country=new Spinner(this);
        List<String> names=new ArrayList<>();for(CountryCatalog.Country c:CountryCatalog.ALL)names.add(c.toString());
        country.setAdapter(new ArrayAdapter<>(this,R.layout.spinner_item,names));
        country.setSelection(CountryCatalog.indexOf(selectedHolidayCountry));selector.addView(country);
        country.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){
            @Override public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){
                selectedHolidayCountry=CountryCatalog.ALL.get(pos).id;
                holidayYear=CalendarRenderer.currentYear(CountryCatalog.ALL.get(pos));
                loadEventsForCountry();
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> p){}
        });
        holidayStatus=text("مناسبت‌ها و تعطیلات رسمی",12,Color.LTGRAY);selector.addView(holidayStatus);
        holidayMonths=vertical();content.addView(holidayMonths);
        selector.addView(button("⟳  بروزرسانی مناسبت‌ها",()->loadEventsForCountry()));
        loadEventsForCountry();
    }

    private void loadEventsForCountry(){
        if(holidayMonths==null||holidayStatus==null)return;
        CountryCatalog.Country c=CountryCatalog.byId(selectedHolidayCountry);
        holidayStatus.setText("در حال دریافت رویدادهای "+c.name+"...");
        HolidayCalendar.load(this,c,CalendarRenderer.currentYear(c),false,(json,cache,error)->runOnUiThread(()->{
            if(currentTab!=3||holidayMonths==null)return;
            holidayMonths.removeAllViews();
            if(json==null){holidayStatus.setText(error==null?"اطلاعات موجود نیست":error);return;}
            try{
                JSONArray arr=new JSONArray(json);holidayStatus.setText(arr.length()+" رویداد رسمی · منبع Nager.Date"+(cache?" · حافظه":" · آنلاین"));
                for(int i=0;i<arr.length();i++){
                    JSONObject item=arr.getJSONObject(i);
                    LinearLayout card=premiumCard(holidayMonths);
                    TextView date=text(item.optString("date"),12,Color.rgb(245,199,107));date.setGravity(Gravity.RIGHT);card.addView(date);
                    TextView title=text(item.optString("localName",item.optString("name")),15,Color.WHITE);title.setGravity(Gravity.RIGHT);card.addView(title);
                    TextView type=text(item.optString("name")+" · "+item.optString("types"),10,Color.LTGRAY);type.setGravity(Gravity.RIGHT);card.addView(type);
                    card.addView(button("افزودن یادآور",()->new android.app.AlertDialog.Builder(this)
                            .setMessage("برای یادآوری این رویداد، اعلان‌های TIME را در تنظیمات Android فعال کنید.")
                            .setPositiveButton("باشه",null).show()));
                }
            }catch(Exception ex){holidayStatus.setText("خطا در نمایش رویدادها");}
        }));
    }

    private void refreshWidget(){IntentHelper.update(this);}
    @Override protected void onResume(){super.onResume();if(prefs!=null&&times[0]!=null&&currentTab==0)ticker.run();}
    @Override protected void onPause(){super.onPause();handler.removeCallbacks(ticker);}
}
