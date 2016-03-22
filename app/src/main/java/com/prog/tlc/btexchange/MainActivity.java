package com.prog.tlc.btexchange;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.prog.tlc.btexchange.gestione_bluetooth.AcceptThread;
import com.prog.tlc.btexchange.gestione_bluetooth.BtUtil;
import com.prog.tlc.btexchange.gestione_bluetooth.ConnectThread;

import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    private BluetoothAdapter btAdapter;
    private Set<BluetoothDevice> dispositivi;

    public BluetoothAdapter getBtAdapter() {
        return btAdapter;
    }

    private ListView lv;
    private ArrayAdapter<String> adapter = null;
    private static final int BLUETOOTH_ON = 1000;
    // Create a BroadcastReceiver for ACTION_FOUND
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // Add the name and address to an array adapter to show in a ListView
                adapter.add(device.getName() + "\n" + device.getAddress());
            } else if (btAdapter.getState() == btAdapter.STATE_OFF) {
                enableBt();
            }
        }
    };

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        //fab.setOnClickListener(new View.OnClickListener() {
        //    public void onClick(View view) {
        //        Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
        //                .setAction("Action", null).show();
        //    }
        //});

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        btAdapter = BluetoothAdapter.getDefaultAdapter();
        lv = (ListView) findViewById(R.id.listview);
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        lv.setAdapter(adapter);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                TextView text = (TextView) view;
                String s = text.getText().toString();
                String[] split = s.split("\n");
                Toast t = Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT);
                t.show();
                String obj = "Ciao da " + btAdapter.getName();
                ConnectThread connectThread = new ConnectThread(btAdapter, btAdapter.getRemoteDevice(split[1]), obj);
                connectThread.start();

            }
        });

        Runnable r = new Runnable() {
            @Override
            public void run() {
                while (true) {
                    AcceptThread acceptThread = new AcceptThread(btAdapter, "Saluto");
                    acceptThread.start();
                    final Object obj = acceptThread.getAnswer();
                    runOnUiThread(new Runnable() {
                        public void run() {
                            Toast t = Toast.makeText(getApplicationContext(), (String) obj, Toast.LENGTH_SHORT);
                            t.show();
                        }
                    });


                }
            }
        };
        try {
            if (!btAdapter.isEnabled())
                enableBt();
        } finally {
            new Thread(r).start();
        }

    }

    private void enableBt() {
        //Intent turnOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        //startActivityForResult(turnOn, BLUETOOTH_ON);
        Intent discoverableIntent = new
                Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0);
        startActivity(discoverableIntent);
    }

    public void scan(View v) {
        if (!btAdapter.isEnabled()) {
            enableBt();
        } else
            load();
    }

    private void load() {
        if (mReceiver.isOrderedBroadcast()) unregisterReceiver(mReceiver);
        btAdapter.cancelDiscovery();
        adapter.clear();
        // Register the BroadcastReceiver
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter); // Don't forget to unregister during onDestroy
        btAdapter.startDiscovery();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == BLUETOOTH_ON && resultCode == RESULT_OK) {
            load();
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
