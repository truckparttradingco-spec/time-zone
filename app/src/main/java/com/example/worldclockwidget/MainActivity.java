package com.example.worldclockwidget;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class MainActivity extends Activity {
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextView v = new TextView(this);
        v.setText("ساعت سه شهر\n\nبرای افزودن ویجت: روی صفحهٔ اصلی نگه دارید ← Widgets ← ساعت سه شهر");
        v.setTextSize(18);
        v.setPadding(24, 24, 24, 24);
        setContentView(v);
    }
}
