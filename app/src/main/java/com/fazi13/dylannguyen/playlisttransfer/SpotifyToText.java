package com.fazi13.dylannguyen.playlisttransfer;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SpotifyToText extends Activity {
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
    private boolean isFirstExport;
    private String exportedPlaylists;
    private final String exportedNothing = "\t\t\tTransferred: nothing";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_spotify_to_text);

        setTitle("Spotify to Text File");

        Intent intent = getIntent();
        spotifyToken = intent.getStringExtra(MainActivity.SPOTIFY_TO_TEXT_TOKEN);

        TextView titleWindow = findViewById(R.id.titleWindow);
        messageWindow = findViewById(R.id.messageWindow);
        final Spinner playlistSpinner = findViewById(R.id.playlistSpinner);
        exportButton = findViewById(R.id.exportBtn);

        isFirstExport = true;
        exportedPlaylists = "\t\t\tTransferred to Text File: ";

        client = new OkHttpClient();


        titleWindow.setText("Output:");
        messageWindow.setText("Getting Spotify Playlists from Server.\n");

        SpotifyHelper.getSpotifyPlaylists(spotifyToken, SpotifyToText.this);

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
                // disable export button while waiting for response from server
                exportButton.setEnabled(false);
                exportButton.setVisibility(View.GONE);
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

    private void getSpotifyTracks(){
        JSONObject jsonObject = new JSONObject();

        try {
            jsonObject.put("spotify_token", spotifyToken);
            jsonObject.put("playlist_name", playlistSelected);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        RequestBody requestBody = RequestBody.create(JSON, jsonObject.toString());
        Request request = new Request.Builder()
                .url(IP_ADDRESS + "get_spotify_tracks")
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                resetExportButton();
                Log.e("SpotifyToTextCall", e.toString());
                SpotifyToText.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //TextView messageWindow = findViewById(R.id.messageWindow);
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
                    String[] tracks = parseTracksJSON(myResponse);
                    tracksStr = tracksToString(tracks);
                    tracksSize = countPlaylistSize(tracks);
                    exportedPlaylists += "\"" + playlistSelected + "\", ";

                    // save to PlaylistTransfer folder in external storage directory
                    externalDir = new File(Environment.getExternalStorageDirectory(), FILE_PATH);
                    if(!externalDir.exists()){
                        Log.d("SpotifyToText", "Created export directory");
                        externalDir.mkdirs();
                    }
                    // check if external storage is accessible
                    if(FileIOHelper.isExternalStorageAvailable()){
                        myExternalFile = new File(externalDir, playlistSelected + ".txt");
                        Log.d("SpotifyToText", "Storage is available");
                        // check for write permissions
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
                            //TextView messageWindow = findViewById(R.id.messageWindow);
                            String text = messageWindow.getText().toString();
                            messageWindow.setText(text + "An Error Occurred with the Server.\n");
                        }
                    });
                }
            }
        });
    }

    // re-enable export button after getting response from server
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

    /*
    private static boolean isExternalStorageAvailable() {
        String extStorageState = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(extStorageState)) {
            return true;
        }
        return false;
    }

    private boolean checkWritePermissions() {
        int result = ContextCompat.checkSelfPermission(SpotifyToText.this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        return (result == PackageManager.PERMISSION_GRANTED);
    }

    private void requestWritePermissions() {
        ActivityCompat.requestPermissions(SpotifyToText.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 458);
    }
    */

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
                    text += "\nExported playlist to: \"" + "/" + FILE_PATH + "/" + playlistSelected + ".txt\"\n";
                    text += "Exported " + tracksSize + " Songs from \"" + playlistSelected + "\":\n\n" + tracksStr;
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
}
