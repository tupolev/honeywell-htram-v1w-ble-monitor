package net.kodesoft.htrammonitor;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {
    static final int REQ=7, BG=Color.rgb(13,14,18), CARD=Color.rgb(29,31,37), ORANGE=Color.rgb(255,157,0), GREEN=Color.rgb(0,230,118), YELLOW=Color.rgb(255,234,0), RED=Color.rgb(255,82,82);
    TextView status,logView,co2Value,tempValue,humValue,batValue,co2State,alarmState,monitorTab,serviceTab,helpTab;
    ScrollView logScroll; boolean followLog=true,loading=true; Spinner profile; EditText warning,alarm,delay,repeat; Switch sound,vibrate; LinearLayout monitorPage,servicePage,helpPage; BroadcastReceiver receiver;

    @Override public void onCreate(Bundle b){super.onCreate(b); buildUi(); load(); loadLatest(); receiver=new BroadcastReceiver(){public void onReceive(Context c,Intent i){
        String t=i.getStringExtra("text"); if(t!=null){status.setText(t);appendLog(t);}
        if(i.hasExtra("co2")) updateReadings(i.getIntExtra("co2",0),i.getIntExtra("temp",0),i.getIntExtra("hum",0),i.getIntExtra("bat",0));
    }};
    if("service".equals(getIntent().getStringExtra("tab"))) showTab(1); else if("help".equals(getIntent().getStringExtra("tab"))) showTab(2); else showTab(0);}

    void buildUi(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(BG);
        LinearLayout head=new LinearLayout(this);head.setPadding(28,28,28,18);head.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout names=new LinearLayout(this);names.setOrientation(LinearLayout.VERTICAL);
        TextView brand=tv("HONEYWELL",12);brand.setTextColor(ORANGE);brand.setTypeface(Typeface.DEFAULT,Typeface.BOLD);names.addView(brand);names.addView(tv("HTRAM-V1-W",20));
        head.addView(names,new LinearLayout.LayoutParams(0,-2,1));status=tv("Resident monitor",12);status.setTextColor(Color.LTGRAY);head.addView(status);root.addView(head);
        LinearLayout tabs=new LinearLayout(this);tabs.setPadding(20,0,20,12);
        monitorTab=tab("MONITOR");serviceTab=tab("SERVICE / LOG");helpTab=tab("HELP");tabs.addView(monitorTab,new LinearLayout.LayoutParams(0,52,1));tabs.addView(serviceTab,new LinearLayout.LayoutParams(0,52,1));tabs.addView(helpTab,new LinearLayout.LayoutParams(0,52,1));root.addView(tabs);
        FrameLayout pages=new FrameLayout(this);root.addView(pages,new LinearLayout.LayoutParams(-1,0,1));
        monitorPage=monitorUi();servicePage=serviceUi();helpPage=helpUi();pages.addView(monitorPage);pages.addView(servicePage);pages.addView(helpPage);
        monitorTab.setOnClickListener(v->showTab(0));serviceTab.setOnClickListener(v->showTab(1));helpTab.setOnClickListener(v->showTab(2));setContentView(root);
    }

    LinearLayout monitorUi(){
        LinearLayout body=column(); ScrollView sv=new ScrollView(this);LinearLayout c=column();c.setPadding(24,8,24,30);sv.addView(c);body.addView(sv,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout co=card();TextView h=small("💨  CO₂ LEVEL");co.addView(h);LinearLayout valueRow=new LinearLayout(this);valueRow.setGravity(Gravity.BOTTOM);co2Value=tv("---",52);co2Value.setTypeface(Typeface.DEFAULT,Typeface.BOLD);co2Value.setTextColor(GREEN);valueRow.addView(co2Value);TextView ppm=tv(" ppm",18);ppm.setTextColor(Color.GRAY);valueRow.addView(ppm);co.addView(valueRow);co2State=small("Waiting for data…");co.addView(co2State);c.addView(co);
        LinearLayout dual=new LinearLayout(this);dual.setPadding(0,14,0,0);LinearLayout tc=card(),hc=card();tc.addView(small("🌡  TEMP"));tempValue=tv("--- °C",31);tc.addView(tempValue);hc.addView(small("💧  HUMIDITY"));humValue=tv("--- %",31);hc.addView(humValue);LinearLayout.LayoutParams half=new LinearLayout.LayoutParams(0,-2,1);half.setMargins(0,0,7,0);dual.addView(tc,half);half=new LinearLayout.LayoutParams(0,-2,1);half.setMargins(7,0,0,0);dual.addView(hc,half);c.addView(dual);
        LinearLayout bc=card();LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.setMargins(0,14,0,0);bc.setLayoutParams(cp);bc.addView(small("🔋  BATTERY"));batValue=tv("---",27);bc.addView(batValue);c.addView(bc);
        LinearLayout ap=card();LinearLayout.LayoutParams apm=new LinearLayout.LayoutParams(-1,-2);apm.setMargins(0,18,0,0);ap.setLayoutParams(apm);
        LinearLayout ah=new LinearLayout(this);TextView at=tv("CO₂ alarms",19);at.setTypeface(Typeface.DEFAULT,Typeface.BOLD);ah.addView(at,new LinearLayout.LayoutParams(0,-2,1));alarmState=small("READY");alarmState.setTextColor(GREEN);ah.addView(alarmState);ap.addView(ah);
        ap.addView(smallPad("PROFILE"));profile=new Spinner(this);profile.setPopupBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.rgb(35,37,43)));
        ArrayAdapter<String> pa=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,new String[]{"Home","Udo awake","Udo sleeping","Custom"}){
            public View getView(int p,View v,android.view.ViewGroup g){TextView t=(TextView)super.getView(p,v,g);t.setTextColor(Color.WHITE);t.setTextSize(17);t.setPadding(16,12,16,12);return t;}
            public View getDropDownView(int p,View v,android.view.ViewGroup g){TextView t=(TextView)super.getDropDownView(p,v,g);t.setTextColor(Color.WHITE);t.setBackgroundColor(Color.rgb(35,37,43));t.setTextSize(17);t.setPadding(24,18,24,18);return t;}};
        pa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);profile.setAdapter(pa);ap.addView(profile);
        LinearLayout r1=new LinearLayout(this);warning=field("800");alarm=field("1200");r1.addView(fieldBox("Warning  ppm",warning),new LinearLayout.LayoutParams(0,-2,1));r1.addView(fieldBox("Alarm  ppm",alarm),new LinearLayout.LayoutParams(0,-2,1));ap.addView(r1);
        LinearLayout r2=new LinearLayout(this);delay=field("2");repeat=field("2");r2.addView(fieldBox("Delay  min",delay),new LinearLayout.LayoutParams(0,-2,1));r2.addView(fieldBox("Repeat  min",repeat),new LinearLayout.LayoutParams(0,-2,1));ap.addView(r2);
        LinearLayout opts=new LinearLayout(this);opts.setGravity(Gravity.CENTER_VERTICAL);sound=new Switch(this);sound.setText("Sound");sound.setTextColor(Color.WHITE);vibrate=new Switch(this);vibrate.setText("Vibrate");vibrate.setTextColor(Color.WHITE);opts.addView(sound);opts.addView(vibrate);ap.addView(opts);
        Button save=button("SAVE ALARM SETTINGS");ap.addView(save);Button silence=button("SILENCE / REARM");ap.addView(silence);c.addView(ap);
        profile.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?> p){} public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){if(loading)return;if(pos==0||pos==1){warning.setText("800");alarm.setText("1200");delay.setText("2");}else if(pos==2){warning.setText("800");alarm.setText("1000");delay.setText("1");}saveSettings();}});
        save.setOnClickListener(v->saveSettings());silence.setOnClickListener(v->{getSharedPreferences("alarm",0).edit().putBoolean("silenced",true).apply();alarmState.setText("SILENCED");alarmState.setTextColor(Color.GRAY);Toast.makeText(this,"Alarm silenced until CO₂ returns below warning",Toast.LENGTH_SHORT).show();});
        sound.setOnCheckedChangeListener((b,x)->{if(!loading)saveSettings();});vibrate.setOnCheckedChangeListener((b,x)->{if(!loading)saveSettings();});
        return body;
    }

    LinearLayout serviceUi(){
        LinearLayout body=column();ScrollView sv=new ScrollView(this);LinearLayout c=column();c.setPadding(24,8,24,30);sv.addView(c);body.addView(sv,new LinearLayout.LayoutParams(-1,0,1));
        TextView sh=tv("Resident monitoring service",20);sh.setTypeface(Typeface.DEFAULT,Typeface.BOLD);c.addView(sh);TextView note=small("Bluetooth continues in the foreground with the screen off. The log below is persistent.");note.setPadding(0,5,0,12);c.addView(note);
        LinearLayout actions=new LinearLayout(this);Button start=button("START");Button stop=button("STOP");actions.addView(start,new LinearLayout.LayoutParams(0,-2,1));actions.addView(stop,new LinearLayout.LayoutParams(0,-2,1));c.addView(actions);
        TextView lt=smallPad("LOG");c.addView(lt);logView=tv("",12);logView.setTextColor(Color.rgb(160,220,170));logView.setBackgroundColor(Color.rgb(5,7,8));logView.setPadding(16,12,16,12);logView.setTypeface(Typeface.MONOSPACE);logView.setTextIsSelectable(true);logView.setFocusable(true);logView.setFocusableInTouchMode(true);logView.setLongClickable(true);
        logScroll=new ScrollView(this);logScroll.setFillViewport(true);logScroll.setVerticalScrollBarEnabled(true);logScroll.setOnTouchListener((v,e)->{if(e.getAction()==MotionEvent.ACTION_DOWN||e.getAction()==MotionEvent.ACTION_MOVE)followLog=false;return false;});logScroll.addView(logView);c.addView(logScroll,new LinearLayout.LayoutParams(-1,520));
        LinearLayout lb=new LinearLayout(this);Button copy=button("COPY");Button clear=button("CLEAR");Button follow=button("FOLLOW");lb.addView(copy,new LinearLayout.LayoutParams(0,-2,1));lb.addView(clear,new LinearLayout.LayoutParams(0,-2,1));lb.addView(follow,new LinearLayout.LayoutParams(0,-2,1));c.addView(lb);
        copy.setOnClickListener(v->{((android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(android.content.ClipData.newPlainText("HTRAM log",logView.getText()));Toast.makeText(this,"Log copied",Toast.LENGTH_SHORT).show();});clear.setOnClickListener(v->{getSharedPreferences("debug",0).edit().remove("log").apply();logView.setText("");});follow.setOnClickListener(v->{followLog=true;logScroll.post(()->logScroll.fullScroll(View.FOCUS_DOWN));});start.setOnClickListener(v->requestAndStart());stop.setOnClickListener(v->{stopService(new Intent(this,HtramService.class));status.setText("Stopped");appendLog("STOP pressed");});
        String old=getSharedPreferences("debug",0).getString("log","");logView.setText(old);logScroll.post(()->logScroll.fullScroll(View.FOCUS_DOWN));return body;
    }

    void showTab(int which){monitorPage.setVisibility(which==0?View.VISIBLE:View.GONE);servicePage.setVisibility(which==1?View.VISIBLE:View.GONE);helpPage.setVisibility(which==2?View.VISIBLE:View.GONE);monitorTab.setTextColor(which==0?ORANGE:Color.GRAY);serviceTab.setTextColor(which==1?ORANGE:Color.GRAY);helpTab.setTextColor(which==2?ORANGE:Color.GRAY);}
    LinearLayout helpUi(){
        LinearLayout body=column();ScrollView sv=new ScrollView(this);LinearLayout c=column();c.setPadding(24,8,24,36);sv.addView(c);body.addView(sv,new LinearLayout.LayoutParams(-1,0,1));
        TextView title=tv("HTRAM Monitor",28);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);c.addView(title);TextView ver=small("Version 0.7.0");ver.setTextColor(ORANGE);ver.setPadding(0,2,0,22);c.addView(ver);
        helpSection(c,"What is this app?","An independent Android monitor for the Honeywell HTRAM. It reads the sensor over Bluetooth Low Energy and can keep monitoring while the screen is off.");
        helpSection(c,"Device","Designed for the Honeywell Transmission Risk Air Monitor HTRAM-V1-W.");
        helpSection(c,"How it works","The resident Android service connects directly to the HTRAM over BLE, reads CO₂, temperature, humidity and battery level, and evaluates alarm thresholds locally on your phone. No Honeywell account or cloud service is required. CO₂ alarms and settings in this app do not change the sensor's own alarm parameters.");
        helpSection(c,"Low battery warning","A separate warning notification is shown when the HTRAM reports battery level 1/4 or lower. It is intentionally distinct from the CO₂ alarm.");
        helpSection(c,"Project","This Android app and the extended web/PWA version are maintained in the tupolev/honeywell-htram-v1w-ble-monitor repository.");
        TextView repo=link("Open project repository","https://github.com/tupolev/honeywell-htram-v1w-ble-monitor");c.addView(repo);
        helpSection(c,"Acknowledgement","Based on the reverse-engineering and original Web Bluetooth project by noname122021. That work documented the HTRAM BLE protocol and made this independent native implementation possible.");
        TextView original=link("Original noname122021 project","https://github.com/noname122021/honeywell-htram-v1w-ble-monitor");c.addView(original);
        return body;
    }
    void helpSection(LinearLayout c,String h,String text){TextView t=tv(h,18);t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);t.setPadding(0,20,0,6);c.addView(t);TextView p=tv(text,14);p.setTextColor(Color.LTGRAY);p.setLineSpacing(0,1.15f);c.addView(p);}
    TextView link(String label,String url){TextView t=tv(label,15);t.setTextColor(Color.rgb(80,170,255));t.setPadding(0,10,0,10);t.setPaintFlags(t.getPaintFlags()|android.graphics.Paint.UNDERLINE_TEXT_FLAG);t.setOnClickListener(v->{try{startActivity(new Intent(Intent.ACTION_VIEW,android.net.Uri.parse(url)));}catch(Exception ignored){}});return t;}
    void updateReadings(int co2,int temp,int hum,int bat){co2Value.setText(String.valueOf(co2));tempValue.setText(temp+" °C");humValue.setText(hum+" %");batValue.setText(bat+"/4");int w=n(warning,800),a=n(alarm,1200);if(co2>=a){co2Value.setTextColor(RED);co2State.setText("High — ventilate");alarmState.setText("ALARM");alarmState.setTextColor(RED);}else if(co2>=w){co2Value.setTextColor(YELLOW);co2State.setText("Warning");alarmState.setText("WARNING");alarmState.setTextColor(YELLOW);}else{co2Value.setTextColor(GREEN);co2State.setText("Good");alarmState.setText("READY");alarmState.setTextColor(GREEN);}}
    void loadLatest(){android.content.SharedPreferences p=getSharedPreferences("latest",0);if(p.contains("co2"))updateReadings(p.getInt("co2",0),p.getInt("temp",0),p.getInt("hum",0),p.getInt("bat",0));}
    void appendLog(String msg){String ts=new java.text.SimpleDateFormat("HH:mm:ss",Locale.getDefault()).format(new Date());logView.append("["+ts+"] "+msg+"\n");if(followLog)logScroll.post(()->logScroll.fullScroll(View.FOCUS_DOWN));}
    void saveSettings(){int w=n(warning,800),a=n(alarm,1200);if(a<=w){Toast.makeText(this,"Alarm must be higher than warning",Toast.LENGTH_LONG).show();return;}getSharedPreferences("alarm",0).edit().putInt("warning",w).putInt("alarm",a).putInt("delay",n(delay,2)).putInt("repeat",Math.max(1,n(repeat,2))).putInt("profile",profile.getSelectedItemPosition()).putBoolean("sound",sound.isChecked()).putBoolean("vibrate",vibrate.isChecked()).putBoolean("silenced",false).apply();}
    void load(){android.content.SharedPreferences p=getSharedPreferences("alarm",0);warning.setText(""+p.getInt("warning",800));alarm.setText(""+p.getInt("alarm",1200));delay.setText(""+p.getInt("delay",2));repeat.setText(""+p.getInt("repeat",2));profile.setSelection(p.getInt("profile",0));sound.setChecked(p.getBoolean("sound",true));vibrate.setChecked(p.getBoolean("vibrate",true));loading=false;}
    void requestAndStart(){saveSettings();status.setText("Checking permissions…");appendLog("START pressed");ArrayList<String> ps=new ArrayList<>();if(Build.VERSION.SDK_INT>=31){if(checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN)!=PackageManager.PERMISSION_GRANTED)ps.add(Manifest.permission.BLUETOOTH_SCAN);if(checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED)ps.add(Manifest.permission.BLUETOOTH_CONNECT);}if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)ps.add(Manifest.permission.POST_NOTIFICATIONS);if(!ps.isEmpty()){appendLog("Requesting Android permissions: "+ps);requestPermissions(ps.toArray(new String[0]),REQ);return;}startMonitor();}
    @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g);if(r==REQ){for(int i=0;i<g.length;i++)if(g[i]!=PackageManager.PERMISSION_GRANTED&&(p[i].equals(Manifest.permission.BLUETOOTH_SCAN)||p[i].equals(Manifest.permission.BLUETOOTH_CONNECT))){status.setText("Bluetooth permission denied");return;}startMonitor();}}
    void startMonitor(){Intent i=new Intent(this,HtramService.class);status.setText("Starting service…");try{if(Build.VERSION.SDK_INT>=26)startForegroundService(i);else startService(i);}catch(Exception e){appendLog("START ERROR: "+e);}}
    @Override protected void onStart(){super.onStart();IntentFilter f=new IntentFilter("net.kodesoft.htrammonitor.STATUS");if(Build.VERSION.SDK_INT>=33)registerReceiver(receiver,f,RECEIVER_NOT_EXPORTED);else registerReceiver(receiver,f);}
    @Override protected void onStop(){try{unregisterReceiver(receiver);}catch(Exception ignored){}super.onStop();}
    int n(EditText e,int d){try{return Integer.parseInt(e.getText().toString());}catch(Exception x){return d;}}
    LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    LinearLayout card(){LinearLayout l=column();l.setPadding(22,20,22,20);l.setBackgroundColor(CARD);return l;}
    LinearLayout fieldBox(String s,EditText e){LinearLayout l=column();l.setPadding(4,12,4,0);l.addView(small(s));l.addView(e);return l;}
    EditText field(String s){EditText e=new EditText(this);e.setText(s);e.setTextColor(Color.WHITE);e.setTextSize(17);e.setInputType(2);e.setBackgroundColor(Color.rgb(20,21,25));e.setPadding(14,10,14,10);return e;}
    TextView tv(String s,int z){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(Color.WHITE);return t;}
    TextView small(String s){TextView t=tv(s,12);t.setTextColor(Color.LTGRAY);return t;} TextView smallPad(String s){TextView t=small(s);t.setPadding(0,18,0,6);return t;}
    TextView tab(String s){TextView t=tv(s,14);t.setText(s);t.setGravity(Gravity.CENTER);t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    Button button(String s){Button b=new Button(this);b.setText(s);b.setTextSize(12);return b;}
}