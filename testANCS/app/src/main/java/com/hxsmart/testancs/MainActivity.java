package com.hxsmart.testancs;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

public class MainActivity extends Activity{

    private Intent myIntent;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        myIntent = new Intent(this, ANCS_Service.class);
        startService(myIntent);
    }

    @Override
    protected void onDestroy() {
//        stopService(myIntent);
        super.onDestroy();
    }


}
