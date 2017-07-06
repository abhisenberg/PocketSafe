package com.example.abheisenberg.pocketsafe;

import android.app.KeyguardManager;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.CountDownTimer;
import android.os.PowerManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    final String TAG = "MainActivity";

    TextView                        tvCount;
    Button                          btLock, btStopAlarm;
    DevicePolicyManager             devicePolicyManager;
    ComponentName                   componentName;
    PowerManager                    powerManager;
    PowerManager.WakeLock           wakeLock;
    KeyguardManager                 keyguardManager;
    KeyguardManager.KeyguardLock    keyguardLock;
    SensorManager                   sensorManager;
    Sensor                          proxSensor;
    SensorEventListener             sensorEventListener;
    MediaPlayer                     alarmSound;
    BroadcastReceiver               deviceUnlockedReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvCount
                = ((TextView)findViewById(R.id.tvCount));
        btLock
                = ((Button)findViewById(R.id.btLock));
        btStopAlarm
                = ((Button)findViewById(R.id.btStopSound));
        devicePolicyManager
                = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        componentName
                = new ComponentName(this, AdminReceiver.class);
        powerManager
                = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock
                = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, TAG);
        keyguardManager
                = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        keyguardLock
                = keyguardManager.newKeyguardLock("NewKeyguradLock");
        sensorManager
                = (SensorManager) getSystemService(SENSOR_SERVICE);
        proxSensor
                = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        alarmSound
                = MediaPlayer.create(this,R.raw.alarmsoundlesslouder);
        PhoneUnlockedReceiver phoneUnlockedReceiver
                = new PhoneUnlockedReceiver();

        phoneUnlockedReceiver.actionAfterUnlock(new PhoneUnlockedReceiver.AfterUnlocked() {
            @Override
            public void stopAlarmHere() {
                if(alarmSound.isPlaying()){
                    alarmSound.stop();
                    Log.d(TAG, "alarm stopped from ACTION_USER, woohoo! ");
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
                if(oldValue == -1){
                    oldValue = sensorEvent.values[0];
                    if(oldValue > 0){
                        tvCount.setText("Please put the phone in pocket!");
                    } else {
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
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: ");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: ");
    }

    @Override
    protected void onDestroy() {
        disableDeviceAdmin();
        if(isWakeLockAcq()){
            wakeLock.release();
        }
        alarmSound.release();
        sensorManager.unregisterListener(sensorEventListener);

        super.onDestroy();
    }

    public void enableDeviceAdmin(){
        Intent intent
                = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName);
        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "For getting wake lock privilages");
        startActivityForResult(intent, 15);
    }

    public void disableDeviceAdmin(){
        devicePolicyManager.removeActiveAdmin(componentName);
    }

    public boolean isAdminActive(){
        if(devicePolicyManager!=null
                && devicePolicyManager.isAdminActive(componentName)) {
            return true;
        }

         else return false;
    }

    public boolean isWakeLockAcq(){
        if(wakeLock.isHeld()){
            Log.d(TAG, "WakeLock is acquired");
            return true;
        } else {
            Log.d(TAG, "WakeLock is released/Not acq");
            return false;
        }
    }

    /*
    Waiting for N (=5 default) seconds, and then starting the proximity sensor and then
     locking the phone simultaneously (if the initial value of proxSensor is < 0, this is
     done in sensorValueChanged function.
     */

    public void putPhoneInsideGraceTime(){
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

    public void enterPwGraceTime(){
        new CountDownTimer(5000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                Log.d(TAG, "time left to enter pw: "+millisUntilFinished/1000);
            }

            @Override
            public void onFinish() {
                alarmSound.start();
            }
        }.start();
    }
}
