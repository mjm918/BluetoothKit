package com.stressoverflow.julfikar.blkit;


import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;

import com.stressoverflow.julfikar.blkit.Listener.BluetoothListener;
import com.stressoverflow.julfikar.blkit.Listener.DeviceListener;
import com.stressoverflow.julfikar.blkit.Listener.DiscoveryListener;
import com.stressoverflow.julfikar.blkit.Util.ThreadRunner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BluetoothKit {

    private static final int        REQ_ENABLE_BT           = 786;
    private static final String     REQ_UUID                = "06ba5b31-81c2-4c20-8072-d090f8c40b2b";


    private Activity                activity;
    private Context                 context;
    private UUID                    uuid;

    private BluetoothManager        bluetoothManager;
    private BluetoothAdapter        bluetoothAdapter;
    private BluetoothDevice         bluetoothDevice,
                                    bluetoothDevicePair;
    private BluetoothSocket         bluetoothSocket;

    private BufferedReader          bufferedReader;
    private OutputStream            outputStream;

    private BluetoothListener       bluetoothListener;
    private DeviceListener          deviceListener;
    private DiscoveryListener       discoveryListener;

    private boolean                 isConnected;
    private boolean                 isOnUi;

    private ArrayList
            <BluetoothDevice>       availableDevices;

    public BluetoothKit(Context context){
        InitBluetoothKit(context,UUID.fromString(REQ_UUID));
    }

    public BluetoothKit(Context context, UUID uuid){
        InitBluetoothKit(context,uuid);
    }

    private void InitBluetoothKit(Context context, UUID uuid){
        this.context                = context;
        this.bluetoothListener      = null;
        this.deviceListener         = null;
        this.discoveryListener      = null;
        this.isConnected            = false;
        this.isOnUi                 = false;
        this.uuid                   = uuid;

        availableDevices            = new ArrayList<BluetoothDevice>();
    }

    public void onStart(){
        if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2){
            bluetoothManager        = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
            if(bluetoothManager != null){
                bluetoothAdapter    = bluetoothManager.getAdapter();
            }
        }else{
            bluetoothAdapter        = BluetoothAdapter.getDefaultAdapter();
        }
        context.registerReceiver(bluetoothReceiver,new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
    }

    public void onStop(){
        context.unregisterReceiver(bluetoothReceiver);
    }

    public void disconnectBluetooth(){
        try{
            bluetoothSocket.close();
        }catch (final IOException e){
            if(deviceListener != null){
                ThreadRunner.handle(isOnUi, activity, new Runnable() {
                    @Override
                    public void run() {
                        deviceListener.onError(e.getMessage());
                    }
                });
            }
        }
    }

    public void send(String message, String charset){
        try{
            if(!TextUtils.isEmpty(charset)){
                outputStream.write(message.getBytes(charset));
            }else{
                outputStream.write(message.getBytes());
            }
        }catch (final IOException e){
            isConnected     = false;
            if(deviceListener != null){
                ThreadRunner.handle(isOnUi, activity, new Runnable() {
                    @Override
                    public void run() {
                        deviceListener.onDeviceIsDisconnected(bluetoothDevice,e.getMessage());
                    }
                });
            }
        }
    }

    public void send(String message){
        send(message,null);
    }

    public void pair(BluetoothDevice device){
        context.registerReceiver(pairReceiver,new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED));
        bluetoothDevicePair         = device;
        try{
            Method method           = device.getClass().getMethod("createBond", (Class[]) null);
            method.invoke(device, (Object[]) null);
        }catch (final Exception e){
            if(discoveryListener != null){
                ThreadRunner.handle(isOnUi, activity, new Runnable() {
                    @Override
                    public void run() {
                        discoveryListener.onDiscoveryError(e.getMessage());
                    }
                });
            }
        }
    }

    public void unpair(BluetoothDevice device){
        context.registerReceiver(pairReceiver,new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED));
        bluetoothDevicePair         = device;
        try{
            Method method           = device.getClass().getMethod("removeBond", (Class[]) null);
            method.invoke(device, (Object[]) null);
        }catch (final Exception e){
            if(discoveryListener != null){
                ThreadRunner.handle(isOnUi, activity, new Runnable() {
                    @Override
                    public void run() {
                        discoveryListener.onDiscoveryError(e.getMessage());
                    }
                });
            }
        }
    }

    final private BroadcastReceiver pairReceiver    = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action                           = intent.getAction();
            if(action != null){
                switch (action){
                    case BluetoothDevice.ACTION_BOND_STATE_CHANGED:
                        final int currentState       = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE,BluetoothDevice.ERROR);
                        final int previousState      = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE,BluetoothDevice.ERROR);
                        if(currentState == BluetoothDevice.BOND_BONDED && previousState == BluetoothDevice.BOND_BONDING){
                            context.unregisterReceiver(pairReceiver);
                            if(discoveryListener != null){
                                ThreadRunner.handle(isOnUi, activity, new Runnable() {
                                    @Override
                                    public void run() {
                                        discoveryListener.onDeviceIsPaired(bluetoothDevicePair);
                                    }
                                });
                            }
                        }else if (currentState == BluetoothDevice.BOND_NONE && previousState == BluetoothDevice.BOND_BONDED){
                            context.unregisterReceiver(pairReceiver);
                            if(discoveryListener != null){
                                ThreadRunner.handle(isOnUi, activity, new Runnable() {
                                    @Override
                                    public void run() {
                                        discoveryListener.onDeviceIsUnpaired(bluetoothDevicePair);
                                    }
                                });
                            }
                        }
                        break;
                }
            }
        }
    };

    private final BroadcastReceiver scanReceiver       = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action           = intent.getAction();
            if(action != null){
                switch (action){
                    case BluetoothAdapter.ACTION_STATE_CHANGED:
                        final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                        if (state == BluetoothAdapter.STATE_OFF) {
                            if (discoveryListener != null) {
                                ThreadRunner.handle(isOnUi, activity, new Runnable() {
                                    @Override
                                    public void run() {
                                        discoveryListener.onDiscoveryError("Bluetooth turned off");
                                    }
                                });
                            }
                        }
                        break;
                    case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                        if(discoveryListener != null){
                            ThreadRunner.handle(isOnUi, activity, new Runnable() {
                                @Override
                                public void run() {
                                    discoveryListener.onDiscoveryIsStarted();
                                }
                            });
                        }
                        break;
                    case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                        //context.unregisterReceiver(scanReceiver);
                        if(discoveryListener != null){
                            ThreadRunner.handle(isOnUi, activity, new Runnable() {
                                @Override
                                public void run() {
                                    discoveryListener.onDiscoveryIsFinished();
                                }
                            });
                        }
                        break;
                    case BluetoothDevice.ACTION_FOUND:
                        final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        if(deviceListener != null){
                            ThreadRunner.handle(isOnUi, activity, new Runnable() {
                                @Override
                                public void run() {
                                    if(device != null){
                                        if(!availableDevices.contains(device)){
                                            availableDevices.add(device);
                                        }
                                        discoveryListener.onDeviceIsFound(device);
                                    }
                                }
                            });
                        }
                        break;
                }
            }
        }
    };

    public void startScanning(){
        IntentFilter filter         = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);

        context.registerReceiver(scanReceiver, filter);
        bluetoothAdapter.startDiscovery();
    }

    public void stopScanning(){
        context.unregisterReceiver(scanReceiver);
        bluetoothAdapter.cancelDiscovery();
    }

    private final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            final String action     = intent.getAction();

            if(action != null && action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)){

                final int state     = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,BluetoothAdapter.ERROR);

                if(bluetoothListener != null){
                    ThreadRunner.handle(isOnUi, activity, new Runnable() {
                        @Override
                        public void run() {
                            switch (state){
                                case BluetoothAdapter.STATE_OFF:
                                    bluetoothListener.onBluetoothIsOff();
                                    break;
                                case BluetoothAdapter.STATE_ON:
                                    bluetoothListener.onBluetoothIsOn();
                                    break;
                                case BluetoothAdapter.STATE_TURNING_OFF:
                                    bluetoothListener.onBluetoothIsTurningOff();
                                    break;
                                case BluetoothAdapter.STATE_TURNING_ON:
                                    bluetoothListener.onBluetoothIsTurningOn();
                                    break;
                            }
                        }
                    });
                }
            }
        }
    };

    private class ConnectionTask extends Thread{
        ConnectionTask(BluetoothDevice device, boolean isInsecureConnection){
            BluetoothKit.this.bluetoothDevice           = device;
            try{
                if(isInsecureConnection){
                    BluetoothKit.this.bluetoothSocket   = device.createInsecureRfcommSocketToServiceRecord(uuid);
                }else{
                    BluetoothKit.this.bluetoothSocket   = device.createRfcommSocketToServiceRecord(uuid);
                }
            }catch (IOException ex){
                if(deviceListener != null){
                    deviceListener.onError(ex.getMessage());
                }
            }
        }

        @Override
        public void run() {

            bluetoothAdapter.cancelDiscovery();

            try{
                bluetoothSocket.connect();
                outputStream                            = bluetoothSocket.getOutputStream();
                bufferedReader                          = new BufferedReader(new InputStreamReader(bluetoothSocket.getInputStream()));

                isConnected                             = true;

                new ReceiverTask().start();

                if(deviceListener != null){
                    ThreadRunner.handle(isOnUi, activity, new Runnable() {
                        @Override
                        public void run() {
                            deviceListener.onDeviceIsConnected(bluetoothDevice);
                        }
                    });
                }

            }catch (final IOException ex){
                if(deviceListener != null){
                    ThreadRunner.handle(isOnUi, activity, new Runnable() {
                        @Override
                        public void run() {
                            deviceListener.onConnectionError(bluetoothDevice,ex.getMessage());
                        }
                    });
                }
                try{
                    bluetoothSocket.close();
                }catch (final IOException e){
                    if(deviceListener != null){
                        ThreadRunner.handle(isOnUi, activity, new Runnable() {
                            @Override
                            public void run() {
                                deviceListener.onError(e.getMessage());
                            }
                        });
                    }
                }
            }
        }
    }

    private class ReceiverTask extends Thread implements Runnable{
        @Override
        public void run() {
            String message;

            try{
                while ((message = bufferedReader.readLine()) != null){
                    if(deviceListener != null){
                        final String msg = message;
                        ThreadRunner.handle(isOnUi, activity, new Runnable() {
                            @Override
                            public void run() {
                                deviceListener.onMessage(msg);
                            }
                        });
                    }
                }
            }catch (final IOException ex){
                isConnected                             = false;
                if(deviceListener != null){
                    ThreadRunner.handle(isOnUi, activity, new Runnable() {
                        @Override
                        public void run() {
                            deviceListener.onDeviceIsDisconnected(bluetoothDevice,ex.getMessage());
                        }
                    });
                }
            }
        }
    }

    public void connectByAddress(String address, boolean insecureConnection){
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
        connectByDevice(device, insecureConnection);
    }

    public void connectByAddress(String address){
        connectByAddress(address,false);
    }

    public void connectByDevice(BluetoothDevice device, boolean insecureConnection){
        new ConnectionTask(device, insecureConnection).start();
    }

    public void connectByDevice(BluetoothDevice device){
        connectByDevice(device, false);
    }

    public void connectByName(String name, boolean insecureConnection){
        for(BluetoothDevice device : bluetoothAdapter.getBondedDevices()){
            if (device.getName().equals(name)){
                connectByDevice(device,insecureConnection);
                return;
            }
        }
    }

    public void connectByName(String name){
        connectByName(name,false);
    }

    public ArrayList<BluetoothDevice> getAvailableDevices() {
        return availableDevices;
    }

    public void showBluetoothEnableDialog(Activity activity){
        if(bluetoothAdapter != null){
            if (!bluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                activity.startActivityForResult(enableBtIntent, REQ_ENABLE_BT);
            }
        }
    }

    public void onActivityResult(int requestCode, final int resultCode){
        if(bluetoothListener!=null){
            if(requestCode==REQ_ENABLE_BT){
                ThreadRunner.handle(isOnUi, activity, new Runnable() {
                    @Override
                    public void run() {
                        if(resultCode==Activity.RESULT_CANCELED){
                            bluetoothListener.onBluetoothActivitionIsRejected();
                        }
                    }
                });
            }
        }
    }

    public void enableBluetooth(){
        if(bluetoothAdapter != null){
            if(!bluetoothAdapter.isEnabled()){
                bluetoothAdapter.enable();
            }
        }
    }

    public void disableBluetooth(){
        if(bluetoothAdapter != null){
            if(bluetoothAdapter.isEnabled()){
                bluetoothAdapter.disable();
            }
        }
    }

    public boolean isEnabled(){
        if(bluetoothAdapter != null){
            return bluetoothAdapter.isEnabled();
        }
        return false;
    }

    public void onUiThread(Activity activity){
        this.activity               = activity;
        this.isOnUi                 = true;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public List<BluetoothDevice>getPairedDevices(){
        return new ArrayList<>(bluetoothAdapter.getBondedDevices());
    }

    public BluetoothManager getBluetoothManager() {
        return bluetoothManager;
    }

    public BluetoothAdapter getBluetoothAdapter() {
        return bluetoothAdapter;
    }

    public BluetoothSocket getBluetoothSocket() {
        return bluetoothSocket;
    }

    public void setBluetoothListener(BluetoothListener listener){
        this.bluetoothListener      = listener;
    }

    public void removeBluetoothListener(){
        this.bluetoothListener      = null;
    }

    public void setDeviceListener(DeviceListener listener){
        this.deviceListener         = listener;
    }

    public void removeDeviceListener(){
        this.deviceListener         = null;
    }

    public void setDiscoveryListener(DiscoveryListener listener){
        this.discoveryListener      = listener;
    }

    public void removeDiscoveryListener(){
        this.discoveryListener      = null;
    }
}