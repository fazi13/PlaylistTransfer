package com.fazi13.dylannguyen.playlisttransfer;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;

public class TextToSpotify extends Activity {
    // Spotify vars
    private String spotifyToken = "";
    private String playlistSelected = "";

    // Text File vars
    private File externalDir;
    private File myExternalFile;
    private File importFile;
    private String tracksStr;
    private int tracksSize;
    private final String FILE_PATH = "PlaylistTransfer/Import";

    // Server vars
    private static String IP_ADDRESS = MainActivity.IP_ADDRESS;
    private static MediaType JSON = MainActivity.JSON;
    private OkHttpClient client;

    // UI vars
    private TextView messageWindow;
    private EditText newPlaylistText;
    private ImageButton exportButton;
    private boolean isFirstExport;
    private boolean isNewPlaylist;
    private boolean isExportable;
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

        isNewPlaylist = false;
        isFirstExport = true;
        isExportable = true;
        exportedPlaylists = "\t\t\tTransferred to Text File: ";

        client = new OkHttpClient();

        titleWindow.setText("Output:");
        messageWindow.setText("Getting Spotify Playlists from Server.\n");

        ArrayList<String> importFiles = getAllFiles();
        if(importFiles.isEmpty()){
            isExportable = false;
            importFiles.add("Add Files to /Playlist Transfer/Import/");
            String text = messageWindow.getText().toString();
            messageWindow.setText(text + "Add Files to \"/Playlist Transfer/Import/\".\n");
        }
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(TextToSpotify.this, android.R.layout.simple_spinner_item, importFiles);
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        textSpinner.setAdapter(arrayAdapter);

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
                isNewPlaylist = b;
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

    private ArrayList<String> getAllFiles(){
        ArrayList<String> files = new ArrayList<>();
        externalDir = new File(Environment.getExternalStorageDirectory(), FILE_PATH);
        if(!externalDir.exists()){
            Log.d("TextToSpotify", "Created import directory");
            externalDir.mkdirs();
        }
        // check if external storage is accessible
        if(FileIOHelper.isExternalStorageAvailable()){
            Log.d("TextToSpotify", "Storage is available");
            // check for write permissions
            if(!FileIOHelper.checkReadPermissions(TextToSpotify.this)){
                Log.d("TextToSpotify", "Requesting read permissions");
                FileIOHelper.requestReadPermissions(TextToSpotify.this, 459);
                files = readAllFiles(externalDir);
            } else {
                Log.d("TextToSpotify", "Read permissions already given");
                files = readAllFiles(externalDir);
            }
        }
        return files;
    }

    private ArrayList<String> readAllFiles(File folder){
        ArrayList<String> files = new ArrayList<>();
        File[] folderContents = folder.listFiles();
        for(File file: folderContents){
            if (!file.isDirectory()){
                files.add(file.getName());
            }
        }
        Log.d("TextToSpotify", "Read all files");
        return files;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            // 458 is my code for write permissions
            case 458: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay!
                    readAllFiles(externalDir);
                } else {
                    Log.d("TextToSpotify", "No permissions given");
                    TextToSpotify.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            String text = messageWindow.getText().toString();
                            messageWindow.setText(text + "Error, please give write permissions\n");
                        }
                    });
                }
            }
        }
        return;
    }
}
