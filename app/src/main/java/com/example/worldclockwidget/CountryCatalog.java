package com.example.worldclockwidget;

import java.util.Arrays;
import java.util.List;

public final class CountryCatalog {
    private CountryCatalog() {}
    public static final class Country {
        public final String id, name, flag, zone, localeTag, calendar;
        Country(String id, String name, String flag, String zone, String localeTag, String calendar) {
            this.id=id; this.name=name; this.flag=flag; this.zone=zone; this.localeTag=localeTag; this.calendar=calendar;
        }
        @Override public String toString() { return flag + "  " + name; }
    }
    public static final List<Country> ALL = Arrays.asList(
        new Country("tehran","ایران · تهران","🇮🇷","Asia/Tehran","fa-IR","persian"),
        new Country("istanbul","ترکیه · استانبول","🇹🇷","Europe/Istanbul","tr-TR","gregory"),
        new Country("shanghai","چین · شانگهای","🇨🇳","Asia/Shanghai","zh-CN","gregory"),
        new Country("germany","آلمان · برلین","🇩🇪","Europe/Berlin","de-DE","gregory"),
        new Country("france","فرانسه · پاریس","🇫🇷","Europe/Paris","fr-FR","gregory"),
        new Country("usa","آمریکا · نیویورک","🇺🇸","America/New_York","en-US","gregory"),
        new Country("uk","انگلیس · لندن","🇬🇧","Europe/London","en-GB","gregory"),
        new Country("russia","روسیه · مسکو","🇷🇺","Europe/Moscow","ru-RU","gregory"),
        new Country("iraq","عراق · بغداد","🇮🇶","Asia/Baghdad","ar-IQ","islamic"),
        new Country("uae","امارات · دبی","🇦🇪","Asia/Dubai","ar-AE","islamic"),
        new Country("qatar","قطر · دوحه","🇶🇦","Asia/Qatar","ar-QA","islamic"),
        new Country("malaysia","مالزی · کوالالامپور","🇲🇾","Asia/Kuala_Lumpur","ms-MY","gregory"),
        new Country("thailand","تایلند · بانکوک","🇹🇭","Asia/Bangkok","th-TH","buddhist"),
        new Country("vietnam","ویتنام · هانوی","🇻🇳","Asia/Ho_Chi_Minh","vi-VN","gregory"),
        new Country("taiwan","تایوان · تایپه","🇹🇼","Asia/Taipei","zh-TW","roc"),
        new Country("india","هند · دهلی نو","🇮🇳","Asia/Kolkata","hi-IN","gregory"),
        new Country("indonesia","اندونزی · جاکارتا","🇮🇩","Asia/Jakarta","id-ID","gregory")
    );
    public static Country byId(String id) {
        for (Country c : ALL) if (c.id.equals(id)) return c;
        return ALL.get(0);
    }
    public static int indexOf(String id) {
        for (int i=0;i<ALL.size();i++) if (ALL.get(i).id.equals(id)) return i;
        return 0;
    }
}
