

## 背景

受到 https://www.jianshu.com/p/88858b8e5e67 的启发，我明确了要发现ANCS服务需要在配对并绑定蓝牙后。此文中的办法是在 ios侧使用 lightblue 模拟一个外设Heart Rate，让Android 搜索连接它，然后触发配对绑定，再接着搜索ANCS服务。操作起来还是比较繁琐。

我期待的是：在ios 的【设置-蓝牙】可以直接点击Android手机蓝牙设备名称，能配对成功并同时android能订阅ANCS。

## 简化操作方式

1.  首先需要Android手机作为外设广播数据，其中的一个GATT服务uuid必须是ios可见的，经过测试，HID的服务是可见的，uuid为 1812 (00001812-0000-1000-8000-00805f9b34fb)

2.  接着，在ios设置中，搜索蓝牙的界面选中第一步android广播出来的外设名称，此时ios会去连接此android外设，将会进入BluetoothGattServerCallback的回调事件

3.  在BluetoothGattServerCallback的已连接事件中，先关闭GattServer。接着判断连接上的bt device（ios）是否已经绑定？ 

如果绑定：

>  mIphoneDevice.connectGatt(getApplicationContext(), false, mGattCallback);

如果未绑定：

```

try {

createBond(device.getClass(),device);

}catch (Exception e) {

e.printStackTrace();

}

//然后在绑定成功的广播接收者中，调用mIphoneDevice.connectGatt(getApplicationContext(), false, mGattCallback);

```

4. 此时android作为外设的使命完成了，就是为了拿到bt device并配对绑定。

5. android转换角色为中央设备，对上面的bt device展开搜索服务、订阅通知等

6. 接下来就是在ANCS的数据源，控制源，通知源中依据ANCS的协议进行显示、控制、数据解析等等了

具体的demo见 github：https://github.com/billzbh/Androdi-testANCSDemo

demo我把ios通知的内容直接发到android的通知栏里了，感觉也蛮有趣！！！
