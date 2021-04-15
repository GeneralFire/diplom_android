package com.example.diplomapp;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
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
import android.widget.ToggleButton;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.Timer;
import java.util.UUID;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {

    /*
    joystick params
     */
    float x, y;
    byte normolizedX, normolizedY;
    ImageView imageViewStick, imageViewBorders;
    boolean downActionWasInsideBorders = false;
    TextView textViewStatus;
    float centeredStickX, centeredStickY;
    ConstraintLayout mainLayout = null;
    ToggleButton tgAccelButton;
    /*
    Bluetooth Stuff
    +ADDR:21:13:41C1
    VERSION:3.0-20170601
    +PIN:"0000"
    +NAME:OLD_HC_05
    +UART:38400,0,0
    */
    final String UUID_STRING_WELL_KNOWN_SPP = "00001101-0000-1000-8000-00805F9B34FB";
    ArrayAdapter<String> pairedDeviceAdapter;
    ThreadConnectBTdevice threadConnectBTdevice;
    boolean buttonTextConnected = false;
    ThreadConnected threadConnected;
    BluetoothAdapter bluetoothAdapter;
    private UUID uuid;
    Button buttonConnectionStatus;
    private static String moduleAdress = "00:21:13:00:41:C1";
    private static String moduleName = "OLD_HC_05";
    private boolean isHcConnected = false;
    private boolean isHcConnecting = false;

    Timer timer;



    private class ThreadConnectBTdevice extends Thread { // Поток для коннекта с Bluetooth
        private BluetoothSocket bluetoothSocket = null;

        private ThreadConnectBTdevice(BluetoothDevice device) {
            try {
                bluetoothSocket = device.createRfcommSocketToServiceRecord(uuid);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        @Override
        public void run() { // Коннект
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //Toast.makeText(MainActivity.this, "Check slave BT device!", Toast.LENGTH_LONG).show();
                    buttonConnectionStatus.setText("Connecting");
                }
            });
            boolean success = false;

            isHcConnected = false;
            isHcConnecting = true;
            try {
                bluetoothSocket.connect();
                success = true;
            }
            catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //Toast.makeText(MainActivity.this, "Check slave BT device!", Toast.LENGTH_LONG).show();
                        buttonConnectionStatus.setText("Not connected");
                    }
                });

                try {
                    bluetoothSocket.close();
                }
                catch (IOException e1) {
                    e1.printStackTrace();
                }
                isHcConnected = false;
                isHcConnecting = false;
            }

            if(success) {  // Если законнектились, тогда открываем панель с кнопками и запускаем поток приёма и отправки данных
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        buttonConnectionStatus.setText("Connected");
                    }
                });
                threadConnected = new ThreadConnected(bluetoothSocket);
                threadConnected.start(); // запуск потока приёма и отправки данных
                isHcConnected = true;
                isHcConnecting = false;
            }

        }
        public void cancel() {

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this, "Check slave BT device!", Toast.LENGTH_LONG).show();
                    buttonConnectionStatus.setText("Disconnected");
                }
            });
            try {
                bluetoothSocket.close();
                isHcConnected = false;
                isHcConnecting = false;
            }
            catch (IOException e) {
                e.printStackTrace();
            }

        }
    } // ThreadConnectBTdevice:

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
        @Override
        public void run() { // Приём данных
            while (true) {
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                try {
                    byte[] toSendBuffer = new byte[4];
                    toSendBuffer[0] = (normolizedX);
                    toSendBuffer[1] = normolizedY;
                    toSendBuffer[2] = (byte)'M';
                    toSendBuffer[3] = tgAccelButton.isChecked()?(byte)'+':(byte)'-';
                    connectedOutputStream.write(toSendBuffer);
                } catch (IOException e) {
                    break;
                }
            }
        }
        public void write(byte[] buffer) {

            try {
                connectedOutputStream.write(buffer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    void returnStickToDefaultPosition() {
        centeredStickX = imageViewBorders.getX() + (imageViewBorders.getWidth() / 2.0f) - imageViewStick.getWidth() / 2.0f;
        centeredStickY = imageViewBorders.getY() + (imageViewBorders.getHeight() / 2.0f) - imageViewStick.getHeight() / 2.0f;
        imageViewStick.setX(centeredStickX);
        imageViewStick.setY(centeredStickY);
        textViewStatus.setText("returnStickToDefaultPosition");
        normolizedX = 0;
        normolizedY = 0;
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
        tgAccelButton = findViewById(R.id.tgAccelControl);

        uuid = UUID.fromString(UUID_STRING_WELL_KNOWN_SPP);

        buttonConnectionStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ///
                /// check bluetooth support
                bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                if (bluetoothAdapter == null)
                    Toast.makeText(getApplicationContext(), "Bluetooth is not supported on this hardware platform", Toast.LENGTH_LONG).show();

                if (!isHcConnected) {
                    Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
                    textViewStatus.setText(String.valueOf(pairedDevices.size()));
                    if (pairedDevices.size() <= 0) {
                        Toast.makeText(getApplicationContext(), "No BT paired device were found", Toast.LENGTH_LONG).show();
                        isHcConnected = false;
                    }else {
                        for (BluetoothDevice device : pairedDevices) { // Добавляем сопряжённые устройства - Имя + MAC-адресс

                            if (device.getName().equals(moduleName) | device.getAddress().equals(moduleAdress)) {

                                BluetoothDevice device2 = bluetoothAdapter.getRemoteDevice(device.getAddress());
                                threadConnectBTdevice = new ThreadConnectBTdevice(device2);
                                threadConnectBTdevice.start();  // Запускаем поток для подключения Bluetooth
                                textViewStatus.setText("threadConnectBTDevice.start were called");
                            }

                        }
                    }
                }
                else {
                    threadConnectBTdevice.cancel();
                    textViewStatus.setText("canceling thread");
                }

            }
        });

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
                                normolizedX = (byte)((x - centeredStickX) / imageViewBorders.getWidth() * 255);
                                normolizedY = (byte)((centeredStickY - y)/ imageViewBorders.getHeight() * 255);
                                textViewStatus.setText("inBrdrMov:" + String.valueOf(normolizedX) + ":" + String.valueOf(normolizedY));
                            } else {
                                float angle = (float) Math.atan2(y - centeredStickY, x - centeredStickX);
                                x = (float) (Math.cos(angle) * maxLength);
                                y = (float) (Math.sin(angle) * maxLength);
                                imageViewStick.setX(centeredStickX + x);
                                imageViewStick.setY(centeredStickY + y);
                                normolizedX = (byte)(127 * Math.cos(angle));
                                normolizedY = (byte)(-127 * Math.sin(angle));
                                textViewStatus.setText("outOfBrdrMov:" + String.valueOf(normolizedX) + ":" + String.valueOf(normolizedY));
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