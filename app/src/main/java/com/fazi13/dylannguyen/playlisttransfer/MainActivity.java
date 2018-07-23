package com.fazi13.dylannguyen.playlisttransfer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.Spinner;
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

import okhttp3.MediaType;

public class MainActivity extends AppCompatActivity implements SpotifyPlayer.NotificationCallback, ConnectionStateCallback{
    // Spotify vars
    private static final String CLIENT_ID = "5047539c966a4a5ca3e43e6b47937493";
    private static final String REDIRECT_URI = "playlisttransfer://callback";
    private static final int REQUEST_CODE = 458;
    private String spotifyToken = "";
    private Player mPlayer;
    private String[] spotifyScopes = new String[]{"user-read-private", "playlist-read-private", "playlist-read-collaborative", "user-library-read", "playlist-modify-private", "playlist-modify-public"};

    // Server Connection vars
    public static final String IP_ADDRESS = "http://dylannguyen.me/";
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    // YouTube vars

    // Export vars
    private String exportFromHere;
    private String exportToHere;

    // UI vars
    private TextView messageWindow;
    private PopupWindow popupWindow;
    private LayoutInflater layoutInflater;
    private LinearLayout linearLayout;


    // Activity vars
    public static final String SPOTIFY_TOKEN = "com.fazi13.dylannguyen.playlisttransfer.SPOTIFY_TOKEN";
    public static final String SPOTIFY_TO_TEXT_PLAYLISTS = "com.fazi13.dylannguyen.playlisttransfer.SPOTIFY_TO_TEXT_PLAYLISTS";
    public static final String TEXT_TO_SPOTIFY_PLAYLISTS = "com.fazi13.dylannguyen.playlisttransfer.TEXT_TO_SPOTIFY_PLAYLISTS";
    public static final String SPOTIFY_TO_SPOTIFY_PLAYLISTS = "com.fazi13.dylannguyen.playlisttransfer.SPOTIFY_TO_SPOTIFY_PLAYLISTS";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Add custom toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        TextView titleWindow = findViewById(R.id.titleWindow);
        messageWindow = findViewById(R.id.messageWindow);
        TextView loginText = findViewById(R.id.loginText);
        ImageButton SpotifyLoginButton = findViewById(R.id.SpotifyLoginBtn);
        ImageButton YouTubeLoginButton = findViewById(R.id.YouTubeLoginBtn);
        ImageButton exportButton = findViewById(R.id.exportBtn);
        Spinner fromSpinner = findViewById(R.id.fromSpinner);
        Spinner toSpinner = findViewById(R.id.toSpinner);
        linearLayout = findViewById(R.id.mainLayout);

        loginText.setText("Please login:");
        titleWindow.setText("Output:");

        // Add list of transfer options available to spinners
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        fromSpinner.setAdapter(adapter);
        fromSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                exportFromHere = adapterView.getItemAtPosition(i).toString();
                Log.d("MainActivity", "User chose from: " + exportFromHere);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        toSpinner.setAdapter(adapter);
        toSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){

            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                exportToHere = adapterView.getItemAtPosition(i).toString();
                Log.d("MainActivity", "User chose to: " + exportToHere);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        // Spotify Login Button
        SpotifyLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!spotifyToken.equals("")){
                    String text = messageWindow.getText().toString();
                    messageWindow.setText(text + "Refreshed Spotify Login Token.\n");
                }
                AuthenticationRequest.Builder builder = new AuthenticationRequest.Builder(CLIENT_ID, AuthenticationResponse.Type.TOKEN, REDIRECT_URI);
                builder.setScopes(spotifyScopes);
                AuthenticationRequest request = builder.build();
                AuthenticationClient.openLoginActivity(MainActivity.this, REQUEST_CODE, request);
            }
        });

        exportButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                String text = messageWindow.getText().toString();

                // Spotify Playlist to Text File
                if(exportFromHere.equalsIgnoreCase("spotify") && exportToHere.equalsIgnoreCase("text file")){
                    // Check if logged in
                    if(!checkSpotifyLogin()){
                        return;
                    }
                    messageWindow.setText(text + "Transferring from Spotify to Text File.\n");
                    openSpotifyToText();
                // Text File to Spotify Playlist
                } else if(exportFromHere.equalsIgnoreCase("text file") && exportToHere.equalsIgnoreCase("spotify")){
                    // Check if logged in
                    if(!checkSpotifyLogin()){
                        return;
                    }
                    messageWindow.setText(text + "Transferring from Text File to Spotify.\n");
                    openTextToSpotify();
                // Spotify Playlist to Spotify Playlist
                } else if(exportFromHere.equalsIgnoreCase("spotify") && exportToHere.equalsIgnoreCase("spotify")){
                    // Check if logged in
                    if(!checkSpotifyLogin()){
                        return;
                    }
                    messageWindow.setText(text + "Transferring from Spotify to Spotify.\n");
                    openSpotifyToSpotify();
                // Text File to Text File... Error
                } else if(exportFromHere.equalsIgnoreCase("text file") && exportToHere.equalsIgnoreCase("textfile")){
                    Toast.makeText(getApplicationContext(), "Check Transfers", Toast.LENGTH_SHORT).show();
                    String text2 = messageWindow.getText().toString();
                    messageWindow.setText(text2 + "Error: Can't Transfer Text File to Text File.\n");
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater mi = getMenuInflater();
        mi.inflate(R.menu.menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.toolbar_help:
                layoutInflater = (LayoutInflater) getApplicationContext().getSystemService(LAYOUT_INFLATER_SERVICE);
                ViewGroup container = (ViewGroup) layoutInflater.inflate(R.layout.activity_help_pop_up, null);
                DisplayMetrics dm = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(dm);
                int width = (int) (dm.widthPixels * 0.75);
                int height = (int) (dm.heightPixels * 0.5);

                popupWindow = new PopupWindow(container, width, height, true);
                popupWindow.setElevation(10);
                popupWindow.showAtLocation(linearLayout, Gravity.CENTER, 0, 0);
                container.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View view, MotionEvent motionEvent) {
                        popupWindow.dismiss();
                        return false;
                    }
                });
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public void openSpotifyToText(){
        Intent intent = new Intent(this, SpotifyToText.class);
        intent.putExtra(SPOTIFY_TOKEN, spotifyToken);
        startActivityForResult(intent, 1);
        Log.d("MainActivity", "Started Spotify to Text");
    }

    public void openTextToSpotify(){
        Intent intent = new Intent(this, TextToSpotify.class);
        intent.putExtra(SPOTIFY_TOKEN, spotifyToken);
        startActivityForResult(intent, 2);
        Log.d("MainActivity", "Started Text to Spotify");
    }

    public void openSpotifyToSpotify(){
        Intent intent = new Intent(this, SpotifyToSpotify.class);
        intent.putExtra(SPOTIFY_TOKEN, spotifyToken);
        startActivityForResult(intent, 3);
        Log.d("MainActivity", "Started Spotify to Spotify");
    }

    private boolean checkSpotifyLogin(){
        if(spotifyToken.equals("")){
            Log.e("MainActivity", "No Spotify token");
            Toast.makeText(getApplicationContext(), "Please Login First", Toast.LENGTH_SHORT).show();
            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String text = messageWindow.getText().toString();
                    messageWindow.setText(text + "Please Login to Spotify First.\n");
                }
            });
            return false;
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        // Spotify Authorization.. also requires Spotify Player, but Player is never used
        if (requestCode == REQUEST_CODE) {
            AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, intent);
            if (response.getType() == AuthenticationResponse.Type.TOKEN) {
                spotifyToken = response.getAccessToken();
                Config playerConfig = new Config(this, spotifyToken, CLIENT_ID);
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
            if(spotifyToken.equals("")){
                Toast.makeText(getApplicationContext(), "Login Failed", Toast.LENGTH_SHORT).show();
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //TextView messageWindow = findViewById(R.id.messageWindow);
                        String text = messageWindow.getText().toString();
                        messageWindow.setText(text + "Login Failed. Try logging out of the Spotify App.\n");
                    }
                });
            }
        }

        // Result from Spotify to Text
        if (requestCode == 1){
            String text = messageWindow.getText().toString();
            String data = intent.getStringExtra(SPOTIFY_TO_TEXT_PLAYLISTS);
            if(resultCode == RESULT_CANCELED){
                messageWindow.setText(text + data + "\n\n");
            } else if(resultCode == RESULT_OK){
                messageWindow.setText(text + removeLastComma(data) + "\n\n");
            }
        }

        // Result from Text to Spotify
        if (requestCode == 2){
            String text = messageWindow.getText().toString();
            String data = intent.getStringExtra(TEXT_TO_SPOTIFY_PLAYLISTS);
            if(resultCode == RESULT_CANCELED){
                messageWindow.setText(text + data + "\n\n");
            } else if(resultCode == RESULT_OK){
                messageWindow.setText(text + removeLastComma(data) + "\n\n");
            }
        }

        // Result from Spotify to Spotify
        if (requestCode == 3){
            String text = messageWindow.getText().toString();
            String data = intent.getStringExtra(SPOTIFY_TO_SPOTIFY_PLAYLISTS);
            if(resultCode == RESULT_CANCELED){
                messageWindow.setText(text + data + "\n\n");
            } else if(resultCode == RESULT_OK){
                messageWindow.setText(text + removeLastComma(data) + "\n\n");
            }
        }
    }

    // Removes Comma at End of String, eg: "Playlist1, Playlist2, "
    private String removeLastComma(String s){
        if(s.charAt(s.length()-1) == ' '){
            return s.substring(0, s.length()-2);
        }
        if(s.charAt(s.length()-1) == ','){
            return s.substring(0, s.length()-1);
        }
        return s;
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
        Log.d("MainActivity", "User logged into Spotify");
        String text = messageWindow.getText().toString();
        messageWindow.setText(text + "Logged into Spotify.\n");
        Toast.makeText(getApplicationContext(), "Logged In", Toast.LENGTH_SHORT).show();
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
