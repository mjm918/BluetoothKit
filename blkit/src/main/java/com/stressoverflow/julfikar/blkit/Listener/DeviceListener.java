package com.stressoverflow.julfikar.blkit.Listener;

import android.bluetooth.BluetoothDevice;

public interface DeviceListener {
    void onDeviceIsConnected(BluetoothDevice device);
    void onDeviceIsDisconnected(BluetoothDevice device, String message);
    void onMessage(String message);
    void onError(String error);
    void onConnectionError(BluetoothDevice device,String error);
}
