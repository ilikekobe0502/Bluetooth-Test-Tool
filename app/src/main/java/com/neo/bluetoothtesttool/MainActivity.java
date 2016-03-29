package com.neo.bluetoothtesttool;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private final static String TAG = MainActivity.class.toString();
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_COARSE_LOCATION_PERMISSIONS = 100;

    private Button mButton_open, mButton_close, mButton_client, mButton_server, mButton_Discover, mButton_stopSearch, mButton_test, mButton_paid;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice mBluetoothDevice;
    private ListView mListView_Device;
    private ListView mListView_Log;

    private ArrayAdapter<String> listAdapter;
    private List<String> mSearchDevice = new ArrayList<>();
    private List<String> mData = new ArrayList<>();
    private List<String> mLogcat = new ArrayList<>();
    private BluetoothService mBlueService = null;
    private List<BluetoothDevice> mDevice = new ArrayList<BluetoothDevice>();

    private Utils utils = new Utils();

    @Override
    protected void onResume() {
        super.onResume();
    }

    /**
     * 藍芽搜尋Receiver
     */
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                //discovery starts, we can show progress dialog or perform other tasks
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                //discovery finishes, dismis progress dialog
            } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                //bluetooth device found
                mBluetoothDevice = (BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                mDevice.add(mBluetoothDevice);

                //取得搜尋到的藍牙裝置資料
                BluetoothDevice device = null;
                for (int i = 0; i < mDevice.size(); i++) {
                    device = mDevice.get(i);
                }
                mSearchDevice.add(device.getName() + "\n" + device.getAddress());
                Log.d(TAG, "Found device " + device.getName() + " " + device.getAddress());

            }

            listAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_list_item_1, mSearchDevice);
            mListView_Device.setAdapter(listAdapter);
        }
    };

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case Constants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    Toast.makeText(getApplication(), readMessage, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "  --- handler --- " + readMessage);

                    if (readMessage.equals("test"))
                        mBlueService.ackBT("ok".getBytes());
                    break;
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mButton_open = (Button) findViewById(R.id.button_open);
        mButton_close = (Button) findViewById(R.id.button_close);
        mButton_client = (Button) findViewById(R.id.button_client);
        mButton_server = (Button) findViewById(R.id.button_Server);
        mButton_Discover = (Button) findViewById(R.id.button_discover);
        mButton_stopSearch = (Button) findViewById(R.id.button_stopSearch);
        mButton_test = (Button) findViewById(R.id.button_test);
        mButton_paid = (Button) findViewById(R.id.button_paid);
        mListView_Device = (ListView) findViewById(R.id.list_DeviceID);
        mListView_Log = (ListView) findViewById(R.id.list_Log);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mBlueService = new BluetoothService(getApplication(), mHandler);
    }

    @Override
    protected void onStart() {
        super.onStart();

        //判斷是裝置是否支援藍芽
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
            Toast.makeText(getApplication(), "不支援藍芽", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "no support");
        }

        //open BT
        mButton_open.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mBluetoothAdapter.isEnabled()) {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                } else {
                    Toast.makeText(getApplication(), "已開啟 BT", Toast.LENGTH_SHORT).show();
                }

                Log.i(TAG, " 1 --- open BT");
            }
        });

        //close BT
        mButton_close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBluetoothAdapter.disable();
                mBlueService.onDestroy();

                mData.clear();
                Log.i(TAG, " 3 --- close BT");
            }
        });

        //show near devices
        mButton_client.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                int hasPermission = ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION);
                if (hasPermission == PackageManager.PERMISSION_GRANTED) {

                    DiscoverDevice();
                    return;
                }

                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{
                                android.Manifest.permission.ACCESS_COARSE_LOCATION},
                        REQUEST_COARSE_LOCATION_PERMISSIONS);
            }
        });

        //cancel discovery BT
        mButton_stopSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBluetoothAdapter.cancelDiscovery();

                Log.i(TAG, "   --- cancel Discovery");
            }
        });

        //wait BT connect
        mButton_server.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //等待連線
                mBlueService.Accept();
            }
        });

        //test BT connected
        mButton_test.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBlueService.testBT("test".getBytes());
            }
        });

        //get paired devices
        mButton_paid.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //找出已配對裝置
                Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
                // If there are paired devices
                if (pairedDevices.size() > 0) {
                    // Loop through paired devices
                    for (BluetoothDevice device : pairedDevices) {
                        // Add the name and address to an array adapter to show in a ListView
                        mDevice.add(device);
                        mData.add(device.getName() + device.getAddress());
                        Log.d(TAG, device.getName() + device.getAddress() + "\n");
                    }
                }
                listAdapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_list_item_1, mData);
                mListView_Device.setAdapter(listAdapter);

                Log.i(TAG, "   --- get Paired devices");
            }
        });

        //can be Discover my devices
        mButton_Discover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //開放被探索
                if (mBluetoothAdapter.isEnabled()) {
                    Intent discoverableIntent = new
                            Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                    discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 30);
                    startActivity(discoverableIntent);
                }
                Log.i(TAG, "   --- can be Discover");
            }
        });

        //將搜尋到的Device選擇做連線
        mListView_Device.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //連線
                if (mDevice != null)
                    mBlueService.Connect(mDevice.get(position));
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        //取消註冊接受廣播
        try {
            unregisterReceiver(mReceiver);
        } catch (Exception e) {

        }

        //將所有Thread關閉
        mBlueService.onDestroy();
    }

    /**
     * 探索附近裝置
     */
    public void DiscoverDevice() {

        //寫入權限
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(mReceiver, filter);
        mBluetoothAdapter.startDiscovery();

        Log.i(TAG, "   --- show near devices");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_COARSE_LOCATION_PERMISSIONS: {
                if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    DiscoverDevice();

                } else {
                    Toast.makeText(this, "請允許權限", Toast.LENGTH_LONG).show();

                    mBluetoothAdapter.cancelDiscovery();

                }
                return;
            }
        }
    }
}
