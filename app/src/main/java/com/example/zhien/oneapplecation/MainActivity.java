package com.example.zhien.oneapplecation;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.sql.Time;
import java.util.UUID;

public class MainActivity extends AppCompatActivity  implements BluetoothAdapter.LeScanCallback, View.OnClickListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final String DEVICE_NAME = "ADD@H_Chat";

    private static final UUID ADD_H_SERVICE = UUID.fromString("42821a40-e477-11e2-82d0-0002a5d5c51b");
    //read
    private static final  UUID ADD_H_DATA_CHAR  = UUID.fromString("a32e5520-e477-11e2-a9e3-0002a5d5c51b");
    //write
    private static final UUID ADD_H_CONFIG_CHAR  = UUID.fromString("340a1b80-cf4b-11e1-ac36-0002a5d5c51b");
    private static final UUID ADD_H_DESCRIPTOR = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private static final int PERMISSION_REQUEST_FINE_LOCATION = 1;

    private BluetoothAdapter mBluetoothAdapter;
    private SparseArray<BluetoothDevice> mDevice;

    private BluetoothGatt mConnectedGatt;

    private TextView mChild;
    private ProgressBar mProgress;
    private Button btnDisconnect;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_main);
        setProgressBarIndeterminate(true);

        mChild = findViewById(R.id.tvChild);
        mProgress = findViewById(R.id.pbChildProgress);

        BluetoothManager manager = (BluetoothManager)  getSystemService(BLUETOOTH_SERVICE);
        mBluetoothAdapter = manager.getAdapter();

        mDevice = new SparseArray<BluetoothDevice>();

        mProgress = new ProgressBar(this);
        mProgress.setIndeterminate(true);
        //mProgress.setCancelable(false);

        btnDisconnect = findViewById(R.id.btnDisconnect);
        btnDisconnect.setOnClickListener(this);

    }





    @Override
    protected void onResume() {
        super.onResume();

        if ((mBluetoothAdapter == null) || (!mBluetoothAdapter.isEnabled())){
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBtIntent);
            finish();
            return;
        }

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)){
            Toast.makeText(this, "No LE Support", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        clearDisplayValues();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //make sure dialog is hidden
        ///mProgress.dismiss();
        //Cancel any scans in progress
        mHandler.removeCallbacks(mStopRunnable);
        mHandler.removeCallbacks(mStartRunnable);
        mBluetoothAdapter.stopLeScan(this);

    }

    @Override
    protected void onStop() {
        super.onStop();
        //Disconnect from ant activity tag connection
        if (mConnectedGatt != null) {
            mConnectedGatt.disconnect();
            mConnectedGatt = null;
        }
    }

    @Override
    public void onClick(View view) {
        if (mConnectedGatt != null) {

            mConnectedGatt.close();
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //add the scan options to menu
        getMenuInflater().inflate(R.menu.main, menu);
        //add any device element we've discovered to the overflow menu
        for (int i = 0; i < mDevice.size(); i++) {
            BluetoothDevice device = mDevice.valueAt(i);
            menu.add(0, mDevice.keyAt(i), 0, device.getName());
        }
          return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_scan:
                mDevice.clear();
                startScan();
                return true;
            default:
                //Obtain the discovered device to connect with
                BluetoothDevice device = mDevice.get(item.getItemId());
                Log.i(TAG, "connection to "+ device.getName());
                /*
                * Make a connection with the  device using the special LE  - specific
                * connectionGatt() method, passing in a callback for GATT events
                */
                mConnectedGatt = device.connectGatt(this, true, mGattCallBack);
                //Display progress UI
                mHandler.sendMessage(Message.obtain(null, MSG_PROGRESS, "Connecting to "+ device.getName() + "...."));
                return super.onOptionsItemSelected(item);
        }
    }

    private void clearDisplayValues (){
    mChild.setText("--");
        }

    private Runnable mStopRunnable = () -> stopScan();
    private Runnable mStartRunnable = () -> startScan();


    //private Runnable mStartRequest = () -> startRequest();
 //  private Runnable mWritecacter = () -> write();

/*
    private void write(){
        new BluetoothGattCallback();
    }*/

    private void startScan() {
        mBluetoothAdapter.startLeScan(this);
        setProgressBarIndeterminateVisibility(true);

        mHandler.postDelayed(mStopRunnable, 2500);
    }

    private void stopScan() {
        mBluetoothAdapter.stopLeScan(this);
        setProgressBarIndeterminateVisibility(false);
    }

    //BluetoothAdapter.LeScanCallback

    @Override
    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecor) {

        Log.d(TAG, "New LE Device = "+ device.getName()+ " @ "+ rssi);

        if (DEVICE_NAME.equals(device.getName())){

            mDevice.put(device.hashCode(), device);
            //update the overflow menu
            invalidateOptionsMenu();
        }
    }

    /*
    * in this call back we've created a bit of a state machine to enforce that only
    * one characteristic be read or written at a time until all of our sensors
    * are enabled and we are registered to get a notifications
    */
    private static final int ENABLE_REQUEST = 0;
    private static final int ENABLE_RESPONSE = 1;
    private BluetoothGattCallback mGattCallBack = new BluetoothGattCallback() {
        private  int mState = 0 ;

        private void reset(){mState=0;}
        private void advance() {mState++;}
        /*
        * Send an enable command to each sensor by writing a configuration
        * characteristic. This is specific to the SensorTag to keep power
        * low by disabling sensors you aren't using
        */
        private void enableNextSensor(BluetoothGatt gatt) {
            BluetoothGattCharacteristic characteristic = null;
            switch (mState){
                case 0:
                    Log.d(TAG, "Enabling child call ");

                        characteristic = gatt.getService(ADD_H_SERVICE)
                                .getCharacteristic(ADD_H_CONFIG_CHAR);
                        characteristic.setValue(new byte[]{0x01, 0x02, 0x01, (byte) 0xFE});
                    break;
                case 1:

                        Log.d(TAG, "Enabling child call ");
                        characteristic = gatt.getService(ADD_H_SERVICE)
                                .getCharacteristic(ADD_H_DATA_CHAR);
                        characteristic.setValue(new byte[]{0x01, 0x02, 0x01, (byte) 0xFE});
                    break;

                default:
                    mHandler.sendEmptyMessage(MSG_DISMISS);
                    Log.d(TAG, "All sensor Enable");
            }
                gatt.writeCharacteristic(characteristic);
        }

        private void readNextSensor(BluetoothGatt gatt){
            BluetoothGattCharacteristic characteristic;
            switch (mState){
                case 0:
                    Log.d(TAG, "reading HACHILD");

                    characteristic  = gatt.getService(ADD_H_SERVICE)
                            .getCharacteristic(ADD_H_DATA_CHAR);
                    break;
                case 1:
                    Log.d(TAG, "reading HACHILD");

                    characteristic  = gatt.getService(ADD_H_SERVICE)
                            .getCharacteristic(ADD_H_CONFIG_CHAR);
                    break;
                default:
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
            switch (mState){
                case 0:
                    Log.d(TAG, "Set notify HAHCILD call");

                        characteristic = gatt.getService(ADD_H_SERVICE)
                                .getCharacteristic(ADD_H_DATA_CHAR);

                    break;

                default:
                    mHandler.sendEmptyMessage(MSG_DISMISS);
                    Log.d(TAG, "Sensor Enabled");
                    return;
            }
            //Enable local notifications
            gatt.setCharacteristicNotification(characteristic, true);
            //Enable remote notifications
            BluetoothGattDescriptor desc = characteristic.getDescriptor(ADD_H_DESCRIPTOR);
            desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            gatt.writeDescriptor(desc);
        }


        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState){
            Log.d(TAG, "Connection State Change: " + status +"->"+ connectionState(newState));
            if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED){
                /*
                *Once successfully connected, we must next discover all the services on the
                *device before we can read and write their characteristics
                */
                gatt.discoverServices();
                mHandler.sendMessage(Message.obtain(null, MSG_PROGRESS, "Discovering service"));
            } else if ( status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_DISCONNECTED) {
                /*
                If at any point we disconnect, send a message to clear the weather values
                * out of the UI
                */
                mHandler.sendEmptyMessage(MSG_CLEAR);
            } else if (status != BluetoothGatt.GATT_SUCCESS){
                /*
                *if there is a failure at any stage, simply disconnect
                */
                gatt.disconnect();


            }
        }


        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.d(TAG, "Service discovered: "+status);
            mHandler.sendMessage(Message.obtain(null, MSG_PROGRESS, "Enabling Sensors ... "));
           /*
           * With services discovered, we are going to reset out state machine and start
           *working through the sensors we need to enable
           */
           reset();
            enableNextSensor(gatt);

        }

        public void onCharacteristicRead (BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status){
            // For each read, pass the data up to the UI thread to update the display

            if (ADD_H_DATA_CHAR.equals(characteristic.getUuid())){
                mHandler.sendMessage(Message.obtain(null, MSG_CHAR, characteristic));
            }
            //After reading the initial value, next we enable notifications

            for (int i = 0; i < 100; i++){
            setNotifyNextSensor(gatt);}
        }



        public  void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
           //After writing the enable flag, next we read the initial value

            readNextSensor(gatt);
            }


        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            /*
            * After notification are enabled, all updates from device on characteristic
            * value changes will be posted here. Similar to read, we hand these up to the
            * UI thread to update the display.
            */

            if (ADD_H_DATA_CHAR.equals(characteristic.getUuid())){
                mHandler.sendMessage(Message.obtain(null, MSG_CHAR, characteristic));
            }
        }

        public void  onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status){
            //Once notifications are enabled, we move to the next sensor and start over with enable
            advance();
            enableNextSensor(gatt);
        }

        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status){
            Log.d(TAG, "Remote RSSI: "+rssi);
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


        /*
        * We have a Handler to process event results on the main thread
        */
        private static final int MSG_CHAR = 102;
        private static final int MSG_CHAR_CONFIG = 103;
        private static final int MSG_PROGRESS = 201;
        private static final int MSG_DISMISS = 202;
        private static final int MSG_CLEAR = 301;
        @SuppressLint("HandlerLeak")
        private Handler mHandler = new Handler(){
            public void handleMessage(Message msg) {
                BluetoothGattCharacteristic characteristic;
                switch (msg.what){
                  case MSG_CHAR:
                      characteristic = (BluetoothGattCharacteristic) msg.obj;
                      if(characteristic.getValue() == null) {
                          Log.d(TAG, "Error obtaining HACHILD_read value");
                          return;
                      }
                      updateHACHILD_Read_Values(characteristic);
                      break;

                  case MSG_PROGRESS:

                      /*mProgress.setMessage((""), msg.obj);
                      if (!mProgress.isShowing()) {
                          mProgress.show();
                      }*/

                      break;
                  case MSG_DISMISS:
                      //mProgress.hide();
                      break;
                  case  MSG_CLEAR:
                      clearDisplayValues();
                      break;
              }
            }
        };

        /*Method to extract sensor data and update the UI*/

        private  byte [] masR;
        private int x=0;
        private int y=0;
        private int z=0;

        private void updateHACHILD_Read_Values(BluetoothGattCharacteristic characteristic) {
            masR = SensorTagData.extractHachild(characteristic);

            x = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 3);
            y = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 4);
            z = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 5);

            mChild.setText(String.valueOf(x)+":"+String.valueOf(y)+":"+String.valueOf(z));
           Log.d(TAG," coordinate x = " + x + " coordinate y = "+ y +" coordinate z = "+z);

        }


        private void LOGclass(String where, String text) {
            Log.d(TAG, " немного текста " +where+ text);
        }



}
