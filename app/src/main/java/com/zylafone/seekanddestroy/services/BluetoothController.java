package com.zylafone.seekanddestroy.services;

/**
 * Created by zylafonez on 1/15/18.
 */

public class BluetoothController implements Controller {

    private BluetoothBleClient mBluetoothClient;
    public BluetoothController(BluetoothBleClient bluetoothClient){

        mBluetoothClient = bluetoothClient;

    }
    @Override
    public void move(Direction direction, int speed) {
        String speedString = String.valueOf(speed);
        switch (direction){
            case Forward:
                mBluetoothClient.sendData("F:"+speedString);
                break;
            case Backward:
                mBluetoothClient.sendData("B:"+speedString);
                break;
            case Left:
                mBluetoothClient.sendData("L:"+speedString);
                break;
            case Right:
                mBluetoothClient.sendData("R:"+speedString);
                break;
            default :
                break;
        }
    }
}