package com.example.esp32test;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;


public class ESP32Communicator {


    private static String ESP32_IP = ""; // Replace with your ESP32 IP address [192.168.100.7]
    private static final int PORT = 80;
    private Socket socket;
    private BufferedReader reader;
    public boolean isConnected = false;  // Flag to track connection status

    //set esp 32 retreived ip address
    public void setEspIpAddress (String esp_ip_address) {
        ESP32_IP = esp_ip_address;
    }
    //

    public interface OnConnectListener {
        void onConnected();
    }

    public interface OnMessageReceivedListener {
        void onMessageReceived(String message);
    }

    public void connect(final OnConnectListener listener) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (!isConnected) { // Loop until connection is established
                    try {
                        socket = new Socket();
                        socket.connect(new InetSocketAddress(ESP32_IP, PORT));
                        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        isConnected = true;
                        if (listener != null) {
                            listener.onConnected();
                        }
                        startListening();  // Start listening for messages after successful connection
                        break;
                    } catch (IOException e) {
                        Log.e("ESP32Communicator", "Error connecting: " + e.getMessage());
                        try {
                            Thread.sleep(2000); // Wait for 2 seconds before retrying
                        } catch (InterruptedException e1) {
                            Log.e("ESP32Communicator", "Error sleeping thread: " + e1.getMessage());
                        }
                    }
                }
            }
        }).start();
    }

    public void sendData(final String data) throws IOException {
        if (isConnected) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        OutputStream outputStream = socket.getOutputStream();
                        outputStream.write(data.getBytes());
                        outputStream.flush();
                    } catch (IOException e) {
                        Log.e("ESP32Communicator", "Error sending data: " + e.getMessage());
                    }
                }
            }).start();
        } else {
            Log.w("ESP32Communicator", "ESP32 not connected. Please reconnect.");
        }
    }

    private void startListening() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (isConnected) {
                    try {
                        String line = reader.readLine();
                        if (line != null && !line.isEmpty()) {
                            OnMessageReceivedListener listener = getOnMessageReceivedListener();
                            if (listener != null) {
                                listener.onMessageReceived(line);
                            }
                        }
                    } catch (IOException e) {
                        Log.e("Listener", "Error reading data: " + e.getMessage());
                        isConnected = false;  // Set flag to indicate connection loss
                    }
                }
            }
        }).start();
    }

    public void stopListening() {
        Log.d("stopListening()", "called");

        isConnected = false;
        if (reader != null) {
            Log.d("reader", "before try");

            try {
                Log.d("reader", "closed");

                reader.close();
            } catch (IOException e) {
                Log.e("Listener", "Error closing reader: " + e.getMessage());
            }
        }
        if (socket != null) {
            Log.d("socket", "null");

            try {
                Log.d("socket", "closed");

                socket.close();
            } catch (IOException e) {
                Log.e("Listener", "Error closing socket: " + e.getMessage());
            }
        }
    }

    public boolean isConnected() {  // Public getter method for connection status
        return isConnected;
    }

    private OnMessageReceivedListener onMessageReceivedListener;

    public void setOnMessageReceivedListener(OnMessageReceivedListener listener) {
        this.onMessageReceivedListener = listener;
    }

    private OnMessageReceivedListener getOnMessageReceivedListener() {
        return onMessageReceivedListener;
    }

    // Added a method to close resources (consider calling from a separate method in MainActivity)
    public void close() {
        stopListening();
    }
}


/*public class ESP32Communicator {

    private static final String ESP32_IP = "192.168.100.6"; // Replace with your ESP32 IP address
    private static final int PORT = 80;
    private Socket socket;
    private BufferedReader reader;
    private boolean isConnected = false;  // Flag to track connection status

    public void setOnConnectListener(OnConnectListener listener) {
        this.onMessageReceivedListener = (OnMessageReceivedListener) listener;
    }

    public interface OnConnectListener {
        void onConnected();
    }

    public interface OnMessageReceivedListener {
        void onMessageReceived(String message);
    }

    public interface OnDisconnectedListener {  // Added interface for disconnection notification
        void onDisconnected();
    }

    public void connect(final OnConnectListener listener) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (!isConnected) { // Loop until connection is established
                    try {
                        socket = new Socket();
                        socket.connect(new InetSocketAddress(ESP32_IP, PORT));
                        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        isConnected = true;
                        if (listener != null) {
                            listener.onConnected();
                        }
                        startListening();  // Start listening for messages after successful connection
                        break;
                    } catch (IOException e) {
                        Log.e("ESP32Communicator", "Error connecting: " + e.getMessage());
                        try {
                            Thread.sleep(2000); // Wait 2 seconds before retrying
                        } catch (InterruptedException e1) {
                            Log.e("ESP32Communicator", "Error sleeping thread: " + e1.getMessage());
                        }
                    }
                }
            }
        }).start();
    }

    public void sendData(final String data) throws IOException {
        if (isConnected) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        OutputStream outputStream = socket.getOutputStream();
                        outputStream.write(data.getBytes());
                        outputStream.flush();
                    } catch (IOException e) {
                        Log.e("ESP32Communicator", "Error sending data: " + e.getMessage());
                    }
                }
            }).start();
        } else {
            Log.w("ESP32Communicator", "ESP32 not connected. Please reconnect.");
        }
    }

    private void startListening() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (isConnected) {
                    try {
                        String line = reader.readLine();
                        if (line != null && !line.isEmpty()) {
                            OnMessageReceivedListener listener = getOnMessageReceivedListener();
                            if (listener != null) {
                                listener.onMessageReceived(line);
                            }
                        }
                    } catch (IOException e) {
                        Log.e("Listener", "Error reading data: " + e.getMessage());
                        isConnected = false;  // Set flag to indicate connection loss
                        OnDisconnectedListener listener = getOnDisconnectedListener();  // Notify about disconnection
                        if (listener != null) {
                            listener.onDisconnected();
                        }
                    }
                }
            }
        }).start();
    }

    public void stopListening() {
        isConnected = false;
        if (reader != null) {
            try {
                reader.close();
            } catch (IOException e) {
                Log.e("Listener", "Error closing reader: " + e.getMessage());
            }
        }
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                Log.e("Listener", "Error closing socket: " + e.getMessage());
            }
        }
    }

    public boolean isConnected() {
        return isConnected;
    }

    private OnMessageReceivedListener onMessageReceivedListener;
    private OnDisconnectedListener onDisconnectedListener;

    public void setOnMessageReceivedListener(OnMessageReceivedListener listener) {
        this.onMessageReceivedListener = listener;
    }

    public void setOnDisconnectedListener(OnDisconnectedListener listener) {
        this.onDisconnectedListener = listener;
    }

    private OnMessageReceivedListener getOnMessageReceivedListener() {
        return onMessageReceivedListener;
    }

    private OnDisconnectedListener getOnDisconnectedListener() {
        return onDisconnectedListener;
    }
}*/


/*

public class ESP32Communicator {

    private static final String ESP32_IP = "192.168.100.6"; // Replace with your ESP32 IP address
    private static final int PORT = 80;
    private Socket socket;
    private BufferedReader reader;
    private boolean isConnected = false;  // Flag to track connection status

    public interface OnConnectListener {
        void onConnected();
    }

    public interface OnMessageReceivedListener {
        void onMessageReceived(String message);
    }

    public void connect(final OnConnectListener listener) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (!isConnected) { // Loop until connection is established
                    try {
                        socket = new Socket();
                        socket.connect(new InetSocketAddress(ESP32_IP, PORT));
                        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        isConnected = true;
                        if (listener != null) {
                            listener.onConnected();
                        }
                        startListening();  // Start listening for messages after successful connection
                        break;
                    } catch (IOException e) {
                        Log.e("ESP32Communicator", "Error connecting: " + e.getMessage());
                        try {
                            Thread.sleep(2000); // Wait for 2 seconds before retrying
                        } catch (InterruptedException e1) {
                            Log.e("ESP32Communicator", "Error sleeping thread: " + e1.getMessage());
                        }
                    }
                }
            }
        }).start();
    }

    public void sendData(final String data) throws IOException {
        if (isConnected) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        OutputStream outputStream = socket.getOutputStream();
                        outputStream.write(data.getBytes());
                        outputStream.flush();
                    } catch (IOException e) {
                        Log.e("ESP32Communicator", "Error sending data: " + e.getMessage());
                    }
                }
            }).start();
        } else {
            Log.w("ESP32Communicator", "ESP32 not connected. Please reconnect.");
        }
    }

    private void startListening() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (isConnected) {
                    try {
                        String line = reader.readLine();
                        if (line != null && !line.isEmpty()) {
                            OnMessageReceivedListener listener = getOnMessageReceivedListener();
                            if (listener != null) {
                                listener.onMessageReceived(line);
                            }
                        }
                    } catch (IOException e) {
                        Log.e("Listener", "Error reading data: " + e.getMessage());
                        isConnected = false;  // Set flag to indicate connection loss
                    }
                }
            }
        }).start();
    }

    public void stopListening() {
        isConnected = false;
        if (reader != null) {
            try {
                reader.close();
            } catch (IOException e) {
                Log.e("Listener", "Error closing reader: " + e.getMessage());
            }
        }
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                Log.e("Listener", "Error closing socket: " + e.getMessage());
            }
        }
    }

    public boolean isConnected() {  // Public getter method for connection status
        return isConnected;
    }

    private OnMessageReceivedListener onMessageReceivedListener;

    public void setOnMessageReceivedListener(OnMessageReceivedListener listener) {
        this.onMessageReceivedListener = listener;
    }

    private OnMessageReceivedListener getOnMessageReceivedListener() {
        return onMessageReceivedListener;
    }

    // Added a method to close resources (consider calling from a separate method in MainActivity)
    public void close() {
        stopListening();
    }
}

*/




/*public class ESP32Communicator {

    private static final String ESP32_IP = "192.168.100.6"; // Replace with your ESP32 IP address
    private static final int PORT = 80;
    private Socket socket;
    private BufferedReader reader;
    private boolean isListening = true;

    private boolean isConnected = false;  // Flag to track connection status

    public void connectAndSendData(final String data, final OnConnectListener listener) throws IOException {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    socket = new Socket();
                    socket.connect(new InetSocketAddress(ESP32_IP, PORT));
                    reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    if (listener != null) {
                        listener.onConnected();
                    }

                    OutputStream outputStream = socket.getOutputStream();
                    outputStream.write(data.getBytes());
                    outputStream.flush();
                } catch (IOException e) {
                    Log.e("ESP32Communicator", "Error connecting or sending data: " + e.getMessage());
                } finally {
                    // Close resources regardless of success or failure
                    close();
                }
            }
        }).start();
    }

    public void startListening(final OnMessageReceivedListener listener) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (isListening) {
                    try {
                        String line = reader.readLine();
                        if (line != null && !line.isEmpty()) {
                            listener.onMessageReceived(line);
                        }
                    } catch (IOException e) {
                        Log.e("Listener", "Error reading data: " + e.getMessage());
                    }
                }
            }
        }).start();
    }

    private void close() {
        // Implement logic to close socket and reader (if applicable)
        if (reader != null) {
            try {
                reader.close();
            } catch (IOException e) {
                Log.e("ESP32Communicator", "Error closing reader: " + e.getMessage());
            }
        }
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                Log.e("ESP32Communicator", "Error closing socket: " + e.getMessage());
            }
        }
    }

    public interface OnConnectListener {
        void onConnected();
    }

    public void stopListening() {
        isListening = false;
        if (reader != null) {
            try {
                reader.close();
            } catch (IOException e) {
                Log.e("Listener", "Error closing reader: " + e.getMessage());
            }
        }
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                Log.e("Listener", "Error closing socket: " + e.getMessage());
            }
        }
    }

    public interface OnMessageReceivedListener {
        void onMessageReceived(String message);
    }

    public Socket getSocket() {
        return socket;
    }

    public boolean isConnected() {
        return isConnected;
    }


    public void connect(final OnConnectListener listener) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (!isConnected) { // Loop until connection is established
                    try {
                        socket = new Socket();
                        socket.connect(new InetSocketAddress(ESP32_IP, PORT));
                        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        isConnected = true;
                        if (listener != null) {
                            listener.onConnected();
                        }
                    } catch (IOException e) {
                        Log.e("ESP32Communicator", "Error connecting: " + e.getMessage());
                        try {
                            Thread.sleep(2000); // Wait for 2 seconds before retrying
                        } catch (InterruptedException e1) {
                            Log.e("ESP32Communicator", "Error sleeping thread: " + e1.getMessage());
                        }
                    }
                }
            }
        }).start();
    }

    public void sendData(final String data) throws IOException {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    OutputStream outputStream = socket.getOutputStream();
                    outputStream.write(data.getBytes());
                    outputStream.flush();
                } catch (IOException e) {
                    Log.e("ESP32Communicator", "Error sending data: " + e.getMessage());
                }
            }
        }).start();
    }

}
*/
