package com.hxsmart.testancs;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.ComponentName;
import android.support.v4.app.NotificationCompat;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.ParcelUuid;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;

public class ANCS_Service extends Service implements Handler.Callback{

    private Messenger ServerMessenger;
    private Handler ServerHandler;
    private HandlerThread handlerThread;
    private systemReceiver m_BTReceiver;
    private NotificationManager m_notificationMgr = null;

    //bt
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice mIphoneDevice;
    private LocalBluetoothGattCallback mGattCallback = new LocalBluetoothGattCallback();
    private BluetoothGatt mConnectedGatt;
    private BluetoothGattService mANCSService;
    private BluetoothGattCharacteristic mNotificationSourceChar;
    private BluetoothGattCharacteristic mPointControlChar;
    private BluetoothGattCharacteristic mDataSourceChar;

    //BLE GATT Server
    private BluetoothGattServer bluetoothGattServer;
    private AdvertiseCallback advertiseCallback;
    private BluetoothLeAdvertiser advertiser;
    private BluetoothGattServerCallback bluetoothGattServerCallback = new LocalBluetoothGattServerCallback();

    @Override
    public void onCreate() {
        super.onCreate();
        handlerThread = new HandlerThread("ServerHandlerThread");
        handlerThread.start();

        m_notificationMgr = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();

        //server adv
        if (mBluetoothAdapter.isEnabled()){
            initGATTServer();
        }else{
            mBluetoothAdapter.enable();
        }

        ServerHandler = new Handler(handlerThread.getLooper(),this);
        ServerMessenger = new Messenger(ServerHandler);
        Log.e("zbh", "onCreate");

        m_BTReceiver = new systemReceiver(ServerHandler);
        IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED);
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(m_BTReceiver,intentFilter);
    }

    @Override
    public void onDestroy() {
        handlerThread.getLooper().quit();
        handlerThread = null;
        //蓝牙线程终止
        ServerHandler = null;
        ServerMessenger = null;
        unregisterReceiver(m_BTReceiver);
        super.onDestroy();
        Log.e("zbh", "onDestroy");
    }



    @Override
    public IBinder onBind(Intent intent) {
        return ServerMessenger.getBinder();
    }

    @Override
    public boolean handleMessage(Message message) {
        switch (message.what){
            case GlobalDefine.BLUETOOTH_BONDED:
                if (mIphoneDevice != null) {
                    Log.d("zbh", "connect gatt");
                    mIphoneDevice.connectGatt(getApplicationContext(), false, mGattCallback);
                }
                break;
            case GlobalDefine.BLUETOOTH_ON:
                initGATTServer();
                break;

            case GlobalDefine.BLUETOOTH_ACCEPT:
                positiveResponseToNotification((byte[]) message.obj);
                break;
            case GlobalDefine.BLUETOOTH_REJECT:
                negativeResponseToNotification((byte[]) message.obj);
                break;
            case GlobalDefine.BLUETOOTH_DISPLAY_INFO:
                byte[] data = (byte[]) message.obj;
                //发出一个通知
                /*
                CommandID：为0；

                NotificationUID：对应之前请求的UID；

                AttributeList：查询结果列表，每一项的格式都是：ID/16bit  Length/Value，每个attribute都是一个字符串，其长度由Length指定，但是此字符串不是以NULL结尾。若找不到对应的Attribute，则Length为0；
                */

                //00
                //00000000
                //01
                //0600E5BEAEE4BFA1032000E5BEAEE4BFA1E694AFE4BB983A20E5BEAEE4BFA1E694AFE4BB98E587ADE8AF81
                Log.i("zbh","CommandID ="+data[0]);
                Log.i("zbh","NotificationUID ="+ StringTools.Bytes2HexString(data,1,4));

                int NotificationUID = 0 ;
                NotificationUID += (data[1] & 0x000000ff);
                NotificationUID += (data[2] & 0x000000ff) << 8;
                NotificationUID += (data[3] & 0x000000ff) << 16;
                NotificationUID += (data[4] & 0x000000ff) << 24;
                int tagIndex = 5;
                byte tag = data[tagIndex];
                int len=0;
                String title="";
                String msg="";
                if (tag==0x01){
                    len = data[tagIndex+1] + data[tagIndex+2]*256;
                    title = new String(data,tagIndex+3,len);
                    Log.i("zbh","title ="+ title);
                    tagIndex =tagIndex + 3 + len;
                }

                tag = data[tagIndex];
                if (tag==0x03){
                    len = data[tagIndex+1] + data[tagIndex+2]*256;
                    msg = new String(data,tagIndex+3,len);
                    Log.i("zbh","message ="+message);
                }
                showNotification(this,NotificationUID,title,msg);
                break;
            case GlobalDefine.BLUETOOTH_GET_MORE_INFO:
                byte[] data2 = (byte[]) message.obj;
                retrieveMoreInfo(data2);

                break;
            default:
                break;
        }
        return false;
    }


    @SuppressWarnings("unchecked")
    public boolean createBond(@SuppressWarnings("rawtypes") Class btClass, BluetoothDevice btDevice)
    {
        Method createBondMethod = null;
        Boolean returnValue = null;
        try {
            createBondMethod = btClass.getMethod("createBond");
            returnValue = (Boolean) createBondMethod.invoke(btDevice);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return returnValue.booleanValue();
    }


    //BLE Server For Adv
    private boolean initServices(Context context) {

        bluetoothGattServer = mBluetoothManager.openGattServer(context, bluetoothGattServerCallback);
        BluetoothGattService service = new BluetoothGattService(UUID.fromString(GlobalDefine.service_hid), BluetoothGattService.SERVICE_TYPE_PRIMARY);
        if (bluetoothGattServer!=null){
            bluetoothGattServer.addService(service);
            Log.i("zbh", "2. initServices ok");
            return true;
        }else{
            closeGattServer();
            return false;
        }
    }


    public void closeGattServer(){
        if (bluetoothGattServer!=null)
        {
            bluetoothGattServer.clearServices();
            bluetoothGattServer.close();
        }

        if (advertiser!=null){
            advertiser.stopAdvertising(advertiseCallback);
        }
    }

    public void initGATTServer() {

        //先初始化好服务
        if(!initServices(getApplicationContext())){
            return;
        }

        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setConnectable(true)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .build();

        AdvertiseData advertiseData = new AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .addServiceUuid(ParcelUuid.fromString(GlobalDefine.service_hid))
                .build();

        advertiseCallback = new AdvertiseCallback() {

            @Override
            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                Log.e("zbh", "#BLE advertisement successfully");
            }

            @Override
            public void onStartFailure(int errorCode) {
                Log.e("zbh", "Failed to add BLE advertisement, reason: " + errorCode);
                if(errorCode == ADVERTISE_FAILED_DATA_TOO_LARGE){
                    Log.e("zbh","Failed to start advertising as the advertise data to be broadcasted is larger than 31 bytes.");
                }else if(errorCode == ADVERTISE_FAILED_TOO_MANY_ADVERTISERS){
                    Log.e("zbh","Failed to start advertising because TOO_MANY_ADVERTISERS.");
                }else if(errorCode == ADVERTISE_FAILED_ALREADY_STARTED){
                    Log.e("zbh","Failed to start advertising as the advertising is already started");
                }else if(errorCode == ADVERTISE_FAILED_INTERNAL_ERROR){
                    Log.e("zbh","Operation failed due to an internal error");
                }else if(errorCode == ADVERTISE_FAILED_FEATURE_UNSUPPORTED){
                    Log.e("zbh","This feature is not supported on this platform");
                }
            }
        };

        advertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
        advertiser.stopAdvertising(advertiseCallback);
        if (advertiser!=null){
            advertiser.startAdvertising(settings, advertiseData,advertiseCallback);
        }
    }


    public void negativeResponseToNotification(byte[] nid) {

        byte[] action = {
                (byte) 0x02,
                //UID
                nid[0], nid[1], nid[2], nid[3],
                //action id
                (byte) 0x01,

        };

        //如果已经绑定，而且此时未断开
        if (mConnectedGatt != null) {
            BluetoothGattService service = mConnectedGatt.getService(UUID.fromString(GlobalDefine.service_ancs));
            if (service == null) {
                Log.d("zbh", "cant find service");
            } else {
                Log.d("zbh", "find service");
                BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(GlobalDefine.characteristics_control_point));
                if (characteristic == null) {
                    Log.d("zbh", "cant find chara");
                } else {
                    Log.d("zbh", "find chara");
                    characteristic.setValue(action);
                    mConnectedGatt.writeCharacteristic(characteristic);
                }
            }
        }
    }

    public void positiveResponseToNotification(byte[] nid) {

        byte[] action = {
                (byte) 0x02,
                //UID
                nid[0], nid[1], nid[2], nid[3],
                //action id
                (byte) 0x00,

        };

        //如果已经绑定，而且此时未断开
        if (mConnectedGatt != null) {
            BluetoothGattService service = mConnectedGatt.getService(UUID.fromString(GlobalDefine.service_ancs));
            if (service == null) {
                Log.d("zbh", "cant find service");
            } else {
                Log.d("zbh", "find service");
                BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(GlobalDefine.characteristics_control_point));
                if (characteristic == null) {
                    Log.d("zbh", "cant find chara");
                } else {
                    Log.d("zbh", "find chara");
                    characteristic.setValue(action);
                    mConnectedGatt.writeCharacteristic(characteristic);
                }
            }
        }
    }

    public void retrieveMoreInfo(byte[] nid) {


        byte[] getNotificationAttribute = {
                (byte) 0x00,
                //UID
                nid[0], nid[1], nid[2], nid[3],

                //title
                (byte) 0x01, (byte) 0xff, (byte) 0xff,
                //subtitle
//                (byte) 0x02, (byte) 0xff, (byte) 0xff,
                //message
                (byte) 0x03, (byte) 0xff, (byte) 0xff
        };

        Log.i("zbh","发送获取详细信息的指令="+StringTools.Bytes2HexString(getNotificationAttribute,0,getNotificationAttribute.length));
        //如果已经绑定，而且此时未断开
        if (mConnectedGatt != null) {
            BluetoothGattService service = mConnectedGatt.getService(UUID.fromString(GlobalDefine.service_ancs));
            if (service == null) {
                Log.d("zbh", "cant find service");
            } else {
                Log.d("zbh", "find service");
                BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(GlobalDefine.characteristics_control_point));
                if (characteristic == null) {
                    Log.d("zbh", "cant find chara");
                } else {
                    Log.d("zbh", "find chara");
                    characteristic.setValue(getNotificationAttribute);
                    mConnectedGatt.writeCharacteristic(characteristic);
                }
            }
        }
    }

    private void setNotificationEnabled(BluetoothGattCharacteristic characteristic) {
        mConnectedGatt.setCharacteristicNotification(characteristic, true);
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(GlobalDefine.descriptor_config));
        if (descriptor != null) {
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mConnectedGatt.writeDescriptor(descriptor);
        }
    }


    private class LocalBluetoothGattCallback extends BluetoothGattCallback {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d("zbh", "connected");
                mConnectedGatt = gatt;
                gatt.discoverServices();
            }
            if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                //外设主动断开
                Log.d("zbh", "disconnected");
                initGATTServer();
                mConnectedGatt = null;
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                BluetoothGattService ancsService = gatt.getService(UUID.fromString(GlobalDefine.service_ancs));
                if (ancsService == null) {
                    Log.d("zbh", "ANCS cannot find");
                } else {
                    Log.d("zbh", "ANCS find");
                    mANCSService = ancsService;
                    mDataSourceChar = ancsService.getCharacteristic(UUID.fromString(GlobalDefine.characteristics_data_source));
                    mPointControlChar = ancsService.getCharacteristic(UUID.fromString(GlobalDefine.characteristics_control_point));
                    mNotificationSourceChar = ancsService.getCharacteristic(UUID.fromString(GlobalDefine.characteristics_notification_source));
                    setNotificationEnabled(mDataSourceChar);
                }
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            Log.d("zbh", " onDescriptorWrite:: " + status);
            // Notification source
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (descriptor.getCharacteristic().getUuid().equals(UUID.fromString(GlobalDefine.characteristics_data_source))) {
                    setNotificationEnabled(mNotificationSourceChar);
                    Log.d("zbh", "data_source 订阅成功 ");
                }
                if (descriptor.getCharacteristic().getUuid().equals(UUID.fromString(GlobalDefine.characteristics_notification_source))) {
                    Log.d("zbh", "notification_source　订阅成功 ");
                }
            }

        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.d("zbh", "onCharacteristicWrite");
            if (GlobalDefine.characteristics_control_point.equals(characteristic.getUuid().toString())) {
                Log.d("zbh", "control_point  Write successful");

            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            if (GlobalDefine.characteristics_notification_source.equals(characteristic.getUuid().toString())) {
                Log.d("zbh", "notification_source Changed");
                byte[] nsData = characteristic.getValue();
                Log.i("zbh","通知数据："+StringTools.Bytes2HexString(nsData,0,nsData.length));


                /*
                EventID：消息类型，添加(0)、修改(1)、删除(2)；

                EventFlags：消息优先级，静默(1)、重要(2)；

                CategoryID：消息类型；

                CategoryCount：消息计数；

                NotificationUID：通知ID，可以通过此ID获取详情；
                */
                //TODO getMoreAboutNotification(nsData);
                Log.i("zbh","EventID ="+nsData[0]);
                Log.i("zbh","EventFlags ="+nsData[1]);
                Log.i("zbh","CategoryID ="+nsData[2]);
                Log.i("zbh","CategoryCount ="+nsData[3]);
                Log.i("zbh","NotificationUID ="+ StringTools.Bytes2HexString(nsData,4,4));

                if (nsData[0]==0x02){
                    Log.i("zbh","通知被iphone删除");
                }else{
                    byte[] NotificationUID = new byte[4];
                    System.arraycopy(nsData,4,NotificationUID,0,4);
                    Message msg = ServerHandler.obtainMessage();
                    msg.what = GlobalDefine.BLUETOOTH_GET_MORE_INFO;
                    msg.obj = NotificationUID;
                    ServerHandler.sendMessage(msg);
                }
            }
            if (GlobalDefine.characteristics_data_source.equals(characteristic.getUuid().toString())) {
                Log.d("zbh", "characteristics_data_source changed");
                byte[] get_data = characteristic.getValue();
                Log.i("zbh","详细数据："+StringTools.Bytes2HexString(get_data,0,get_data.length));

                //TODO 显示通知消息
                Message msg = ServerHandler.obtainMessage();
                msg.what = GlobalDefine.BLUETOOTH_DISPLAY_INFO;
                msg.obj = get_data;
                ServerHandler.sendMessage(msg);
            }

            if (GlobalDefine.characteristics_control_point.equals(characteristic.getUuid().toString())) {
                Log.d("zbh", "characteristics_control_point changed");
                byte[] cpData = characteristic.getValue();
                Log.i("zbh","控制数据："+StringTools.Bytes2HexString(cpData,0,cpData.length));
            }
        }

    }

    private class LocalBluetoothGattServerCallback extends BluetoothGattServerCallback{

        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            mIphoneDevice = device;
            if (newState==2){
                closeGattServer();
                String MacAddress = mIphoneDevice.getAddress();
                Log.i("zbh","已连接设备MAC："+ MacAddress);
                if (mIphoneDevice.getBondState()==BluetoothDevice.BOND_BONDED){
                    mIphoneDevice.connectGatt(getApplicationContext(), false, mGattCallback);
                }else{
                    createBond(device.getClass(),device);
                }
            }
        }
    }


    public static void showNotification(Context context,int id,String title,String message) {
        Notification notification = new NotificationCompat.Builder(context)
                /**设置通知左边的大图标**/
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher))
                /**设置通知右边的小图标**/
                .setSmallIcon(R.mipmap.ic_launcher)
                /**通知首次出现在通知栏，带上升动画效果的**/
                .setTicker("通知来了")
                /**设置通知的标题**/
                .setContentTitle(title)
                /**设置通知的内容**/
                .setContentText(message)
                /**通知产生的时间，会在通知信息里显示**/
                .setWhen(System.currentTimeMillis())
                /**设置该通知优先级**/
                .setPriority(Notification.PRIORITY_DEFAULT)
                /**设置这个标志当用户单击面板就可以让通知将自动取消**/
                .setAutoCancel(true)
                /**设置他为一个正在进行的通知。他们通常是用来表示一个后台任务,用户积极参与(如播放音乐)或以某种方式正在等待,因此占用设备(如一个文件下载,同步操作,主动网络连接)**/
                .setOngoing(false)
                /**向通知添加声音、闪灯和振动效果的最简单、最一致的方式是使用当前的用户默认设置，使用defaults属性，可以组合：**/
                .setDefaults(Notification.DEFAULT_VIBRATE | Notification.DEFAULT_SOUND)
                .setContentIntent(PendingIntent.getActivity(context, 1, new Intent(context, MainActivity.class), PendingIntent.FLAG_CANCEL_CURRENT))
                .build();
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        /**发起通知**/
        notificationManager.notify(id, notification);
    }
}
