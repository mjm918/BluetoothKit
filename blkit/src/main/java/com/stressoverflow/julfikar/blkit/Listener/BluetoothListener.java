package com.stressoverflow.julfikar.blkit.Listener;

public interface BluetoothListener {
    void onBluetoothIsTurningOn();
    void onBluetoothIsOn();
    void onBluetoothIsTurningOff();
    void onBluetoothIsOff();
    void onBluetoothActivitionIsRejected();
}
