package com.example.abheisenberg.pocketsafe;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

public class SettingsActivity extends AppCompatActivity {
    /*
    This activity uses shared preference to store the settings. Defaults have been set already so if the user
    doesn't change anything default values are returned.
     */

    Switch swCountdownSound, swExtraPw, swFlashlight;
    Spinner spnTimes;

    public static final String TAG = "SettingsAct";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        swCountdownSound = ((Switch)findViewById(R.id.swCountdownSound));
        swExtraPw = ((Switch)findViewById(R.id.swExtraPw));
        swFlashlight = ((Switch)findViewById(R.id.swFlashlight));
        spnTimes = ((Spinner)findViewById(R.id.spnTimes));
        spnTimes.setSelection(Preferences.getTimeToShowOnSpinner(this));

        swExtraPw.setChecked(Preferences.getIfExtraPw(this));
        swCountdownSound.setChecked(Preferences.getCountdownSound(this));
        swFlashlight.setChecked(Preferences.getIfFlashOn(this));

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()){
                    case R.id.swCountdownSound:
                        Log.d(TAG, "extra pw ");
                        Preferences.setCountdownSound(SettingsActivity.this, swCountdownSound.isChecked());
                        swCountdownSound.setChecked(Preferences.getCountdownSound(SettingsActivity.this));
                        break;

                    case R.id.swExtraPw:
                        if(swExtraPw.isChecked()){
                            Log.d(TAG, "extra pw ");
                            Preferences.setIfExtraPw(SettingsActivity.this, true);
                            enterExtraPw();
                        } else {
                            Preferences.setIfExtraPw(SettingsActivity.this, false);
                            Preferences.setExtraPw(SettingsActivity.this, "");
                        }
                        swExtraPw.setChecked(Preferences.getIfExtraPw(SettingsActivity.this));
                        break;

                    case R.id.swFlashlight:
                        Log.d(TAG, "Flashlight toggled ");
                        Preferences.setIfFlashOn(SettingsActivity.this, swFlashlight.isChecked());
                        swFlashlight.setChecked(Preferences.getIfFlashOn(SettingsActivity.this));
                        break;
                }
            }
        };

        spnTimes.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int itemSelected = Integer.valueOf(parent.getSelectedItem().toString());
                Log.d(TAG, "onItemSelected: "+itemSelected);
                Preferences.setGraceTime(SettingsActivity.this, itemSelected);
                Preferences.setTimeToShowOnSpinner(SettingsActivity.this, position);
                Toast.makeText(SettingsActivity.this, "Restart the app if you change the grace time", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        swCountdownSound.setOnClickListener(listener);
        swExtraPw.setOnClickListener(listener);
        swFlashlight.setOnClickListener(listener);
    }

    private void enterExtraPw(){
        /*
        Dialog to set the new extra password.
         */
        Log.d(TAG, "Dialog");
        AlertDialog.Builder alert
                = new AlertDialog.Builder(this);
        alert.setTitle("Enter New PIN");
        alert.setMessage("This will be required to stop the alert sound after unlocking the device.");

        final EditText input
                = new EditText((this));
        alert.setView(input);

        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Preferences.setExtraPw(SettingsActivity.this, input.getText().toString());
                Toast.makeText(SettingsActivity.this, "Password setup successfully!", Toast.LENGTH_SHORT).show();
            }
        });

        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(SettingsActivity.this, "Extra Password was not setup!", Toast.LENGTH_SHORT).show();
                Preferences.setIfExtraPw(SettingsActivity.this, false);
                Preferences.setExtraPw(SettingsActivity.this, "");
            }
        });

        alert.create();
        alert.show();
    }

}
