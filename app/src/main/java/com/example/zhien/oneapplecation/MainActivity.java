package com.example.zhien.oneapplecation;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
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
import android.net.wifi.aware.Characteristics;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Handler;

import java.util.UUID;

public class MainActivity extends AppCompatActivity  implements BluetoothAdapter.LeScanCallback{

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final String DEVICE_NAME = "ADD@H_Chat";

    private static final UUID ADDH_SERVICE = BluetoothParameters.ADD_H_SERVICE_UUID;
    private static final UUID CONFIG_DESCRIPTOR = BluetoothParameters.ADD_H_DESCRIPTOR_UUID;

    private BluetoothAdapter mBluetoothAdapter;
    private SparseArray<BluetoothDevice> mDevice;

    private BluetoothGatt mConnectedGatt;

    private TextView mChild;

    private ProgressDialog mProgress;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_main);
        setProgressBarIndeterminate(true);

        BluetoothManager manager = (BluetoothManager)  getSystemService(BLUETOOTH_SERVICE);
        mBluetoothAdapter = manager.getAdapter();

        mDevice = new SparseArray<BluetoothDevice>();

        mProgress = new ProgressDialog(this);
        mProgress.setIndeterminate(true);
        mProgress.setCancelable(false);

        mChild = findViewById(R.id.tvChild);
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
    }

    @Override
    protected void onPause() {
        super.onPause();

        mProgress.dismiss();

        mHandler.removeCallbacks(mStopRunnable);
        mHandler.removeCallbacks(mStartRunnable);
        mBluetoothAdapter.stopLeScan(this);

    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mConnectedGatt != null) {
            mConnectedGatt.disconnect();
            mConnectedGatt = null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.main, menu);

        for (int i = 0; i < mDevice.size(); i++) {
            BluetoothDevice device = mDevice.valueAt(i);
            menu.add(0, mDevice.keyAt(i), 0, device.getName());
        }
          return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.action_scan;
            mDevice.clear();
            startScan();
            return true;

            default:
            BluetoothDevice device = mDevice.get(item.getItemId());
                Log.i(TAG, "connection to "+ device.getName());

            mConnectedGatt = device.connectGatt(this, true, mGattCallBack);

            mHandler.sendMessage(Message.obtain(null, MSG_PROGRESS, "Connecting to "+ device.getName() + "...."));
            return super.onOptionsItemSelected(item);
        }
    }

    private void clearDisplayValues (){
    mChild.setText("--");
        }

        private Runnable mStopRunnable = () -> {stopScan()};
        private Runnable mStartRunnable = () -> {startScan()};

        private void startScan() {
        mBluetoothAdapter.startLeScan(this);
        setProgressBarIndeterminateVisibility(true);

        mHandler.postDelayed(mStopRunnable, 2500);
        }

        private void stopScan() {
        mBluetoothAdapter.stopLeScan(this);
        setProgressBarIndeterminateVisibility(false);
        }

    @Override
    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecor) {
            Log.d(TAG, "New LE Device = "+ device.getName()+ " @ "+ rssi);
            if (DEVICE_NAME.equals(device.getName())){
                mDevice.put(device.hashCode(), device);
                //update the overflow menu
                invalidateOptionsMenu();
            }
    }

    private BluetoothGattCallback mGattCallBack = new BluetoothGattCallback() {

            private  int mState = 0 ;

            private void reset(){mState=0;}
            private void advance() {mState++;}

            private void enableNextSensor(BluetoothGatt gatt) {
                BluetoothGattCharacteristic characteristic;
                switch (mState){
                    case 0:
                        Log.d(TAG, "Enabling child ");
                        characteristic = gatt.getService(HACHILD)
                                .getCharacteristic(HACHILD);
                        characteristic.setValue(new byte [] {0x01, 0x02, 0x01, (byte) 0xFE});
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
                        characteristic  = gatt.getService(HACHILD)
                                .getCharacteristic(HACHILD);
                        break;
                        default:
                            return;
                }
                gatt.readCharacteristic(characteristic);
            }

            private void setNotifyNextSensor(BluetoothGatt gatt) {
                BluetoothGattCharacteristic characteristic;
                switch (mState){
                    case 0:
                        Log.d(TAG, "Set notify HAHCILD call");
                        characteristic = gatt.getService(HACHILD)
                                .getCharacteristic(HACHILD);
                        break;
                        default:
                            mHandler.sendEmptyMessage(MSG_DISMISS);
                            Log.d(TAG, "Sensor Enabled");
                            return;
                }

                gatt.setCharacteristicNotification(characteristic, true);

                BluetoothGattDescriptor desc = characteristic.getDescriptor(CONFIG_DESCRIPTOR);
                desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                gatt.writeDescriptor(desc);
            }


            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState){
                Log.d(TAG, "Connection State Change: " + status +"->"+ connectionState(newState));
                if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED){
                    gatt.discoverServices();
                    mHandler.sendMessage(Message.obtain(null, MSG_PROGRESS, "Discovering service"));
                } else if ( status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_DISCONNECTED) {
                    mHandler.sendEmptyMessage(MSG_CLEAR);
                } else if (status != BluetoothGatt.GATT_SUCCESS){
                    gatt.disconnect();

                }
            }

            public void onServiceDiscrovered(BluetoothGatt gatt, int status){
                Log.d(TAG, "Service discovered: "+status);
                mHandler.sendMessage(Message.obtain(null, MSG_PROGRESS, "Enabling Sensors ... "));
                reset();
                enableNextSensor(gatt);
            }

            public void onCharacteristicRead (BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status){
                if (HACHILD.equals(characteristic.getUuid())){
                    mHandler.sendMessage(Message.obtain(null, MSG_HACHILD, characteristic));
                }
                setNotifyNextSensor(gatt);
            }

            public  void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                readNextSensor(gatt);
            }

            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                if (HACHILD.equals(characteristic.getUuid())){
                    mHandler.sendMessage(Message.obtain(null, MSG_HACHILD, characteristic));
                }
            }

            public void  onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status){
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


        private static final int MSG_HACHILD = 101;
        private static final int MSG_PROGRESS = 201;
        private static final int MSG_DISMISS = 202;
        private static final int MSG_CLEAR = 301;

        @SuppressLint("HandlerLeak")
        private Handler mHandler = new Handler(){
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                BluetoothGattCharacteristic characteristic;

              switch (msg.what){
                  case MSG_HACHILD:
                      characteristic = (BluetoothGattCharacteristic) msg.obj;
                      if(characteristic.getValue() == null) {
                          Log.d(TAG, "Error obtaining HACHILD value");
                          return;
                      }
                      updateHACHILDValues(characteristic);
                      break;
                  case MSG_PROGRESS:
                      mProgress.setMessage((String), msg.obj);
                      if (!mProgress.isShowing()) {
                          mProgress.show();
                      }
                      break;
                  case MSG_DISMISS:
                      mProgress.hide();
                      break;
                  case  MSG_CLEAR:
                      clearDisplayValues();
                      break;
              }
            }
        };

        private void updateHACHILDValues(BluetoothGattCharacteristic characteristic) {
            double hachild = SensorTagData.extractHachild(characteristic);

            mChild.setText.String.format("%.0f%%", hachild);
        }




}
