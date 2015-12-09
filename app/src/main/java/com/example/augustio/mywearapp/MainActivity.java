package com.example.augustio.mywearapp;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.os.Bundle;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {

    private final static int REQUEST_ENABLE_BT = 1;
    private final static int REQUEST_SELECT_DEVICE = 2;
    private final static int CONNECTED = 3;
    private final static int DISCONNECTED = 4;

    private TextView tv;
    private Button connectButton;
    private BluetoothAdapter mBluetoothAdapter;
    private String mSelectedSensor;
    private BluetoothGatt mConnectedSensor;
    private int mConnectionState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tv = (TextView) findViewById(R.id.device_name);
        tv.setText("No Device");
        connectButton = (Button) findViewById(R.id.btn_connect);

        mConnectedSensor = null;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mConnectionState= DISCONNECTED;
        if (mBluetoothAdapter == null) {
            finish();
            return;
        }
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mBluetoothAdapter.isEnabled()) {
                    Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
                } else {
                    if (connectButton.getText().toString().equalsIgnoreCase("Connect")) {
                        //Connect button pressed, open SensorList class, with popup window that scan for devices
                        Intent newIntent = new Intent(MainActivity.this, DeviceList.class);
                        startActivityForResult(newIntent, REQUEST_SELECT_DEVICE);
                    } else if (connectButton.getText().toString().equalsIgnoreCase("Disconnect")) {
                        disconnect(mSelectedSensor);
                    }
                }
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {

            case REQUEST_SELECT_DEVICE:
                //When the DeviceListActivity return, with the selected device address
                if (resultCode == Activity.RESULT_OK && data != null) {
                    mSelectedSensor = data.getStringExtra("SENSOR_LIST");
                    connect(mSelectedSensor);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode != Activity.RESULT_OK) {
                    finish();
                }
                break;
            default:
                break;
        }
    }

    public void connect(String sensorAddress) {

        if (mBluetoothAdapter == null) {
            return;
        }
        mBluetoothAdapter.getRemoteDevice(sensorAddress)
                .connectGatt(getApplicationContext(), false, mGattCallback);
    }


    private void disconnect(String sensorAddress){
        if (mBluetoothAdapter == null) {
            return;
        }
        if(mConnectedSensor != null){
           mConnectedSensor.disconnect();
        }
    }

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String sensorAddress = gatt.getDevice().getAddress();
            String sensorName = gatt.getDevice().getName();

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d("MainActivity", "Connected to " + sensorAddress + ": " + sensorName);
                mConnectedSensor = gatt;
                runOnUiThread(new Runnable(){
                    public void run(){
                        tv.setText(mConnectedSensor.getDevice().getName());
                        connectButton.setText("Disconnect");
                    }
                });
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d("MainActiviy", "Disconnected from " + sensorAddress + ": " + sensorName);
                mConnectedSensor = null;
                runOnUiThread(new Runnable(){
                    public void run(){
                        tv.setText("No Device");
                        connectButton.setText("Connect");
                    }
                });
            }
        }
    };
}

