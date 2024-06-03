package com.example.esp32test;

import android.util.Log;

public class MessageListener implements ESP32Communicator.OnMessageReceivedListener {

    @Override
    public void onMessageReceived(String message) {
        Log.i("MainActivity", "Received message from ESP32: " + message);
        // Handle the received message here (e.g., update UI)
    }
}

