package com.fazi13.dylannguyen.playlisttransfer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SpotifyToSpotify extends Activity {
    // Spotify vars
    private String spotifyToken = "";
    private String fromPlaylist = "";
    private String playlistSelected = "";

    // Server vars
    private static String IP_ADDRESS = MainActivity.IP_ADDRESS;
    private static MediaType JSON = MainActivity.JSON;
    private OkHttpClient client;

    // UI vars
    private TextView messageWindow;
    private EditText newPlaylistText;
    private ImageButton exportButton;
    private Spinner playlistSpinner;
    private Spinner fromPlaylistSpinner;
    private boolean isNewPlaylist;
    private boolean isFirstExport;
    private String exportedPlaylists; // Send list of exported playlists back to Main Activity
    private final String exportedNothing = "\t\t\tTransferred: nothing";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_spotify_to_spotify);
        setTitle("Spotify to Spotify");

        // Get Spotify token from Main Activity
        Intent intent = getIntent();
        spotifyToken = intent.getStringExtra(MainActivity.SPOTIFY_TOKEN);

        TextView titleWindow = findViewById(R.id.titleWindow);
        messageWindow = findViewById(R.id.messageWindow);
        newPlaylistText = findViewById(R.id.newPlaylistText);
        fromPlaylistSpinner = findViewById(R.id.fromPlaylistSpinner);
        playlistSpinner = findViewById(R.id.playlistSpinner);
        exportButton = findViewById(R.id.exportBtn);
        CheckBox checkBox = findViewById(R.id.checkBox);

        isNewPlaylist = false;
        isFirstExport = true;
        exportedPlaylists = "\t\t\tTransferred: ";

        // Change client timeouts for large playlists
        client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();

        // Get Spotify Playlists from server and add to spinners
        titleWindow.setText("Output:");
        messageWindow.setText("Getting Spotify Playlists from Server.\n");

        fromPlaylistSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                fromPlaylist = adapterView.getItemAtPosition(i).toString();
                Log.d("SpotifyToSpotify", "User chose from playlist: " + fromPlaylist);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        playlistSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                playlistSelected = adapterView.getItemAtPosition(i).toString();
                Log.d("SpotifyToSpotify", "User chose to playlist: " + playlistSelected);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        // Change view depending on if user wants to add a new playlist
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                isNewPlaylist = b;
                TextView selectPlaylistText = findViewById(R.id.selectPlaylist);
                if(b){
                    Log.d("SpotifyToSpotify", "User creating new playlist");
                    newPlaylistText.setVisibility(View.VISIBLE);
                    newPlaylistText.setHint("Playlist Name");
                    playlistSpinner.setVisibility(View.INVISIBLE);
                    selectPlaylistText.setText("Add a Playlist:");
                } else {
                    Log.d("SpotifyToSpotify", "User does not want new playlist");
                    newPlaylistText.setVisibility(View.GONE);
                    playlistSpinner.setVisibility(View.VISIBLE);
                    selectPlaylistText.setText("Select a Playlist:");
                }
            }
        });

        // Start transferring playlists
        exportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Disable export button while waiting for response from server
                exportButton.setEnabled(false);
                exportButton.setVisibility(View.INVISIBLE);

                // Reset output window for new exports
                String text = "";
                if(isFirstExport){
                    text = messageWindow.getText().toString();
                }
                messageWindow.setText(text + "Sending Data to Server...\n");

                if(isNewPlaylist){
                    playlistSelected = newPlaylistText.getText().toString();
                    SpotifyHelper.createPlaylist(spotifyToken, playlistSelected, SpotifyToSpotify.this);

                    // Create delay for Spotify to refresh playlists before adding tracks
                    final Handler timer = new Handler();
                    timer.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            addSpotifyTracks();
                        }
                    }, 200);
                } else {
                    addSpotifyTracks();
                }
            }
        });
    }

    // Refresh Spotify Playlists
    @Override
    public void onResume(){
        super.onResume();
        SpotifyHelper.getSpotifyPlaylists(spotifyToken, SpotifyToSpotify.this, fromPlaylistSpinner);
        SpotifyHelper.getSpotifyPlaylists(spotifyToken, SpotifyToSpotify.this, playlistSpinner);
    }

    // Add tracks from playlist A to playlist B
    private void addSpotifyTracks(){
        // Create JSON object to send to server
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("spotify_token", spotifyToken);
            jsonObject.put("playlist_from", fromPlaylist);
            jsonObject.put("playlist_to", playlistSelected);
        } catch (JSONException e) {
            Log.e("SpotifyToSpotify", e.toString());
        }

        // Add JSON object and IP Address for request
        RequestBody requestBody = RequestBody.create(JSON, jsonObject.toString());
        Request request = new Request.Builder()
                .url(IP_ADDRESS + "spotify_to_spotify")
                .post(requestBody)
                .build();

        // Start requesting data from server
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                resetExportButton();
                Log.e("SpotifyToSpotify", e.toString());
                SpotifyToSpotify.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String text = messageWindow.getText().toString();
                        messageWindow.setText(text + "An Error Occurred Connecting to the Server.\n");
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                resetExportButton();
                String myResponse = response.body().string();
                if(response.isSuccessful()){
                    Log.d("SpotifyToSpotify", myResponse);
                    isFirstExport = false;
                    exportedPlaylists += "\"" + fromPlaylist + "\" to \"" + playlistSelected + "\", ";
                    SpotifyToSpotify.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            String text = messageWindow.getText().toString();
                            text += "Transferred Tracks from \"" + fromPlaylist + "\" to \"" + playlistSelected + "\"\n";
                            messageWindow.setText(text);
                        }
                    });
                } else {
                    Log.e("SpotifyToSpotify", myResponse);
                    SpotifyToSpotify.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            String text = messageWindow.getText().toString();
                            messageWindow.setText(text + "An Error Occurred with the Server.\n");
                        }
                    });
                }
            }
        });
    }

    // Re-enable export button after getting response from server
    private void resetExportButton(){
        SpotifyToSpotify.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                exportButton.setEnabled(true);
                exportButton.setVisibility(View.VISIBLE);
                Log.d("TextToSpotify", "Reset export button");
            }
        });
    }

    // Send list of exported playlists back to Main Activity
    @Override
    public void onBackPressed() {
        Intent intent = new Intent();
        if(isFirstExport){
            intent.putExtra(MainActivity.SPOTIFY_TO_SPOTIFY_PLAYLISTS, exportedNothing);
            setResult(RESULT_CANCELED, intent);
            finish();
        } else {
            intent.putExtra(MainActivity.SPOTIFY_TO_SPOTIFY_PLAYLISTS, exportedPlaylists);
            setResult(RESULT_OK, intent);
            finish();
        }
    }
}
