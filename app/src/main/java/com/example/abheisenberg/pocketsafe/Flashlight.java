package com.example.abheisenberg.pocketsafe;

import android.hardware.Camera;
import android.os.AsyncTask;
import android.util.Log;


/**
 * Created by abheisenberg on 10/7/17.
 */

public class Flashlight {
    /*
    Make blinking flashlight in the background using an async task.
     */

    public static final String TAG = "Flashlight";

    private Blinker blinker;

    public Flashlight(){
        Log.d(TAG, "Flashlight: ");
        blinker = new Blinker();
    }

    public void start(){
        Log.d(TAG, "start: ");
        blinker.execute();
    }

    public void stop(){
        Log.d(TAG, "stop: ");
        blinker.cancel(true);
    }

    private class Blinker extends AsyncTask<Void, Void, Void> {

        private boolean             isOn        = true;
        private Camera              camera      = Camera.open();
        private Camera.Parameters   params      = camera.getParameters();
        private int                 blinkRate   = 100;          //The rate of flashlight being in ON and OFF state, in millisecs.

        @Override
        protected Void doInBackground(Void... params) {
            while(!isCancelled()){
                blink(isOn);
                isOn = !isOn;
                try {
                    Thread.sleep(blinkRate);                      //Wait for the blink period.
                } catch (InterruptedException e){
                    Log.d(TAG, "Error while sleeping thread during blinking.");
                }
            }
            return null;
        }

        @Override
        protected void onCancelled() {          //Release the camera as soon as the phone wakes up.
            camera.release();
            Log.d(TAG, "Camera released.");
            return;
        }

        private void blink(boolean isOn){
            if(isOn){
                params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            } else {
                params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            }
            camera.setParameters(params);
        }

    }

}
