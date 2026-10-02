package net.kodesoft.htrammonitor;

import android.Manifest;
import android.app.*;
import android.bluetooth.*;
import android.bluetooth.le.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.*;
import android.os.ParcelUuid;
import androidx.annotation.Nullable;
import java.util.*;

public class HtramService extends Service {
    static final UUID SERVICE=UUID.fromString("fc247940-6e08-11e4-80fc-0002a5d5c51b");
    static final UUID WRITE=UUID.fromString("3d115840-6e0b-11e4-b24f-0002a5d5c51b");
    static final UUID NOTIFY=UUID.fromString("f833d6c0-6e0b-11e4-9136-0002a5d5c51b");
    static final UUID CCCD=UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    static final String CH="htram_monitor", ALARM_CH="htram_alarm";
    Handler h=new Handler(Looper.getMainLooper());
    BluetoothGatt gatt; BluetoothGattCharacteristic write;
    boolean stopped=false; long highSince=0,lastAlarm=0; int reconnectAttempt=0;

    @Override public void onCreate(){super.onCreate();channels();startForeground(100,notification("Searching for HTRAM…"));scan();}
    @Override public int onStartCommand(Intent i,int f,int id){return START_STICKY;}
    @Override public void onDestroy(){stopped=true;h.removeCallbacksAndMessages(null);if(gatt!=null)gatt.close();super.onDestroy();}
    @Nullable @Override public android.os.IBinder onBind(Intent i){return null;}

    void channels(){
        NotificationManager n=getSystemService(NotificationManager.class);
        NotificationChannel c=new NotificationChannel(CH,"HTRAM monitoring",NotificationManager.IMPORTANCE_LOW);c.setDescription("Persistent HTRAM connection");n.createNotificationChannel(c);
        NotificationChannel a=new NotificationChannel(ALARM_CH,"CO₂ alarms",NotificationManager.IMPORTANCE_HIGH);a.enableVibration(true);a.setVibrationPattern(new long[]{0,300,150,300,150,600});n.createNotificationChannel(a);
    }
    Notification notification(String text){
        Intent open=new Intent(this,MainActivity.class);
        PendingIntent pi=PendingIntent.getActivity(this,0,open,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        return new Notification.Builder(this,CH).setSmallIcon(android.R.drawable.stat_sys_data_bluetooth).setContentTitle("HTRAM Monitor").setContentText(text).setContentIntent(pi).setOngoing(true).setOnlyAlertOnce(true).build();
    }
    boolean perm(){return Build.VERSION.SDK_INT<31 || checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN)==PackageManager.PERMISSION_GRANTED;}
    void scan(){
        if(stopped||!perm())return;
        BluetoothAdapter a=getSystemService(BluetoothManager.class).getAdapter(); if(a==null||!a.isEnabled()){update("Bluetooth is off");retry();return;}
        ScanFilter f=new ScanFilter.Builder().setServiceUuid(new ParcelUuid(SERVICE)).build();
        ScanSettings s=new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
        try{a.getBluetoothLeScanner().startScan(Collections.singletonList(f),s,scanCb);h.postDelayed(()->{try{a.getBluetoothLeScanner().stopScan(scanCb);}catch(Exception ignored){} if(gatt==null)retry();},12000);}catch(Exception e){retry();}
    }
    final ScanCallback scanCb=new ScanCallback(){
        @Override public void onScanResult(int type,ScanResult r){BluetoothDevice d=r.getDevice();String name=null;try{name=d.getName();}catch(SecurityException ignored){} if(name==null||name.startsWith("HTRAM")){try{getSystemService(BluetoothManager.class).getAdapter().getBluetoothLeScanner().stopScan(this);}catch(Exception ignored){} connect(d);}}
        @Override public void onScanFailed(int e){retry();}
    };
    void connect(BluetoothDevice d){if(stopped)return;update("Connecting to "+safeName(d)+"…");try{gatt=d.connectGatt(this,false,cb,BluetoothDevice.TRANSPORT_LE);}catch(SecurityException e){retry();}}
    String safeName(BluetoothDevice d){try{return d.getName()==null?"HTRAM":d.getName();}catch(Exception e){return"HTRAM";}}
    final BluetoothGattCallback cb=new BluetoothGattCallback(){
        @Override public void onConnectionStateChange(BluetoothGatt g,int st,int state){
            if(state==BluetoothProfile.STATE_CONNECTED){reconnectAttempt=0;update("Connected — discovering sensor…");try{g.discoverServices();}catch(SecurityException e){retry();}}
            else if(state==BluetoothProfile.STATE_DISCONNECTED){write=null;try{g.close();}catch(Exception ignored){}gatt=null;update("Disconnected — reconnecting…");retry();}
        }
        @Override public void onServicesDiscovered(BluetoothGatt g,int st){
            BluetoothGattService s=g.getService(SERVICE);if(s==null){retry();return;}write=s.getCharacteristic(WRITE);BluetoothGattCharacteristic n=s.getCharacteristic(NOTIFY);if(write==null||n==null){retry();return;}
            try{g.setCharacteristicNotification(n,true);BluetoothGattDescriptor d=n.getDescriptor(CCCD);if(d!=null){d.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);g.writeDescriptor(d);}else init();}catch(SecurityException e){retry();}
        }
        @Override public void onDescriptorWrite(BluetoothGatt g,BluetoothGattDescriptor d,int st){init();}
        @Override public void onCharacteristicChanged(BluetoothGatt g,BluetoothGattCharacteristic c){parse(c.getValue());}
    };
    void init(){write(packet(new int[]{0x74,0x58},new int[]{1,1,0}));h.postDelayed(this::syncTime,800);h.postDelayed(poll,1400);}
    void syncTime(){Calendar c=Calendar.getInstance(TimeZone.getTimeZone("UTC"));write(packet(new int[]{0x22,0x42},new int[]{1,c.get(Calendar.YEAR)%100,c.get(Calendar.MONTH)+1,c.get(Calendar.DAY_OF_MONTH),c.get(Calendar.HOUR_OF_DAY),c.get(Calendar.MINUTE),c.get(Calendar.SECOND)}));}
    final Runnable poll=new Runnable(){public void run(){if(stopped)return;if(write!=null){write(packet(new int[]{0x40,0x44},new int[]{2,0}));h.postDelayed(this,5000);}}};
    void write(byte[] b){if(gatt==null||write==null)return;try{write.setValue(b);gatt.writeCharacteristic(write);}catch(SecurityException ignored){}}
    void parse(byte[] d){if(d==null||d.length<13||d[4]!=(byte)0x41||d[5]!=(byte)0x44)return;int co2=((d[7]&255)<<8)|(d[8]&255);int tr=d[9]&255,temp=tr<=128?tr:tr-256,hum=d[10]&255,bat=d[11]&255;update("CO₂ "+co2+" ppm  •  "+temp+"°C  •  "+hum+"%  •  battery "+bat+"/4");alarm(co2);}
    void alarm(int co2){
        android.content.SharedPreferences p=getSharedPreferences("alarm",0);int warning=p.getInt("warning",800), limit=p.getInt("alarm",1200), delay=p.getInt("delay",2), repeat=p.getInt("repeat",2);
        long now=System.currentTimeMillis();if(co2<warning){highSince=0;lastAlarm=0;return;}if(co2<limit){highSince=0;lastAlarm=0;return;}
        if(highSince==0)highSince=now;if(now-highSince<delay*60000L)return;if(lastAlarm!=0&&now-lastAlarm<repeat*60000L)return;lastAlarm=now;
        Uri u=RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        Notification n=new Notification.Builder(this,ALARM_CH).setSmallIcon(android.R.drawable.stat_notify_error).setContentTitle("High CO₂: "+co2+" ppm").setContentText("Ventilate the area. HTRAM alarm threshold exceeded.").setPriority(Notification.PRIORITY_MAX).setSound(u).setAutoCancel(true).build();
        getSystemService(NotificationManager.class).notify(101,n);
        try{Ringtone r=RingtoneManager.getRingtone(this,u);r.play();h.postDelayed(r::stop,5000);}catch(Exception ignored){}
        if(Build.VERSION.SDK_INT>=31){android.os.VibratorManager vm=(android.os.VibratorManager)getSystemService(VIBRATOR_MANAGER_SERVICE);vm.getDefaultVibrator().vibrate(android.os.VibrationEffect.createWaveform(new long[]{0,300,150,300,150,600},-1));}
        else {android.os.Vibrator v=(android.os.Vibrator)getSystemService(VIBRATOR_SERVICE);v.vibrate(android.os.VibrationEffect.createWaveform(new long[]{0,300,150,300,150,600},-1));}
    }
    void update(String s){getSystemService(NotificationManager.class).notify(100,notification(s));}
    void retry(){if(stopped)return;write=null;if(gatt!=null){try{gatt.close();}catch(Exception ignored){}gatt=null;}long wait=Math.min(30000,2000L*(1L<<Math.min(reconnectAttempt++,4)));h.postDelayed(this::scan,wait);}
    static final int[] CRC={0x0000,0x8005,0x800F,0x000A,0x801B,0x001E,0x0014,0x8011,0x8033,0x0036,0x003C,0x8039,0x0028,0x802D,0x8027,0x0022};
    byte[] packet(int[] cmd,int[] body){int len=2+body.length+3;byte[] pre=new byte[6+body.length];pre[0]=0x7b;pre[1]=0x41;pre[2]=0;pre[3]=(byte)len;pre[4]=(byte)cmd[0];pre[5]=(byte)cmd[1];for(int i=0;i<body.length;i++)pre[6+i]=(byte)body[i];int crc=crc16(pre);byte[] out=Arrays.copyOf(pre,pre.length+3);out[pre.length]=(byte)(crc>>8);out[pre.length+1]=(byte)crc;out[pre.length+2]=0x7d;return out;}
    int crc16(byte[] data){int crc=0;for(byte b:data){crc^=(b&255)<<8;for(int i=0;i<8;i++)crc=(crc&0x8000)!=0?((crc<<1)^0x8005):(crc<<1);crc&=0xffff;}return crc;}
}
