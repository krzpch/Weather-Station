package com.example.bt_test_4;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
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

    Button send, connect;
    TextView status;
    EditText writeMsg;
    ListView list;

    BluetoothAdapter bluetoothAdapter=BluetoothAdapter.getDefaultAdapter();
    ArrayList<String> data= new ArrayList<>();

    SendReceive sendReceive;

    static final int STATE_CONNECTING=1;
    static final int STATE_CONNECTED=2;
    static final int STATE_CONNECTION_FAILED=3;
    static final int STATE_MESSAGE_RECEIVED=4;

    boolean connected = false;

    int REQUEST_ENABLE_BLUETOOTH=1;

    private static final String APP_NAME = "Bt_test_4";
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

        connect.setOnClickListener(new View.OnClickListener() { // when button connect is pressed
            @Override
            public void onClick(View view) {
                Set<BluetoothDevice> bt=bluetoothAdapter.getBondedDevices(); // get list of bonded devices
                String MACaddr = "";
                if(bt.size()>0) {
                    for (BluetoothDevice device : bt) { // search list
                        if (device.getName().equals("HC-05")) { // if HC-05 is found
                            MACaddr = device.getAddress(); // get its MAC address
                        }
                    }
                }

                if(!MACaddr.isEmpty()) { // if HC-05 was found in the list
                    BluetoothDevice hc05 = bluetoothAdapter.getRemoteDevice(MACaddr);
                    ClientClass clientClass=new ClientClass(hc05);
                    clientClass.start(); // start connection as client
                    status.setText("Connecting");
                }
                else {
                    Toast.makeText(getApplicationContext(), "Device is not bonded.", Toast.LENGTH_LONG).show(); // show prompt
                }
            }
        });

        send.setOnClickListener(new View.OnClickListener() { // when button send is pressed
            @Override
            public void onClick(View view) {
                String string = String.valueOf(writeMsg.getText());  // add timestamp to entered message
                if(connected) { // if connection is established
                    sendReceive.write(string.getBytes()); // send
                }
                else {
                    Toast.makeText(getApplicationContext(), "Not connected.", Toast.LENGTH_LONG).show(); // show prompt
                }
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
                    display(tempMsg);
                    break;
            }
            return true;
        }
    });

    private void findEachViewById() { // method associating widgets with java objects
        send=(Button) findViewById(R.id.send);
        connect=(Button) findViewById(R.id.connect);
        status=(TextView) findViewById(R.id.status);
        writeMsg=(EditText) findViewById(R.id.writemsg);
        list=(ListView) findViewById(R.id.listView);
    }

    private void display(String input) { // method displaying list of received through bluetooth data
        data.add(0,input);
        ArrayAdapter<String> arrayAdapter=new ArrayAdapter<String>(getApplicationContext(),android.R.layout.simple_list_item_1,data);
        list.setAdapter(arrayAdapter);
    }

    private class ClientClass extends Thread
    {
        private BluetoothDevice device;
        private BluetoothSocket socket;

        public ClientClass (BluetoothDevice device1) // constructor
        {
            device=device1;

            try {
                socket=device.createRfcommSocketToServiceRecord(MY_UUID); //  try to create a socket
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
        private final BluetoothSocket bluetoothSocket;
        private final InputStream inputStream;
        private final OutputStream outputStream;

        public SendReceive (BluetoothSocket socket) // constructor
        {
            bluetoothSocket=socket;
            InputStream tempIn=null;
            OutputStream tempOut=null;

            try { // try to get input and output streams
                tempIn=bluetoothSocket.getInputStream();
                tempOut=bluetoothSocket.getOutputStream();
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
            String temp = "";

            while (true)
            {
                try {
                    while(inputStream.available()==0); // block socket until data received
                    while((inputStream.available()>0) || (!temp.endsWith("\n"))) { // read input stream until there is nothing to read and end of data symbol is received
                        bytes = inputStream.read(buffer);
                        temp += new String(buffer, 0, bytes);
                    }
                    handler.obtainMessage(STATE_MESSAGE_RECEIVED, -1, -1, temp).sendToTarget(); // send to handler to display
                    temp="";
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
