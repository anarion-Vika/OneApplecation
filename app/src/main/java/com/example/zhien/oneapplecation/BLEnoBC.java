package com.example.zhien.oneapplecation;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.menu.MenuView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.util.ArrayList;

import static android.content.Context.BLUETOOTH_SERVICE;
import static com.example.zhien.oneapplecation.BluetoothParametrs.ADD_H_CHARACTER_REQUEST_UUID;
import static com.example.zhien.oneapplecation.BluetoothParametrs.ADD_H_CHARACTER_RESPONCE_UUID;
import static com.example.zhien.oneapplecation.BluetoothParametrs.ADD_H_DESCRIPTOR_UUID;
import static com.example.zhien.oneapplecation.BluetoothParametrs.ADD_H_SERVICE_REQUEST_UUID;
import static com.example.zhien.oneapplecation.BluetoothParametrs.ADD_H_SERVICE_RESPONSE_UUID;

public class BLEnoBC extends Fragment implements BluetoothAdapter.LeScanCallback {

    private static final String TAG = BLEnoBC.class.getSimpleName();

    private static final String DEVICE_NAME = "ADD@H_Chat";

    private static final int PERMISSION_REQUEST_FINE_LOCATION = 1;

    private BluetoothAdapter mBluetoothAdapter;
    private SparseArray<BluetoothDevice> mDevice;
    private BluetoothGatt mConnectedGatt;
    private TextView mDataNowValue;
    private TextView mMaxValue;
    private TextView mAverageValue;
    private TextView mDispersionValue;


    private Button btnDisconnect;
    private Thread thread;
    private boolean plotData = true;
    MenuView.ItemView itemView;
    private LineChart lineChart;
    private MenuItem mItemScan;
    private MenuItem mItemDisconnect;

    public static ArrayList<Float> mAccelerationVectors = new ArrayList<>();
    public static ArrayList<Double> mAccelerationVectorsSecond = new ArrayList<>();
    public static ArrayList<Double> mAccelerationVectorsMinute = new ArrayList<>();
    public static ArrayList<Double> XAxis = new ArrayList<>();
    public static ArrayList<Double> YAxis = new ArrayList<>();
    public static ArrayList<Double> ZAxis = new ArrayList<>();

    public static BLEnoBC newInstance() {
        Bundle args = new Bundle();
        BLEnoBC blEnoBC = new BLEnoBC();
        blEnoBC.setArguments(args);
        return blEnoBC;
    }


    boolean isBleSupported(Context context) {
        return BluetoothAdapter.getDefaultAdapter() != null
                && context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_blenobc, container, false);
        mDataNowValue = view.findViewById(R.id.tvDataNowValue);
        mMaxValue = view.findViewById(R.id.tvMaxValue);
        mAverageValue = view.findViewById(R.id.tvAverageValue);
        mDispersionValue = view.findViewById(R.id.tvDisperionValue);


        lineChart = view.findViewById(R.id.linearChartGraph1);
        BluetoothManager manager = (BluetoothManager) getActivity().getSystemService(BLUETOOTH_SERVICE);
        mBluetoothAdapter = manager.getAdapter();
        mDevice = new SparseArray<BluetoothDevice>();
        Toolbar toolbar = view.findViewById(R.id.toolbar);
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
        //  setActionBarTitle("Acceleration");
        ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        i = 0;
        setRetainInstance(true);
        setHasOptionsMenu(true);
        requestLocationPermissionIfNeeded();
        LineData data = new LineData();
        lineChart.setData(data);
        feedMultiple();
        itemView = view.findViewById(R.id.action_scan);
        return view;
    }


    private void addEntry(float vector) {
        LineData data = lineChart.getData();
        if (data != null) {
            ILineDataSet set = data.getDataSetByIndex(0);
            if (set == null) {
                set = createSet();
                data.addDataSet(set);
            }
            data.addEntry(new Entry(set.getEntryCount(), vector), 0);
            data.notifyDataChanged();
            lineChart.notifyDataSetChanged();
            lineChart.setVisibleXRangeMaximum(60);
            lineChart.moveViewToX(data.getEntryCount());
        }
    }

    private LineDataSet createSet() {
        LineDataSet set = new LineDataSet(null, "Dynamic Data");
        set.setLineWidth(3f);
        set.setColor(Color.MAGENTA);
        set.setHighlightEnabled(false);
        set.setDrawValues(false);
        set.setDrawCircles(false);
        set.setCubicIntensity(0.2f);
        return set;
    }

    private void feedMultiple() {
        if (thread != null) {
            thread.interrupt();
        }
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    plotData = true;
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        });
        thread.start();
    }

    public void setActionBarTitle(String title) {
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(title);
    }

    // region Permissions
    @TargetApi(Build.VERSION_CODES.M)
    private void requestLocationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M Permission check 
            if (getActivity().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle("This app needs location access");
                builder.setMessage("Please grant location access so this app can scan for Bluetooth peripherals");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(dialog -> requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_FINE_LOCATION));
                builder.show();
            }
        }
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        setRetainInstance(true);
        setHasOptionsMenu(true);
        super.onCreate(savedInstanceState);

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.main, menu);
        mItemScan = menu.findItem(R.id.action_scan);
        mItemDisconnect = menu.findItem(R.id.action_disconenct);
        mItemDisconnect.setVisible(false);
        for (int i = 0; i < mDevice.size(); i++) {
            BluetoothDevice device = mDevice.valueAt(i);
            menu.add(0, mDevice.keyAt(i), 0, device.getName());
        }

        super.onCreateOptionsMenu(menu, inflater);

    }

    public void onBackPressed() {
        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.popBackStack();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_disconenct:
                mConnectedGatt.disconnect();
                mConnectedGatt = null;
                mItemDisconnect.setVisible(false);
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.action_scan:
                mDevice.clear();
                startScan();
                return true;
            default:
                //Obtain the discovered device to connect with
                BluetoothDevice device = mDevice.get(item.getItemId());
                Log.i(TAG, "connection to " + device.getName());
                /*
                 * Make a connection with the  device using the special LE  - specific
                 * connectionGatt() method, passing in a callback for GATT events
                 */
                mConnectedGatt = device.connectGatt(getActivity(), true, mGattCallBack);
                //Display progress UI
                mHandler.sendMessage(Message.obtain(null, MSG_PROGRESS, "Connecting to " + device.getName() + "...."));

                mItemDisconnect.setVisible(true);
                return super.onOptionsItemSelected(item);
        }
    }

    private void clearDisplayValues() {
        mDataNowValue.setText("-------");
        mMaxValue.setText("-------");
        mAverageValue.setText("-------");
        mDispersionValue.setText("-------");
    }

    private Runnable mStopRunnable = () -> stopScan();
    private Runnable mStartRunnable = () -> startScan();


    private void startScan() {
        mBluetoothAdapter.startLeScan(this);
        mHandler.postDelayed(mStopRunnable, 2500);
    }

    private void stopScan() {
        mBluetoothAdapter.stopLeScan(this);
    }

    //BluetoothAdapter.LeScanCallback

    @Override
    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecor) {
        Log.d(TAG, "New LE Device = " + device.getName() + " @ " + rssi);
        if (DEVICE_NAME.equals(device.getName())) {
            mDevice.put(device.hashCode(), device);
            //update the overflow menu
            getActivity().invalidateOptionsMenu();
        }
    }

    /*
     * in this call back we've created a bit of a state machine to enforce that only
     * one characteristic be read or written at a time until all of our sensors
     * are enabled and we are registered to get a notifications
     */
    private BluetoothGattCallback mGattCallBack = new BluetoothGattCallback() {
        private int mState = 0;

        private void reset() {
            mState = 0;
        }

        private void advance() {
            mState++;
        }

        /*
         * Send an enable command to each sensor by writing a configuration
         * characteristic. This is specific to the SensorTag to keep power
         * low by disabling sensors you aren't using
         */
        private void enableNextSensor(BluetoothGatt gatt) {
            BluetoothGattCharacteristic characteristic = null;
            switch (mState) {
                case 0:
                    Log.d(TAG, "Enabling child call ");
                    characteristic = gatt.getService(ADD_H_SERVICE_REQUEST_UUID)
                            .getCharacteristic(ADD_H_CHARACTER_REQUEST_UUID);
                    characteristic.setValue((new byte[]{0x01, 0x03, 0x01, 0x01, (byte) 0xFE}));
                    break;
                case 1:
                    Log.d(TAG, "Enabling child call ");
                    characteristic = gatt.getService(ADD_H_SERVICE_RESPONSE_UUID)
                            .getCharacteristic(ADD_H_CHARACTER_RESPONCE_UUID);
                    characteristic.setValue((new byte[]{0x01, 0x03, 0x01, 0x01, (byte) 0xFE}));
                    break;
                default:
                    mHandler.sendEmptyMessage(MSG_DISMISS);
                    Log.d(TAG, "All sensor Enable");
            }
            gatt.writeCharacteristic(characteristic);
        }

        private void readNextSensor(BluetoothGatt gatt) {
            BluetoothGattCharacteristic characteristic;
            switch (mState) {
                case 0:
                    Log.d(TAG, "reading HACHILD");
                    characteristic = gatt.getService(ADD_H_SERVICE_RESPONSE_UUID)
                            .getCharacteristic(ADD_H_CHARACTER_RESPONCE_UUID);
                    break;
                case 1:
                    Log.d(TAG, "reading HACHILD");
                    characteristic = gatt.getService(ADD_H_SERVICE_REQUEST_UUID)
                            .getCharacteristic(ADD_H_CHARACTER_REQUEST_UUID);
                    break;
                default:
                    mHandler.sendEmptyMessage(MSG_DISMISS);
                    Log.d(TAG, "Sensor Enabled");
                    return;
            }
            gatt.readCharacteristic(characteristic);
        }

        /*
         * Enable notifications of changes on the data characteristic for each sensor
         * by writing the ENABLE_NOTIFICATION_VALUE flag to that characteristic's
         * configuration descriptor
         */
        private void setNotifyNextSensor(BluetoothGatt gatt) {
            BluetoothGattCharacteristic characteristic;
            switch (mState) {
                case 0:
                    Log.d(TAG, "Set notify HAHCILD call");
                    characteristic = gatt.getService(ADD_H_SERVICE_RESPONSE_UUID)
                            .getCharacteristic(ADD_H_CHARACTER_RESPONCE_UUID);
                    break;
                case 1:
                    Log.d(TAG, "Set notify HAHCILD call");
                    characteristic = gatt.getService(ADD_H_SERVICE_REQUEST_UUID)
                            .getCharacteristic(ADD_H_CHARACTER_REQUEST_UUID);
                    break;
                default:
                    mHandler.sendEmptyMessage(MSG_DISMISS);
                    Log.d(TAG, "Sensor Enabled");
                    return;
            }
            //Enable local notifications
            gatt.setCharacteristicNotification(characteristic, true);
            //Enable remote notifications
            BluetoothGattDescriptor desc = characteristic.getDescriptor(ADD_H_DESCRIPTOR_UUID);
            desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            gatt.writeDescriptor(desc);
        }

        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.d(TAG, "Connection State Change: " + status + "->" + connectionState(newState));
            if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                /*
                 *Once successfully connected, we must next discover all the services on the
                 *device before we can read and write their characteristics
                 */
                gatt.discoverServices();
                mHandler.sendMessage(Message.obtain(null, MSG_PROGRESS, "Discovering service"));
            } else if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_DISCONNECTED) {
                /*
                If at any point we disconnect, send a message to clear the weather values
                * out of the UI
                */
                mHandler.sendEmptyMessage(MSG_CLEAR);
            } else if (status != BluetoothGatt.GATT_SUCCESS) {
                /*
                 *if there is a failure at any stage, simply disconnect
                 */
                gatt.disconnect();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.d(TAG, "Service discovered: " + status);
            mHandler.sendMessage(Message.obtain(null, MSG_PROGRESS, "Enabling Sensors ... "));
            /*
             * With services discovered, we are going to reset out state machine and start
             *working through the sensors we need to enable
             */
            reset();
            enableNextSensor(gatt);
        }

        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            // For each read, pass the data up to the UI thread to update the display
            if (ADD_H_CHARACTER_RESPONCE_UUID.equals(characteristic.getUuid())) {
                mHandler.sendMessage(Message.obtain(null, MSG_CHAR, characteristic));
            }
            //After reading the initial value, next we enable notifications
            if (ADD_H_CHARACTER_REQUEST_UUID.equals(characteristic.getUuid())) {
                mHandler.sendMessage(Message.obtain(null, MSG_REQ, characteristic));
            }
            //After reading the initial value, next we enable notifications
            setNotifyNextSensor(gatt);
        }

        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            //After writing the enable flag, next we read the initial value
            readNextSensor(gatt);
        }

        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            /*
             * After notification are enabled, all updates from device on characteristic
             * value changes will be posted here. Similar to read, we hand these up to the
             * UI thread to update the display.
             */
            if (ADD_H_CHARACTER_RESPONCE_UUID.equals(characteristic.getUuid())) {
                mHandler.sendMessage(Message.obtain(null, MSG_CHAR, characteristic));
            }
            if (ADD_H_CHARACTER_REQUEST_UUID.equals(characteristic.getUuid())) {
                mHandler.sendMessage(Message.obtain(null, MSG_REQ, characteristic));
            }
        }

        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            //Once notifications are enabled, we move to the next sensor and start over with enable
            advance();
            enableNextSensor(gatt);
        }

        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            Log.d(TAG, "Remote RSSI: " + rssi);
        }

        private String connectionState(int status) {
            switch (status) {
                case BluetoothProfile.STATE_CONNECTED:
                    return "Connceted";
                case BluetoothProfile.STATE_DISCONNECTED:
                    return "Disconnected";
                case BluetoothProfile.STATE_CONNECTING:
                    return "Connecting";
                case BluetoothProfile.STATE_DISCONNECTING:
                    return "Disconnecting";
                default:
                    return String.valueOf(status);
            }
        }
    };


    @Override
    public void onResume() {
        super.onResume();

        if ((mBluetoothAdapter == null) || (!mBluetoothAdapter.isEnabled())) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBtIntent);
            getActivity().finish();
            return;
        }
        if (!getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(getActivity(), "No LE Support", Toast.LENGTH_LONG).show();
            getActivity().finish();
            return;
        }
        clearDisplayValues();
        Log.d(TAG, " OnResume");
    }

    @Override
    public void onPause() {
        super.onPause();
        //Cancel any scans in progress
        mHandler.removeCallbacks(mStopRunnable);
        mHandler.removeCallbacks(mStartRunnable);
        mBluetoothAdapter.stopLeScan(this);
        Log.d(TAG, " OnPause");
    }

    @Override
    public void onStop() {
        super.onStop();
        //Disconnect from ant activity tag connection
        if (mConnectedGatt != null) {
            mConnectedGatt.disconnect();
            mConnectedGatt = null;
        }
        Log.d(TAG, " OnStrop");
    }

    /*
     * We have a Handler to process event results on the main thread
     */
    private static final int MSG_CHAR = 102;
    private static final int MSG_REQ = 103;
    private static final int MSG_PROGRESS = 201;
    private static final int MSG_DISMISS = 202;
    private static final int MSG_CLEAR = 301;
    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            BluetoothGattCharacteristic characteristic;
            switch (msg.what) {
                case MSG_CHAR:
                    characteristic = (BluetoothGattCharacteristic) msg.obj;
                    if (characteristic.getValue() == null) {
                        Log.d(TAG, "Error obtaining HACHILD_read value");
                        return;
                    }
                    updateHACHILD_Read_Values(characteristic);
                    break;
                case MSG_REQ:
                    characteristic = (BluetoothGattCharacteristic) msg.obj;
                    if (characteristic.getValue() == null) {
                        Log.d(TAG, "Error obtaining HACHILD_read value");
                        return;
                    }
                    updateHACHILD_Read_Values(characteristic);
                    break;
                case MSG_PROGRESS:
                    break;
                case MSG_DISMISS:
                    break;
                case MSG_CLEAR:
                    clearDisplayValues();
                    break;
            }
        }
    };

    /*Method to extract sensor data and update the UI*/
    private double x_filter = 0;
    private double y_filter = 0;
    private double z_filter = 0;

    private int x = 0;
    private int y = 0;
    private int z = 0;

    private float vectorAcceleration = 0;
    int xor = 0xFF;
    int i = 0;
    final float gConst = (float) 0.018;

    private void updateHACHILD_Read_Values(BluetoothGattCharacteristic characteristic) {
        if (i > 10) {
            Log.d(TAG, "/////////////" + i + " координаты ////////////////////////////////////////////////////");
          //  Log.d(TAG, "---------------------------------------------------------------------");
           // Log.d(TAG, "hexToString =                  " + Utils.hexToString(characteristic.getValue()));
            x = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 3);
            y = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 4);
            z = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 5);
            if (x > 126) {
                x = x - 1;
                x = x ^ xor;
            }
            if (y > 126) {
                y = y - 1;
                y = y ^ xor;
            }
            if (z > 126) {
                z = z - 1;
                z = z ^ xor;
            }
            //Log.d(TAG, "========================================================-");
           // Log.d(TAG, "и координаты = " + x + "  " + y + "  " + z);

// transef to milg (9.8)
            x_filter = x * gConst;
            y_filter = y * gConst;
            z_filter = z * gConst;

            XAxis.add(x_filter);
            YAxis.add(y_filter);
            ZAxis.add(z_filter);
            if ((x_filter * x_filter != 3.2399997210502685E-4) && (y_filter * y_filter != 3.2399997210502685E-4) && (z_filter * z_filter != 3.2399997210502685E-4)) {
                vectorAcceleration = (float) Math.sqrt(x_filter * x_filter + y_filter * y_filter + z_filter * z_filter);
              //  Log.d(TAG, "x*x = " + x_filter * x_filter + " y*y  = " + y_filter * y_filter + " z*z = " + z_filter * z_filter);
                mAccelerationVectors.add(vectorAcceleration);
                onEnteringCounter(vectorAcceleration);
          //      onDataForGraphsInSecond(vectorAcceleration);
                Log.d(TAG, " вектор ускорения = " + vectorAcceleration);
                mDataNowValue.setText(format(vectorAcceleration));
                Log.d(TAG, "---------------------------------------------------------------------");

                if (plotData) {
                    addEntry(vectorAcceleration);
                    plotData = false;
                }
            }
        }
        i++;
    }

    int enteringSecond = 0;
    double vectorAccelerationMiddleSecond = 0;

    public void onDataForGraphsInSecond(double vectorAcceleration) {
        enteringSecond++;
        if (enteringSecond % 10 != 0) {
            vectorAccelerationMiddleSecond = vectorAccelerationMiddleSecond + vectorAcceleration;
        } else {
            vectorAccelerationMiddleSecond = vectorAccelerationMiddleSecond / 10;
            mAccelerationVectorsSecond.add(vectorAccelerationMiddleSecond);
            onDataForGraphsInMinute(vectorAccelerationMiddleSecond);
          //  Log.d(TAG, " среднее значение в секунду " + vectorAccelerationMiddleSecond);
            vectorAccelerationMiddleSecond = 0;
            enteringSecond = 0;
        }
    }

    int enteringMinute = 0;
    double vectorAccelerationMiddleMinute = 0;

    public void onDataForGraphsInMinute(double vectorAccelerationMidleSecond) {
        enteringMinute++;
        if (enteringMinute % 60 != 0) {
            vectorAccelerationMiddleMinute = vectorAccelerationMiddleMinute + vectorAccelerationMidleSecond;
        } else {
            vectorAccelerationMiddleMinute = vectorAccelerationMiddleMinute / 60;
            mAccelerationVectorsMinute.add(vectorAccelerationMiddleMinute);
          //  Log.d(TAG, " среднее значение в минуту " + vectorAccelerationMiddleMinute);
            vectorAccelerationMiddleMinute = 0;
            enteringMinute = 0;
        }
    }

    private int enteringCounter = 0;
    // %600 = 10(times)*60(sec)
    private ArrayList<Float> vectorList = new ArrayList<>();
    private void onEnteringCounter(float vector) {
        vectorList.add(vector);
        enteringCounter++;
        if (enteringCounter % 100 == 0) {
            onCalculateAverage(vectorList);
            onCalculateMax(vectorList);
            onCalculateDispersion(vectorList);
            vectorList.clear();
            enteringCounter = 0;
        }
    }

    private void onCalculateMax(ArrayList<Float> vectorList) {
        double max = 0;
        for (int i = 0; i < vectorList.size(); i++) {
            if (max < vectorList.get(i)) {
                max = vectorList.get(i);
            }
        }
        mMaxValue.setText(format(max));
    }

    private double onCalculateAverage(ArrayList<Float> vectorList) {
        double summary = 0;
        double average = 0;
        for (int i = 0; i < vectorList.size(); i++) {
            summary = summary + vectorList.get(i);
        }
        average = summary / vectorList.size();
        mAverageValue.setText(format(average));
        Log.d(TAG, " average = " + average);
        Log.d(TAG, " summary = " + summary);
        return average;
    }

    private void onCalculateDispersion(ArrayList<Float> vectorList) {
        double averageValue = 0;
        float deviationSquare = 0;
        double dispersion = 0;
        averageValue = onCalculateAverage(vectorList);
        ArrayList<Float> vectorListDeviationSquare = new ArrayList<>();
        for (int i = 0; i < vectorList.size(); i++) {
            deviationSquare = (float) Math.pow((vectorList.get(i) - averageValue), 2);
            vectorListDeviationSquare.add(deviationSquare);
        }
        dispersion = Math.sqrt(onCalculateAverage(vectorListDeviationSquare));
        mDispersionValue.setText(format(dispersion));
    }

    String format(double values) {
        return String.format("%1$.4f\t", values);
    }

}
