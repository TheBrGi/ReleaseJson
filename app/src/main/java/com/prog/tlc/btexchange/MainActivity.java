package com.prog.tlc.btexchange;


import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
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
import com.prog.tlc.btexchange.gestioneDispositivo.Dispositivo;
import com.prog.tlc.btexchange.gestioneDispositivo.Node;
import com.prog.tlc.btexchange.gestione_bluetooth.BtUtil;
import com.prog.tlc.btexchange.protocollo.AODV;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private ListView lv;
    private ArrayAdapter<String> adapter = null;
    IntentFilter filter;
    String tag = "debugging";
    public static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private AODV protocollo;
    private Dispositivo mioDispositivo;
    private final long TEMPO_INVIO_GREET = 3000;
    private boolean sending = false;
    ArrayList<Node> vicini;

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
        BtUtil.setContext(this.getApplicationContext());
        BtUtil.setActivity(this);
        BtUtil.accendiBt();
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();
        //BtUtil.bc1.build(this);

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);


        lv = (ListView) findViewById(R.id.listview);
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        lv.setAdapter(adapter);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                TextView text = (TextView) view;
                String s = text.getText().toString();
                String[] split = s.split("\n");
                //Toast t = Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT);
                //t.show();
                String obj = "Ciao da " + BtUtil.getBtAdapter().getName();
                Node nodo = vicini.get(position);
                BluetoothDevice selectedDevice = BtUtil.getBtAdapter().getRemoteDevice(nodo.getMACAddress());

                BtUtil.mandaMessaggio(selectedDevice, obj);
                Log.i(tag, "in click listener");
                //BtUtil.mandaStringa(obj, split[1]);
            }
        });
        /*while (btAdapter.getState() != btAdapter.STATE_ON) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        BtUtil.startServer();*/
        mioDispositivo = new Dispositivo(BtUtil.getBtAdapter().getName());
        protocollo = new AODV(mioDispositivo,TEMPO_INVIO_GREET);

    }


    private void stampaNodiAVideo() {

        Runnable r = new Runnable() {
            @Override
            public void run() {
                while (true) {
                    //TODO controllare che stampi i nodi giusti
                    vicini = new ArrayList<>(mioDispositivo.getListaNodi());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            adapter.clear();
                            adapter.addAll((Collection) vicini);
                        }
                    });
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        new Thread(r).start();
    }


    public void scan(View v) {
        Log.d(tag, "cerco devices");
        stampaNodiAVideo();
    }


    @Override
    protected void onPause() {
        super.onPause();

        BtUtil.unregisterReceiver();

    }

    @Override
    protected void onResume() {
        super.onResume();
        BtUtil.registerReceiver();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_CANCELED) {
            Toast.makeText(getApplicationContext(), "Bluetooth must be enabled to continue", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            while (BtUtil.getBtAdapter().getState() != BtUtil.getBtAdapter().STATE_ON) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            BtUtil.startServer();
            BtUtil.registerReceiver();

        }
    }
    /*@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == BLUETOOTH_ON && resultCode == RESULT_OK) {
            load();
        }
    }*/

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
