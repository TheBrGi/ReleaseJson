package com.prog.tlc.btexchange.gestione_bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.prog.tlc.btexchange.MainActivity;
import com.prog.tlc.btexchange.gestioneDispositivo.Node;

import com.prog.tlc.btexchange.protocollo.NeighborGreeting;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by BrGi on 16/03/2016.
 */
public class BtUtil {
    public static MainActivity mainActivity;
    public static final UUID MY_UUID = UUID.fromString("d7a628a4-e911-11e5-9ce9-5e5517507c66");
    private final static long ATTESA_DISCOVERY = 6000;
    public static final String GREETING = "greeting";
    private static Context context;
    private static BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
    private static String tag = "BtExchange debug:";
    private static AcceptThread acceptThread;
    protected static final int SUCCESS_CONNECT = 0;
    protected static final int MESSAGE_READ = 1;
    private static ConcurrentHashMap<String, BluetoothSocket> sockets = new ConcurrentHashMap<>();
    private static ArrayList<BluetoothDevice> deviceVisibili = new ArrayList<>();

    private static BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                deviceVisibili.add(device);
                Log.d("device trovato", device.toString());
            } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                // run some code
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                // run some code
            } else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                if (btAdapter.getState() == btAdapter.STATE_OFF) {
                    BtUtil.stopServer();
                    accendiBt();
                } else if (btAdapter.getState() == btAdapter.STATE_ON) {
                    if (BtUtil.isInterrupted()) {
                        BtUtil.startServer();
                    }
                }
            }
        }
    };
    static Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            // TODO Auto-generated method stub
            Log.i(tag, "in handler");
            super.handleMessage(msg);
            switch (msg.what) {
                case SUCCESS_CONNECT:
                    // DO something
                    BluetoothSocket sock = (BluetoothSocket) msg.obj;
                    sockets.put(sock.getRemoteDevice().getAddress(), sock);
                    ConnectedThread connectedThread = new ConnectedThread((BluetoothSocket) msg.obj);
                    connectedThread.start();
                    Toast.makeText(getContext(), "CONNECT", Toast.LENGTH_SHORT).show();
                    String s = "successfully connected";
                    connectedThread.write(s);
                    Log.i(tag, "connected");
                    break;
                case MESSAGE_READ:
                    //byte[] readBuf = (byte[]) msg.obj;
                    //String string = new String(readBuf);
                    String string = (String) msg.obj;
                    Toast.makeText(getContext(), string, Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    private BtUtil() {
    }

    public static void setActivity(MainActivity activity) {
        mainActivity=activity;
    }

    public static void accendiBt(){
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0);
        mainActivity.startActivityForResult(discoverableIntent, 1);
    }

    public static void startServer() {
        acceptThread=new AcceptThread();
        acceptThread.start();
        Log.d(tag,"start AcceptThread");
    }

    public static void stopServer() {
        acceptThread.cancel();
        acceptThread.interrupt();
        Log.d(tag,"stop AcceptThread");
    }

    public static boolean isInterrupted() {
        return acceptThread.isInterrupted();
    }

    public static void setContext(Context c) {
        context = c;
    }

    public static Context getContext() {
        return context;
    }

    public static BluetoothAdapter getBtAdapter() {
        return btAdapter;
    }

    public static ArrayList<BluetoothDevice> getViciniVisibli() { return  deviceVisibili; }

    public static ArrayList<Node> cercaVicini() {
        Log.d(tag,"chiamata cerca vicini");
        deviceVisibili.clear();
        btAdapter.startDiscovery();
        Log.d(tag,"discovery started");
        try {
            Thread.sleep(ATTESA_DISCOVERY);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        btAdapter.cancelDiscovery();
        Log.d(tag,"discovery canceled");
        Log.d("lista devices",deviceVisibili.toString());
        ArrayList<Node> list=new ArrayList<>();
        for(BluetoothDevice bd: deviceVisibili) {
            Node n = new Node(bd.getName(),bd.getAddress());
            list.add(n);
        }
        Log.d("lista",list.toString());
        return list;
    }

    public static String riceviStringa() {

        return null;
    }

    public static void mandaStringa(final String s, final String addr) {

    }

    public static void inviaGreeting(NeighborGreeting greet, Node vicino) {
        BluetoothAdapter btAdapter = getBtAdapter();
        BluetoothDevice btDevice = btAdapter.getRemoteDevice(vicino.getMACAddress());
        //ConnectThread connect = new ConnectThread(btAdapter, btDevice, greet,lock);
        //connect.start();
    }

    public static NeighborGreeting riceviGreeting() {

        return null;
    }

    public static String getMACMioDispositivo() {
        return BluetoothAdapter.getDefaultAdapter().getAddress();
    }

    public static void mandaMessaggio(BluetoothDevice selectedDevice, String obj) {
        /*if (btAdapter.isDiscovering()) {
            btAdapter.cancelDiscovery();
        }*/
        if (sockets.containsKey(selectedDevice.getAddress())) {
            BluetoothSocket k = sockets.get(selectedDevice.getAddress());
            if (k.isConnected()) {
                Log.d(tag, "socket presente, mando msg");
                new ConnectedThread(k).write(obj);
            } else {
                Log.d(tag, "socket disconnesso, riconnetto");
                sockets.remove(selectedDevice.getAddress());
                ConnectThread connect = new ConnectThread(selectedDevice, obj);
                connect.start();
            }
        } else {
            Log.d(tag, "socket non presente in map, connetto");
            ConnectThread connect = new ConnectThread(selectedDevice, obj);
            connect.start();
        }
    }

    public static void unregisterReceiver() {
        try {
            mainActivity.unregisterReceiver(receiver);
        }catch (IllegalArgumentException e){
            e.printStackTrace();
        }
    }

    public static void registerReceiver() {
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        mainActivity.registerReceiver(receiver, filter);
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        mainActivity.registerReceiver(receiver, filter);
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        mainActivity.registerReceiver(receiver, filter);
        filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        mainActivity.registerReceiver(receiver, filter);
    }

    private static class ConnectThread extends Thread {

        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private Object obj;

        public ConnectThread(BluetoothDevice device, Object obj) {
            // Use a temporary object that is later assigned to mmSocket,
            // because mmSocket is final
            this.obj = obj;
            BluetoothSocket tmp = null;
            mmDevice = device;
            Log.i(tag, "construct");
            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
                // MY_UUID is the app's UUID string, also used by the server code
                tmp = device.createInsecureRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                Log.i(tag, "get socket failed");

            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it will slow down the connection
            //btAdapter.cancelDiscovery();
            Log.i(tag, "connect - run");
            try {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                mmSocket.connect();
                Log.i(tag, "connect - succeeded");
            } catch (IOException connectException) {
                Log.i(tag, "connect failed");
                // Unable to connect; close the socket and get out
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                }
                return;
            }

            // Do work to manage the connection (in a separate thread)
            new ConnectedThread(mmSocket).write(obj);
            mHandler.obtainMessage(SUCCESS_CONNECT, mmSocket).sendToTarget();
        }


        /**
         * Will cancel an in-progress connection, and close the socket
         */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
            }
        }
    }

    private static class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer;  // buffer store for the stream
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    //buffer = new byte[1024];
                    //bytes = mmInStream.read(buffer);
                    ObjectInputStream ois = new ObjectInputStream(mmInStream);
                    Object obj = ois.readObject();
                    // Send the obtained bytes to the UI activity
                    mHandler.obtainMessage(MESSAGE_READ, obj)
                            .sendToTarget();

                } catch (IOException e) {
                    Log.d(tag, "lettura fallita");
                    cancel();
                    break;
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(Object obj) {
            try {
                //mmOutStream.write(bytes);
                ObjectOutputStream oos = new ObjectOutputStream(mmOutStream);
                oos.writeObject(obj);
                oos.flush();
            } catch (IOException e) {
                Log.d(tag, "invio fallito");
                cancel();
            }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
            }
        }
    }

    private static class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            // Use a temporary object that is later assigned to mmServerSocket,
            // because mmServerSocket is final
            BluetoothServerSocket tmp = null;
            try {
                // MY_UUID is the app's UUID string, also used by the client code
                tmp = btAdapter.listenUsingInsecureRfcommWithServiceRecord("app", MY_UUID);
            } catch (IOException e) {
            }
            mmServerSocket = tmp;
        }

        public void run() {
            BluetoothSocket socket = null;
            // Keep listening until exception occurs or a socket is returned
            while (true) {
                try {
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    break;
                }
                // If a connection was accepted
                if (socket != null) {
                    // Do work to manage the connection (in a separate thread)
                    manageConnectedSocket(socket);
                    /*try {
                        mmServerSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                        break;
                    }*/

                }
            }
        }

        private void manageConnectedSocket(BluetoothSocket s) {
            sockets.put(s.getRemoteDevice().getAddress(), s);
            new ConnectedThread(s).start();
        }

        /**
         * Will cancel the listening socket, and cause the thread to finish
         */
        public void cancel() {
            try {
                mmServerSocket.close();
            } catch (IOException e) {
            }
        }
    }
}
