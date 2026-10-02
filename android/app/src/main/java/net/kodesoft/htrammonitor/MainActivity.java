package net.kodesoft.htrammonitor;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {
    private static final int REQ = 7;
    private TextView status;
    private Spinner profile;
    private EditText warning, alarm, delay, repeat;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL); root.setPadding(40,50,40,40);
        root.setBackgroundColor(Color.rgb(16,18,22));

        TextView title = tv("HTRAM Monitor", 30); root.addView(title);
        TextView sub = tv("Resident CO₂ monitor", 16); sub.setTextColor(Color.LTGRAY); root.addView(sub);
        status = tv("Stopped", 18); status.setPadding(0,30,0,25); root.addView(status);

        profile = new Spinner(this);
        ArrayAdapter<String> pa = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Home","Udo awake","Udo sleeping","Custom"});
        profile.setAdapter(pa); root.addView(label("Profile")); root.addView(profile);

        warning = number("800"); alarm = number("1200"); delay = number("2"); repeat = number("2");
        root.addView(label("Warning ppm")); root.addView(warning);
        root.addView(label("Alarm ppm")); root.addView(alarm);
        root.addView(label("Alarm delay (minutes)")); root.addView(delay);
        root.addView(label("Repeat alarm every (minutes)")); root.addView(repeat);

        profile.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            public void onNothingSelected(android.widget.AdapterView<?> p) {}
            public void onItemSelected(android.widget.AdapterView<?> p, View v, int pos, long id) {
                if (pos == 0 || pos == 1) { warning.setText("800"); alarm.setText("1200"); delay.setText("2"); }
                else if (pos == 2) { warning.setText("800"); alarm.setText("1000"); delay.setText("1"); }
            }
        });

        Button start = button("START MONITORING"); root.addView(start);
        Button stop = button("STOP"); root.addView(stop);
        TextView note=tv("Android will keep a foreground Bluetooth service active and show a persistent notification. You can turn the screen off after it connects.",14);
        note.setTextColor(Color.LTGRAY); note.setPadding(0,25,0,0); root.addView(note);
        setContentView(root);

        load();
        start.setOnClickListener(v -> requestAndStart());
        stop.setOnClickListener(v -> { stopService(new Intent(this,HtramService.class)); status.setText("Stopped"); });
    }

    private TextView tv(String s,int size){ TextView t=new TextView(this); t.setText(s);t.setTextSize(size);t.setTextColor(Color.WHITE);return t; }
    private TextView label(String s){ TextView t=tv(s,14);t.setPadding(0,15,0,4);return t; }
    private EditText number(String s){ EditText e=new EditText(this);e.setText(s);e.setTextColor(Color.WHITE);e.setInputType(2);e.setBackgroundColor(Color.rgb(40,43,50));e.setPadding(18,8,18,8);return e; }
    private Button button(String s){ Button b=new Button(this);b.setText(s);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,22,0,0);b.setLayoutParams(p);return b; }

    private void requestAndStart(){
        ArrayList<String> ps=new ArrayList<>();
        if(Build.VERSION.SDK_INT>=31){
            if(checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN)!=PackageManager.PERMISSION_GRANTED) ps.add(Manifest.permission.BLUETOOTH_SCAN);
            if(checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED) ps.add(Manifest.permission.BLUETOOTH_CONNECT);
        }
        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) ps.add(Manifest.permission.POST_NOTIFICATIONS);
        if(!ps.isEmpty()){ requestPermissions(ps.toArray(new String[0]),REQ); return; }
        startMonitor();
    }
    @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){ super.onRequestPermissionsResult(r,p,g); if(r==REQ){ for(int x:g)if(x!=PackageManager.PERMISSION_GRANTED)return; startMonitor(); } }
    private int n(EditText e,int d){try{return Integer.parseInt(e.getText().toString());}catch(Exception x){return d;}}
    private void startMonitor(){
        int w=n(warning,800), a=n(alarm,1200); if(a<=w){Toast.makeText(this,"Alarm must be higher than warning",Toast.LENGTH_LONG).show();return;}
        getSharedPreferences("alarm",0).edit().putInt("warning",w).putInt("alarm",a).putInt("delay",n(delay,2)).putInt("repeat",Math.max(1,n(repeat,2))).putInt("profile",profile.getSelectedItemPosition()).apply();
        Intent i=new Intent(this,HtramService.class); ContextCompatShim.startForeground(this,i); status.setText("Starting… watch the notification");
    }
    private void load(){
        android.content.SharedPreferences p=getSharedPreferences("alarm",0);
        warning.setText(String.valueOf(p.getInt("warning",800))); alarm.setText(String.valueOf(p.getInt("alarm",1200)));
        delay.setText(String.valueOf(p.getInt("delay",2))); repeat.setText(String.valueOf(p.getInt("repeat",2)));
        profile.setSelection(p.getInt("profile",0));
    }
    static class ContextCompatShim { static void startForeground(Context c,Intent i){ if(Build.VERSION.SDK_INT>=26)c.startForegroundService(i);else c.startService(i); } }
}
