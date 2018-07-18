package com.example.dylan.playlisttransfer;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import org.w3c.dom.Text;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TextView titleWindow = findViewById(R.id.titleWindow);
        TextView messageWindow = findViewById(R.id.messageWindow);
        TextView loginText = findViewById(R.id.loginText);
        ImageButton SpotifyLoginButton = findViewById(R.id.SpotifyLoginBtn);
        ImageButton YouTubeLoginButton = findViewById(R.id.YouTubeLoginBtn);

        loginText.setText("Please login:");
        titleWindow.setText("Output:");
        StringBuilder stringBuilder = new StringBuilder();
        String someMessage = "This is some message. ";
        for(int i = 0; i < 100; i++){
            stringBuilder.append(someMessage);
        }
        messageWindow.setText(stringBuilder.toString());
    }
}
