package com.example.zhien.oneapplecation;

import java.util.UUID;

public interface BluetoothParameters {

    String TAG = BluetoothParameters.class.getName();

    String NAME_DEVICE ="ADD@H_Chat";

    UUID ADD_H_SERVICE_UUID = UUID.fromString("42821a40-e477-11e2-82d0-0002a5d5c51b");

    UUID ADD_H_CHARACTER_ANSWER_UUID  = UUID.fromString("a32e5520-e477-11e2-a9e3-0002a5d5c51b");
    UUID ADD_H_CHARACTER_QUERY_UUID  = UUID.fromString("340a1b80-cf4b-11e1-ac36-0002a5d5c51b");

    UUID ADD_H_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
}
