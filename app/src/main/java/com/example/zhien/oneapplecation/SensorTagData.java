package com.example.zhien.oneapplecation;

import android.bluetooth.BluetoothGattCharacteristic;

public class SensorTagData {


        public static byte[] extractHachild(BluetoothGattCharacteristic c) {
            byte  [] rawR = c.getValue();
            return rawR;
        }


}
