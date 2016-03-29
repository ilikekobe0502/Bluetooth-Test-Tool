package com.neo.bluetoothtesttool;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
 * Created by neo on 2016/3/24.
 */
public class BluetoothService {

    private final static String TAG = BluetoothService.class.toString();

    private final BluetoothAdapter mBluetoothAdapter;
    private final Handler mHandler;
    private final String NAME = "BBTest";
    private final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");


    private AcceptThread mAcceptThread;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;

    private Application mContext;
    private Utils utils = new Utils();

    public BluetoothService(Application context, Handler handler) {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mHandler = handler;
        mContext = context;
    }

    /**
     * 等待接收
     */
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
//                    manageConnectedSocket(socket);
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
                Log.d(TAG,connectException.toString());
                try {
                    mmSocket.close();
                } catch (IOException closeException) { }
                return;
            }

            // Do work to manage the connection (in a separate thread)
//            manageConnectedSocket(mmSocket);
            Connected(mmSocket);
        }

        /** Will cancel an in-progress connection, and close the socket */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }

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


    public void testBT(byte[] data){
        mConnectedThread.write(data);
    }

    public void ackBT(byte[] data){
        mConnectedThread.write(data);
    }

    /**
     * 將所有Thread關閉
     */
    public void onDestroy(){
        if(mAcceptThread != null) {
            mAcceptThread.cancel();
            mAcceptThread = null;
        }

        if(mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if(mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        Log.d(TAG, " 3 --- cancel BT");
    }


}
