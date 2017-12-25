package com.zylafone.seekanddestroy;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.zylafone.seekanddestroy.services.BluetoothBleClient;

public class MainActivity extends AppCompatActivity {

    private final int REQUEST_ENABLE_BT = 40;
    private BluetoothBleClient mBluetoothClient;

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
        else{
            mBluetoothClient = new BluetoothBleClient(this);
            mBluetoothClient.connect();
        }
    }
    @Override
    protected void onStop() {
        mBluetoothClient.dispose();
        super.onStop();

    }

    @Override
    protected void onResume(){
        mBluetoothClient.connect();
        super.onResume();
    }

    public void forwardButtonClick(View v){
        Button forwardButton = (Button)v;
        //forwardButton.setBackgroundColor(Color.parseColor("#ffff00"));
        mBluetoothClient.sendData("F");
    }

    public void backwardButtonClick(View v){
        Button forwardButton = (Button)v;
        //forwardButton.setBackgroundColor(Color.parseColor("#ffff00"));
        mBluetoothClient.sendData("B");
    }

    public void leftButtonClick(View v){
        Button forwardButton = (Button)v;
        //forwardButton.setBackgroundColor(Color.parseColor("#ffff00"));
        mBluetoothClient.sendData("L");
    }

    public void rightButtonClick(View v){
        Button forwardButton = (Button)v;
        //forwardButton.setBackgroundColor(Color.parseColor("#ffff00"));
        mBluetoothClient.sendData("R");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == REQUEST_ENABLE_BT) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                mBluetoothClient = new BluetoothBleClient(this);
                mBluetoothClient.connect();
            }
        }
    }
}
