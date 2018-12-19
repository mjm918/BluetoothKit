package com.stressoverflow.julfikar.blkit.Listener;

import android.bluetooth.BluetoothDevice;

public interface DiscoveryListener {
    void onDiscoveryIsStarted();
    void onDiscoveryIsFinished();
    void onDeviceIsFound(BluetoothDevice device);
    void onDeviceIsPaired(BluetoothDevice device);
    void onDeviceIsUnpaired(BluetoothDevice device);
    void onDiscoveryError(String error);
}
