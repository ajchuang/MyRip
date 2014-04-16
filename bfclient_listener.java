import java.util.*;
import java.net.*;
import java.io.*;

public class bfclient_listener implements Runnable {

    final static int MAX_PACKET_SIZE = 60*1024;
    static bfclient_listener m_listener;
      
    static {
        m_listener = null;
    }
    
    public static bfclient_listener getListener () {
        
        if (m_listener == null) {
            m_listener = new bfclient_listener ();
        }
        
        return m_listener;
    } 

    private bfclient_listener () {
    }
    
    public void run () {
        try {
            bfclient.logInfo ("bfclient listener is starting...");
            bfclient_repo repo = bfclient_repo.getRepo ();
            DatagramSocket socket = new DatagramSocket (repo.getPort ());
            
            while (true) {
                byte[] receiveData = new byte[MAX_PACKET_SIZE];
                DatagramPacket receivePacket = new DatagramPacket (receiveData, receiveData.length);
                socket.receive (receivePacket);
                
                
            }
        } catch (Exception e) {
            bfclient.logExp (e, true);
        }
    }
}