package com.example.diplomapp;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.graphics.Color;
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

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.Scanner;
import java.util.Set;
import java.util.Timer;
import java.util.UUID;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static java.lang.Thread.MAX_PRIORITY;

public class MainActivity extends AppCompatActivity {

    /*
    accel values
    */
    float   accelX = 0,
            accelY = 0,
            accelZ = 0;

    /*
    joystick params
     */
    float x, y;
    byte normolizedX, normolizedY;
    ImageView imageViewStick, imageViewBorders;
    boolean downActionWasInsideBorders = false;
    TextView textViewStatus, threadTicker;
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

    boolean buttonTextConnected = false;
    ThreadConnected threadConnected;
    BluetoothAdapter bluetoothAdapter;
    private UUID uuid;
    Button buttonConnectionStatus;
    private BluetoothSocket bluetoothSocket = null;
    private static String moduleAdress = "00:21:13:00:41:C1";
    private static String moduleName = "OLD_HC_05";
    private boolean isHcConnected = false;
    private boolean isHcConnecting = false;
    final byte[] toSendBuffer = new byte[4];
    final byte[] mpuDataByte = new byte[47];
    final short[] mpuDataShort = new short[3];
    byte[] firstCharacter = new byte[1];
    public static short getShort(byte[] b) {
        return (short) (((b[1] << 8) | b[0] & 0xff));
    }

    // gpaph
    GraphView graph;
    private int lastX = 0;
    LineGraphSeries<DataPoint> seriesX, seriesY, seriesZ;
    Viewport viewport;

    private class ThreadConnected extends Thread {    // Поток - приём и отправка данных
        private InputStream connectedInputStream;
        private OutputStream connectedOutputStream;
        private String sbprint;
        private boolean exit = false;
        BluetoothSocket classSocket;
        private int tickerton = 0;
        DataInputStream dataInput;
        BufferedReader r;
        StringBuilder total;
        String line;
        public ThreadConnected(BluetoothSocket socket) {
            classSocket = socket;
            InputStream in = null;
            OutputStream out = null;

            try {
                in = socket.getInputStream();
                out = socket.getOutputStream();

            } catch (IOException e) {
                e.printStackTrace();
            }

            connectedInputStream = in;
            connectedOutputStream =  out;
        }

        @Override
        public void run() { // Приём данных
            while (!exit) {

                // print inputAvailable
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            threadTicker.setText(String.valueOf(connectedInputStream.available()));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });

                    /*
                    if (connectedInputStream.available() > 12)
                        connectedInputStream.skip(connectedInputStream.available());

                    connectedOutputStream.flush();
                    */

                // sleep 10 ms
                try {
                    Thread.sleep(10);

                    // connectedInputStream.skip(connectedInputStream.available());

                    // send getAccelData cmd
                    toSendBuffer[0] = (byte) 'A';
                    toSendBuffer[1] = (byte) 'C';
                    toSendBuffer[2] = (byte) 'C';
                    toSendBuffer[3] = (byte) 'E';

                    connectedOutputStream.write(toSendBuffer);
                    /**/
                    // while(connectedInputStream.available() == 0);
                    connectedOutputStream.flush();

                    // send jostick data
                    toSendBuffer[0] = normolizedX;
                    toSendBuffer[1] = normolizedY;
                    toSendBuffer[2] = (byte)'M';
                    toSendBuffer[3] = tgAccelButton.isChecked()?(byte)'+':(byte)'-';


                    connectedOutputStream.write(toSendBuffer);

                    int currentInputAvailable = connectedInputStream.available();
                    boolean isReadedValid = false;

                    if (currentInputAvailable > 150) {
                        connectedInputStream.skip(connectedInputStream.available());
                    }

                    if (currentInputAvailable > 48) {
                        while(true) {
                            connectedInputStream.read(firstCharacter);
                            if (firstCharacter[0] == (byte) 'S')
                                break;
                        }
                        //
                        //  может пока искали 'S' от потока ничего не осталось(

                        if (connectedInputStream.available() >= 45) {
                            connectedInputStream.read(firstCharacter);      // skip ":"

                            connectedInputStream.read(mpuDataByte, 0, 45);
                            String str = new String(mpuDataByte);
                            String[] msgParts = str.split("\\:");
                            Scanner scanner = new Scanner(str);
                            accelX = Float.parseFloat(msgParts[0]);
                            accelY = Float.parseFloat(msgParts[1]);
                            accelZ = Float.parseFloat(msgParts[2]);
                            if (msgParts[3].toCharArray()[0] == 'E')
                                isReadedValid = true;
                            else
                                isReadedValid = false;
                        }
                        /*
                        mpuDataShort[0] = (short) (getShort(mpuDataByte) & 0xFFFF);

                        connectedInputStream.read(mpuDataByte, 0, 2);
                        mpuDataShort[1] = (short) (getShort(mpuDataByte) & 0xFFFF);

                        connectedInputStream.read(mpuDataByte, 0, 2);
                        mpuDataShort[2] = (short) (getShort(mpuDataByte) & 0xFFFF);

                        Formatter formatter = new Formatter();
                        formatter.format("%02x", mpuDataShort[0]);
                        Log.d("MY_APP", formatter.toString());
                        */
                        connectedInputStream.skip(connectedInputStream.available());
                    }


                    final boolean finalIsReadedValid = isReadedValid;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // Toast.makeText(MainActivity.this, "Success sended", Toast.LENGTH_LONG).show();
                            // textViewStatus.setText("Sended: " + String.valueOf(toSendBuffer[0]) + ":" + String.valueOf(toSendBuffer[1]));
                            /*
                            String str = new String(mpuDataByte);
                            textViewStatus.setText("Gotten: "
                                    + str
                            );
                            */
                            if (finalIsReadedValid) {
                                textViewStatus.setText("Gotten: "
                                        + String.valueOf(accelX)
                                        + ":"
                                        + String.valueOf(accelY)
                                        + ":"
                                        + String.valueOf(accelZ)
                                );
                                seriesX.appendData(new DataPoint(++lastX, accelX), true, 50);
                                seriesY.appendData(new DataPoint(++lastX, accelY), true, 50);
                                seriesZ.appendData(new DataPoint(++lastX, accelZ), true, 50);

                                viewport.scrollToEnd();

                                if (lastX > 50) {
                                    viewport.setMaxX(lastX);
                                    viewport.setMinX(lastX - 50);
                                }

                                graph.onDataChanged(false, false);


                            }
                            /*
                            textViewStatus.setText("Gotten: "
                                    + Integer.toHexString(mpuDataShort[0])
                                    + ":"
                                    + Integer.toHexString(mpuDataShort[1])
                                    + ":"
                                    + Integer.toHexString(mpuDataShort[2])
                            );
                            */
                        }

                    });
                } catch (IOException | InterruptedException e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "writeException1", Toast.LENGTH_LONG).show();
                            //buttonConnectionStatus.setText("writeException1");
                        }
                    });
                    e.printStackTrace();
                }
            }
        }
        public void write(byte[] buffer) throws IOException {
            try {
                connectedOutputStream.write(buffer);
            } catch (IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "writeException3", Toast.LENGTH_LONG).show();
                        //buttonConnectionStatus.setText("writeException1");
                    }
                });
                e.printStackTrace();
                return;
            }
        }
        public void cancel() throws IOException {
            exit = true;
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this, "exit were called", Toast.LENGTH_LONG).show();
                    //buttonConnectionStatus.setText("writeException1");
                }
            });
            classSocket.close();

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
        threadTicker = findViewById(R.id.ThreadTicker);

        uuid = UUID.fromString(UUID_STRING_WELL_KNOWN_SPP);

        graph = (GraphView) findViewById(R.id.graph);
        viewport = graph.getViewport();
        viewport.setYAxisBoundsManual(true);
        viewport.setXAxisBoundsManual(true);

        viewport.setMinY(-2);
        viewport.setMaxY(2);
        viewport.setScrollable(true);

        seriesX = new LineGraphSeries<DataPoint>();
        seriesX.setColor(Color.RED);

        seriesY = new LineGraphSeries<DataPoint>();
        seriesY.setColor(Color.GREEN);

        seriesZ = new LineGraphSeries<DataPoint>();
        seriesY.setColor(Color.BLUE);

        graph.addSeries(seriesX);
        graph.addSeries(seriesY);
        graph.addSeries(seriesZ);

        graph.setTitle("MPU data");

        graph.getGridLabelRenderer().setHorizontalAxisTitle("ticks");
        graph.getGridLabelRenderer().setVerticalAxisTitle("value");
        graph.getGridLabelRenderer().setPadding(32);
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

                                try {
                                    bluetoothSocket = device2.createRfcommSocketToServiceRecord(uuid);
                                    bluetoothSocket.connect();
                                    isHcConnected = true;
                                    threadConnected = new ThreadConnected(bluetoothSocket);
                                    threadConnected.start(); // запуск потока приёма и отправки данных
                                    buttonConnectionStatus.setText("Connected");
                                    textViewStatus.setText("threadConnectBTDevice.start were called");
                                    // threadConnected.setPriority(MAX_PRIORITY);
                                    break;
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }

                            }

                        }
                    }
                }
                else {
                    isHcConnected = false;
                    try {
                        threadConnected.cancel();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    textViewStatus.setText("canceling thread");
                    buttonConnectionStatus.setText("Disconnected");
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

                                /*
                                if (threadConnected != null) {
                                    textViewStatus.setText(
                                            String.valueOf(threadConnected.getState())
                                                    + ":inBrdrMov:"
                                                    + String.valueOf(normolizedX)
                                                    + ":" + String.valueOf(normolizedY
                                            ));

                                }
                                */

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