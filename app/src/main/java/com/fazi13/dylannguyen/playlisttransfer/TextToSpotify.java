package com.fazi13.dylannguyen.playlisttransfer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.File;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;

public class TextToSpotify extends Activity {
    // Spotify vars
    private String spotifyToken = "";
    private String playlistSelected = "";

    // Text File vars
    private File externalDir;
    private File myExternalFile;
    private String tracksStr;
    private int tracksSize;
    private final String FILE_PATH = "PlaylistTransfer/Spotify Import";

    // Server vars
    private static String IP_ADDRESS = MainActivity.IP_ADDRESS;
    private static MediaType JSON = MainActivity.JSON;
    private OkHttpClient client;

    // UI vars
    private TextView messageWindow;
    private EditText newPlaylistText;
    private ImageButton exportButton;
    private boolean firstExport;
    private boolean newPlaylist;
    private String exportedPlaylists;
    private final String exportedNothing = "\t\t\tTransferred: nothing";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_text_to_spotify);

        setTitle("Text File to Spotify");

        Intent intent = getIntent();
        spotifyToken = intent.getStringExtra(MainActivity.SPOTIFY_TO_TEXT_TOKEN);

        TextView titleWindow = findViewById(R.id.titleWindow);
        messageWindow = findViewById(R.id.messageWindow);
        newPlaylistText = findViewById(R.id.newPlaylistText);
        final Spinner textSpinner = findViewById(R.id.textSpinner);
        final Spinner playlistSpinner = findViewById(R.id.playlistSpinner);
        exportButton = findViewById(R.id.exportBtn);
        CheckBox checkBox = findViewById(R.id.checkBox);

        newPlaylist = false;
        firstExport = true;
        exportedPlaylists = "\t\t\tTransferred to Text File: ";

        client = new OkHttpClient();

        titleWindow.setText("Output:");
        messageWindow.setText("Getting Spotify Playlists from Server.\n");

        SpotifyHelper.getSpotifyPlaylists(spotifyToken, TextToSpotify.this);

        playlistSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                playlistSelected = adapterView.getItemAtPosition(i).toString();
                Log.d("TextToSpotify", "User chose playlist: " + playlistSelected);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                newPlaylist = b;
                if(b){
                    Log.d("TextToSpotify", "User creating new playlist");
                    newPlaylistText.setVisibility(View.VISIBLE);
                    newPlaylistText.setHint("Playlist Name");
                    playlistSpinner.setVisibility(View.INVISIBLE);
                } else {
                    Log.d("TextToSpotify", "User does not want new playlist");
                    newPlaylistText.setVisibility(View.GONE);
                    playlistSpinner.setVisibility(View.VISIBLE);
                }
            }
        });
    }
}
