package com.fazi13.dylannguyen.playlisttransfer;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
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
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.Spinner;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SpotifyToText extends AppCompatActivity {
    // Spotify vars
    private String spotifyToken = "";
    private String playlistSelected = "";

    // Text File vars
    private File externalDir;
    private File myExternalFile;
    private String tracksStr;
    private int tracksSize;
    private final String FILE_PATH = "PlaylistTransfer/Spotify Export";

    // Server vars
    private static final String IP_ADDRESS = MainActivity.IP_ADDRESS;
    private static final MediaType JSON = MainActivity.JSON;
    private OkHttpClient client;

    // UI vars
    private TextView messageWindow;
    private ImageButton exportButton;
    private Spinner playlistSpinner;
    private PopupWindow popupWindow;
    private LayoutInflater layoutInflater;
    private LinearLayout linearLayout;

    private boolean isFirstExport;
    private String exportedPlaylists;
    private final String exportedNothing = "\t\t\tTransferred: nothing";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_spotify_to_text);

        // Add custom toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle("Spotify to Text File");

        Intent intent = getIntent();
        spotifyToken = intent.getStringExtra(MainActivity.SPOTIFY_TOKEN);

        TextView titleWindow = findViewById(R.id.titleWindow);
        messageWindow = findViewById(R.id.messageWindow);
        playlistSpinner = findViewById(R.id.playlistSpinner);
        exportButton = findViewById(R.id.exportBtn);
        linearLayout = findViewById(R.id.spotifyToTextLayout);

        isFirstExport = true;
        exportedPlaylists = "\t\t\tTransferred to Text File: ";

        client = new OkHttpClient();


        titleWindow.setText("Output:");
        messageWindow.setText("Getting Spotify Playlists from Server.\n");

        // Set Spotify Playlist Spinner

        playlistSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                playlistSelected = adapterView.getItemAtPosition(i).toString();
                Log.d("SpotifyToText", "User chose playlist: " + playlistSelected);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        exportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Disable export button while waiting for response from server
                exportButton.setEnabled(false);
                exportButton.setVisibility(View.GONE);

                // Reset output window for new exports
                String text = "";
                if(isFirstExport){
                    text = messageWindow.getText().toString();
                }
                messageWindow.setText(text + "Getting Data from Server...\n");
                getSpotifyTracks();
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

    // Refresh Spotify Playlists
    @Override
    public void onResume(){
        super.onResume();
        SpotifyHelper.getSpotifyPlaylists(spotifyToken, SpotifyToText.this, playlistSpinner);
    }

    // Created a custom JSON parser for JSON Array of Tracks
    // Returns tracks in form of "Artist - Track Name"
    private String[] parseTracksJSON(String tracksJSON){
        tracksJSON = tracksJSON.replaceAll("\"Tracks\":", "");
        tracksJSON = tracksJSON.replaceAll("\\{", "");
        tracksJSON = tracksJSON.replaceAll("\\}", "");
        tracksJSON = tracksJSON.replaceAll("\\[", "");
        tracksJSON = tracksJSON.replaceAll("\\]", "");
        String[] tracksSplit = tracksJSON.split(",");
        String[] tracks = new String[tracksSplit.length/2];
        int i = 0;
        int j = 0;
        while(i < tracksSplit.length){
            String artist = "";
            String name = "";
            // Parse Artist Name
            String[] split = tracksSplit[i].split(":");
            if(split[0].contains("artist")){
                artist = split[1].replace("\"", "");
                i++;
            }
            // Check if artist name had a comma
            if(i < tracksSplit.length) {
                split = tracksSplit[i].split(":");
                if(!split[0].contains("artist") && !split[0].contains("name")){
                    artist += ",";
                    artist += split[0].replace("\"", "");
                    i++;
                }
            }

            // Parse Track Name
            split = tracksSplit[i].split(":");
            if(split[0].contains("name")){
                name = split[1].replace("\"", "");
                i++;
            }

            // Check if track name had a comma
            if(i < tracksSplit.length) {
                split = tracksSplit[i].split(":");
                if(!split[0].contains("artist") && !split[0].contains("name")){
                    name += ",";
                    name += split[0].replace("\"", "");
                    i++;
                }
            }
            tracks[j] = artist + " - " + name;
            j++;
        }
        return tracks;
    }

    // Converts Tracks Array to String
    private String tracksToString(String[] tracks){
        String s = "";
        for(int i = 0; i < tracks.length; i++){
            if(tracks[i] != null) {
                s += tracks[i] + "\n";
            }
        }
        return s;
    }

    private int countPlaylistSize(String[] tracks){
        int count = 0;
        for(int i = 0; i < tracks.length; i++){
            if(tracks[i] != null) {
                count++;
            }
        }
        return count;
    }

    // Get Spotify Playlist Tracks from server
    private void getSpotifyTracks(){
        // Create JSON object to send to server
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("spotify_token", spotifyToken);
            jsonObject.put("playlist_name", playlistSelected);
        } catch (JSONException e) {
            Log.e("SpotifyToText", e.toString());
        }

        // Add JSON object and IP Address for request
        RequestBody requestBody = RequestBody.create(JSON, jsonObject.toString());
        Request request = new Request.Builder()
                .url(IP_ADDRESS + "get_spotify_tracks")
                .post(requestBody)
                .build();

        // Start requesting data from server
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                resetExportButton();
                Log.e("SpotifyToTextCall", e.toString());
                SpotifyToText.this.runOnUiThread(new Runnable() {
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
                SpotifyToText.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String text = messageWindow.getText().toString();
                        messageWindow.setText(text + "Received Data from Server. Exporting...\n");
                    }
                });
                if(response.isSuccessful()){
                    Log.d("SpotifyToTextResponse", response.toString());
                    String myResponse = response.body().string();
                    // Parse JSON received from server to tracks array
                    String[] tracks = parseTracksJSON(myResponse);
                    // Convert to String to output and write to file
                    tracksStr = tracksToString(tracks);
                    tracksSize = countPlaylistSize(tracks);
                    exportedPlaylists += "\"" + playlistSelected + "\", ";

                    // Save to PlaylistTransfer folder in external storage directory
                    externalDir = new File(Environment.getExternalStorageDirectory(), FILE_PATH);
                    if(!externalDir.exists()){
                        Log.d("SpotifyToText", "Created export directory");
                        externalDir.mkdirs();
                    }
                    // Check if external storage is accessible
                    if(FileIOHelper.isExternalStorageAvailable()){
                        myExternalFile = new File(externalDir, playlistSelected + ".txt");
                        Log.d("SpotifyToText", "Storage is available");
                        // Check for write permissions
                        if(!FileIOHelper.checkWritePermissions(SpotifyToText.this)){
                            Log.d("SpotifyToText", "Requesting write permissions");
                            FileIOHelper.requestWritePermissions(SpotifyToText.this, 458);
                            writeTracksToFile();
                        } else {
                            Log.d("SpotifyToText", "Write permissions already given");
                            writeTracksToFile();
                        }
                    }
                } else {
                    Log.e("SpotifyToTextResponse", response.toString());
                    SpotifyToText.this.runOnUiThread(new Runnable() {
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
        SpotifyToText.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                exportButton.setEnabled(true);
                exportButton.setVisibility(View.VISIBLE);
                Log.d("SpotifyToText", "Reset export button");
            }
        });
    }

    // Writes tracksStr to file as "Playlist Name.txt"
    private void writeTracksToFile(){
        FileOutputStream fos = null;
        try {
            myExternalFile.createNewFile();
            fos = new FileOutputStream(myExternalFile);
            fos.write(tracksStr.getBytes());
            Log.d("SpotifyToText", "File written success");
            SpotifyToText.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String text = messageWindow.getText().toString();
                    text += "\nTransferred Playlist to: \"" + "/" + FILE_PATH + "/" + playlistSelected + ".txt\"\n";
                    text += "Transferred " + tracksSize + " Songs from \"" + playlistSelected + "\":\n\n" + tracksStr;
                    messageWindow.setText(text);
                }
            });
            isFirstExport = false;
        } catch (IOException e) {
            Log.e("SpotifyToText", e.toString());
        } finally {
            try {
                if(fos != null){
                    fos.close();
                }
            } catch (IOException e) {
                Log.e("SpotifyToText", e.toString());
            }
        }
    }


    // Write file after permissions are given
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
                    writeTracksToFile();
                } else {
                    Log.d("SpotifyToText", "No permissions given");
                    SpotifyToText.this.runOnUiThread(new Runnable() {
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
            intent.putExtra(MainActivity.SPOTIFY_TO_TEXT_PLAYLISTS, exportedNothing);
            setResult(RESULT_CANCELED, intent);
            finish();
        } else {
            intent.putExtra(MainActivity.SPOTIFY_TO_TEXT_PLAYLISTS, exportedPlaylists);
            setResult(RESULT_OK, intent);
            finish();
        }
    }
}
