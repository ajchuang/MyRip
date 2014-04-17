import java.util.*;
import java.net.*;
import java.io.*;
import java.nio.*;

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
                
                // handle packets
                decodeRawPacket (receiveData);
                
                
            }
        } catch (Exception e) {
            bfclient.logExp (e, true);
        }
    }
    
    void decodeRawPacket (byte[] rawPacket) {
        
        bfclient.logInfo ("Decoding packet");
        
        try {
            byte[] destRawAddr = new byte[4];
            byte[] destRawPort = new byte[4];
            byte[] srcRawAddr  = new byte[4];
            byte[] srcRawPort  = new byte[4];
            byte[] control     = new byte[4];
        
            byte[] rawDataLen  = new byte[4];
            byte[] rawData;
        
            System.arraycopy (rawPacket,   0, destRawAddr, 0, 4);
            System.arraycopy (rawPacket,   4, destRawPort, 0, 4);
            System.arraycopy (rawPacket,   8, srcRawAddr,  0, 4);
            System.arraycopy (rawPacket,  12, srcRawPort,  0, 4);
            System.arraycopy (rawPacket,  16, control,     0, 4);
            System.arraycopy (rawPacket,  20, rawDataLen,  0, 4);
        
            bfclient.printMsg ("[DEC] destRawAddr: " + InetAddress.getByAddress (destRawAddr));
            bfclient.printMsg ("[DEC] destRawPort: " + ByteBuffer.wrap (destRawPort).getInt ());
            bfclient.printMsg ("[DEC] srcRawAddr: "  + InetAddress.getByAddress (srcRawAddr));
            bfclient.printMsg ("[DEC] srcRawPort: "  + ByteBuffer.wrap (srcRawPort).getInt ());
            //bfclient.logInfo ("control: "     + control.length);
            //bfclient.logInfo ("rawDataLen: "  + rawDataLen.length);
        } catch (Exception e) {
            bfclient.logExp (e, false);
        }
    }
}