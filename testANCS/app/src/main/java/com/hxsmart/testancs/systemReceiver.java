package com.hxsmart.testancs;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * 电量变化必须动态注册，这里顺便注册了蓝牙的连接状态
 * Created by zhangbiaohe on 2018/3/27.
 */
public class systemReceiver extends BroadcastReceiver {


    private Handler mhandler;
    public systemReceiver(Handler handler){
        this.mhandler = handler;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(intent.getAction())){

                BluetoothDevice btDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                if (btDevice.getBondState()==BluetoothDevice.BOND_BONDED){

                    Log.e("zbh","已经绑定 state="+btDevice.getBondState());
                    Message msg = mhandler.obtainMessage();
                    msg.what = GlobalDefine.BLUETOOTH_BONDED;
                    mhandler.sendMessage(msg);
                }else if(btDevice.getBondState()==BluetoothDevice.BOND_BONDING){
                    Log.e("zbh","正在绑定 state="+btDevice.getBondState());
                    Message msg = mhandler.obtainMessage();
                    msg.what = GlobalDefine.BLUETOOTH_BONDING;
                    mhandler.sendMessage(msg);
                }else{
                    Log.e("zbh","解除绑定 state="+btDevice.getBondState());
                    Log.e("zbh","正在绑定 state="+btDevice.getBondState());
                    Message msg = mhandler.obtainMessage();
                    msg.what = GlobalDefine.BLUETOOTH_BONDNONE;
                    mhandler.sendMessage(msg);
                }

            }else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(intent.getAction())) {

                Log.e("zbh","断开连接");
                Message msg = mhandler.obtainMessage();
                msg.what = GlobalDefine.BLUETOOTH_DISCONNECT;
                mhandler.sendMessage(msg);
            }else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(intent.getAction())) {

                Log.e("zbh","已连接");
                Message msg = mhandler.obtainMessage();
                msg.what = GlobalDefine.BLUETOOTH_CONNECT;
                mhandler.sendMessage(msg);
            }else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())){
                Message msg = mhandler.obtainMessage();
                if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) == BluetoothAdapter.STATE_ON) {
                    Log.e("zbh","蓝牙打开");
                    msg.what = GlobalDefine.BLUETOOTH_ON;
                }
                if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) == BluetoothAdapter.STATE_OFF) {
                    Log.e("zbh","蓝牙关闭");
                    msg.what = GlobalDefine.BLUETOOTH_OFF;
                }
                mhandler.sendMessage(msg);
            }else if (intent.getAction().equals(GlobalDefine.androidResponseAction)){
                Message msg = mhandler.obtainMessage();
                msg.what = -1;
                if(intent.getIntExtra(GlobalDefine.androidResponseActionCode,-1)==1){
                    Log.e("zbh","接受");
                    msg.what = GlobalDefine.BLUETOOTH_ACCEPT;
                }

                else if(intent.getIntExtra(GlobalDefine.androidResponseActionCode,-1)==2){
                    Log.e("zbh","拒绝");
                    msg.what = GlobalDefine.BLUETOOTH_REJECT;
                }
                msg.obj = intent.getByteArrayExtra("NotificationID");
                mhandler.sendMessage(msg);
            }
    }
}
