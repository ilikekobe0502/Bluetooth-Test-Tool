# Bluetooth-Test-Tool

---AndroidManifests.xml---

1.在AndroidManifests.xml新增開啟Bluetooth的權限

    <uses-permission android:name="android.permission.BLUETOOTH" />
    <uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />

---MainActivity---

1.判斷裝置是否support Bluetooth

        final BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
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
            Log.d(TAG, "");
        }
