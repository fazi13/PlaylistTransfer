package com.fazi13.dylannguyen.playlisttransfer;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
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
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class TextToSpotify extends AppCompatActivity {
    // Spotify vars
    private String spotifyToken = "";
    private String playlistSelected = "";
    private int tracksSize;
    private int failedTracksSize;

    // Text File vars
    private File externalDir;
    private File importFile;
    private final String FILE_PATH = "PlaylistTransfer/Import";

    // Server vars
    private static String IP_ADDRESS = MainActivity.IP_ADDRESS;
    private static MediaType JSON = MainActivity.JSON;
    private OkHttpClient client;

    // UI vars
    private TextView messageWindow;
    private EditText newPlaylistText;
    private ImageButton exportButton;
    private Spinner textSpinner;
    private Spinner playlistSpinner;
    private PopupWindow popupWindow;
    private LayoutInflater layoutInflater;
    private LinearLayout linearLayout;

    private boolean isNewPlaylist;
    private boolean isFirstExport;
    private boolean isExportable;
    private String exportedPlaylists;
    private final String exportedNothing = "\t\t\tTransferred: nothing";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_text_to_spotify);

        // Add custom toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle("Spotify to Text File");
        toolbar.setTitle("Text File to Spotify");

        Intent intent = getIntent();
        spotifyToken = intent.getStringExtra(MainActivity.SPOTIFY_TOKEN);

        TextView titleWindow = findViewById(R.id.titleWindow);
        messageWindow = findViewById(R.id.messageWindow);
        newPlaylistText = findViewById(R.id.newPlaylistText);
        textSpinner = findViewById(R.id.textSpinner);
        playlistSpinner = findViewById(R.id.playlistSpinner);
        exportButton = findViewById(R.id.exportBtn);
        CheckBox checkBox = findViewById(R.id.checkBox);
        linearLayout = findViewById(R.id.textToSpotifyLayout);

        isNewPlaylist = false;
        isFirstExport = true;
        isExportable = true;
        exportedPlaylists = "\t\t\tTransferred to Spotify: ";

        // Change client timeouts for large playlists
        client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();

        titleWindow.setText("Output:");
        messageWindow.setText("Getting Spotify Playlists from Server.\n");

        // Get Spotify Playlists from server and add to spinner
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

        // Get files from Import folder and add to spinner
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
                TextView selectPlaylistText = findViewById(R.id.selectPlaylist);
                if(b){
                    Log.d("TextToSpotify", "User creating new playlist");
                    newPlaylistText.setVisibility(View.VISIBLE);
                    newPlaylistText.setHint("Playlist Name");
                    playlistSpinner.setVisibility(View.INVISIBLE);
                    selectPlaylistText.setText("Add a Playlist:");
                } else {
                    Log.d("TextToSpotify", "User does not want new playlist");
                    newPlaylistText.setVisibility(View.GONE);
                    playlistSpinner.setVisibility(View.VISIBLE);
                    selectPlaylistText.setText("Select a Playlist:");
                }
            }
        });

        exportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Don't export if no file is selected
                if(!isExportable){
                    Log.e("TextToSpotify", "User must import a file first");
                    String text = "";
                    if(isFirstExport){
                        text = messageWindow.getText().toString();
                    }
                    messageWindow.setText(text + "Please Add Files Before Exporting.\n");
                    Toast.makeText(getApplicationContext(), "Error, Add Files First", Toast.LENGTH_SHORT).show();
                    return;
                }
                // Disable export button while waiting for response from server
                exportButton.setEnabled(false);
                exportButton.setVisibility(View.INVISIBLE);
                String text = messageWindow.getText().toString();
                messageWindow.setText(text + "Sending Data to Server...\n");
                Log.d("TextToSpotify", "New playlist: " + Boolean.toString(isNewPlaylist));
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

    // Refresh Text Files and Spotify Playlists
    @Override
    public void onResume(){
        super.onResume();
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

        SpotifyHelper.getSpotifyPlaylists(spotifyToken, TextToSpotify.this, playlistSpinner);
    }

    private void addSpotifyTracks(){
        // Create JSON object to send to server
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("spotify_token", spotifyToken);
            jsonObject.put("playlist_name", playlistSelected);
            jsonObject.put("tracks", readTracks(importFile));
        } catch (JSONException e) {
            Log.e("TextToSpotify", e.toString());
        }

        // Add JSON object and IP Address for request
        RequestBody requestBody = RequestBody.create(JSON, jsonObject.toString());
        Request request = new Request.Builder()
                .url(IP_ADDRESS + "add_spotify_tracks")
                .post(requestBody)
                .build();

        // Start requesting data from server
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
                    isFirstExport = false;
                    exportedPlaylists += "\"" + playlistSelected + "\", ";
                    // Output results of tracks that succeeded, failed, and were duplicates and not added
                    final String[] result = parseTracksResponse(myResponse);
                    TextToSpotify.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            String text = messageWindow.getText().toString();
                            text += "\nTransferred " + tracksSize + " Tracks to Playlist: \"" + playlistSelected + "\"\n";
                            text += failedTracksSize + " Transfers Failed.\n\n";
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

    // Parse the response from the server to show to user
    // Gets list of tracks that are: successful, failed, and duplicates
    // Gets number of tracks that failed and total tracks sent to server for transfer
    private String[] parseTracksResponse(String response){
        JSONObject jsonObject;
        String[] result = null;
        try {
            result = new String[3];
            jsonObject = new JSONObject(response);
            JSONArray success = jsonObject.getJSONArray("Successful");
            JSONArray fail = jsonObject.getJSONArray("Failed");
            JSONArray duplicate = jsonObject.getJSONArray("Duplicates");
            tracksSize = Integer.parseInt(jsonObject.get("Total Tracks").toString());
            failedTracksSize = Integer.parseInt(jsonObject.get("Tracks Failed").toString());

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
        }  catch (Exception e) {
            Log.e("TextToSpotify", e.toString());
        }
        return result;
    }

    // Re-enable export button after getting response from server
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

    // Get filenames for every file in the Import folder
    private ArrayList<String> getAllFiles(){
        ArrayList<String> files = new ArrayList<>();
        externalDir = new File(Environment.getExternalStorageDirectory(), FILE_PATH);
        if(!externalDir.exists()){
            Log.d("TextToSpotify", "Created import directory");
            externalDir.mkdirs();
        }
        // Check if external storage is accessible
        if(FileIOHelper.isExternalStorageAvailable()){
            Log.d("TextToSpotify", "Storage is available");
            // Check for read permissions
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

    // Helper method that actually gets the file names
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

    // Reads tracks and adds to an ArrayList
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

    // Read file after permissions are given
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            // 459 is my code for read permissions
            case 459: {
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

    // Send list of exported playlists back to Main Activity
    @Override
    public void onBackPressed() {
        Intent intent = new Intent();
        if(isFirstExport){
            intent.putExtra(MainActivity.TEXT_TO_SPOTIFY_PLAYLISTS, exportedNothing);
            setResult(RESULT_CANCELED, intent);
            finish();
        } else {
            intent.putExtra(MainActivity.TEXT_TO_SPOTIFY_PLAYLISTS, exportedPlaylists);
            setResult(RESULT_OK, intent);
            finish();
        }
    }
}
