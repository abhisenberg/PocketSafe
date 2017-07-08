package com.example.abheisenberg.pocketsafe;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

public class SettingsActivity extends AppCompatActivity {

    Switch swCountdownSound, swExtraPw;
    SharedPreferences sharedPreferences;

    public static final String TAG = "SettingsAct";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        swCountdownSound = ((Switch)findViewById(R.id.swCountdownSound));
        swExtraPw = ((Switch)findViewById(R.id.swExtraPw));

        swExtraPw.setChecked(Preferences.getIfExtraPw(this));
        swCountdownSound.setChecked(Preferences.getCountdownSound(this));

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
                }
            }
        };

        swCountdownSound.setOnClickListener(listener);
        swExtraPw.setOnClickListener(listener);
    }

    private void enterExtraPw(){
        Log.d(TAG, "Dialog");
        AlertDialog.Builder alert
                = new AlertDialog.Builder(this);
        alert.setTitle("Enter New PIN");

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
    }

}
