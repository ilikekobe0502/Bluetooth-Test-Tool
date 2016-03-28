# Bluetooth-Test-Tool

---AndroidManifests.xml---

1.在AndroidManifests.xml新增開啟Bluetooth的權限

    <uses-permission android:name="android.permission.BLUETOOTH" />
    <uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />

---BluetoothService.java---

創建一個Class來管理Bluetooth所需要用到的Thread

1.新增一個建構子來傳入參數

    public BluetoothService(Application context, Handler handler) {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mHandler = handler;
        mContext = context;
    }

基本上Thread裡面的東西都不用動，只需要針對你要做的事情改面要呼叫的Method

2.等待接收別的裝置的連接請求(Server)

    private class AcceptThread extends Thread{

        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            // Use a temporary object that is later assigned to mmServerSocket,
            // because mmServerSocket is final
            BluetoothServerSocket tmp = null;
            try {
                // MY_UUID is the app's UUID string, also used by the client code
                tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
            } catch (IOException e) { }
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
                    //寫連線之後要做的事情
                    Connected(socket);
                    
                    try {
                        mmServerSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }

        /** Will cancel the listening socket, and cause the thread to finish */
        public void cancel() {
            try {
                mmServerSocket.close();
            } catch (IOException e) { }
        }

    }
    
這裡要提到的是

    tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
MY_UUID可以看成是一個Channel的概念，不同的UUID會有不同的權限，範例用的這組UUID權限算是蠻大的，mobile To mobile 或 To Device 或 To PC都是可以work的

NAME則是自己取的名字

2.主動連線(Client)

    /**
     * 主動連線
     */
    private class ConnectThread extends Thread{

        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket,
            // because mmSocket is final
            BluetoothSocket tmp = null;
            mmDevice = device;

            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
                // MY_UUID is the app's UUID string, also used by the server code
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) { }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it will slow down the connection
            mBluetoothAdapter.cancelDiscovery();

            Log.d(TAG,"連線中");

            try {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                mmSocket.connect();

                InputStream inputStream = mmSocket.getInputStream();
                OutputStream outputStream = mmSocket.getOutputStream();
                outputStream.write(new byte[]{(byte) 0xa0, 0, 7, 16, 0, 4, 0});
            } catch (IOException connectException) {
                // Unable to connect; close the socket and get out
                try {
                    mmSocket.close();
                } catch (IOException closeException) { }
                return;
            }

            // Do work to manage the connection (in a separate thread)
            //連線動作
            Connected(mmSocket);
        }

        /** Will cancel an in-progress connection, and close the socket */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }
    
3.連線之後的動作，是由while迴圈讓他一直監聽handler有沒有資料傳進來

    /**
     * 連線後動作
     */
    private class ConnectedThread extends Thread{

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
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                    // Send the obtained bytes to the UI activity
                    mHandler.obtainMessage(Constants.MESSAGE_READ, bytes, -1, buffer)
                            .sendToTarget();
                    Log.d(TAG, "connect~~~~~");
                } catch (IOException e) {
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) { }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }

    }
    
4.Thread建立好之後,準備Method讓主程式使用這些Thread

    /**
     * 主動連線
     * @param device Bluetooth MAC 位址
     */
    public void Connect(BluetoothDevice device){
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
        Log.d(TAG, " 2 --- connect ");
    }

    /**
     * 等待連線
     */
    public void Accept(){
        mAcceptThread = new AcceptThread();
        mAcceptThread.start();
        Log.d(TAG, " 2 --- wait connect");
    }

    /**
     * 連線中
     * @param socket
     */
    public void Connected(BluetoothSocket socket){
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();
        Log.d(TAG, " 3 --- BT connected");
    }

---MainActivity.java---

1.創建一個Handler來監聽

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
            }
        }
    };

readMessage是接收到的資料，因為藍芽在傳遞都是用byte在傳遞所以傳送的時候需要轉成byte[]，接收也需要再轉回來

2.在onCreate初始化藍芽參數

    mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    mBlueService = new BluetoothService(getApplication(), mHandler);

3.判斷裝置是否support Bluetooth

        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
            Toast.makeText(getApplication(), "不支援藍芽", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "no support");
        }

4.判斷藍芽是否開啟，若沒開啟則詢問

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            Toast.makeText(getApplication(),"已開啟 BT",Toast.LENGTH_SHORT).show();
        }

這時候自己的裝置還不能被別人探索到，如果想要被探索到必須要開啟被別人探索

5.如果藍芽有開啟，則開放讓別人探索，後面的秒數，是可以被開放搜索的時間，最長是3600秒，0的話表設定成一直可被探索不關掉。
下面的範例設定為30秒

        if (mBluetoothAdapter.isEnabled()) {
          Intent discoverableIntent = new
                    Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 30);
            startActivity(discoverableIntent);
            Log.d(TAG, "   --- can be Discover");
        }

6.找出已經配對好的裝置

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

7.(Client)探索附近裝置，必須先寫一個Receiver來接收搜尋到藍芽裝置的資料

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

8.(Server)等待被連接，Bluetooth Service的Accept Method

        mBlueService.Accept();

9.將要連線藍牙裝置的Device資料傳給Service的Connect做連線

        //連線
        if (mDevice != null)
            mBlueService.Connect(mDevice.get(position));

10.在App結束的時候關掉所有Thread跟Receiver

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
