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
                bfclient.logInfo ("incoming packet received");
                bfclient_packet rcv = new bfclient_packet (receiveData);
                packetProcessor (rcv);
            }
        } catch (Exception e) {
            bfclient.logExp (e, true);
        }
    }
    
    void packetProcessor (bfclient_packet inc) {
        
        //  if I am the receiver.
        //      check packet type
        //          a. ping: respond to the ping
        //          b. traceroute: respond traceroute
        //          c. router update: update rtable
        //          d. data: save data
        //  else 
        //      if i can forward
        //          forward
        //      else
        //          drop with a message    
        
        bfclient_repo repo = bfclient_repo.getRepo ();
        InetAddress myAddr = repo.getLocalAddr ();
        bfclient.logInfo ("Rcvd my ip: "   + myAddr.getHostAddress ());
        bfclient.logInfo ("Rcvd my port: " + Integer.toString (repo.getPort ()));
        bfclient.logInfo ("Rcvd destination ip: "   + inc.getDstAddr ().getHostAddress ());
        bfclient.logInfo ("Rcvd destination port: " + Integer.toString (inc.getDstPort ()));
        bfclient.logInfo ("Rcvd packet type: " + Byte.toString (inc.getType ()));
        
        if (myAddr.equals (inc.getDstAddr ()) && repo.getPort () == inc.getDstPort ()) {
            
            // I am the destination
            if (inc.getType () == bfclient_packet.M_PING_REQ) {
                bfclient.logInfo ("Receiving ping request");
                bfclient_msg msg = new bfclient_msg ();
                msg.enqueue (bfclient_msg.M_PING_RSP);
                msg.enqueue (inc.getSrcAddr ().getHostAddress ());
                msg.enqueue (Integer.toString (inc.getSrcPort ()));
                bfclient_proc.getMainProc ().enqueueMsg (msg);
            } if (inc.getType () == bfclient_packet.M_PING_RSP) {
                bfclient.logInfo ("Receiving ping response");
                bfclient.printMsg (
                    "> ping response from " + 
                    inc.getSrcAddr ().getHostAddress () + ":" + 
                    Integer.toString (inc.getSrcPort ()));
            } else if (inc.getType () == bfclient_packet.M_ROUTER_UPDATE) {
                bfclient.logInfo ("Receiving Router Update");
            } else if (inc.getType () == bfclient_packet.M_HOST_UNKNOWN_PACKET) {
                bfclient.logInfo ("Receiving unknown packet error");
            } else {
                bfclient.logInfo ("Receiving unknown packet");
                // unknown type - drop and return an error msg
                bfclient_msg msg = new bfclient_msg ();
                msg.enqueue (bfclient_msg.M_UNKNOWN_PKT);
                msg.enqueue (inc.getSrcAddr ().getHostAddress ());
                msg.enqueue (Integer.toString (inc.getSrcPort ()));
                bfclient_proc.getMainProc ().enqueueMsg (msg);
            }
        } else {
            bfclient.logInfo ("packet to forward");
            // check if I can forward
        }
    }
}