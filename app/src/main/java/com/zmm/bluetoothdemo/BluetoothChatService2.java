package com.zmm.bluetoothdemo;

/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
 * This class does all the work for setting up and managing Bluetooth
 * connections with other devices. It has a thread that listens for
 * incoming connections, a thread for connecting with a device, and a
 * thread for performing data transmissions when connected.
 */
public class BluetoothChatService2 {
    // Debugging
    private static final String TAG = "BluetoothChatService";
    private static final boolean D = true;

    // Name for the SDP record when creating server socket
    private static final String NAME = "BluetoothChat";

    // Unique UUID for this application
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // Member fields
    private final BluetoothAdapter mAdapter;
    private final Handler mHandler;
    private AcceptThread mAcceptThread;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private int mState;

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device

    private BluetoothSocket socket = null;
    private BluetoothSocket socket_two = null;
    private BluetoothSocket socket_three = null;
    private BluetoothSocket socket_four = null;

    /**
     * Constructor. Prepares a new BluetoothChat session.
     * @param context  The UI Activity Context
     * @param handler  A Handler to send messages back to the UI Activity
     */
    public BluetoothChatService2(Context context, Handler handler) {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
        mHandler = handler;
    }

    /**
     * Set the current state of the chat connection
     * @param state  An integer defining the current connection state
     */
    private synchronized void setState(int state) {
        if (D) Log.d(TAG, "setState() " + mState + " -> " + state);
        mState = state;

        // Give the new state to the Handler so the UI Activity can update
        mHandler.obtainMessage(MainActivity.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }

    /**
     * Return the current connection state. */
    public synchronized int getState() {
        return mState;
    }

    /**
     * Start the chat service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume() */
    public synchronized void start() {
        if (D) Log.d(TAG, "start");

        // Cancel any thread attempting to make a connection
//        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
//
//        // Cancel any thread currently running a connection
//        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        // Start the thread to listen on a BluetoothServerSocket
        if (mAcceptThread == null) {
            mAcceptThread = new AcceptThread();
            mAcceptThread.start();
        }
        setState(STATE_LISTEN);
    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     * @param device  The BluetoothDevice to connect
     */
    public synchronized void connect(BluetoothDevice device) {
        if (D) Log.d(TAG, "connect to: " + device);

//        // Cancel any thread attempting to make a connection
//        if (mState == STATE_CONNECTING) {
//            if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
//        }
//
//        // Cancel any thread currently running a connection
//        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
        setState(STATE_CONNECTING);
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     * @param socket  The BluetoothSocket on which the connection was made
     * @param device  The BluetoothDevice that has been connected
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        if (D) Log.d(TAG, "connected");

        Log.d(TAG,"连接设备 name = "+device.getName()+",address = "+device.getAddress());

//        // Cancel the thread that completed the connection
//        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
//
//        // Cancel any thread currently running a connection
//        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}
//
//        // Cancel the accept thread because we only want to connect to one device
//        if (mAcceptThread != null) {mAcceptThread.cancel(); mAcceptThread = null;}

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();

        // Send the name of the connected device back to the UI Activity
        Message msg = mHandler.obtainMessage(MainActivity.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(MainActivity.DEVICE_NAME, device.getName());
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        setState(STATE_CONNECTED);
    }

    /**
     * Stop all threads
     */
    public synchronized void stop() {
        if (D) Log.d(TAG, "stop");
        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}
        if (mAcceptThread != null) {mAcceptThread.cancel(); mAcceptThread = null;}
        setState(STATE_NONE);
    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     * @param out The bytes to write
     * @see ConnectedThread#write(byte[])
     */
    public void write(byte[] out) {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        // Perform the write unsynchronized
        r.write(out);
    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionFailed() {
        setState(STATE_LISTEN);

        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(MainActivity.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(MainActivity.TOAST, "连接失败!");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost() {
        setState(STATE_LISTEN);

        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(MainActivity.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(MainActivity.TOAST, "连接失败!");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }

    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted
     * (or until cancelled).
     */
    private class AcceptThread extends Thread {
        // The local server socket
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;
            // Create a new listening server socket
            try {
                tmp = mAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
            } catch (IOException e) {
                Log.e("sxd", " ======== ufcomm exception =======" , e);
            }
            mmServerSocket = tmp;
        }

        public void run() {
            if (D) Log.d(TAG, "BEGIN mAcceptThread" + this);
            setName("AcceptThread");
            while (true) {
                try {
                    Log.d(TAG, "thread is start and accept");
                    if (socket == null) {
                        socket = mmServerSocket.accept();
                        // If a connection was accepted
                        if (socket != null) {
                            Log.d(TAG, "蓝牙Socket accept1 ok");
                            synchronized (BluetoothChatService2.this) {
                                switch (mState) {
                                    case STATE_LISTEN:
                                    case STATE_CONNECTING:
                                        // Situation normal. Start the connected thread.
                                        connected(socket, socket.getRemoteDevice());
                                        break;
                                    case STATE_NONE:
                                    case STATE_CONNECTED:
                                        // Either not ready or already connected. Terminate new socket.
                                        try {
                                            socket.close();
                                        } catch (IOException e) {
                                            Log.e(TAG, "Could not close unwanted socket", e);
                                        }
                                        break;
                                }
                            }
                        }
                    }
                    if (socket_two == null) {
                        Log.d(TAG, "wait two");
                        socket_two = mmServerSocket.accept();
                        // If a connection was accepted
                        if (socket_two != null) {
                            Log.d(TAG, "蓝牙Socket2 accept1 ok");
                            synchronized (BluetoothChatService2.this) {
                                switch (mState) {
                                    case STATE_LISTEN:
                                    case STATE_CONNECTING:
                                        // Situation normal. Start the connected thread.
                                        connected(socket_two, socket_two.getRemoteDevice());
                                        break;
                                    case STATE_NONE:
                                    case STATE_CONNECTED:
                                        // Either not ready or already connected. Terminate new socket.
                                        try {
                                            socket_two.close();
                                        } catch (IOException e) {
                                            Log.e(TAG, "Could not close unwanted socket", e);
                                        }
                                        break;
                                }
                            }
                        }
                    }
                    if (socket_three == null) {
                        socket_three = mmServerSocket.accept();
                        // If a connection was accepted
                        if (socket_three != null) {
                            Log.d(TAG, "蓝牙Socket accept3 ok");

                            synchronized (BluetoothChatService2.this) {
                                switch (mState) {
                                    case STATE_LISTEN:
                                    case STATE_CONNECTING:
                                        // Situation normal. Start the connected thread.
                                        connected(socket_three, socket_three.getRemoteDevice());
                                        break;
                                    case STATE_NONE:
                                    case STATE_CONNECTED:
                                        // Either not ready or already connected. Terminate new socket.
                                        try {
                                            socket_three.close();
                                        } catch (IOException e) {
                                            Log.e(TAG, "Could not close unwanted socket", e);
                                        }
                                        break;
                                }
                            }
                        }
                    }
                    if (socket_four == null) {
                        socket_four = mmServerSocket.accept();
                        // If a connection was accepted
                        if (socket_four != null) {
                            Log.d(TAG, "蓝牙Socket accept4 ok");

                            synchronized (BluetoothChatService2.this) {
                                switch (mState) {
                                    case STATE_LISTEN:
                                    case STATE_CONNECTING:
                                        // Situation normal. Start the connected thread.
                                        connected(socket_four, socket_four.getRemoteDevice());
                                        break;
                                    case STATE_NONE:
                                    case STATE_CONNECTED:
                                        // Either not ready or already connected. Terminate new socket.
                                        try {
                                            socket_four.close();
                                        } catch (IOException e) {
                                            Log.e(TAG, "Could not close unwanted socket", e);
                                        }
                                        break;
                                }
                            }
                            break;
                        }
                    }
                } catch (IOException e) {
                    break;
                }

            }




            if (D) Log.i(TAG, "END mAcceptThread");
        }

        public void cancel() {
            if (D) Log.d(TAG, "cancel " + this);
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of server failed", e);
            }
        }
    }


    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "create() failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectThread");
            setName("ConnectThread");

            // Always cancel discovery because it will slow down a connection
            mAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket.connect();
            } catch (IOException e) {
                Log.e("sxd", "disconnect",e);
                connectionFailed();
                // Close the socket
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() socket during connection failure", e2);
                }
                // Start the service over to restart listening mode
                BluetoothChatService2.this.start();
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (BluetoothChatService2.this) {
                mConnectThread = null;
            }

            // Start the connected thread
            connected(mmSocket, mmDevice);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     *
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            Log.d(TAG, "create ConnectedThread");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public  void run()
        {
            Log.i(TAG, "BEGIN mConnectedThread");
            byte[] buffer = new byte[9];
            int bytes;

            // Keep listening to the InputStream while connected
            while (true)
            {
                try {
                    // Read from the InputStream
                    if((bytes = mmInStream.read(buffer)) > 1) {

                        Log.d(TAG,"正在读写中...");
                        byte[] buf_data = new byte[bytes];
                        for (int i = 0; i<bytes; i++)
                        {
                            buf_data[i] = buffer[i];
                        }

                        String readMsg = new String(buf_data);
                        Log.d(TAG,"bluetooth readMsg = "+readMsg);

                    /*bytes = mmInStream.read(buffer);*/
                        // Send the obtained bytes to the UI Activity
                    /*mHandler.obtainMessage(MainActivity.MESSAGE_READ, bytes, -1, buffer)
                            .sendToTarget();*/

                        Message msg = mHandler.obtainMessage();
                        msg.what = MainActivity.MESSAGE_READ;

                        Bundle bundle = new Bundle();
                    /*bundle.putString("info",buffer.toString());*/
                        bundle.putByteArray("info", buf_data);
                        msg.setData(bundle);
                        mHandler.sendMessage(msg);

                    }

                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    connectionLost();
                    break;
                }
            }
        }
        /**
         * Write to the connected OutStream.
         * @param buffer  The bytes to write
         */
        public void write(byte[] buffer)
        {
            try
            {
                mmOutStream.write(buffer);
                // Share the sent message back to the UI Activity
                mHandler.obtainMessage(MainActivity.MESSAGE_WRITE, -1, -1, buffer)
                        .sendToTarget();
            }
            catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        public void cancel()
        {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    public void sendMessage(String msg){
        if (socket == null) {
            Log.d(TAG, "socket is not connect");
        } else {
            try {
                OutputStream os = socket.getOutputStream();
                os.write(msg.getBytes());
                Log.d(TAG, "write to outputstream success,socket");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (socket_two == null) {
            Log.d(TAG, "socket_two is not connect");
        } else {
            try {
                OutputStream os = socket_two.getOutputStream();
                os.write(msg.getBytes());
                Log.d(TAG, "write to outputstream success,socket_two");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (socket_three == null) {
            Log.d(TAG, "socket_three is not connect");
        } else {
            try {
                OutputStream os = socket_three.getOutputStream();
                os.write(msg.getBytes());
                Log.d(TAG, "write to outputstream success,socket_three");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (socket_four == null) {
            Log.d(TAG, "socket_four is not connect");
        } else {
            try {
                OutputStream os = socket_four.getOutputStream();
                os.write(msg.getBytes());
                Log.d(TAG, "write to outputstream success,socket_four");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

