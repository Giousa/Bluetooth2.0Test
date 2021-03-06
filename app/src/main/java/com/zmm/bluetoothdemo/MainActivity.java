package com.zmm.bluetoothdemo;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

/**
 * 蓝牙2.0 搜索、连接
 */
public class MainActivity extends AppCompatActivity implements BluetoothChatService.OnReadBluetoothListener {

    private static String TAG = MainActivity.class.getSimpleName();
    private static final UUID MY_UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");

    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;

    @InjectView(R.id.recycler_view)
    RecyclerView mRecyclerView;
    private BluetoothAdapter mBluetoothAdapter;
    private static final long SCAN_PERIOD = 10000;
    private List<BluetoothDevice> mBluetoothDeviceList;
    private MyAdapter mMyAdapter;
    private BluetoothChatService mChatService = null;
    private HashMap<BluetoothDevice,BluetoothSocket> mDeviceHashMap;


    private final Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what)
            {
                case MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothChatService.STATE_CONNECTED:
                            Log.d(TAG,"---已经连接---");
                            break;
                        case BluetoothChatService.STATE_CONNECTING:
                            Log.d(TAG,"---正在连接中---");
                            break;
                        case BluetoothChatService.STATE_LISTEN:
                        case BluetoothChatService.STATE_NONE:
                            Log.d(TAG,"---未连接上---");
                            break;
                    }
                    break;

                case MESSAGE_READ:
                    Log.d(TAG,"---读取数据---");
                    Bundle bundle=msg.getData();
                    byte[] array=bundle.getByteArray("info");
                    Log.d(TAG,"read array = "+new String(array,0,array.length));
                    break;

            }
        }
    };



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.inject(this);
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        int widthPixels = dm.widthPixels;
        int heightPixels = dm.heightPixels;
        Log.d(TAG, "width = " + widthPixels + ",height = " + heightPixels);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    private void initView() {
        mBluetoothDeviceList = new ArrayList<>();
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(layoutManager);
        mMyAdapter = new MyAdapter(mBluetoothDeviceList);
        mRecyclerView.setAdapter(mMyAdapter);
        mMyAdapter.setOnItemSelectedListener(new MyAdapter.OnItemSelectedListener() {
            @Override
            public void OnItemSelect(int position) {
                BluetoothDevice bluetoothDevice = mBluetoothDeviceList.get(position);
                String show = "name = "+bluetoothDevice.getName()+",address = "+bluetoothDevice.getAddress();
                Toast.makeText(MainActivity.this,show, Toast.LENGTH_SHORT).show();
                bluetoothConnect(position);
            }
        });

        // 获得一个已经配对的蓝牙设备的set集合
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                Log.d(TAG,"已配对：name = "+device.getName() + ",address = " + device.getAddress());
            }
        } else {
            Toast.makeText(this, "没有已配对的设备", Toast.LENGTH_SHORT).show();
        }


        //当发现一个新的蓝牙设备时注册广播
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mReceiver, filter);

        //当搜索完毕后注册广播
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mReceiver, filter);
    }

    private void bluetoothConnect(final int position) {
        if(mBluetoothAdapter.isDiscovering()){
            mBluetoothAdapter.cancelDiscovery();
        }

        String msg = mBluetoothDeviceList.get(position).getName();

        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("是否选择连接");
        dialog.setMessage(msg);
        dialog.setCancelable(false); //设置按下返回键不能消失
        dialog.setPositiveButton("连接", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

                BluetoothDevice device = mBluetoothDeviceList.get(position);
                mChatService.connect(device);
                dialog.dismiss();
            }
        });
        dialog.setNegativeButton("取消", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        dialog.show();


    }


    @OnClick({R.id.btn_open, R.id.btn_query,R.id.btn_stop,R.id.btn_write})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_open:
                openBlueTooth();
                break;
            case R.id.btn_query:
                Toast.makeText(this, "Start Scan", Toast.LENGTH_SHORT).show();
                if(mBluetoothDeviceList != null  && mBluetoothDeviceList.size()>0){
                    mBluetoothDeviceList.clear();
                    mBluetoothDeviceList = null;
                }
                initView();
                scanLeDevice();
                break;

            case R.id.btn_stop:
                stop();
                break;

            case R.id.btn_write:
                writeData();
                break;
        }
    }

    private void writeData() {
        byte[] bytes = {1,9,2,8,3,7,4,5,0};
    }

    private void stop() {

    }

    private void scanLeDevice() {
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }
        mBluetoothAdapter.startDiscovery();
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG,"广播开启:");
            // 当发现一个新的蓝牙设备时
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.d(TAG,"device 设备名称 = "+device.getName()+",设备地址 = "+device.getAddress());
                mMyAdapter.add(device, 0);
                mRecyclerView.scrollToPosition(0);
                // If it's already paired, skip it, because it's been listed already
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
//                    Log.d(TAG,"未配对： "+ device.getName() + "\n" + device.getAddress());

                }
                // When discovery is finished, change the Activity title
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Log.d(TAG,"广播结束:");
                if (mBluetoothDeviceList.size() == 0) {
                    Toast.makeText(MainActivity.this, "没有发现新设备", Toast.LENGTH_SHORT).show();
                }
            }
        }
    };

    @Override
    public void onStart() {
        super.onStart();
//        if (!mBluetoothAdapter.isEnabled()) {
//            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
//        } else {
//            if (mChatService == null) {
//                mChatService = new BluetoothChatService(this, mHandler);
//            }
//        }
    }

    private void openBlueTooth() {

        if (mBluetoothAdapter == null) {
            Toast.makeText(MainActivity.this, "蓝牙不支持", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }

        if(mDeviceHashMap != null && mDeviceHashMap.size()>0){
            mDeviceHashMap.clear();
        }
        mDeviceHashMap = new HashMap<>();

//        if (mChatService != null) {
//            // Only if the state is STATE_NONE, do we know that we haven't started already
//            if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
//                // Start the Bluetooth chat services
//                mChatService.start();
//                mChatService.setOnReadBluetoothListener(this);
//            }
//        }

    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            Log.d(TAG, "---蓝牙已开启---");
            if (mChatService == null) {
                mChatService = new BluetoothChatService(this, mHandler);
                mChatService.start();
                mChatService.setOnReadBluetoothListener(this);
            }
        }
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop the Bluetooth chat services
        if (mChatService != null) mChatService.stop();

        // Make sure we're not doing discovery anymore
        if (mBluetoothAdapter != null) {
            mBluetoothAdapter.cancelDiscovery();
        }
        // Unregister broadcast listeners
        this.unregisterReceiver(mReceiver);
    }

    @Override
    public void onReadBluetooth(BluetoothSocket socket, BluetoothDevice device, byte[] bytes) {
        Log.d(TAG,"socket = "+socket+",devcie = "+device.getName()+",bytes = "+new String(bytes));
        mDeviceHashMap.put(device,socket);
    }
}
