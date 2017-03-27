package com.zmm.bluetoothdemo;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

/**
 * Description: 4.0搜索方法
 * Author:zhangmengmeng
 * Date:2017/3/27
 * Time:下午2:20
 */

public class SecondActivity extends AppCompatActivity{
    private static String TAG = MainActivity.class.getSimpleName();
    private static final UUID MY_UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
    private static int REQUEST_ENABLE_BT = 1;
    @InjectView(R.id.recycler_view)
    RecyclerView mRecyclerView;
    private BluetoothAdapter mBluetoothAdapter;
    private Handler mHandler;
    private static final long SCAN_PERIOD = 10000;
    private List<BluetoothDevice> mBluetoothDeviceList;
    private MyAdapter mMyAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.inject(this);
        mHandler = new Handler();
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);//display = getWindowManager().getDefaultDisplay();display.getMetrics(dm)（把屏幕尺寸信息赋值给DisplayMetrics dm）;
        int widthPixels = dm.widthPixels;
        int heightPixels = dm.heightPixels;
        Log.d(TAG, "width = " + widthPixels + ",height = " + heightPixels);


        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
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
                Toast.makeText(SecondActivity.this,show, Toast.LENGTH_SHORT).show();
                bluetoothConnect(position);
            }
        });
    }

    private void bluetoothConnect(int position) {
        BluetoothDevice device = mBluetoothDeviceList.get(position);
    }


    @OnClick({R.id.btn_open, R.id.btn_query})
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
        }
    }

    private void scanLeDevice() {
        mBluetoothAdapter.startLeScan(mLeScanCallback);
        invalidateOptionsMenu();
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //只将名为“GongJinMedical_0”的蓝牙设备添加的显示列表当中
                            Log.d(TAG, "name = " + device.getName() + ",address = " + device.getAddress());
//                            mBluetoothDeviceList.add(bluetoothBean);
                            mMyAdapter.add(device, 0);
                            mRecyclerView.scrollToPosition(0);
                        }
                    });
                }
            };

    private void openBlueTooth() {

        if (mBluetoothAdapter == null) {
            Toast.makeText(SecondActivity.this, "蓝牙不支持", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }


    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            Log.d(TAG, "---蓝牙已开启---");
        }
    }

}
