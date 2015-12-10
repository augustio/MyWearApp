package com.example.augustio.mywearapp;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.List;
import java.util.UUID;

public class MainActivity extends WearableActivity {

    private final static String TAG = "MainActiviy";

    private final static int REQUEST_ENABLE_BT = 1;
    private final static int REQUEST_SELECT_DEVICE = 2;
    private final static int CONNECTED = 3;
    private final static int DISCONNECTED = 4;

    private final static UUID CCCD_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    private final static UUID UART_SERVICE_UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
    private final static UUID TX_CHAR_UUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");
    private final static UUID RX_CHAR_UUID = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e");
    private final static UUID HR_SERVICE_UUID = UUID.fromString("0000180D-0000-1000-8000-00805f9b34fb");
    private static final UUID HRM_CHARACTERISTIC_UUID = UUID.fromString("00002A37-0000-1000-8000-00805f9b34fb");
    private static final UUID ECG_SENSOR_LOCATION_CHARACTERISTIC_UUID = UUID.fromString("00002A38-0000-1000-8000-00805f9b34fb");


    private TextView tv, dataView;
    private Button connectButton;
    private BluetoothAdapter mBluetoothAdapter;
    private String mSelectedSensor;
    private int mConnectionState;

    private BluetoothManager mBluetoothManager;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothGattCharacteristic mHRLocationCharacteristic;
    private BluetoothGattCharacteristic mRXCharacteristic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setAmbientEnabled();

        setContentView(R.layout.activity_main);
        tv = (TextView) findViewById(R.id.device_name);
        tv.setText("No Device");
        dataView = (TextView)findViewById(R.id.data_view);
        connectButton = (Button) findViewById(R.id.btn_connect);

        initialize();
        mBluetoothGatt = null;
        mConnectionState= DISCONNECTED;
        if (mBluetoothAdapter == null) {
            finish();
            return;
        }
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        } else {
            Intent newIntent = new Intent(MainActivity.this, DeviceList.class);
            startActivityForResult(newIntent, REQUEST_SELECT_DEVICE);
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

    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);
        disconnect(mSelectedSensor);
    }

    @Override
    public void onExitAmbient() {
        super.onExitAmbient();
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        } else {
            Intent newIntent = new Intent(MainActivity.this, DeviceList.class);
            startActivityForResult(newIntent, REQUEST_SELECT_DEVICE);
        }
    }

    public void connect(String sensorAddress) {

        if (mBluetoothAdapter == null) {
            return;
        }
        mBluetoothGatt = mBluetoothAdapter.getRemoteDevice(sensorAddress)
                .connectGatt(getApplicationContext(), false, mGattCallback);
    }


    private void disconnect(String sensorAddress){
        if (mBluetoothAdapter == null) {
            return;
        }
        if(mBluetoothGatt != null){
           mBluetoothGatt.disconnect();
        }
    }

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String sensorAddress = gatt.getDevice().getAddress();
            String sensorName = gatt.getDevice().getName();

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(TAG, "Connected to " + sensorAddress + ": " + sensorName);
                mConnectionState = CONNECTED;
                runOnUiThread(new Runnable(){
                    public void run(){
                        tv.setText(mBluetoothGatt.getDevice().getName());
                        connectButton.setText("Disconnect");
                    }
                });
                Log.d(TAG, "Attempting to start service discovery:" +
                        mBluetoothGatt.discoverServices());
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d(TAG, "Disconnected from " + sensorAddress + ": " + sensorName);
                mConnectionState = CONNECTED;
                close();
                runOnUiThread(new Runnable(){
                    public void run(){
                        tv.setText("No Device");
                        dataView.setText("");
                        connectButton.setText("Connect");
                    }
                });
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "mBluetoothGatt = " + mBluetoothGatt);
                List<BluetoothGattService> services = gatt.getServices();
                for (BluetoothGattService service : services) {
                    if (service.getUuid().equals(UART_SERVICE_UUID)) {
                        mRXCharacteristic = service.getCharacteristic(RX_CHAR_UUID);
                    } else if (service.getUuid().equals(HR_SERVICE_UUID)) {
                        mHRLocationCharacteristic = service.getCharacteristic(ECG_SENSOR_LOCATION_CHARACTERISTIC_UUID);
                        //Read sensor location
                        readECGSensorLocation();
                    }
                }
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (characteristic.getUuid().equals(ECG_SENSOR_LOCATION_CHARACTERISTIC_UUID)) {
                    enableRXNotification();
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            final BluetoothGattCharacteristic characteristic) {
            if(characteristic.getUuid().equals(RX_CHAR_UUID)) {
                runOnUiThread(new Runnable(){
                    public void run(){
                        dataView.setText(""+(characteristic.getValue()[0]));
                        Log.w(TAG, "RX: " + characteristic.getValue()[0]);
                    }
                });
            }
        }
    };

    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    private void readECGSensorLocation() {
        if (mHRLocationCharacteristic != null) {
            mBluetoothGatt.readCharacteristic(mHRLocationCharacteristic);
        }
    }

    public void enableRXNotification()
    {
        if (mRXCharacteristic != null && mConnectionState == CONNECTED) {
            mBluetoothGatt.setCharacteristicNotification(mRXCharacteristic, true);
            BluetoothGattDescriptor descriptor = mRXCharacteristic.getDescriptor(CCCD_UUID);
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mBluetoothGatt.writeDescriptor(descriptor);
        }else if(mRXCharacteristic == null){
            Log.e(TAG, "Charateristic not found!");
        }
    }

    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        Log.d(TAG, "mBluetoothGatt closed");
        mBluetoothGatt = null;
    }

}

