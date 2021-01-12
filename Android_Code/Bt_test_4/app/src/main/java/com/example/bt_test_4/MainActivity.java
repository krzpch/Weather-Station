package com.example.bt_test_4;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;


public class MainActivity extends AppCompatActivity {

    Button history, connect, getCurrent;
    TextView status, current;
    ListView list;

    BluetoothAdapter bluetoothAdapter=BluetoothAdapter.getDefaultAdapter();
    ArrayList<String> data= new ArrayList<>();

    SendReceive sendReceive;

    static final int STATE_CONNECTING=1;
    static final int STATE_CONNECTED=2;
    static final int STATE_CONNECTION_FAILED=3;
    static final int STATE_MESSAGE_RECEIVED=4;

    boolean connected = false;
    boolean hist_recv = false;

    int REQUEST_ENABLE_BLUETOOTH=1;
    int hist_count= 0;

    //private static final String APP_NAME = "Weather Station Companion";
    private static final UUID MY_UUID=UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findEachViewById();

        if(!bluetoothAdapter.isEnabled()) // enable bluetooth if it is off
        {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent,REQUEST_ENABLE_BLUETOOTH);
        }

        btConnectSendReceive(); // Method for handling bluetooth communication

    }

    private void btConnectSendReceive() {

        // when button connect is pressed
        connect.setOnClickListener(view -> {
            Set<BluetoothDevice> bt=bluetoothAdapter.getBondedDevices(); // get list of bonded devices
            String MACaddr = "";
            if(!connected) {
                if (bt.size() > 0) {
                    for (BluetoothDevice device : bt) { // search list
                        if (device.getName().equals("HC-05")) { // if HC-05 is found
                            MACaddr = device.getAddress(); // get its MAC address
                        }
                    }
                }

                if (!MACaddr.isEmpty()) { // if HC-05 was found in the list
                    BluetoothDevice hc05 = bluetoothAdapter.getRemoteDevice(MACaddr);
                    ClientClass clientClass = new ClientClass(hc05);
                    clientClass.start(); // start connection as client
                    status.setText("Connecting");
                } else {
                    Toast.makeText(getApplicationContext(), "Device is not bonded.", Toast.LENGTH_LONG).show(); // show prompt
                }
            }
            else {
                Toast.makeText(getApplicationContext(), "Already connected.", Toast.LENGTH_LONG).show(); // show prompt
            }
        });

        // when button history is pressed
        history.setOnClickListener(view -> {
            String string = "HIST";  // history request command
            if(connected) { // if connection is established
                sendReceive.write(string.getBytes()); // send
                hist_recv=true;
            }
            else {
                Toast.makeText(getApplicationContext(), "Not connected.", Toast.LENGTH_LONG).show(); // show prompt
            }
        });

        getCurrent.setOnClickListener(view -> {
            String string = "DATA";  // current temperature and humidity request command
            if(connected) { // if connection is established
                if(!hist_recv) {
                    sendReceive.write(string.getBytes()); // send
                }
                else {
                    Toast.makeText(getApplicationContext(), "Not available.", Toast.LENGTH_LONG).show(); // show prompt
                }
            }
            else {
                Toast.makeText(getApplicationContext(), "Not connected.", Toast.LENGTH_LONG).show(); // show prompt
            }
        });
    }

    Handler handler=new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {

            switch (msg.what)
            {
                case STATE_CONNECTING:
                    status.setText("Connecting");
                    connected = false;
                    break;
                case STATE_CONNECTED:
                    status.setText("Connected");
                    connected = true;
                    break;
                case STATE_CONNECTION_FAILED:
                    status.setText("Connection Failed");
                    connected = false;
                    break;
                case STATE_MESSAGE_RECEIVED:
                    //byte[] readBuff= (byte[]) msg.obj;
                    //String tempMsg=new String(readBuff,0,msg.arg1);
                    String tempMsg = (String) msg.obj;
                    if(hist_recv) {
                        display(tempMsg);
                    }
                    else {
                        current.setText(tempMsg);
                    }
                    break;
            }
            return true;
        }
    });

    private void findEachViewById() { // method associating widgets with java objects
        history= findViewById(R.id.history);
        connect= findViewById(R.id.connect);
        status= findViewById(R.id.status);
        list= findViewById(R.id.listView);
        current= findViewById(R.id.current);
        getCurrent= findViewById(R.id.getCurrent);
    }

    private void display(String input) { // method displaying list of received through bluetooth data
        if(hist_count==0) {
            data.clear();
        }
        data.add(0,input);
        ArrayAdapter<String> arrayAdapter=new ArrayAdapter<>(getApplicationContext(),android.R.layout.simple_list_item_1,data);
        list.setAdapter(arrayAdapter);
        hist_count++;

        if(hist_count==24) {
            hist_recv=false;
            hist_count=0;
        }
    }

    private class ClientClass extends Thread
    {
        private BluetoothSocket socket;

        public ClientClass (BluetoothDevice device) // constructor
        {

            try {
                socket= device.createRfcommSocketToServiceRecord(MY_UUID); //  try to create a socket
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run()
        {
            try {
                socket.connect(); // try connecting
                Message message=Message.obtain(); // if connection is successful send message to handler
                message.what=STATE_CONNECTED;
                handler.sendMessage(message);

                sendReceive=new SendReceive(socket); // start sending and receiving
                sendReceive.start();

            } catch (IOException e) { // if connection fails
                e.printStackTrace();
                Message message=Message.obtain(); // send message to handler that connection failed
                message.what=STATE_CONNECTION_FAILED;
                handler.sendMessage(message);
            }
        }
    }

    private class SendReceive extends Thread
    {
        private final InputStream inputStream;
        private final OutputStream outputStream;

        public SendReceive (BluetoothSocket socket) // constructor
        {
            InputStream tempIn=null;
            OutputStream tempOut=null;

            try { // try to get input and output streams
                tempIn= socket.getInputStream();
                tempOut= socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            inputStream=tempIn;
            outputStream=tempOut;

            String string = "SYNC " + System.currentTimeMillis(); // when establishing connection send timestamp to synchronize time
            write(string.getBytes());

        }

        public void run()
        {
            byte[] buffer=new byte[512];
            int bytes;
            StringBuilder temp = new StringBuilder();

            while (true)
            {
                try {
                    while(!temp.toString().endsWith("\n")) { // read input stream until end of data symbol is received
                        bytes = inputStream.read(buffer);
                        temp.append(new String(buffer, 0, bytes));
                    }
                    handler.obtainMessage(STATE_MESSAGE_RECEIVED, -1, -1, temp.toString()).sendToTarget(); // send to handler to display
                    temp = new StringBuilder();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }

        public void write(byte[] bytes) // write table of bytes
        {
            try {
                outputStream.write(bytes); // write to output stream
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
