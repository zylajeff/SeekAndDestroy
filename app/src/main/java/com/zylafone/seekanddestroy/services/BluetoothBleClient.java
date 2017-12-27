package com.zylafone.seekanddestroy.services;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.widget.ImageView;

import com.polidea.rxandroidble.RxBleClient;
import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.RxBleDevice;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import rx.Subscription;

public class BluetoothBleClient {
    private final String REMOTE_BLUETOOTH_ADDRESS = "DA:EF:EE:BB:10:97";
    private final UUID CHARACTERISTIC_TX_UUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");
    private RxBleDevice mBleDevice;
    private RxBleConnection mBleConnectedDevice;
    private List<Subscription> mOpenSubscriptions = new ArrayList<Subscription>();

    public BluetoothBleClient(Context appContext){
        RxBleClient rxBleClient = RxBleClient.create(appContext);
        mBleDevice = rxBleClient.getBleDevice(REMOTE_BLUETOOTH_ADDRESS);
    }

    public void connect(ImageView view){
        //TODO: Eventually decouple this from the view.

        Subscription subscription = mBleDevice.establishConnection(false).subscribe(
                rxBleConnection -> {
                    mBleConnectedDevice = rxBleConnection;
                    GradientDrawable backgroundGradient = (GradientDrawable)view.getBackground();
                    backgroundGradient.setColor(Color.GREEN);
                },
                throwable -> {
                    //There was an error
                }
        );

        mOpenSubscriptions.add(subscription);

    }

    public void sendData(String payload){
        if(mBleConnectedDevice != null) {
            Subscription subscription = mBleConnectedDevice.writeCharacteristic(CHARACTERISTIC_TX_UUID, payload.getBytes()).subscribe(
                    result -> {
                        int resultLength = result.length;
                        resultLength =999;
                    },
                    throwable -> {
                        System.out.println("stuff");
                        String x = throwable.getLocalizedMessage();
                        x= "Overwrite";
                        //Error writing®
                    }
            );

            mOpenSubscriptions.add(subscription);
        }
    }

    public void dispose(){
        //TODO: Probably shouldnt pile up all of these subscriptions.  Seems like this will keep growing and growing.
        mOpenSubscriptions.forEach(s -> s.unsubscribe());
    }
}