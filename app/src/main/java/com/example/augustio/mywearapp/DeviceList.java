package com.example.augustio.mywearapp;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DeviceList extends Activity {

    public static final String TAG = "SensorList";

    private static final String[] UUIDS = {"6e400001-b5a3-f393-e0a9-e50e24dcca9e",
            "0000180D-0000-1000-8000-00805f9b34fb"};
    private static final long SCAN_PERIOD = 10000;

    private TextView mEmptyList;

    List<BluetoothDevice> mSensorList;
    private DeviceAdapter mDeviceAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mLEScanner;
    private ScanSettings settings;
    private List<ScanFilter> filters;
    private Handler mHandler;
    private boolean mScanning;
    private Button btnScanCancel;
    private ListView newSensorsListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_list);
        mEmptyList = (TextView) findViewById(R.id.empty);
        btnScanCancel = (Button) findViewById(R.id.btn_cancel);
        newSensorsListView = (ListView) findViewById(R.id.new_sensors);

        mHandler = new Handler();

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Ble not supported", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }


        mLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
        settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();
        filters = new ArrayList<>();
        for(int i = 0; i < UUIDS.length; i++) {
            ScanFilter ecgFilter = new ScanFilter.Builder()
                    .setServiceUuid(ParcelUuid.fromString(UUIDS[0].toString())).build();

            filters.add(ecgFilter);
        }

        btnScanCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!mScanning){
                    mEmptyList.setVisibility(View.VISIBLE);
                    scanLeDevice(true);
                }
                else finish();
            }
        });

        populateList();
    }

    private void populateList() {
        /* Initialize device list container */
        Log.d(TAG, "populateList");
        mSensorList = new ArrayList<>();
        mDeviceAdapter = new DeviceAdapter(this, mSensorList);
        newSensorsListView.setAdapter(mDeviceAdapter);
        newSensorsListView.setOnItemClickListener(mSensorClickListener);
        scanLeDevice(true);

    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    btnScanCancel.setText("Scan");
                    mLEScanner.stopScan(mLeScanCallback);

                    mEmptyList.setVisibility(View.GONE);

                }
            }, SCAN_PERIOD);

            mScanning = true;
            btnScanCancel.setText("Cancel");
            mLEScanner.startScan(filters, settings, mLeScanCallback);
        } else {
            mScanning = false;
            mLEScanner.stopScan(mLeScanCallback);
        }
    }

    private ScanCallback mLeScanCallback = new ScanCallback() {

        @Override
        public void onScanResult(int callbackType, final ScanResult result) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            addSensor(result.getDevice(), result.getRssi());
                        }
                    });

                }
            });
        }
    };

    private void addSensor(BluetoothDevice sensor, int rssi) {
        boolean sensorFound = false;

        for (BluetoothDevice listDev : mSensorList) {
            if (listDev.getAddress().equals(sensor.getAddress())) {
                sensorFound = true;
                break;
            }
        }
        if (!sensorFound) {
            mSensorList.add(sensor);
            mEmptyList.setVisibility(View.GONE);

            mDeviceAdapter.notifyDataSetChanged();
        }
    }

    private OnItemClickListener mSensorClickListener = new OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            String sensor = mSensorList.get(position).getAddress();
            mLEScanner.stopScan(mLeScanCallback);
            Intent result = new Intent();
            result.putExtra("SENSOR_LIST", sensor);
            setResult(Activity.RESULT_OK, result);
            finish();
        }
    };

    class DeviceAdapter extends BaseAdapter {
        Context context;
        List<BluetoothDevice> sensors;
        LayoutInflater inflater;

        public DeviceAdapter(Context context, List<BluetoothDevice> sensors) {
            this.context = context;
            inflater = LayoutInflater.from(context);
            this.sensors = sensors;
        }

        @Override
        public int getCount() {
            return sensors.size();
        }

        @Override
        public Object getItem(int position) {
            return sensors.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewGroup vg;

            if (convertView != null) {
                vg = (ViewGroup) convertView;
            } else {
                vg = (ViewGroup) inflater.inflate(R.layout.sensor_element, null);
            }

            BluetoothDevice sensor = sensors.get(position);
            final TextView tvname = ((TextView) vg.findViewById(R.id.name));

            tvname.setText(sensor.getName());
            return vg;
        }
    }
}