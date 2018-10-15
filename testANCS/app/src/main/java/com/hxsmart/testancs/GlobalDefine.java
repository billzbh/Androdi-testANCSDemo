package com.hxsmart.testancs;

/**
 * Created by hxsmart on 2018/3/23.
 */

public class GlobalDefine {

    static public final int BLUETOOTH_ACCEPT        =  999;
    static public final int BLUETOOTH_REJECT        =  1000;
    static public final int BLUETOOTH_ON            =  1001;
    static public final int BLUETOOTH_OFF           =  1002;
    static public final int BLUETOOTH_CONNECT       =  1003;
    static public final int BLUETOOTH_DISCONNECT    =  1004;
    static public final int BLUETOOTH_BONDED        =  1005;
    static public final int BLUETOOTH_BONDNONE      =  1006;
    static public final int BLUETOOTH_BONDING       =  1007;

    static public final int BLUETOOTH_GET_MORE_INFO =  1008;
    static public final int BLUETOOTH_DISPLAY_INFO  =  1009;

    public static final String service_hid = "00001812-0000-1000-8000-00805f9b34fb";
    public static final String service_ancs = "7905f431-b5ce-4e99-a40f-4b1e122d00d0";
    public static final String characteristics_notification_source = "9fbf120d-6301-42d9-8c58-25e699a21dbd";
    public static final String characteristics_data_source = "22eac6e9-24d6-4bb5-be44-b36ace7c7bfb";
    public static final String characteristics_control_point = "69d1d8f3-45e1-49a8-9821-9bbdfdaad9d9";
    public static final String descriptor_config = "00002902-0000-1000-8000-00805f9b34fb";

    public static final String destoryActionString = "com.zhangbh.billchang.service.destory";
    public static final String androidResponseAction = "com.zhangbh.billchang.NotifyActionString";
    public static final String androidResponseActionCode = "com.zhangbh.billchang.androidResponseActionCode";
}
