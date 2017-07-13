package com.example.abheisenberg.pocketsafe;

import android.Manifest;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.CountDownTimer;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

public class MainActivity extends AppCompatActivity {
    final String TAG = "MainActivity";
    public static final String MainText = "PRESS THE BUTTON ABOVE AND PROTECT YOUR PHONE FROM THEFT";

    /*
    All the required variables are declared here.
     */

    private TextView                        tvTextOnButton, tvCountdown;
    private TextSwitcher                    tsMainText, tsInPocketPrompt;
    private DevicePolicyManager             devicePolicyManager;
    private ComponentName                   componentName;
    private PowerManager.WakeLock           wakeLock;
    private SensorManager                   sensorManager;
    private Sensor                          proxSensor;
    private ImageButton                     btLock;
    private SensorEventListener             sensorEventListener;
    private MediaPlayer                     alarmSound, countdownSound;
    private AudioManager                    audioManager;
    private PhoneUnlockedReceiver           phoneUnlockedReceiver;
    private IntentFilter                    phoneUnlockedIntent;
    private CountDownTimer                  timeToEnterPw, putPhoneInsideGraceTimer;
    private int                             oldVolume;
    private boolean                         isDialogCreated, isSafetyLockStarted;
    private Flashlight                      flashlight;

    @Override
    protected void  onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /*
        For making the splash screen before the main activity shows up.
         */
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        setContentView(R.layout.activity_main);

        /*
        Making a new action bar to embed a settings icon on it.
         */

        android.support.v7.app.ActionBar actionBar
                = getSupportActionBar();
        actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#7b8bff")));

        /*
        All variables are initialized here, when the app starts.
         */
        PowerManager powerManager
                = (PowerManager) getSystemService(Context.POWER_SERVICE);
        tsInPocketPrompt
                = ((TextSwitcher) findViewById(R.id.tsInPocketPrompt));
        tvTextOnButton
                = ((TextView)findViewById(R.id.tvTextOnButton));
        btLock
                = ((ImageButton)findViewById(R.id.btLock));
        audioManager
                = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        tsMainText
                = ((TextSwitcher) findViewById(R.id.tsMainText));
        tsMainText.setFactory(new ViewSwitcher.ViewFactory() {
            @Override
            public View makeView() {
                TextView textView = new TextView(MainActivity.this);
                textView.setTextSize(18);
                textView.setGravity(Gravity.CENTER);
                return textView;
            }
        });
        tsInPocketPrompt.setFactory(new ViewSwitcher.ViewFactory() {
            @Override
            public View makeView() {
                TextView textView = new TextView(MainActivity.this);
                textView.setTextSize(18 );
                textView.setGravity(Gravity.CENTER);
                return textView;
            }
        });
        tsMainText.setInAnimation(this, android.R.anim.fade_in);
        tsMainText.setOutAnimation(this, android.R.anim.fade_out);
        tsInPocketPrompt.setInAnimation(this, android.R.anim.fade_in);
        tsInPocketPrompt.setOutAnimation(this, android.R.anim.fade_out);
        tvCountdown
                = ((TextView)findViewById(R.id.tvCountdown));
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

        timeToEnterPw                //Countdown timer to enter password.
                = new CountDownTimer((long)Preferences.getGraceTime(this)*1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                Log.d(TAG, "time left to enter pw: "+millisUntilFinished/1000);
                if(Preferences.getCountdownSound(MainActivity.this)){
                    Log.d(TAG, "Playing sound ");
                    audioManager.setStreamVolume
                            (AudioManager.STREAM_MUSIC, audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)/2, 0);
                    countdownSound.setLooping(true);
                    countdownSound.start();
                }
            }

            @Override
            public void onFinish() {
                /*
                    First stop the countdown sound.
                    Then start the actual alarm sound.
                 */
                stopCurrentlyPlayingAlarm();

                oldVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                Log.d(TAG, "starting alarm sound ");
                audioManager.setStreamVolume
                        (AudioManager.STREAM_MUSIC, audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC),0);
                alarmSound.start();

                if(Preferences.getIfFlashOn(MainActivity.this)){
                    flashlight
                            = new Flashlight();
                    flashlight.start();
                }
            }
        };
        putPhoneInsideGraceTimer
                = new CountDownTimer(6000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                Log.d(TAG, "onTick: "+millisUntilFinished/1000);

                tsInPocketPrompt.setText("PUT YOUR PHONE IN POCKET");
                tsMainText.setText("YOUR PHONE WILL LOCK AUTOMATICALLY AFTER\n");
                tvCountdown.setText(millisUntilFinished/1000+"s");
            }

            @Override
            public void onFinish() {
                Log.d(TAG, "Now trying to acquire Wake Lock");

                sensorManager.registerListener(sensorEventListener, proxSensor, SensorManager.SENSOR_DELAY_UI);
            }
        };
        alarmSound
                = new MediaPlayer();
        countdownSound
                = new MediaPlayer();
        phoneUnlockedReceiver
                = new PhoneUnlockedReceiver();
        phoneUnlockedIntent
                = new IntentFilter();
        phoneUnlockedIntent.addAction(Intent.ACTION_USER_PRESENT);

        registerReceiver(phoneUnlockedReceiver, phoneUnlockedIntent);
        /*
        Enable admin rights as soon as the app starts, to prevent any 'no-admin found' problems.
         */
        enableDeviceAdmin();

        /*
        Ask for the permission to use camera flashlight. If the user accepts, set the flashlight feature to ON,
        else set the flashlight feature to OFF
         */

        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[] {
                            Manifest.permission.CAMERA
                    }, 111);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(permissions[0].equals(Manifest.permission.CAMERA)){
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Preferences.setIfFlashOn(MainActivity.this, true);      //Set the default of flashlight to ON
            } else {
                Preferences.setIfFlashOn(MainActivity.this, false);     //Set the default of flashlight to OFF
            }
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.getMenuInflater().inflate(R.menu.main, menu);          //Embedding the settings icon on the action bar.
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
    protected void  onResume() {        //All the work starts here, as soon as the activity is prepared.
        super.onResume();
        Log.d(TAG, "onResume: ");

        /*
        Only for the feature for 'extra password dialog', it is to prevent multiple dialogs being created asking
        for passwords.
         */
        isDialogCreated
                = false;

        /*
        Safety check and creating new alarm.
         */
        if(!alarmSound.isPlaying()){
            alarmSound = MediaPlayer.create(this, R.raw.alarmsoundlesslouder);
        }
        if(!countdownSound.isPlaying()){
            countdownSound = MediaPlayer.create(this, R.raw.countdownsound);
        }
        /*
        If the app is resumed after the phone has been woken up after being in safe lock, stop the alarm.
        There can be either the countdown timer alarm playing, of the actual alarm playing. Check which one is being
         played and stop it, preparing for the next usage.
         */
        phoneUnlockedReceiver.actionAfterUnlock(new PhoneUnlockedReceiver.AfterUnlocked() {
            @Override
            public void stopAlarmHere() {
                Log.d(TAG, "Attempting to stop alarm ");

                resetView();
                MediaPlayer currentlyPlaying = whichAlarmIsPlaying();

               if(currentlyPlaying.isPlaying()){
                   if(Preferences.getIfExtraPw(MainActivity.this)){
                       enterExtraPINtoStopAlarm();
                   } else {
                       stopCurrentlyPlayingAlarm();
                   }
               }
            }
        });

        /*
        When the 'Start' Button is clicked, wait for 5 seconds for the user to put the phone in pocket.
        1) Then start the proximity sensor, the initial value that the sensor will catch should be 0, which indicates
        that the phone is finally inside the pocket. If it is not zero, it means the phone is still outside, and
        a prompt should be displayed on screen alarming the user about the issue.
        2) After 5 seconds grace period, use the DevicePolicyManager to lock the phone.
         */

        isSafetyLockStarted = false;       //Check whether the app will show blue button for starting safety lock or red button to
                                             //cancel if it is already started.
        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()){
                    case R.id.btLock:
                        if(!isSafetyLockStarted && isAdminActive()){ //Start the safety lock button clicked.
                            putPhoneInsideGraceTime();              //'N' is the grace period, default is 5 sec.
                            btLock.setImageResource(R.drawable.red_round_button);
                            tvTextOnButton.setText("CANCEL");
                            isSafetyLockStarted = true;
                        } else {                                    //Cancel the safety lock button clicked.
                            lockCancelled();
                            resetView();
                            isSafetyLockStarted = false;
                        }
                        break;
                }
            }
        };

        btLock.setOnClickListener(listener);

        /*
        Register the change in value of proximity sensor i.e. when the phone is taken out of the pocket
        and when is it inside the pocket.
         */
        sensorEventListener
                = new SensorEventListener() {
            float oldValue = -1, newValue = -1;
            //Manually comparing 2 values of prox sensor because
            // onSensorChanged not working properly in some devices.
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                Log.d(TAG, String.valueOf(sensorEvent.values[0]));
                if(oldValue < 0){
                    oldValue = sensorEvent.values[0];
                    if(oldValue > 0){
                        //The case where The user did not put the phone in pocket, hence cancel all the operations.

                        tsMainText.setText("Please put the phone in pocket!");
                        lockCancelled();;
                    } else {
                        /*  The user put the phone in pocket.
                            Lock the device here and start the service to disable volume buttons.
                            And disable power button if possible.
                         */
                        devicePolicyManager.lockNow();
                    }
                } else {
                    /*
                    Someone took the phone out of the pocket, wakeup the screen, start the countdown for the
                    user to enter password and release the wakelock to save battery.
                     */
                    newValue = sensorEvent.values[0];
                    if(oldValue != newValue){
                        Log.d(TAG, "trying to acquire wakelock ");
                        wakeLock.acquire();         //Waking the device up.
                        isWakeLockAcq();

                        sensorManager.unregisterListener(sensorEventListener);  //unregistering prox sensor to save battery
                        wakeLock.release();         //releasing wake-lock to save battery.

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
    protected void  onDestroy() { //Unregister and release everything.
        Log.d(TAG, "onDestroy: ");

        disableDeviceAdmin();
        if(isWakeLockAcq()){
            wakeLock.release();
        }
        countdownSound.release();
        alarmSound.release();
        sensorManager.unregisterListener(sensorEventListener);
        unregisterReceiver(phoneUnlockedReceiver);

        super.onDestroy();
    }

    @Override
    protected void  onStop() {
        Log.d(TAG, "onStop: ");
        super.onStop();
    }

    public void     enableDeviceAdmin(){        //Without enabling device-admin the app won't be able to
                                                //lock the device.
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

    public void putPhoneInsideGraceTime(){
         /*
            Waiting for N (=5 default) seconds, and then starting the proximity sensor and then
            locking the phone simultaneously (if the initial value of proxSensor is < 0, this is
            done in sensorValueChanged function. And start a service to disable volume buttons and
            power buttons.
        */
        putPhoneInsideGraceTimer.start();
    }

    public void enterPwGraceTime(){
        timeToEnterPw.start();
    }

    public void enterExtraPINtoStopAlarm(){
        Log.d(TAG, "Dialog");
        //Dialog to enter the password and validate it to stop the alarm.

       final AlertDialog.Builder alert
                = new AlertDialog.Builder(this);

        alert.setTitle("Enter PIN");
        alert.setMessage("To stop the alarm");

        final EditText input
                = new EditText((this));
        LinearLayout.LayoutParams layoutParams
                = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
        );
        input.setLayoutParams(layoutParams);
        alert.setView(input);

        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String enteredPIN = input.getText().toString();
                if(enteredPIN.equals(Preferences.getExtraPw(MainActivity.this))){
                    Log.d(TAG, "Correct pw entered ");
                    stopCurrentlyPlayingAlarm();
                } else {
                    Log.d(TAG, "Wrong pw entered ");
                    Toast.makeText(MainActivity.this, "Wrong PIN, please enter again!", Toast.LENGTH_SHORT).show();
                    isDialogCreated = false;
                    enterExtraPINtoStopAlarm();
                }
            }
        });

        alert.setCancelable(false);
        Log.d(TAG, "Creating dialog ");
        alert.create();
        if(!isDialogCreated){
            isDialogCreated = true;
            alert.show();
        }
    }

    public void stopCurrentlyPlayingAlarm(){
        MediaPlayer currentlyPlaying = whichAlarmIsPlaying();
        if(currentlyPlaying.isPlaying()){
            currentlyPlaying.stop();
            timeToEnterPw.cancel();
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,oldVolume, 0);
            currentlyPlaying.prepareAsync();
        }
        if(Preferences.getIfFlashOn(MainActivity.this)){
            if(flashlight != null){
                Log.d(TAG, "Flashlight stopping ");
                flashlight.stop();
            }
        }
    }

    public MediaPlayer whichAlarmIsPlaying(){
        if(alarmSound.isPlaying()){
            return alarmSound;
        }
        if(countdownSound.isPlaying()){
            return countdownSound;
        }
        else
            return new MediaPlayer();
    }

    public void lockCancelled(){
        sensorManager.unregisterListener(sensorEventListener);
        if(wakeLock.isHeld()){
            wakeLock.release();
        }
        putPhoneInsideGraceTimer.cancel();
        timeToEnterPw.cancel();
        onResume();
    }

    public void resetView(){
        /*
        Reset every text-view to it's original value after alarm has been sounded, the screen is woken
        up and the app resumes.s
         */
        btLock.setImageResource(R.drawable.blue_round_button);
        tvTextOnButton.setText("START \nSAFE LOCK");
        tsMainText.setText(MainText);
        tsInPocketPrompt.setText(" ");
        tvCountdown.setText(" ");
    }
}
