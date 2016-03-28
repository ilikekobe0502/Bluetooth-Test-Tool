# Bluetooth-Test-Tool

---AndroidManifests.xml---

1.在AndroidManifests.xml新增開啟Bluetooth的權限

    <uses-permission android:name="android.permission.BLUETOOTH" />
    <uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />

---MainActivity---

1.判斷裝置是否support Bluetooth

        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
            Toast.makeText(getApplication(), "不支援藍芽", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "no support");
        }

2.判斷藍芽是否開啟，若沒開啟則詢問

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            Toast.makeText(getApplication(),"已開啟 BT",Toast.LENGTH_SHORT).show();
        }

這時候自己的裝置還不能被別人探索到，如果想要被探索到必須要開啟被別人探索

3.如果藍芽有開啟，則開放讓別人探索，後面的秒數，是可以被開放搜索的時間，最長是3600秒，0的話表設定成一直可被探索不關掉。
下面的範例設定為30秒

        if (mBluetoothAdapter.isEnabled()) {
          Intent discoverableIntent = new
                    Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 30);
            startActivity(discoverableIntent);
            Log.d(TAG, "   --- can be Discover");
        }

4.找出已經配對好的裝置

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
        listAdapter = new ArrayAdapter<String>(getApplication(), android.R.layout.simple_list_item_1, mData);
        mListView_Device.setAdapter(listAdapter);

5.(Client)探索附近裝置，必須先寫一個Receiver來接收搜尋到藍芽裝置的資料

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
                mSearchDevice.add(device.getName() + "\n" + device.getAddress() + "\n" + device.getUuids());
                Log.d(TAG, "Found device " + device.getName() + " " + device.getAddress() + " " + device.getUuids());

            }

            listAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_list_item_1, mSearchDevice);
            mListView_Device.setAdapter(listAdapter);
        }
    };

然後必須寫入想要探索的權限ACTION_FOUND、ACTION_DISCOVERY_STARTED、ACTION_DISCOVERY_FINISHED

        //寫入權限
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(mReceiver, filter);
        mBluetoothAdapter.startDiscovery();

6.(Server)等待被連接，Bluetooth Service的Accept Method

        mBlueService.Accept();
