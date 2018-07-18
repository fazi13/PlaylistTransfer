package com.example.dylan.playlisttransfer;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.MainThread;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.spotify.sdk.android.player.Config;
import com.spotify.sdk.android.player.ConnectionStateCallback;
import com.spotify.sdk.android.player.Error;
import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.PlayerEvent;
import com.spotify.sdk.android.player.Spotify;
import com.spotify.sdk.android.player.SpotifyPlayer;

import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class MainActivity extends Activity implements SpotifyPlayer.NotificationCallback, ConnectionStateCallback{
    private static final String CLIENT_ID = "5047539c966a4a5ca3e43e6b47937493";
    private static final String REDIRECT_URI = "playlisttransfer://callback";
    private static final int REQUEST_CODE = 458;
    private String authToken = "";
    private Player mPlayer;
    TcpClient mTcpClient;
    private String[] scopes = new String[]{"user-read-private", "playlist-read-private", "playlist-read-collaborative", "user-library-read", "playlist-modify-private", "playlist-modify-public"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TextView titleWindow = findViewById(R.id.titleWindow);
        TextView messageWindow = findViewById(R.id.messageWindow);
        TextView loginText = findViewById(R.id.loginText);
        ImageButton SpotifyLoginButton = findViewById(R.id.SpotifyLoginBtn);
        ImageButton YouTubeLoginButton = findViewById(R.id.YouTubeLoginBtn);
        ImageButton exportButton = findViewById(R.id.exportBtn);
        new ConnectTask().execute("");
        Log.d("MainActivity", "Connect to server");

        loginText.setText("Please login:");
        titleWindow.setText("Output:");
        StringBuilder stringBuilder = new StringBuilder();
        String someMessage = "This is some message. ";
        for(int i = 0; i < 100; i++){
            stringBuilder.append(someMessage);
        }
        messageWindow.setText(stringBuilder.toString());

        // Spotify Authentication
        SpotifyLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AuthenticationRequest.Builder builder = new AuthenticationRequest.Builder(CLIENT_ID, AuthenticationResponse.Type.TOKEN, REDIRECT_URI);
                builder.setScopes(scopes);
                AuthenticationRequest request = builder.build();

                AuthenticationClient.openLoginActivity(MainActivity.this, REQUEST_CODE, request);
            }
        });

        // Export Authentication token
        exportButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                //new SendData().execute(authToken);
                //Log.d("ExportButton", "Sent token: " + authToken);
                if (mTcpClient != null) {
                    mTcpClient.sendMessage(authToken);
                }
                Log.d("ExportButton", "Sent msg to server");
                //new ConnectTask().execute("");
                //mTCPClient.sendMessage(authToken);
                /*
                Runnable background = new Runnable() {
                    @Override
                    public void run() {
                        ReceiveData rd = new ReceiveData();
                        rd.run();
                        new SendData().execute(authToken);
                        Log.d("ExportButton", "Sent token: " + authToken);
                        String msg = rd.getMsg();
                        Log.d("ExportButton", "Received: " + msg);
                    }
                };
                new Thread(background).start();
                */
            }
        });

        // Receive data


    }

    public class ConnectTask extends AsyncTask<String, String, TcpClient> {

        @Override
        protected TcpClient doInBackground(String... message) {

            //we create a TCPClient object
            mTcpClient = new TcpClient(new TcpClient.OnMessageReceived() {
                @Override
                //here the messageReceived method is implemented
                public void messageReceived(String message) {
                    //this method calls the onProgressUpdate
                    publishProgress(message);
                }
            });
            mTcpClient.run();

            return null;
        }

        @Override
        protected void onProgressUpdate(final String... values) {
            super.onProgressUpdate(values);
            //response received from server
            Log.d("test", "response " + values[0]);
            //process server response here....
            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    TextView messageWindow = findViewById(R.id.messageWindow);
                    messageWindow.setText(values[0]);
                }
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        if (requestCode == REQUEST_CODE) {
            AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, intent);
            if (response.getType() == AuthenticationResponse.Type.TOKEN) {
                authToken = response.getAccessToken();
                Config playerConfig = new Config(this, authToken, CLIENT_ID);
                Spotify.getPlayer(playerConfig, this, new SpotifyPlayer.InitializationObserver() {
                    @Override
                    public void onInitialized(SpotifyPlayer spotifyPlayer) {
                        mPlayer = spotifyPlayer;
                        mPlayer.addConnectionStateCallback(MainActivity.this);
                        mPlayer.addNotificationCallback(MainActivity.this);
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        Log.e("MainActivity", "Could not initialize player: " + throwable.getMessage());
                    }
                });
            }
        }
    }

    @Override
    protected void onDestroy() {
        Spotify.destroyPlayer(this);
        super.onDestroy();
    }

    @Override
    public void onPlaybackEvent(PlayerEvent playerEvent) {
        Log.d("MainActivity", "Playback event received: " + playerEvent.name());
        switch (playerEvent) {
            // Handle event type as necessary
            default:
                break;
        }
    }

    @Override
    public void onPlaybackError(Error error) {
        Log.d("MainActivity", "Playback error received: " + error.name());
        switch (error) {
            // Handle error type as necessary
            default:
                break;
        }
    }

    @Override
    public void onLoggedIn() {
        Log.d("MainActivity", "User logged in");
        TextView loginText = findViewById(R.id.loginText);
        loginText.setText("Logged in");
        Toast.makeText(getApplicationContext(), "Logged in", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onLoggedOut() {
        Log.d("MainActivity", "User logged out");
    }

    @Override
    public void onLoginFailed(Error var1) {
        Log.d("MainActivity", "Login failed");
    }

    @Override
    public void onTemporaryError() {
        Log.d("MainActivity", "Temporary error occurred");
    }

    @Override
    public void onConnectionMessage(String message) {
        Log.d("MainActivity", "Received connection message: " + message);
    }
}
