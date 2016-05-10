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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;
import com.prog.tlc.btexchange.MainActivity;
import com.prog.tlc.btexchange.gestioneDispositivo.Node;
import com.prog.tlc.btexchange.protocollo.Messaggio;
import com.prog.tlc.btexchange.protocollo.NeighborGreeting;
import com.prog.tlc.btexchange.protocollo.RouteError;

import com.prog.tlc.btexchange.protocollo.RouteReply;
import com.prog.tlc.btexchange.protocollo.RouteRequest;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by BrGi on 16/03/2016.
 */


public class BtUtil {
    public static MainActivity mainActivity;
    public static final UUID MY_UUID = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");
    private final static long ATTESA_DISCOVERY = 4000;//tempo necessario dal bt a vedere dispositivo
    private static Context context;
    private static BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
    private static String tag = "BtExchange debug:";
    private static AcceptThread acceptThread;
    protected static final int SUCCESS_CONNECT = 0;
    protected static final int MESSAGE_READ = 1;
    private static ConcurrentHashMap<String, BluetoothSocket> sockets = new ConcurrentHashMap<>();
    private static ConcurrentLinkedQueue<BluetoothDevice> deviceVisibili = new ConcurrentLinkedQueue<>();
    private static ConcurrentLinkedQueue<RouteRequest> rreqs = new ConcurrentLinkedQueue<>();
    private static ConcurrentLinkedQueue<RouteReply> rreps = new ConcurrentLinkedQueue<>();
    private static ConcurrentLinkedQueue<NeighborGreeting> greetings = new ConcurrentLinkedQueue<>();
    private static ConcurrentLinkedQueue<Messaggio> messages = new ConcurrentLinkedQueue<>();
    private static ConcurrentLinkedQueue<RouteError> errors = new ConcurrentLinkedQueue<>();
    private static Contatore contInvii = new Contatore();
    private static Contatore contRicez = new Contatore();
    private static Contatore contFail = new Contatore();
    private static long offset;

    //NTP server list: http://tf.nist.gov/tf-cgi/servers.cgi
    public static final String TIME_SERVER = "time-a.nist.gov";
    static long returnTime;

    public static long getCurrentNetworkTime() {

        Runnable r = new Runnable() {
            @Override
            public void run() {
                NTPUDPClient timeClient = new NTPUDPClient();
                InetAddress inetAddress = null;
                try {
                    inetAddress = InetAddress.getByName(TIME_SERVER);
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
                TimeInfo timeInfo = null;
                try {
                    timeInfo = timeClient.getTime(inetAddress);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //long returnTime = timeInfo.getReturnTime();   //local device time
                returnTime = timeInfo.getMessage().getTransmitTimeStamp().getTime();   //server time

                Date time = new Date(returnTime);
                Log.d(tag, "Time from " + TIME_SERVER + ": " + time);

            }
        };
        Thread t = new Thread(r);
        t.start();
        try {
            t.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return returnTime;

    }

    public static void setOffset() {
        long timefromserver = getCurrentNetworkTime();
        long mytime = Calendar.getInstance().getTimeInMillis();
        offset = timefromserver - mytime;
        Log.d("tempo server", String.valueOf(timefromserver));
        Log.d("tempo mio", String.valueOf(mytime));
    }


    private static BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                deviceVisibili.add(device);
                Log.d("device trovato", device.toString());
            } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
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


    public static void appendLog(String text) {
        File logFile = new File("sdcard/Btlog.txt");

        if (!logFile.exists()) {
            try {
                logFile.createNewFile();
                BufferedWriter buff = new BufferedWriter(new FileWriter(logFile, true));
                buff.append("                     TIME                                    EVENT\n");
                buff.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            //BufferedWriter for performance, true to set append to file flag

            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
            buf.append(text);
            buf.newLine();
            buf.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void appendLogGreet(String text) {
        File logFile = new File("sdcard/BtGreetlog.txt");
        if (!logFile.exists()) {
            try {
                logFile.createNewFile();
                BufferedWriter buff = new BufferedWriter(new FileWriter(logFile, true));
                buff.append("                     TIME                                    EVENT\n");
                buff.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {

            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
            buf.append(text);
            buf.newLine();
            buf.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void setActivity(MainActivity activity) {
        mainActivity = activity;
    }

    public static void accendiBt() {
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0);
        mainActivity.startActivityForResult(discoverableIntent, 1);
    }

    public static void startServer() {
        acceptThread = new AcceptThread();
        acceptThread.start();
        Log.d(tag, "start AcceptThread");
    }

    public static void stopServer() {
        acceptThread.cancel();
        Log.d(tag, "stop AcceptThread");
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

    public static String getMACMioDispositivo() {
        return btAdapter.getAddress();
    }

    public static ArrayList<Node> cercaVicini() {
        Log.d(tag, "chiamata cerca vicini");
        deviceVisibili.clear();
        btAdapter.startDiscovery();
        Log.d(tag, "discovery started");
        try {
            Thread.sleep(ATTESA_DISCOVERY);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        btAdapter.cancelDiscovery();
        Log.d(tag, "discovery canceled");
        Log.d("lista devices", deviceVisibili.toString());
        ArrayList<Node> list = new ArrayList<>();
        for (BluetoothDevice bd : deviceVisibili) {
            Node n = new Node(bd.getName(), bd.getAddress());
            list.add(n);
        }
        Log.d("lista", list.toString());
        return list;
    }

    public static void inviaGreeting(NeighborGreeting greet, String MAC) {
        BluetoothDevice dest = btAdapter.getRemoteDevice(MAC);
        mandaMessaggio(dest, greet);
    }

    public static void inviaRREQ(RouteRequest rr, String MAC) {
        BluetoothDevice dest = btAdapter.getRemoteDevice(MAC);
        mandaMessaggio(dest, rr);
    }

    public static void inviaRREP(RouteReply rr, String MAC) {
        BluetoothDevice dest = btAdapter.getRemoteDevice(MAC);
        mandaMessaggio(dest, rr);
    }

    public static void inviaMess(Messaggio m, String MAC) {
        BluetoothDevice dest = btAdapter.getRemoteDevice(MAC);
        mandaMessaggio(dest, m);
    }

    public static void inviaError(RouteError re, String MAC) {
        BluetoothDevice dest = btAdapter.getRemoteDevice(MAC);
        mandaMessaggio(dest, re);
    }

    public static RouteError riceviError() {
        while (true) {
            if (!errors.isEmpty()) {
                RouteError re = errors.poll();
                Log.d("RiceviErrore", "RRR");
                return re;
            }
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static RouteRequest riceviRichiesta() {
        while (true) {
            if (!rreqs.isEmpty()) {
                RouteRequest rr = rreqs.poll();
                Log.d("RiceviRichiesta", rr.getSource_addr());
                return rr;
            }
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static RouteReply riceviRisposta() {
        while (true) {
            if (!rreps.isEmpty()) {
                RouteReply rr = rreps.poll();
                return rr;
            }
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static NeighborGreeting riceviGreeting() {
        while (true) {
            if (!greetings.isEmpty()) {
                NeighborGreeting ng = greetings.poll();
                return ng;
            }
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static Messaggio riceviMessaggio() {
        while (true) {
            if (!messages.isEmpty()) {
                Messaggio m = messages.poll();
                return m;
            }
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


    public static void mandaMessaggio(BluetoothDevice selectedDevice, Object obj) {
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
        } catch (IllegalArgumentException e) {
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

    public static void mostraMess(final String mex) {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast t = Toast.makeText(getContext(), mex, Toast.LENGTH_SHORT);
                t.show();
            }
        });

    }

    public static boolean checkSocket(String nextHop) {
        if (sockets.containsKey(nextHop))
            return sockets.get(nextHop).isConnected();
        return false;
    }

    public static String objToString(Object obj) {
        Gson gson = new Gson();
        String s = null;
        Wrapper w = new Wrapper(obj);
        s = gson.toJson(w);
        return s;
    }

    public static Object strToObj(String json) {
        Log.d("json", json);
        Gson gson = new Gson();
        JsonReader reader = new JsonReader(new StringReader(json));
        reader.setLenient(true);
        Wrapper w = gson.fromJson(reader, Wrapper.class);
        return w.getContent();
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
            Log.d(tag, "create ConnectedThread");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(tag, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            Log.i(tag, "BEGIN mConnectedThread");
            byte[] buffer = new byte[2048];
            int bytes;
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                    String json = new String(buffer, 0, bytes);
                    Object ric = strToObj(json);
                    Calendar c = Calendar.getInstance();
                    long tempoRic = c.getTimeInMillis() + offset;
                    String tempo = String.valueOf(tempoRic);
                    String mittente = mmSocket.getRemoteDevice().getAddress();
                    if (ric instanceof NeighborGreeting) {
                        contRicez.incrNum_Greet();
                        greetings.add((NeighborGreeting) ric);
                        BtUtil.appendLogGreet(tempo + " ricevuto Neighbor Greeting n. " + contRicez.getNum_Greet() + "da " + mittente);
                    } else if (ric instanceof RouteReply) {
                        contRicez.incrNum_RREP();
                        rreps.add((RouteReply) ric);
                        BtUtil.appendLog(tempo + " ricevuto Route Reply n. " + contRicez.getNum_RREP() + "da " + mittente + " source: " + ((RouteReply) ric).getSource_addr());
                    } else if (ric instanceof RouteRequest) {
                        RouteRequest rr = (RouteRequest) ric;
                        Log.d("ConnectedThread", rr.getSource_addr());
                        contRicez.incrNum_RREQ();
                        rreqs.add((RouteRequest) ric);
                        BtUtil.appendLog(tempo + " ricevuto Route Request n. " + contRicez.getNum_RREQ() + "da " + mittente + " source: " + ((RouteRequest) ric).getSource_addr());
                    } else if (ric instanceof Messaggio) {
                        contRicez.incrNum_Mess();
                        messages.add((Messaggio) ric);
                        if (((Messaggio) ric).getDest().getMACAddress().equals(getMACMioDispositivo())) {
                            long tempoE2E = tempoRic - ((Messaggio) ric).getTimeStamp();
                            BtUtil.appendLog(tempo + " ricevuto Messaggio n. " + contRicez.getNum_Mess() + "da " + mittente + " source: " + ((Messaggio) ric).getSource() + " TEMPO E2E : " + String.valueOf(tempoE2E));
                        } else
                            BtUtil.appendLog(tempo + " ricevuto Messaggio n. " + contRicez.getNum_Mess() + "da " + mittente + " source: " + ((Messaggio) ric).getSource());
                    } else if (ric instanceof RouteError) {
                        contRicez.incrNum_RERR();
                        errors.add((RouteError) ric);
                        BtUtil.appendLog(tempo + " ricevuto Route Error n. " + contRicez.getNum_RERR() + "da " + mittente + " source: " + ((RouteError) ric).getSource());
                    }

                } catch (IOException e) {
                    Log.d(tag, "lettura fallita");
                    cancel();
                    break;
                } catch (NegativeArraySizeException e) {
                    Log.e(tag, "negative array exception");
                    cancel();
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(Object obj) {
            try {
                //mmOutStream.write(bytes);

                BluetoothDevice selectedDevice = mmSocket.getRemoteDevice();
                Calendar c = Calendar.getInstance();
                long timeStamp = c.getTimeInMillis() + offset;
                String tempo = String.valueOf(timeStamp);
                if (obj instanceof NeighborGreeting) {
                    contInvii.incrNum_Greet();
                    BtUtil.appendLogGreet(tempo + " invio greeting " + "n. " + contInvii.getNum_Greet() + " a " + selectedDevice.getAddress());
                } else if (obj instanceof RouteReply) {
                    contInvii.getNum_RREP();
                    BtUtil.appendLog(tempo + " invio Route Reply " + "n. " + contInvii.getNum_RREP() + " a " + selectedDevice.getAddress() + " source: " + ((RouteReply) obj).getSource_addr());
                } else if (obj instanceof RouteRequest) {
                    contInvii.incrNum_RREQ();
                    BtUtil.appendLog(tempo + " invio Route Request " + "n. " + contInvii.getNum_RREQ() + " a " + selectedDevice.getAddress() + " source: " + ((RouteRequest) obj).getSource_addr());
                } else if (obj instanceof Messaggio) {
                    contInvii.incrNum_Mess();
                    if (((Messaggio) obj).getSource().equals(getMACMioDispositivo()))
                        ((Messaggio) obj).setTimeStamp(timeStamp);
                    BtUtil.appendLog(tempo + " invio messaggio a " + "n. " + contInvii.getNum_Mess() + " a " + selectedDevice.getAddress() + " source: " + ((Messaggio) obj).getSource());
                } else if (obj instanceof RouteError) {
                    contInvii.incrNum_RERR();
                    BtUtil.appendLog(tempo + " invio Route Error a " + "n. " + contInvii.getNum_RERR() + " a " + selectedDevice.getAddress() + " source: " + ((RouteError) obj).getSource());
                }
                String nomeClasse = obj.getClass().getSimpleName();
                Log.d("write: ", nomeClasse);

                mmOutStream.write(objToString(obj).getBytes());
                mmOutStream.flush();
            } catch (IOException e) {
                Log.d(tag, "invio fallito");
                cancel();//TODO
                BluetoothDevice selectedDevice = mmSocket.getRemoteDevice();
                Calendar c = Calendar.getInstance();
                String tempo = String.valueOf(c.getTimeInMillis() + offset);
                if (obj instanceof NeighborGreeting) {
                    contFail.incrNum_Greet();
                    BtUtil.appendLogGreet(tempo + " fallito invio greeting " + "n. " + contFail.getNum_Greet() + " a " + selectedDevice.getAddress());
                } else if (obj instanceof RouteReply) {
                    contFail.getNum_RREP();
                    BtUtil.appendLog(tempo + " fallito invio Route Reply" + "n. " + contFail.getNum_RREP() + " a " + selectedDevice.getAddress() + " source: " + ((RouteReply) obj).getSource_addr());
                } else if (obj instanceof RouteRequest) {
                    contFail.incrNum_RREQ();
                    BtUtil.appendLog(tempo + " fallito invio Route Request " + "n. " + contFail.getNum_RREQ() + " a " + selectedDevice.getAddress() + " source: " + ((RouteRequest) obj).getSource_addr());
                } else if (obj instanceof Messaggio) {
                    contFail.incrNum_Mess();
                    BtUtil.appendLog(tempo + " fallito invio messaggio a " + "n. " + contFail.getNum_Mess() + " a " + selectedDevice.getAddress() + " source: " + ((Messaggio) obj).getSource());
                } else if (obj instanceof RouteError) {
                    contFail.incrNum_RERR();
                    BtUtil.appendLog(tempo + " fallito invio Route Error a " + "n. " + contFail.getNum_RERR() + " a " + selectedDevice.getAddress() + " source: " + ((RouteError) obj).getSource());
                }
            } catch (NullPointerException e) {
            }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                if (mmOutStream != null) mmOutStream.close();
                if (mmInStream != null) mmInStream.close();
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
            while (tmp == null) {
                try {
                    // MY_UUID is the app's UUID string, also used by the client code
                    tmp = btAdapter.listenUsingInsecureRfcommWithServiceRecord("app", MY_UUID);
                } catch (IOException e) {
                }
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
