package com.fazi13.dylannguyen.playlisttransfer;

import android.app.Activity;
import android.os.Environment;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SpotifyHelper {
    public static void getSpotifyPlaylists(final String spotifyToken, final Activity activity){
        final TextView messageWindow = activity.findViewById(R.id.messageWindow);
        OkHttpClient client = new OkHttpClient();
        JSONObject jsonObject = new JSONObject();

        try {
            jsonObject.put("spotify_token", spotifyToken);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        RequestBody requestBody = RequestBody.create(MainActivity.JSON, jsonObject.toString());
        Request request = new Request.Builder()
                .url(MainActivity.IP_ADDRESS + "get_spotify_playlists")
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("SpotifyHelper.java", e.toString());
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //TextView messageWindow = findViewById(R.id.messageWindow);
                        String text = messageWindow.getText().toString();
                        messageWindow.setText(text + "An Error Occurred with the Server.\n");
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException { ;
                if(response.isSuccessful()){
                    Log.d("SpotifyHelper.java", response.toString());
                    final String myResponse = response.body().string();
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //TextView messageWindow = findViewById(R.id.messageWindow);
                            String text = messageWindow.getText().toString();
                            Log.d("SpotifyHelper.java", myResponse);
                            messageWindow.setText(text + "Received Spotify Playlists from Server.\n");
                            setPlaylistSpinner(myResponse, activity);
                        }
                    });
                } else {
                    Log.e("SpotifyHelper.java", response.toString());
                    activity.runOnUiThread(new Runnable() {
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

    private static void setPlaylistSpinner(String playlist, final Activity activity){
        HashMap<String,String> playlist_map = new Gson().fromJson(playlist, new TypeToken<HashMap<String, String>>(){}.getType());
        String playlistStr = playlist_map.get("Spotify Playlists");
        final String[] playlists = playlistStr.split(", ");
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_item, playlists);
                arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                Spinner playlistSpinner = activity.findViewById(R.id.playlistSpinner);
                playlistSpinner.setAdapter(arrayAdapter);
            }
        });
    }

    public static void createPlaylist(final String spotifyToken, final String playlist) {
        OkHttpClient client = new OkHttpClient();
        final String IP_ADDRESS = MainActivity.IP_ADDRESS;
        final MediaType JSON = MainActivity.JSON;
        JSONObject jsonObject = new JSONObject();

        try {
            jsonObject.put("spotify_token", spotifyToken);
            jsonObject.put("playlist_name", playlist);
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
                Log.e("SpotifyHelper.java", e.toString());
            }

            @Override
            public void onResponse(Call call, Response response) {
                if(response.isSuccessful()){
                    Log.d("SpotifyHelper.java", response.toString());
                } else {
                    Log.e("SpotifyHelper.java", response.toString());
                }
            }
        });
    }
}
