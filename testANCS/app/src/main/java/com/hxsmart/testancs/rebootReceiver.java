package com.hxsmart.testancs;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class rebootReceiver extends BroadcastReceiver {


    @Override
    public void onReceive(Context context, Intent intent) {

        if ( intent.getAction().equals(GlobalDefine.destoryActionString)) {
            Intent myIntent = new Intent(context, ANCS_Service.class);
            myIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startService(myIntent);
        }
    }
}
