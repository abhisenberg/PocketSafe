package com.example.abheisenberg.pocketsafe;

import android.app.ActionBar;
import android.app.KeyguardManager;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    final String TAG = "MainActivity";

    private TextView                        tvCount;
    private DevicePolicyManager             devicePolicyManager;
    private ComponentName                   componentName;
    private PowerManager.WakeLock           wakeLock;
    private SensorManager                   sensorManager;
    private Sensor                          proxSensor;
    private SensorEventListener             sensorEventListener;
    private MediaPlayer                     alarmSound, countdownSound;
    private AudioManager                    audioManager;
    private CountDownTimer                  timeToEnterPw;
    private int                             oldVolume;

    @Override
    protected void  onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        setContentView(R.layout.activity_main);

        android.support.v7.app.ActionBar actionBar
                = getSupportActionBar();
        actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#7b8bff")));

        Button btLock
                = ((Button)findViewById(R.id.btLock));
        Button btStopAlarm
                = ((Button)findViewById(R.id.btStopSound));
        PowerManager powerManager
                = (PowerManager) getSystemService(Context.POWER_SERVICE);
        audioManager
                = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        tvCount
                = ((TextView)findViewById(R.id.tvCount));
        devicePolicyManager
                = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        componentName
                = new ComponentName(this, AdminReceiver.class);
        wakeLock
                = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, TAG);
        sensorManager
                = (SensorManager) getSystemService(SENSOR_SERVICE);
        proxSensor
                = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        alarmSound
                = MediaPlayer.create(this,R.raw.alarmsoundlesslouder);
        countdownSound
                = MediaPlayer.create(this, R.raw.countdownsound);
        timeToEnterPw
                = new CountDownTimer(5000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                Log.d(TAG, "time left to enter pw: "+millisUntilFinished/1000);
                if(Preferences.getCountdownSound(MainActivity.this)){
                    Log.d(TAG, "Playing sound ");
                    countdownSound.setLooping(true);
                    countdownSound.start();
                }
            }

            @Override
            public void onFinish() {
                if(countdownSound.isPlaying()){
                    countdownSound.stop();
                }
                oldVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                audioManager.setStreamVolume
                        (AudioManager.STREAM_MUSIC, audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC),0);
                alarmSound.start();
            }
        };

        PhoneUnlockedReceiver phoneUnlockedReceiver
                = new PhoneUnlockedReceiver();

        phoneUnlockedReceiver.actionAfterUnlock(new PhoneUnlockedReceiver.AfterUnlocked() {
            @Override
            public void stopAlarmHere() {
                if(alarmSound.isPlaying()){
                    alarmSound.stop();
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, oldVolume, 0);
                    Log.d(TAG, "alarm stopped from ACTION_USER, woohoo! ");
                } else {
                    timeToEnterPw.cancel();
                }
            }
        });

        IntentFilter phoneUnlockedIntent
                = new IntentFilter();
        phoneUnlockedIntent.addAction(Intent.ACTION_USER_PRESENT);

        registerReceiver(phoneUnlockedReceiver, phoneUnlockedIntent);

        /*
        Enable admin rights as soon as the app starts, to prevent any 'no-admin found' problems.
         */
        enableDeviceAdmin();

        /*
        When the 'Start' Button is clicked, wait for 5 seconds for the user to put the phone in pocket.
        1) Then start the proximity sensor, the initial value that the sensor will catch should be 0, which indicates
        that the phone is finally inside the pocket. If it is not zero, it means the phone is still outside, and
        a prompt should be displayed on screen alarming the user about the issue.
        2) After 5 seconds grace period, use the DevicePolicyManager to lock the phone.
         */

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()){
                    case R.id.btLock:
                        if(isAdminActive()){
                            putPhoneInsideGraceTime();              //'N' is the grace period, default is 5 sec.
                        } break;

                    case R.id.btStopSound:
                        if(alarmSound.isPlaying()){
                            alarmSound.stop();
                        }
                }
            }
        };

        btLock.setOnClickListener(listener);
        btStopAlarm.setOnClickListener(listener);

        /*
        Register the change in value of proximity sensor i.e. when the phone is taken out of the pocket
        and when is it inside the pocket.

         */
        sensorEventListener
                = new SensorEventListener() {
            float oldValue = -1, newValue = -1;            //Manually comparing 2 values of prox sensor because
                                                            // onSensorChanged not working properly in some devices.
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                Log.d(TAG, String.valueOf(sensorEvent.values[0]));
                if(oldValue < 0){
                    oldValue = sensorEvent.values[0];
                    if(oldValue > 0){
                        tvCount.setText("Please put the phone in pocket!");
                    } else {
                        /*
                            Lock the device here and start the service to disable volume buttons.
                            And disable power button if possible.
                         */

                        devicePolicyManager.lockNow();
                    }
                } else {
                    newValue = sensorEvent.values[0];
                    if(oldValue != newValue){
                        Log.d(TAG, "value changed "+oldValue+" "+newValue);

                        wakeLock.acquire();
                        sensorManager.unregisterListener(sensorEventListener);

                        //Start Sound
                        enterPwGraceTime();         //Plays alarm after the grace period for entering password
                    }
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {

            }
        };
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        /*
          Making the options icon clickable and starting settings activity
         */
        switch (item.getItemId()){
            case R.id.btIconSettings:
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void  onStart() {
        super.onStart();

    }

    @Override
    protected void  onPause() {
        super.onPause();
        Log.d(TAG, "onPause: ");
    }

    @Override
    protected void  onResume() {
        super.onResume();
        Log.d(TAG, "onResume: ");
    }

    @Override
    protected void  onDestroy() {
        disableDeviceAdmin();
        if(isWakeLockAcq()){
            wakeLock.release();
        }
        countdownSound.release();
        alarmSound.release();
        sensorManager.unregisterListener(sensorEventListener);

        super.onDestroy();
    }

    @Override
    protected void  onStop() {
        super.onStop();
    }

    public void     enableDeviceAdmin(){
        Intent intent
                = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName);
        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "For getting wake lock privilages");
        startActivityForResult(intent, 15);
    }

    public void     disableDeviceAdmin(){
        devicePolicyManager.removeActiveAdmin(componentName);
    }

    public boolean  isAdminActive(){
        if(devicePolicyManager!=null
                && devicePolicyManager.isAdminActive(componentName)) {
            return true;
        }

         else return false;
    }

    public boolean  isWakeLockAcq(){
        if(wakeLock.isHeld()){
            Log.d(TAG, "WakeLock is acquired");
            return true;
        } else {
            Log.d(TAG, "WakeLock is released/Not acq");
            return false;
        }
    }

    public void     putPhoneInsideGraceTime(){
         /*
            Waiting for N (=5 default) seconds, and then starting the proximity sensor and then
            locking the phone simultaneously (if the initial value of proxSensor is < 0, this is
            done in sensorValueChanged function. And start a service to disable volume buttons and
            power buttons.
        */
        new CountDownTimer(5000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                Log.d(TAG, "onTick: "+millisUntilFinished/1000);

                tvCount.setText(String.valueOf(millisUntilFinished/1000));
            }

            @Override
            public void onFinish() {
                Log.d(TAG, "Now trying to acquire Wake Lock");

                sensorManager.registerListener(sensorEventListener, proxSensor, SensorManager.SENSOR_DELAY_UI);
            }
        }.start();
    }

    public void     enterPwGraceTime(){
        timeToEnterPw.start();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if(event.getKeyCode() == KeyEvent.KEYCODE_POWER){
            Log.d(TAG, "power button pressed ");
            Intent closeDialog = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
            sendBroadcast(closeDialog);
            return true;
        }

        if(event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_DOWN ||
                event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP){
            Log.d(TAG, "overriding volume button");
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    private class onLockedReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getStringExtra("message").equals("disable_volume")) {
                dispatchKeyEvent(new KeyEvent(KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.ACTION_DOWN));
            }
        }
    }

}
