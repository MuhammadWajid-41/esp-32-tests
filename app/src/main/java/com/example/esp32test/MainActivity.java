package com.example.esp32test;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

public class MainActivity extends AppCompatActivity {

    private Button send_command;

    private Handler handler = new Handler(Looper.getMainLooper()); // Handler for UI updates

    private TextView continous_receving_data, connection_status;
    private final Handler handler_for_bg_operations = new Handler();
    private Socket socket; // Declare socket as a member variable

    private BufferedReader reader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activitymain);

        send_command = findViewById(R.id.send_data_button);

        continous_receving_data = findViewById(R.id.continous_data);
        connection_status = findViewById(R.id.connected);

        // Establish connection in the main thread (optional: move to background if needed)
        attemptConnectionInBackground();

        // Button click listener for sending data
        send_command.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                if (isConnected())
                {
                    new SendDataTask().execute("Hi! from Android to ESP32"); // Replace with your message
                }
                else
                {
                    connect_to_socket();
                    Log.w("MainActivity", "Socket not connected, cannot send data");
                    // Optionally, handle reconnection or notify user
                }
            }
        });
    }

    private void attemptConnectionInBackground() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (!isConnected()) {
                    connect_to_socket(); // Call the connection method

                    // Implement retry logic with delay
                    try {
                        Thread.sleep(2000); // Wait for 2 seconds before retrying
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        Log.e("MainActivity", "Error during sleep: " + e.getMessage());
                    }
                }

                // Notify main thread about successful connection (optional)
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        // Update UI to indicate connection success (e.g., TextView)
                        connection_status.setText("Connected!"); // Assuming you have a TextView for this purpose
                    }
                });
            }
        }).start();
    }


    private void connect_to_socket()
    {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    socket = new Socket();
                    //socket.connect(new InetSocketAddress("192.168.4.1", 80), 5000); // Connect with timeout
                    socket.connect(new InetSocketAddress("192.168.4.1", 80)); // Connect without timeout

                    Log.i("MainActivity", "Connected to ESP32");

                    // Start a separate thread for receiving data
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            receiveData(socket);
                        }
                    }).start();

                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e("MainActivity", "Failed to connect to ESP32: " + e.getMessage());
                    // Optionally handle connection failure here (e.g., notify user)
                }
            }
        }).start();
    }



    private boolean isConnected() {
        return socket != null && socket.isConnected();
    }


    private void receiveData(Socket socket) {
        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            reader.lines().forEach(line -> {
                line = line.trim(); // Trim leading/trailing whitespace (optional)

                // Process the received data (you can use the line variable here)
                Log.d("Received data:", line);

                // Update the TextView on the UI thread using the Handler
                String finalLine = line;
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (!TextUtils.isEmpty(finalLine)) {

                            continous_receving_data.setText("Random Number: {"+finalLine+"}");  // Append data with newline
                        }
                    }
                });
            });

            reader.close();
            Log.i("MainActivity", "Connection closed by remote device");
            // Optionally, handle reconnection
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("MainActivity", "Error receiving data: " + e.getMessage());
            // Optionally handle exceptions (e.g., notify user, attempt reconnection)
        } finally {
            // Close the socket if necessary (consider implementing a clean disconnect logic)
            if (socket != null) {
                try {
                    socket.close();
                    Log.i("MainActivity", "Socket closed");
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e("MainActivity", "Error closing socket: " + e.getMessage());
                }
            }
        }
    }



    /*private void receiveData(Socket socket) {
        try {
            //BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            if(reader == null)
            {
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            }

            reader.lines().forEach(line ->
            {
                line = line.trim(); // Trim leading/trailing whitespace (optional)

                // Process the received data (you can use the line variable here)
                Log.d("Received data:", line);

                if (!TextUtils.isEmpty(line))
                {  // Check if line is not empty
                    continous_receving_data.setText(line);  // set data with newline
                }

                // You can also display the data using Toast or update a UI element using a Handler
            });

            reader.close();
            Log.i("MainActivity", "Connection closed by remote device");
            // Optionally, handle reconnection
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("MainActivity", "Error receiving data: " + e.getMessage());
            // Optionally handle exceptions (e.g., notify user, attempt reconnection)
        } finally {
            // Close the socket if necessary (consider implementing a clean disconnect logic)
            if (socket != null) {
                try {
                    socket.close();
                    Log.i("MainActivity", "Socket closed");
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e("MainActivity", "Error closing socket: " + e.getMessage());
                }
            }
        }
    }*/

    public class SendDataTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... messages) {
            String message = messages[0];

            if (isConnected()) {
                try {
                    OutputStream outputStream = socket.getOutputStream();
                    outputStream.write(message.getBytes());
                    outputStream.flush();  // Ensure data is sent immediately
                    Log.i("MainActivity", "Data sent to ESP32: " + message);
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e("MainActivity", "Error sending data: " + e.getMessage());
                }
            } else {
                Log.w("MainActivity", "Socket not connected, cannot send data");
            }

            return null;
        }
    }
}



/*package com.example.esp32test; not full duplex working | only 1 way working

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;


public class MainActivity extends AppCompatActivity
{

    private Button send_command;
    private final Handler handler = new Handler();

    private Socket socket;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activitymain);


        //========================================================================================//sending...

        send_command = findViewById(R.id.send_data_button);

        send_command.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                // Send message when button is clicked
                new SendDataTask().execute("1");
            }
        });

        //========================================================================================//end


        //{receive random numbers from esp32}
        //new ConnectTask().execute();


    } //oncreate end

    public class SendDataTask extends AsyncTask<String, Void, Void>
    {

        @Override
        protected Void doInBackground(String... messages) {
            String message = messages[0];
            try {
                if(socket == null || !socket.isConnected())
                {
                    socket = new Socket(); // Create the socket in the background thread
                    socket.connect(new InetSocketAddress("192.168.4.1", 80));
                    OutputStream outputStream = socket.getOutputStream();
                    outputStream.write(message.getBytes());
                    outputStream.close();
                    socket.close();
                }
                else {
                    OutputStream outputStream = socket.getOutputStream();
                    outputStream.write(message.getBytes());
                    outputStream.close();
                    socket.close();
                }

            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid)
        {
            super.onPostExecute(aVoid);
            // Handle successful or failed sending (e.g., Toast message)
        }
    }

    //receive data
    private class ConnectTask extends AsyncTask<Void, Void, Socket>
    {

        @Override
        protected Socket doInBackground(Void... voids)
        {
            try
            {
                Socket socket = new Socket();
                socket.bind(null);
                socket.connect((new InetSocketAddress("192.168.4.1", 80)));
                return socket;
            }
            catch (IOException e)
            {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(Socket socket)
        {
            if (socket != null)
            {
                // Socket connection successful, proceed with data receiving in another thread
                receiveData(socket);
            }
            else
            {
                // Handle connection failure
                //Toast.makeText(MainActivity.this, "Failed to connect to ESP32", Toast.LENGTH_SHORT).show();
                Log.d("Failed to connect to ESP32","failed");
            }
        }
    }


    private void receiveData(Socket socket) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    //String line = "";

                    //line = line + reader.lines().toString().trim() + "\n";

                    //Log.d("Received data:", line);

                    reader.lines().forEach(line ->
                    {
                        line = line.trim(); // Trim leading/trailing whitespace (optional)

                        // Process the received data (you can use the line variable here)
                        Log.d("Received data:", line);

                        // You can also display the data using Toast or update a UI element using a Handler
                    });

                    reader.close();
                    Log.d("Connection closed", "Socket connection closed");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
    //


} //onmain end


*/



/*working send 1
package com.example.esp32test;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;


public class MainActivity extends AppCompatActivity
{

    private Button send_command;
    private final Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activitymain);


        //========================================================================================//sending...

        send_command = findViewById(R.id.send_data_button);

        send_command.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                // Send message when button is clicked
                new SendDataTask().execute("1");
            }
        });

        //========================================================================================//end


        //{receive random numbers from esp32}
        new ConnectTask().execute();


    } //oncreate end

    public class SendDataTask extends AsyncTask<String, Void, Void>
    {

        @Override
        protected Void doInBackground(String... messages) {
            String message = messages[0];
            try {
                Socket socket = new Socket(); // Create the socket in the background thread
                socket.connect(new InetSocketAddress("192.168.4.1", 80));
                OutputStream outputStream = socket.getOutputStream();
                outputStream.write(message.getBytes());
                outputStream.close();
                socket.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid)
        {
            super.onPostExecute(aVoid);
            // Handle successful or failed sending (e.g., Toast message)
        }
    }

} //onmain end

*/



//SEND COMMAND TO ESP 32 SEND FUNCTION ONLY NEEDS TO BE TESTED
/*
package com.example.esp32test;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;


public class MainActivity extends AppCompatActivity
{

    private Button send_command;
    private final Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activitymain);


        //========================================================================================//sending...

        send_command = findViewById(R.id.send_data_button);

        send_command.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                // Send message when button is clicked
                new SendDataTask().execute("1");
            }
        });

        //========================================================================================//end


        //{receive random numbers from esp32}
        //new ConnectTask().execute();


    } //oncreate end

    public class SendDataTask extends AsyncTask<String, Void, Void>
    {

        @Override
        protected Void doInBackground(String... messages) {
            String message = messages[0];
            try {
                Socket socket = new Socket(); // Create the socket in the background thread
                socket.connect(new InetSocketAddress("192.168.4.1", 80));
                OutputStream outputStream = socket.getOutputStream();
                outputStream.write(message.getBytes());
                outputStream.close();
                socket.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid)
        {
            super.onPostExecute(aVoid);
            // Handle successful or failed sending (e.g., Toast message)
        }
    }







} //onmain end
*/




//===========================================RECIEVE CONTINUOUS FYNCTION()=============================//
/*private void receiveData(Socket socket)
{
    new Thread(new Runnable()
    {
        @Override
        public void run()
        {
            try
            {
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                //String line = "";

                //line = line + reader.lines().toString().trim() + "\n";

                //Log.d("Received data:", line);

                reader.lines().forEach(line -> {
                    line = line.trim(); // Trim leading/trailing whitespace (optional)

                    // Process the received data (you can use the line variable here)
                    Log.d("Received data:", line);

                    // You can also display the data using Toast or update a UI element using a Handler
                });

                reader.close();
                Log.d("Connection closed", "Socket connection closed");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }).start();
}*/
//=============================================END======================================================//


/*
package com.example.esp32test;


import static android.text.format.Formatter.formatIpAddress;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;

public class MainActivity extends AppCompatActivity implements ESP32Communicator.OnConnectListener,
        ESP32Communicator.OnMessageReceivedListener
{

    private EditText ssidEditText;
    private EditText passwordEditText;
    private TextView ipAddressTextView;

    private Button connect_btn, send_message_button;

    private BroadcastReceiver wifiReceiver;
    private ESP32Communicator communicator;

    private String esp32IpAddress="";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activitymain);

        ssidEditText = findViewById(R.id.editTextSSID);
        passwordEditText = findViewById(R.id.editTextPassword);
        ipAddressTextView = findViewById(R.id.ip_address);


        send_message_button = findViewById(R.id.send_data_button);
        connect_btn = findViewById(R.id.connect_button);

        connect_btn.setOnClickListener(v -> sendCredentials());


            //============

        communicator = new ESP32Communicator();
        communicator.setOnMessageReceivedListener(this);  // Set listener for received messages

        if (!communicator.isConnected()) {
            communicator.connect(MainActivity.this);  // Pass 'this' as the listener
        }


        send_message_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("isConnected value in btn: ", String.valueOf(communicator.isConnected));

                if (!communicator.isConnected()) {
                    communicator.connect(MainActivity.this);  // Pass 'this' as the listener
                    //Log.d("isConnected value: ", String.valueOf(communicator.isConnected));
                } else {
                    try {
                        communicator.sendData("Change AC temperature to 20"); // Modify message as needed
                    } catch (IOException e) {
                        Log.e("MainActivity", "Error sending data: " + e.getMessage());
                    }
                }
            }
        });

        wifiReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                    checkWifiConnection();
                }
            }
        };

        registerReceiver(wifiReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));


        //============

    } //onCreate end

    private void sendCredentials()
    {
        Log.d("inside", "sendCredentials");

        String ssid = ssidEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (ssid.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter SSID and password", Toast.LENGTH_SHORT).show();
            return;
        }

        new SendCredentialsTask(ssid, password).execute();
    }

    private class SendCredentialsTask extends AsyncTask<Void, Void, String>
    {

        private final String ssid;
        private final String password;

        public SendCredentialsTask(String ssid, String password) {
            this.ssid = ssid;
            this.password = password;
        }

        @Override
        protected String doInBackground(Void... voids)
        {
            Log.d("inside", "doInBackground");
            try
            {
                Log.d("inside", "doInBackground try...");

                // Get ESP32 P2P IP address (replace with your discovery method)
                esp32IpAddress = getEsp32IpAddress();  // Use existing method from previous example

                communicator.setEspIpAddress(esp32IpAddress); //send esp 32 ip address

                ipAddressTextView.setText("esp 32 ip [fetched] address : "+esp32IpAddress);

                // Connect to ESP32 server
                Socket socket = new Socket(InetAddress.getByName(esp32IpAddress), 8080);

                // Send SSID and password
                String credentials = ssid + "," + password;
                socket.getOutputStream().write(credentials.getBytes());

                // Receive ESP32 IP address (optional, based on your broadcasting implementation)
                byte[] buffer = new byte[1024];
                int bytesRead = socket.getInputStream().read(buffer);
                if (bytesRead > 0)
                {
                    Log.d("inside", "if (bytesRead > 0) ");

                    String receivedMessage = new String(buffer, 0, bytesRead);
                    return receivedMessage.substring(7); // Extract IP after "My IP: "
                }

                socket.close();
                return null;
            }
            catch (IOException e)
            {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String ipAddress)
        {
            Log.d("inside", "onPostExecute()");

            if (ipAddress != null)
            {
                Log.d("inside", "if (ipAddress != null)");

                ipAddressTextView.setText(ipAddress);
                Toast.makeText(MainActivity.this, "ESP32 connected and IP received", Toast.LENGTH_SHORT).show();
            }
            else
            {
                Log.d("inside", "if (ipAddress == null)");
                Toast.makeText(MainActivity.this, "Failed to send credentials or receive IP", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private String getEsp32IpAddress()
    {
        Log.d("inside", "getEsp32IpAddress()");
        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();

        if (dhcpInfo != null)
        {
            Log.d("inside", "if (dhcpInfo != null)");

            // Check if connected on P2P group (assuming ESP32 creates a P2P group)
            if (wifiManager.getConnectionInfo().getIpAddress() != 0)
            {
                // Get the ESP32's IP address (assuming it's the gateway in P2P mode)
                int gateway = dhcpInfo.gateway;
                Log.d("formatIpAddress(gateway)", formatIpAddress(gateway).toString().trim());
                return formatIpAddress(gateway);

            }
            else
            {
                Log.d("IP Address", "Not connected to ESP32 P2P group");
                return null; // Indicate ESP32 P2P not found
            }
        } else {
            Log.d("IP Address", "Failed to get DHCP information");
            return null; // Handle case where DHCP info is unavailable
        }
    }

    //------------------------------ESP32 methods------------------------------//

    private void checkWifiConnection() {
        Log.d("checkWifiConnection = ", "Checking...");
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            // WiFi is  re - connection...
            if (!communicator.isConnected()) {
                communicator.connect(MainActivity.this);
                Log.d("wifi-reconnection isConnected = ", "True");
            }
        } else {
            // WiFi is disconnected
            communicator.isConnected = false; // Update isConnected variable
            Log.d("isConnected = ", "False");
            communicator.close();

            // Handle WiFi disconnection (optional: display message, disable send button)
        }
    }


    @Override
    public void onConnected() {
        Log.i("MainActivity", "Connected to ESP32");
    }

    @Override
    public void onMessageReceived(String message) {
        Log.i("MainActivity", "Received message from ESP32: " + message);
        // Update UI or handle received message as needed
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(wifiReceiver); // Unregister receiver when activity is destroyed
        communicator.close();  // Ensure resources are closed when activity is destroyed
    }

    //------------------------------------------------------------------------//

}//onMain end

*/


/* fetch ip working
import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity
{

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activitymain);

       String ip_address = getEsp32IpAddress(this);
        Log.d("ip address is: ", ip_address);
    }
    public String getEsp32IpAddress (Context context)
    {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();
        if (dhcpInfo != null)
        {
            // Get the gateway IP (usually the ESP32's IP in AP mode)
            int gateway = dhcpInfo.gateway;
            return formatIpAddress(gateway);
        }
        else
        {
            return null; // Handle case where DHCP info is unavailable
        }
    }
    private String formatIpAddress ( int ipAddress)
    {
        return (ipAddress & 0xFF) + "." + ((ipAddress >> 8) & 0xFF) + "." + ((ipAddress >> 16) & 0xFF) + "." + ((ipAddress >> 24) & 0xFF);
    }
}
fetch ip working... */

/*
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class MainActivity extends AppCompatActivity implements ESP32Communicator.OnConnectListener,
        ESP32Communicator.OnMessageReceivedListener {

    private static final String TAG = "MainActivity";
    private static final int SERVER_PORT = 80; // Replace with your ESP32 server port (if different)
    private static final long SCAN_DELAY_MS = 2000; // Delay between IP address scan attempts

    private String routerSSID;
    private String routerPassword; // Not used in this code, but provided for completeness
    private EditText ipAddressEditText;
    private Button scanButton;
    private Button sendButton;
    private BroadcastReceiver wifiReceiver;
    private ESP32Communicator communicator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activitymain);

        // Assuming you have these values from user input or elsewhere
        routerSSID = "Majid"; // Replace with your router's SSID
        routerPassword = "Pakistan007"; // Not used in this code, for completeness

        ipAddressEditText = findViewById(R.id.ip_address_edit_text); // Replace with your EditText ID from layout
        scanButton = findViewById(R.id.scan_button); // Replace with your Button ID from layout
        sendButton = findViewById(R.id.send_button); // Replace with your Button ID from layout

        communicator = new ESP32Communicator();
        communicator.setOnMessageReceivedListener(this);

        // Disable buttons initially
        sendButton.setEnabled(false);

        wifiReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                    checkWifiConnection();
                }
            }
        };

        registerReceiver(wifiReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    private void checkWifiConnection() {
        Log.d(TAG, "Checking WiFi connection...");
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        if (networkInfo != null && networkInfo.isConnected()) {
            // WiFi is connected, check if connected to the router SSID
            WifiInfo wifiInfo = ((WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE)).getConnectionInfo();
            String connectedSSID = wifiInfo.getSSID();
            Log.d("wifi ssid: ", connectedSSID);
            if (connectedSSID != null && connectedSSID.equals(routerSSID)) {
                // Connected to the router, enable scan button for IP discovery
                scanButton.setEnabled(true);
            } else {
                // Not connected to the router, disable buttons and display message
                scanButton.setEnabled(false);
                sendButton.setEnabled(false);
                ipAddressEditText.setText("");
                Toast.makeText(getApplicationContext(), "Please connect to your router WiFi: " + routerSSID, Toast.LENGTH_SHORT).show();
            }
        } else {
            // WiFi is disconnected, disable buttons and display message
            scanButton.setEnabled(false);
            sendButton.setEnabled(false);
            ipAddressEditText.setText("");
            Toast.makeText(getApplicationContext(), "Please connect to WiFi", Toast.LENGTH_SHORT).show();
// ... existing code from previous part ...

        }
    }

    public void scanForESP32(View view) {
        // Disable scan button to prevent multiple clicks
        scanButton.setEnabled(false);

        // Start a new thread for IP address scanning
        new Thread(new Runnable() {
            @Override
            public void run() {
                String discoveredIP = null;
                for (int subnetAddress = 1; subnetAddress <= 254; subnetAddress++) {
                    String ipAddress = routerSSID.substring(0, routerSSID.lastIndexOf(".")) + "." + subnetAddress;
                    if (isESP32Reachable(ipAddress)) {
                        discoveredIP = ipAddress;
                        break; // Stop scanning if ESP32 found
                    }
                    try {
                        Thread.sleep(SCAN_DELAY_MS); // Delay between scan attempts
                    } catch (InterruptedException e) {
                        Log.e(TAG, "Scan interrupted: " + e.getMessage());
                        break;
                    }
                }

                // Update UI on the main thread
                String finalDiscoveredIP = discoveredIP;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (finalDiscoveredIP != null) {
                            ipAddressEditText.setText(finalDiscoveredIP);
                            sendButton.setEnabled(true); // Enable send button if IP found
                        } else {
                            Toast.makeText(getApplicationContext(), "ESP32 not found on network", Toast.LENGTH_SHORT).show();
                            scanButton.setEnabled(true); // Re-enable scan button for retries
                        }
                    }
                });
            }
        }).start();
    }

    private boolean isESP32Reachable(String ipAddress) {
        try {
            InetAddress inetAddress = InetAddress.getByName(ipAddress);
            return inetAddress.isReachable(1000); // Check reachability with timeout
        } catch (IOException e) {
            Log.d(TAG, "Error checking reachability of " + ipAddress + ": " + e.getMessage());
            return false;
        }
    }

    public void sendMessage(View view) {
        String ipAddress = ipAddressEditText.getText().toString().trim();
        if (!ipAddress.isEmpty()) {
            communicator.connect(this);
            // ... rest of your code for sending messages to the ESP32 ...
        } else {
            Toast.makeText(getApplicationContext(), "Please enter or scan for ESP32 IP address", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onConnected() {
        Log.i("MainActivity", "Connected to ESP32");
    }

    @Override
    public void onMessageReceived(String message) {
        Log.i("MainActivity", "Received message from ESP32: " + message);
        // Update UI or handle received message as needed
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(wifiReceiver); // Unregister receiver when activity is destroyed
        communicator.close();  // Ensure resources are closed when activity is destroyed
    }

// ... other methods for your ESP32 communicator logic (if applicable) ...
}
not working*/


/*working=============================================================================================DONE
package com.example.esp32test;//Code to send "command" to an ESP32 receiving device via WiFi

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;

public class MainActivity extends AppCompatActivity implements ESP32Communicator.OnConnectListener,
        ESP32Communicator.OnMessageReceivedListener
{

    private BroadcastReceiver wifiReceiver;
    private ESP32Communicator communicator;
    private Button sendButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activitymain);

        sendButton = findViewById(R.id.send_button); // Replace with your button ID from your layout file

        communicator = new ESP32Communicator();
        communicator.setOnMessageReceivedListener(this);  // Set listener for received messages

        if (!communicator.isConnected()) {
            communicator.connect(MainActivity.this);  // Pass 'this' as the listener
        }


        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("isConnected value in btn: ", String.valueOf(communicator.isConnected));

                if (!communicator.isConnected()) {
                    communicator.connect(MainActivity.this);  // Pass 'this' as the listener
                    //Log.d("isConnected value: ", String.valueOf(communicator.isConnected));
                } else {
                    try {
                        communicator.sendData("Change AC temperature to 20"); // Modify message as needed
                    } catch (IOException e) {
                        Log.e("MainActivity", "Error sending data: " + e.getMessage());
                    }
                }
            }
        });

        wifiReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                    checkWifiConnection();
                }
            }
        };

        registerReceiver(wifiReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

    }//onCreate end

    private void checkWifiConnection() {
        Log.d("checkWifiConnection = ", "Checking...");
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            // WiFi is  re - connection...
            if (!communicator.isConnected()) {
                communicator.connect(MainActivity.this);
                Log.d("wifi-reconnection isConnected = ", "True");
            }
        } else {
            // WiFi is disconnected
            communicator.isConnected = false; // Update isConnected variable
            Log.d("isConnected = ", "False");
            communicator.close();

            // Handle WiFi disconnection (optional: display message, disable send button)
        }
    }


    @Override
    public void onConnected() {
        Log.i("MainActivity", "Connected to ESP32");
    }

    @Override
    public void onMessageReceived(String message) {
        Log.i("MainActivity", "Received message from ESP32: " + message);
        // Update UI or handle received message as needed
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(wifiReceiver); // Unregister receiver when activity is destroyed
        communicator.close();  // Ensure resources are closed when activity is destroyed
    }
}
*/



/*import static androidx.core.content.ContextCompat.getSystemService;

import android.app.Activity;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;

import android.content.Context;

public class MainActivity extends Activity {

    private Button sendButton;
    private ESP32Communicator communicator;

    private MessageListener messageListener; // New listener variable

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activitymain); // Replace with your layout resource ID

        sendButton = findViewById(R.id.send_button); // Replace with your button ID
        communicator = new ESP32Communicator();

        // Create message listener
        messageListener = new MessageListener();

        // Set OnConnectListener (optional)
        communicator.setOnConnectListener(new ESP32Communicator.OnConnectListener() {
            @Override
            public void onConnected() {
                Log.i("MainActivity", "Connected to ESP32");
            }
        });

        // Set OnMessageReceivedListener using the created listener
        communicator.setOnMessageReceivedListener(messageListener);

        //communicator.setOnMessageReceivedListener(message -> Log.i("MainActivity", "Received message from ESP32: " + message)); //lambda function only API>24


        // Set OnDisconnectedListener (optional)
        communicator.setOnDisconnectedListener(new ESP32Communicator.OnDisconnectedListener() {
            @Override
            public void onDisconnected() {
                Log.w("MainActivity", "Disconnected from ESP32");
            }
        });

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!communicator.isConnected()) {
                    if (isWifiConnected()) {
                        communicator.connect(null); // Consider passing a listener if needed
                    } else {
                        Log.w("MainActivity", "WiFi not connected. Please connect and try again.");
                    }
                } else {
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                communicator.sendData("Change AC temperature to 20"); // Modify message as needed
                            } catch (IOException e) {
                                Log.e("MainActivity", "Error sending data: " + e.getMessage());
                            }
                        }
                    }, 100); // Delay by 100 milliseconds (adjust as needed)
                }
            }
        });
    }

    // Implement a method to check WiFi connectivity using libraries like ConnectivityManager
    private boolean isWifiConnected() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }
}*/



/*public class MainActivity extends AppCompatActivity implements ESP32Communicator.OnConnectListener,
        ESP32Communicator.OnMessageReceivedListener {

    private ESP32Communicator communicator;
    private Button sendButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activitymain);

        sendButton = findViewById(R.id.send_button); // Replace with your button ID from your layout file

        communicator = new ESP32Communicator();
        communicator.setOnMessageReceivedListener(this);  // Set listener for received messages

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!communicator.isConnected()) {
                    communicator.connect(MainActivity.this);  // Pass 'this' as the listener
                } else {
                    try {
                        communicator.sendData("Change AC temperature to 20"); // Modify message as needed
                    } catch (IOException e) {
                        Log.e("MainActivity", "Error sending data: " + e.getMessage());
                    }
                }
            }
        });
    }

    @Override
    public void onConnected() {
        Log.i("MainActivity", "Connected to ESP32");
    }

    @Override
    public void onMessageReceived(String message) {
        Log.i("MainActivity", "Received message from ESP32: " + message);
        // Update UI or handle received message as needed
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        communicator.close();  // Ensure resources are closed when activity is destroyed
    }
}
working but button double tap -> introduce small delay solution*/

/*public class MainActivity extends AppCompatActivity implements ESP32Communicator.OnMessageReceivedListener
{

    private ESP32Communicator communicator;

    private TextView sensorValueTextView;

    private Button send_btn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activitymain);

        sensorValueTextView = findViewById(R.id.sensor_value_text);
        send_btn = findViewById(R.id.send_button);

        // Connect to ESP32 in a background thread
        communicator = new ESP32Communicator();


        // Button click listener for sending commands
        send_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    communicator.connectAndSendData("Change AC temperature to 20", null);  // Modify message as needed
                } catch (IOException e) {
                    Log.e("MainActivity", "Error sending data: " + e.getMessage());
                }
            }
        });

    } //on create end


    @Override
    public void onMessageReceived(final String message) {
        Log.d("Received:", message);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                sensorValueTextView.setText(message);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        communicator.stopListening();
    }


} //main end

*/





/*package com.example.esp32test;//Code to send "1" to an ESP32 receiving device via WiFi 2nd method with json->string

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;


public class MainActivity extends AppCompatActivity {

    private Socket socket;

    private BufferedReader reader = null;
    Button send_message_Button;
    private boolean isListening = true; // Flag to control listening state
    private final String ESP32_IP = "192.168.100.6"; // Replace with your ESP32 IP address
    private final int PORT = 80; // Replace with your communication port

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activitymain);

        send_message_Button = (Button)  findViewById(R.id.send_button);

        // Button click listener or other trigger for sending data
        send_message_Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendData("Message from Android"); // Modify message as needed
            }
        });

        // Start listening thread in a background thread
        new Thread(new Runnable() {
            @Override
            public void run() {
                startListening();
            }
        }).start();
    }

    private void sendData(final String message) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Connect or reuse existing socket connection
                    if (socket == null || socket.isClosed()) {
                        socket = new Socket();
                        socket.bind(null);
                        socket.connect((new InetSocketAddress(ESP32_IP, PORT)), 1000);
                        //reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    }

                    Log.d("sendData(): ", "ok called");

                    OutputStream outputStream = socket.getOutputStream();
                    outputStream.write(message.getBytes());
                    outputStream.flush(); // Flush data without closing the socket

                } catch (IOException e) {
                    Log.e("Sender", "Error sending data: " + e.getMessage());
                }
            }
        }).start();
    }

    private void startListening() {
        try {
            // Connect or reuse existing socket connection
            if (socket == null || socket.isClosed()) {
                socket = new Socket();
                socket.bind(null);
                socket.connect((new InetSocketAddress(ESP32_IP, PORT)), 1000);
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            }

            //reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));


            Log.d("Listneing state in startListening(): ", String.valueOf(isListening));

            while (isListening) {
                String line = reader.readLine().toString().trim();
                if (line != null && !line.isEmpty()) {
                    // Process received data (update UI, perform actions)
                    Log.d("Received:", line);
                    // You can update UI elements using a Handler or runOnUiThread()
                }
            }

        } catch (IOException e) {
            Log.e("Listener", "Error reading data: " + e.getMessage());
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    Log.e("Listener", "Error closing reader: " + e.getMessage());
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isListening = false; // Set flag to stop listening and close resources (if needed)
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                Log.e("MainActivity", "Error closing socket: " + e.getMessage());
            }
        }
    }

    public void stopListening() {
        isListening = false; // Set flag to stop listening
    }


}

----------------------------------------------------------------------------*/


/*public class MainActivity extends AppCompatActivity {

    private Button sendButton;

    private boolean isListening = true; // Flag to control listening state
    private String line="";
    private final Handler handler = new Handler(); // Handler for displaying Toast messages

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activitymain);

        sendButton = findViewById(R.id.send_button);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Send message when button is clicked
                sendMessage("1");
            }
        });
    }

    private void sendMessage(final String message) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Socket socket = null;
                try {
                    socket = new Socket();
                    socket.bind(null);
                    socket.connect((new InetSocketAddress("192.168.100.6", 80)), 500);

                    OutputStream outputStream = socket.getOutputStream();
                    outputStream.write(message.getBytes());
                    //outputStream.flush(); // Flush data without closing the socket

                    // Receive data from the ESP32 (assuming receiveData handles socket closing)
                    BufferedReader reader = null;
                    try {
                        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        Log.d("in try", "trying2...");                        Log.d("in try", "trying2...");
                        Log.d("reader: ", reader.readLine().toString().trim());


                        // ... rest of your code using the reader object
                        //String line;
                        while (!(line = reader.readLine()).equals("")) {
                            // Display received data on the UI thread using Toast

                            Log.d("final_line", line.trim());
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    Log.d("Received: ", line.trim());
                                }
                            }, 100); // Delay for 100 milliseconds
                        }
                    } catch (IOException e) {
                        Log.e("in try", "Error creating BufferedReader: " + e.getMessage());
                        // Handle exception (e.g., retry connection)
                    } finally {
                        if (reader != null) {
                            try {
                                reader.close();
                            } catch (IOException e) {
                                Log.e("MainActivity", "Error closing reader: " + e.getMessage());
                            }
                        }

                        // **Move socket closing here**
                        if (socket != null) {
                            // Check for available data before closing (optional)
                            if (socket.getInputStream().available() == 0) {
                                socket.close();
                            } else {
                                // Read remaining data before closing
                                // ...
                                socket.close();
                            }
                        }
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void startListening() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Socket socket = null;
                BufferedReader reader = null;
                try {
                    // Connect to ESP32 (similar to your sendMessage logic)
                    socket = new Socket();
                    socket.bind(null);
                    socket.connect((new InetSocketAddress("192.168.100.6", 80)), 500);


                    reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                    while (isListening) {
                        String line = reader.readLine();
                        if (line != null && !line.isEmpty()) {
                            // Process received data (update UI, perform actions)
                            Log.d("Received:", line);
                            // You can update UI elements using a Handler orrunOnUiThread()
                        }
                    }

                } catch (IOException e) {
                    Log.e("Listener", "Error reading data: " + e.getMessage());
                } finally {
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
            }
        }).start();
    }

    public void stopListening() {
        isListening = false; // Set flag to stop listening
    }


    private void sendMessage(final String message) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Socket socket = null;
                try {
                    socket = new Socket();
                    socket.bind(null);
                    socket.connect((new InetSocketAddress("192.168.100.6", 80)), 2000);

                    OutputStream outputStream = socket.getOutputStream();
                    outputStream.write(message.getBytes());
                    //outputStream.close();

                    outputStream.flush();

                    // Receive data from the ESP32
                    BufferedReader reader = null;
                    try {
                        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        Log.d("in try", "trying2...");

                        // ... rest of your code using the reader object
                        //String line;
                        while ((line = reader.readLine()) != null) {
                            // Display received data on the UI thread using Toast
                            //String finalLine = line;
                            Log.d("final_line", finalLine.trim());
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    Log.d("Received: ", finalLine.trim());
                                }
                            }, 100); // Delay for 100 milliseconds
                        }
                    } catch (IOException e) {
                        Log.e("in try", "Error creating BufferedReader: " + e.getMessage());
                        // Handle exception (e.g., retry connection)
                    } finally {
                        if (reader != null) {
                            try {
                                reader.close();
                            } catch (IOException e) {
                                Log.e("MainActivity", "Error closing reader: " + e.getMessage());
                            }
                        }
                        if (socket != null) {
                            try {
                                socket.close();
                            } catch (IOException e) {
                                Log.e("MainActivity", "Error closing socket: " + e.getMessage());
                            }
                        }
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }



    private void receiveData(Socket socket) {
        Log.d("in receiveData()...", "get data");
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.d("in try", "trying...");

                    BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String line;

                    // Continuously read data until the connection is closed
                    while ((line = reader.readLine()) != null) {
                        // Display received data on the UI thread using Toast
                        String finalLine = line;
                        Log.d("final_line", finalLine.trim());
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                Log.d("Received: ", finalLine.trim());
                            }
                        }, 100); // Delay for 100 milliseconds
                    }

                    reader.close();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}

*/




/*
package com.example.esp32test;

import static android.os.Looper.getMainLooper;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
// Remove OkHttp import
import okhttp3.Callback;
// Remove OkHttp import
import okhttp3.MediaType;
// Remove OkHttp import
import okhttp3.OkHttpClient;
// Remove OkHttp import
import okhttp3.RequestBody;
// Remove OkHttp import
import okhttp3.Request;
// Remove OkHttp import
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private static final String ESP32_IP = "192.168.100.6"; // Replace with your ESP32's IP
    private static final int ESP32_PORT = 8266; // Replace with your ESP32's port
    private Button sendButton;
    private TextView messageTextView;
    private final Handler handler = new Handler(); // Handler for UI updates

    private Socket socket;
    private Thread receiveThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activitymain);

        messageTextView = findViewById(R.id.message); // Assuming you have a TextView with this id

        sendButton = findViewById(R.id.send_button);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendData("Hello from Android!"); // Send message to ESP32
            }
        });

        // Connect to ESP32 on app launch (optional)
        new Thread(new Runnable() {
            @Override
            public void run() {
                connectToESP32();
            }
        }).start();
    }

    private void connectToESP32() {
        try {
            SocketAddress address = new InetSocketAddress(ESP32_IP, ESP32_PORT);
            socket = new Socket();
            socket.connect(address, 10000); // Set a timeout for connection

            // Start a thread to receive data from ESP32
            receiveThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    receiveData();
                }
            });
            receiveThread.start();

            Log.d("MainActivity", "Connected to ESP32!");
        } catch (IOException e) {
            Log.e("MainActivity", "Error connecting to ESP32: " + e.getMessage());
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    messageTextView.setText("Error connecting to ESP32");
                }
            });
        }
    }

    private void sendData(String message) {
        if (socket != null && socket.isConnected()) {
            try {
                socket.getOutputStream().write(message.getBytes());
                socket.getOutputStream().flush();
                Log.d("MainActivity", "Sent data to ESP32: " + message);
            } catch (IOException e) {
                Log.e("MainActivity", "Error sending data to ESP32: " + e.getMessage());
            }
        } else {
            Log.w("MainActivity", "ESP32 connection lost, cannot send data");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    messageTextView.setText("ESP32 connection lost");
                }
            });
        }
    }

    private void receiveData() {
        try {
            while (socket != null && socket.isConnected()) {
                byte[] buffer = new byte[1024]; // Buffer for received data
                int bytesRead = socket.getInputStream().read(buffer);

                if (bytesRead > 0) {
                    String data = new String(buffer, 0, bytesRead);
                    Log.d("MainActivity", "Received data from ESP32: " + data);

                    // Update UI with received data
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            messageTextView.setText(data);
                        }
                    });

                    // Process received data (optional)
                    // You can parse data (e.g., JSON) and extract sensor values
                } else if (bytesRead == -1) {
                    // Connection closed by ESP32
                    Log.w("MainActivity", "Connection to ESP32 closed");


                    // Attempt reconnection (optional)
                    reconnectToESP32();

                    break;
                } else {
                    // No data received (may be a temporary issue)
                    Log.d("MainActivity", "No data received from ESP32");
                }
            }
        } catch (IOException e) {
            Log.e("MainActivity", "Error receiving data from ESP32: " + e.getMessage());
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    messageTextView.setText("Error receiving data from ESP32");
                    // Handle connection loss (optional: attempt to reconnect)
                }
            });
        } finally {
            // Close socket if necessary
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    Log.e("MainActivity", "Error closing socket: " + e.getMessage());
                }
            }
        }
    }

    private void reconnectToESP32() {
        socket = null;
        receiveThread = null;

        // Implement logic to reconnect with a delay (e.g., using a Handler)
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        connectToESP32();
                    }
                }).start();
            }
        }, 5000); // Delay for 5 seconds before attempting reconnect
    }

}
ERROR CONNECTING TO ESP 32...*/





/*working okhttp code
package com.example.esp32test;

import static android.widget.Toast.LENGTH_LONG;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private static final String ESP32_URL = "http://192.168.100.6/"; // Replace with your ESP32's IP
    private Button sendButton;
    private TextView messageTextView;
    private final Handler handler = new Handler(); // Handler for UI updates

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activitymain);

        messageTextView = findViewById(R.id.message); // Assuming you have a TextView with this id

        sendButton = findViewById(R.id.send_button);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendData("1"); // Send message to ESP32
            }
        });

        // Send data on app launch (optional)
        //sendData("1");
    }

    private void sendData(String message_to_esp32) {
        OkHttpClient client = new OkHttpClient();

        // Prepare data as a JSON string (optional, modify if needed)
        String jsonData = "{\"data\": \"" + message_to_esp32 + "\"}";
        Toast.makeText(this, "jsonData is: " + jsonData, LENGTH_LONG).show();

        // Create request body with JSON data
        RequestBody requestBody = RequestBody.create(jsonData, MediaType.parse("application/json; charset=utf-8"));

        // Build the POST request
        Request request = new Request.Builder()
                .url(ESP32_URL)
                .post(requestBody)  // Use POST method to send data
                .build();

        // Send the request asynchronously
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("MainActivity", "Error sending data: " + e.getMessage());
                // Update UI with error message
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        messageTextView.setText("Error sending data: " + e.getMessage());
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.d("in get res fun()", "ok1"); // Can be removed for cleaner logs
                // Log.d("in get res fun()", String.valueOf(response.body())); // Can be removed for cleaner logs

                if (response.isSuccessful()) {
                    try {
                        // Introduce a small delay to allow ESP32 to finish sending data (optional)
                        Thread.sleep(10);

                        String responseBody = response.body().string();
                        if (responseBody != null) {
                            // Parse JSON response using GSON
                            Gson gson = new Gson();
                            JsonObject json_obj = gson.fromJson(responseBody, JsonObject.class);

                            // Access sensor value using the "data" key
                            String sensorValue = json_obj.get("data").getAsString();
                            messageTextView.setText("Sensor value: " + sensorValue);
                        } else {
                            Log.w("MainActivity", "Empty response body received from ESP32");
                            // Optional: Check ESP32 logs for confirmation of data being sent
                        }
                    } catch (InterruptedException e) {
                        // Handle interruption exception (optional)
                    } catch (JsonSyntaxException e) { // Catch specific parsing errors (optional)
                        Log.e("MainActivity", "Error parsing JSON response: " + e.getMessage());
                    }
                } else {
                    Log.e("MainActivity", "Error receiving data: " + response.code());
                }
            }
        });
    }
}

*/