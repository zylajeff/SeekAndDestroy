package com.zylafone.seekanddestroy;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.polidea.rxandroidble.RxBleClient;
import com.polidea.rxandroidble.RxBleDevice;

import java.util.UUID;

import rx.Subscription;

public class MainActivity extends AppCompatActivity {

    private final int REQUEST_ENABLE_BT = 40;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // Device doesn't support Bluetooth
        }

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        RxBleClient rxBleClient = RxBleClient.create(this);
        RxBleDevice device = rxBleClient.getBleDevice("DA:EF:EE:BB:10:97");
        Subscription subscription = device.establishConnection(false)
                .subscribe(
                        rxBleConnection -> {
                             rxBleConnection.writeCharacteristic(UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e".toUpperCase()), "Sendme".getBytes()).subscribe(
                                result -> {
                                },
                                throwable -> {
                             });
                        },
                        throwable -> {
                            // Handle an error here.
                        }
                );
        // When done, just unsubscribe.
        //flowSubscription.unsubscribe();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == REQUEST_ENABLE_BT) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                // The user turned on Bluetooth
            }
        }
    }

}
