package com.example.dylan.playlisttransfer;

import android.os.AsyncTask;

import java.io.OutputStreamWriter;
import java.net.Socket;
import java.io.PrintWriter;

public class SendData extends AsyncTask<String, Void, Void> {
    @Override
    protected Void doInBackground(String... params){
        try {
            Socket socket = new Socket("192.168.0.116", 4444);
            PrintWriter output = new PrintWriter(
                    new OutputStreamWriter(
                                socket.getOutputStream()
                    )
            );
            output.print(params[0]);
            output.flush();
        } catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }
}
