package com.example.dylan.playlisttransfer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class ReceiveData  {
    Socket s;
    ServerSocket ss;
    InputStreamReader isr;
    BufferedReader br;
    String msg;

    public void run(){
        try {
            ss = new ServerSocket(4444);
            s = ss.accept();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getMsg(){
        while(true) {
            try{
                isr = new InputStreamReader(s.getInputStream());
                br = new BufferedReader(isr);
                msg = br.readLine();
            } catch (Exception e){
                e.printStackTrace();
            }

        }
    }
}
