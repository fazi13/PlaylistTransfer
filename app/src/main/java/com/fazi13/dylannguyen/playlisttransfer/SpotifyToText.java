package com.fazi13.dylannguyen.playlisttransfer;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
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
    // Spotify Vars
    private String spotifyToken = "";
    private String playlistSelected = "";

    // Text File Vars
    private final String FILE_PATH = "/PlaylistTransfer/";

    // OkHTTP Vars
    private static String IP_ADDRESS;
    private static MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    OkHttpClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_spotify_to_text);

        Intent intent = getIntent();
        IP_ADDRESS = intent.getStringExtra(MainActivity.EXTRA_IP_ADDRESS);
        spotifyToken = intent.getStringExtra(MainActivity.SPOTIFY_TO_TEXT_TOKEN);

        TextView titleWindow = findViewById(R.id.titleWindow);
        final TextView messageWindow = findViewById(R.id.messageWindow);
        final Spinner playlistSpinner = findViewById(R.id.playlistSpinner);
        ImageButton exportButton = findViewById(R.id.exportBtn);
        client = new OkHttpClient();
        titleWindow.setText("Output:");
        messageWindow.setText("Getting Spotify Playlists from Server.\n");

        getSpotifyPlaylists();

        playlistSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                playlistSelected = adapterView.getItemAtPosition(i).toString();
                Log.d("MainActivity", "User chose playlist: " + playlistSelected);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        exportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String text = messageWindow.getText().toString();
                messageWindow.setText(text + "Getting Data from Server...\n");
                getSpotifyTracks();
            }
        });
    }

    private void getSpotifyPlaylists(){
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("spotify_token", spotifyToken);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.d("SpotifyToTextResponse", jsonObject.toString());
        RequestBody requestBody = RequestBody.create(JSON, jsonObject.toString());
        Request request = new Request.Builder()
                .url(IP_ADDRESS + "get_spotify_playlists")
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("SpotifyToTextCall", e.toString());
                SpotifyToText.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        TextView messageWindow = findViewById(R.id.messageWindow);
                        String text = messageWindow.getText().toString();
                        messageWindow.setText(text + "An Error Occurred with the Server.\n");
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                //Log.d("ExportButton", response.body().string());
                if(response.isSuccessful()){
                    final String myResponse = response.body().string();
                    SpotifyToText.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            TextView messageWindow = findViewById(R.id.messageWindow);
                            String text = messageWindow.getText().toString();
                            Log.d("SpotifyToTextResponse", myResponse);
                            messageWindow.setText(text + "Received Spotify Playlists from Server.\n");
                            setPlaylistSpinner(myResponse);
                        }
                    });
                } else {
                    Log.e("SpotifyToTextResponse", response.toString());
                    SpotifyToText.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            TextView messageWindow = findViewById(R.id.messageWindow);
                            String text = messageWindow.getText().toString();
                            messageWindow.setText(text + "An Error Occurred with the Server.\n");
                        }
                    });
                }
            }
        });
    }

    private void setPlaylistSpinner(String playlist){
        HashMap<String,String> playlist_map = new Gson().fromJson(playlist, new TypeToken<HashMap<String, String>>(){}.getType());
        String playlistStr = playlist_map.get("Spotify Playlists");
        final String[] playlists = playlistStr.split(", ");
        SpotifyToText.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(SpotifyToText.this,android.R.layout.simple_spinner_item, playlists);
                arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                Spinner playlistSpinner = findViewById(R.id.playlistSpinner);
                playlistSpinner.setAdapter(arrayAdapter);
            }
        });
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
        Log.d("SpotifyToTextResponse", jsonObject.toString());
        RequestBody requestBody = RequestBody.create(JSON, jsonObject.toString());
        Request request = new Request.Builder()
                .url(IP_ADDRESS + "get_spotify_tracks")
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("SpotifyToTextCall", e.toString());
                SpotifyToText.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        TextView messageWindow = findViewById(R.id.messageWindow);
                        String text = messageWindow.getText().toString();
                        messageWindow.setText(text + "An Error Occurred with the Server.\n");
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                //Log.d("ExportButton", response.body().string());
                if(response.isSuccessful()){
                    final String myResponse = response.body().string();
                    SpotifyToText.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            TextView messageWindow = findViewById(R.id.messageWindow);
                            String text = messageWindow.getText().toString();
                            Log.d("SpotifyToTextResponse", myResponse);
                            String[] tracks = parseTracksJSON(myResponse);
                            String trackStr = tracksToString(tracks);
                            saveToFile(playlistSelected + ".txt", trackStr);
                            messageWindow.setText(text + "Exported " + countPlaylistSize(tracks) + " Songs from \"" + playlistSelected + "\":\n\n" + trackStr);
                        }
                    });
                } else {
                    Log.e("SpotifyToTextResponse", response.toString());
                    SpotifyToText.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            TextView messageWindow = findViewById(R.id.messageWindow);
                            String text = messageWindow.getText().toString();
                            messageWindow.setText(text + "An Error Occurred with the Server.\n");
                        }
                    });
                }
            }
        });
    }

    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    public File getPublicStorageDir(String filename) {
        // Get the directory for the user's public pictures directory.
        File file = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOCUMENTS), filename);
        if (!file.mkdirs()) {
            Log.e("SpotifyToText", "Directory not created");
        }
        return file;
    }

    private boolean checkPermissions(String permission){
        int res = SpotifyToText.this.getApplicationContext().checkCallingOrSelfPermission(permission);
        return (res == PackageManager.PERMISSION_GRANTED);
    }

    private void saveToFile(final String filename, String text){
        // Not able to write file
        if(!isExternalStorageWritable()){
            Log.e("SpotifyToText", "Storage not found");
            return;
        }
        if(checkPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE)){
            FileOutputStream fos = null;
            PrintWriter pw = null;
            File file = getPublicStorageDir(filename);
            try {
                fos = new FileOutputStream(file);
                pw = new PrintWriter(fos);
                pw.print(text);
                SpotifyToText.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        TextView messageWindow = findViewById(R.id.messageWindow);
                        String text = messageWindow.getText().toString();
                        messageWindow.setText(text + "Exported to: " + getFilesDir() + "/" + FILE_PATH + filename);
                        Toast.makeText(getApplicationContext(), "Export Successful", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (FileNotFoundException e) {
                Log.e("SpotifyToText", e.toString());
            } finally {
                if(pw != null){
                    pw.close();
                }
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException e) {
                        Log.e("SpotifyToText", e.toString());
                    }
                }
            }
        } else {
            Log.e("SpotifyToText", "Permission denied");
            return;
        }

    }
}
