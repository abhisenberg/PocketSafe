package com.example.abheisenberg.pocketsafe;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by abheisenberg on 4/7/17.
 */

public class AdminReceiver extends DeviceAdminReceiver {

    /*
    Receive the admin permission to lock the phone.
     */

    final String TAG = "AdminReceiver";
    @Override
    public void onEnabled(Context context, Intent intent) {
        super.onEnabled(context, intent);
        Log.d(TAG, "Admin Enabled");
    }

    @Override
    public void onDisabled(Context context, Intent intent) {
        super.onDisabled(context, intent);
        Log.d(TAG, "Admin Disabled ");
    }
}
