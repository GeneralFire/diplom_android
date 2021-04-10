package com.example.diplomapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View.OnTouchListener;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {

    /*
    joystick params
     */
    float x, y;
    ImageView imageViewStick, imageViewBorders;
    boolean downActionWasInsideBorders = false;
    TextView textViewStatus;
    float centeredStickX, centeredStickY;
    ConstraintLayout mainLayout = null;

    /*
    Bluetooth Stuff
    */
    ArrayAdapter<String> pairedDeviceAdapter;
    ThreadConnectBTdevice threadConnectBTdevice;
    ThreadConnected threadConnected;
    BluetoothAdapter bluetoothAdapter;
    private UUID uuid;
    Button buttonConnectionStatus;

    ArrayList<String> pairedDeviceArrayList;
    //@SuppressWarnings(value = "warning")

    private class ThreadConnectBTdevice extends Thread { // Поток для коннекта с Bluetooth
        private BluetoothSocket bluetoothSocket = null;

        private ThreadConnectBTdevice(BluetoothDevice device) {
            try {
                bluetoothSocket = device.createRfcommSocketToServiceRecord(uuid);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

        private class ThreadConnected extends Thread {    // Поток - приём и отправка данных
            private final InputStream connectedInputStream;
            private final OutputStream connectedOutputStream;
            private String sbprint;

            public ThreadConnected(BluetoothSocket socket) {
                InputStream in = null;
                OutputStream out = null;
                try {
                    in = socket.getInputStream();
                    out = socket.getOutputStream();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                connectedInputStream = in;
                connectedOutputStream = out;
            }
        }

    void returnStickToDefaultPosition() {
        centeredStickX = imageViewBorders.getX() + (imageViewBorders.getWidth() / 2.0f) - imageViewStick.getWidth() / 2.0f;
        centeredStickY = imageViewBorders.getY() + (imageViewBorders.getHeight() / 2.0f) - imageViewStick.getHeight() / 2.0f;
        imageViewStick.setX(centeredStickX);
        imageViewStick.setY(centeredStickY);
        textViewStatus.setText("returnStickToDefaultPosition");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imageViewStick = findViewById(R.id.imageViewStick);
        imageViewBorders = findViewById(R.id.imageViewBorders);
        textViewStatus = findViewById(R.id.textViewConnectionStatus);
        textViewStatus.setText("onCreate");
        mainLayout = findViewById(R.id.mainLayout);
        buttonConnectionStatus = findViewById(R.id.buttonConnectionStatus);



        View.OnClickListener onlButtonConnectionStatus = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)){
                    Toast.makeText(getApplicationContext(), "BLUETOOTH NOT support", Toast.LENGTH_LONG).show();
                    finish();
                    return;

                }
            }
        };

        mainLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int maxLength = imageViewBorders.getWidth() / 2;

                x = event.getX() - imageViewStick.getWidth() / 2.0f;
                y = event.getY() - imageViewStick.getHeight() / 2.0f;
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if (Math.sqrt((centeredStickX - x) * (centeredStickX - x) + (centeredStickY - y) * (centeredStickY - y)) < maxLength) {
                            downActionWasInsideBorders = true;
                            imageViewStick.setX(x);
                            imageViewStick.setY(y);
                        }
                        else
                            downActionWasInsideBorders = false;
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if (downActionWasInsideBorders == true) {
                            if (Math.sqrt((centeredStickX - x) * (centeredStickX - x) + (centeredStickY - y) * (centeredStickY - y)) < maxLength) {
                                imageViewStick.setX(x);
                                imageViewStick.setY(y);
                                textViewStatus.setText("inBorderMoving moving");
                            } else {
                                float angle = (float) Math.atan2(y - centeredStickY, x - centeredStickX);
                                x = (float) (Math.cos(angle) * maxLength);
                                y = (float) (Math.sin(angle) * maxLength);
                                imageViewStick.setX(centeredStickX + x);
                                imageViewStick.setY(centeredStickY + y);
                                textViewStatus.setText("outOfBorder moving:" + String.valueOf(angle));
                            }
                        }
                        break;
                    case MotionEvent.ACTION_UP: // отпускание
                    case MotionEvent.ACTION_CANCEL:
                        returnStickToDefaultPosition();
                        break;
                    default:
                        textViewStatus.setText("unkn MotionEvent");
                }
                return true;
            }
        });

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        returnStickToDefaultPosition();
        textViewStatus.setText("onWindowFocusChanged");
    }


}