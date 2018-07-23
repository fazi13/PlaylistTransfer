package com.fazi13.dylannguyen.playlisttransfer;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

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

        client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

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

        textSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                importFile = new File(externalDir, adapterView.getItemAtPosition(i).toString());
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

        exportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                exportButton.setEnabled(false);
                exportButton.setVisibility(View.INVISIBLE);
                String text = messageWindow.getText().toString();
                messageWindow.setText(text + "Sending Data to Server...\n");
                Log.d("isNewPlaylist", Boolean.toString(isNewPlaylist));
                if(isNewPlaylist){
                    playlistSelected = newPlaylistText.getText().toString();
                    SpotifyHelper.createPlaylist(spotifyToken, playlistSelected, TextToSpotify.this);

                    // create delay for Spotify to refresh playlists before adding tracks
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

    private void addSpotifyTracks(){
        JSONObject jsonObject = new JSONObject();

        try {
            jsonObject.put("spotify_token", spotifyToken);
            jsonObject.put("playlist_name", playlistSelected);
            jsonObject.put("tracks", readTracks(importFile));
        } catch (JSONException e) {
            Log.e("TextToSpotify", e.toString());
        }
        Log.d("TextToSpotify", jsonObject.toString());

        RequestBody requestBody = RequestBody.create(JSON, jsonObject.toString());
        Request request = new Request.Builder()
                .url(IP_ADDRESS + "add_spotify_tracks")
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                resetExportButton();
                Log.e("TextToSpotify", e.toString());
                TextToSpotify.this.runOnUiThread(new Runnable() {
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
                    Log.d("TextToSpotifyResponse", myResponse);
                    exportedPlaylists += playlistSelected + ", ";
                    final String[] result = parseTracksResponse(myResponse);
                    TextToSpotify.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            String text = messageWindow.getText().toString();
                            text += "Added tracks to playlist \"" + playlistSelected + "\"\n";
                            text += result[0] + "\n" + result[1] + "\n" + result[2] + "\n";
                            messageWindow.setText(text);
                        }
                    });
                } else {
                    Log.e("SpotifyToTextResponse", myResponse);
                    TextToSpotify.this.runOnUiThread(new Runnable() {
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

    private String removeLastComma(String s){
        if(s.charAt(s.length()-1) == ' '){
            return s.substring(0, s.length()-2);
        }
        if(s.charAt(s.length()-1) == ','){
            return s.substring(0, s.length()-1);
        }
        return s;
    }
    private String[] parseTracksResponse(String response){
        JSONObject jsonObject = null;
        String[] result = null;
        try {
            result = new String[3];
            jsonObject = new JSONObject(response);
            JSONArray success = jsonObject.getJSONArray("Successful");
            JSONArray fail = jsonObject.getJSONArray("Failed");
            JSONArray duplicate = jsonObject.getJSONArray("Duplicates");

            result[0] = "Successful: ";
            result[1] = "Failed: ";
            result[2] = "Duplicates: ";

            for (int i = 0; i < success.length(); i++){
                if (i == 0){
                    result[0] += "\n";
                }
                result[0] += "\t\t" + success.get(i).toString() + "\n";
            }

            for (int i = 0; i < fail.length(); i++){
                if (i == 0){
                    result[1] += "\n";
                }
                result[1] += "\t\t" + fail.get(i).toString() + "\n";
            }

            for (int i = 0; i < duplicate.length(); i++){
                if (i == 0){
                    result[2] += "\n";
                }
                result[2] += "\t\t" + duplicate.get(i).toString() +  "\n";
            }

            if (success.length() == 0){
                result[0] += "none";
            }
            if (fail.length() == 0){
                result[1] += "none";
            }
            if (duplicate.length() == 0){
                result[2] += "none";
            }
        } catch (JSONException e) {
            Log.e("TextToSpotify", e.toString());
        }
        return result;
    }

    // re-enable export button after getting response from server
    private void resetExportButton(){
        TextToSpotify.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                exportButton.setEnabled(true);
                exportButton.setVisibility(View.VISIBLE);
                Log.d("TextToSpotify", "Reset export button");
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

    private ArrayList<String> readTracks(File file){
        ArrayList<String> tracks = new ArrayList<>();
        FileInputStream fis = null;
        BufferedReader br = null;
        try {
            fis = new FileInputStream(file);
            br = new BufferedReader(new InputStreamReader(fis));
            String line;
            while((line = br.readLine()) != null){
                tracks.add(line);
            }
        } catch (IOException e) {
            Log.e("TextToSpotify", e.toString());
        } finally {
            if(fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    Log.e("TextToSpotify", e.toString());
                }
            }
            if(br != null) {
                try{
                    br.close();
                } catch (IOException e){
                    Log.e("TextToSpotify", e.toString());
                }
            }
        }
        Log.d("TextToSpotify", "Read all files");
        return tracks;
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
