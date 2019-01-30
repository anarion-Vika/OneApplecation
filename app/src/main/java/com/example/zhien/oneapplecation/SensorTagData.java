package com.example.zhien.oneapplecation;

import android.bluetooth.BluetoothGattCharacteristic;

public class SensorTagData {


        public static double extractHachild(BluetoothGattCharacteristic c) {
            int rawT = shortUnsignedAtOffset(c,2);
            return rawT;
        }


}
