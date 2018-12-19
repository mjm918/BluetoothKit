package com.stressoverflow.julfikar.bl;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.stressoverflow.julfikar.blkit.BluetoothKit;
import com.stressoverflow.julfikar.blkit.Listener.BluetoothListener;
import com.stressoverflow.julfikar.blkit.Listener.DeviceListener;
import com.stressoverflow.julfikar.blkit.Listener.DiscoveryListener;

import java.lang.reflect.Array;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    private TextView tv;
    private BluetoothKit bluetoothKit;

    int REQ_ENABLE_BT   = 786;
    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkLocationPermission();

        tv              = this.findViewById(R.id.tv);

        bluetoothKit    = new BluetoothKit(this);

        bluetoothKit.setBluetoothListener(new BluetoothListener() {
            @Override
            public void onBluetoothIsTurningOn() {
                tv.setText("BL is turning on");
            }

            @Override
            public void onBluetoothIsOn() {
                tv.setText("BL is on");
            }

            @Override
            public void onBluetoothIsTurningOff() {
                tv.setText("BL is turning off");
            }

            @Override
            public void onBluetoothIsOff() {
                tv.setText("BL is off");
            }

            @Override
            public void onBluetoothActivitionIsRejected() {
                tv.setText("BL activition is rejected by user");
                bluetoothKit.showBluetoothEnableDialog(MainActivity.this);
                bluetoothKit.onActivityResult(REQ_ENABLE_BT,0);
            }
        });

        bluetoothKit.setDiscoveryListener(new DiscoveryListener(){
            @Override
            public void onDiscoveryIsStarted() {
                tv.setText("Discovery started");
            }

            @Override
            public void onDiscoveryIsFinished() {
                tv.setText("Discovery finished");
                StringBuilder names = new StringBuilder();
                int size = bluetoothKit.getAvailableDevices().size();
                Log.d("Size of devices ",String.valueOf(size));
                for(BluetoothDevice device : bluetoothKit.getAvailableDevices()){
                    names.append("\n").append(device.getName());
                }
                tv.setText(names.toString());
            }

            @Override
            public void onDeviceIsFound(BluetoothDevice device) {
                tv.setText("Discovery device found "+device.getName());
            }

            @Override
            public void onDeviceIsPaired(BluetoothDevice device) {
                tv.setText("Device is paired"+device.getName());
            }

            @Override
            public void onDeviceIsUnpaired(BluetoothDevice device) {
                tv.setText("Device is unpaired"+device.getName());
            }

            @Override
            public void onDiscoveryError(String error) {
                tv.setText("Discovery error "+error);
            }
        });

        bluetoothKit.setDeviceListener(new DeviceListener(){

            @Override
            public void onDeviceIsConnected(BluetoothDevice device) {
                tv.setText("Device is connected "+device.getName());
            }

            @Override
            public void onDeviceIsDisconnected(BluetoothDevice device, String message) {
                tv.setText("Device is disconnected"+device.getName());
            }

            @Override
            public void onMessage(String message) {
                tv.setText("Got message "+message);
            }

            @Override
            public void onError(String error) {
                tv.setText("Device error"+error);
            }

            @Override
            public void onConnectionError(BluetoothDevice device, String error) {
                tv.setText("Device connection error "+error);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        bluetoothKit.onStart();
        bluetoothKit.enableBluetooth();
        bluetoothKit.startScanning();
    }

    @Override
    protected void onResume() {
        super.onResume();
        bluetoothKit.onStart();
        bluetoothKit.startScanning();
    }

    @Override
    protected void onStop() {
        super.onStop();
        bluetoothKit.onStop();
        bluetoothKit.stopScanning();
    }

    public boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                new AlertDialog.Builder(this)
                        .setTitle("We want location")
                        .setMessage("Give me your location")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Prompt the user once explanation has been shown
                                ActivityCompat.requestPermissions(MainActivity.this,
                                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                        MY_PERMISSIONS_REQUEST_LOCATION);
                            }
                        })
                        .create()
                        .show();


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // location-related task you need to do.
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {
                    }

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.

                }
                return;
            }

        }
    }
}
