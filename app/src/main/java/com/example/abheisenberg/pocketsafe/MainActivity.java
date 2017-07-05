package com.example.abheisenberg.pocketsafe;

import android.app.KeyguardManager;
import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.CountDownTimer;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    final String TAG = "MainActivity";

    TextView                    tvProxValue;
    Button                      btLock;
    DevicePolicyManager         devicePolicyManager;
    ComponentName               componentName;
    PowerManager                powerManager;
    PowerManager.WakeLock       wakeLock;
    KeyguardManager             keyguardManager;
    KeyguardManager.KeyguardLock keyguardLock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvProxValue
                = ((TextView)findViewById(R.id.tvProxiValues));
        btLock
                = ((Button)findViewById(R.id.btLock));
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
        SensorManager sensorManager
                = (SensorManager) getSystemService(SENSOR_SERVICE);
        Sensor proxSensor
                = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);


        enableDeviceAdmin();
        isAdminActive();

        btLock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isAdminActive()){
                    devicePolicyManager.lockNow();
                    wakeUpAfterNSec();
                }
            }
        });

        SensorEventListener sensorEventListener
                = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                float v = sensorEvent.values[0];
                tvProxValue.setText(String.valueOf(v));
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {

            }
        };

        sensorManager.registerListener(sensorEventListener, proxSensor, SensorManager.SENSOR_DELAY_UI);

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
        isAdminActive();

        wakeLock.release();
        isWakeLockAcq();

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

    public void wakeUpAfterNSec(){
        new CountDownTimer(5000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                Log.d(TAG, "onTick: "+millisUntilFinished/1000);
            }

            @Override
            public void onFinish() {
                Log.d(TAG, "Now trying to acquire Wake Lock");
                if(wakeLock != null && !isWakeLockAcq()){
                    wakeLock.acquire();
                    keyguardLock.disableKeyguard();
                }
                isWakeLockAcq();
            }
        }.start();
    }
}
