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
                
                if (bfclient_packet.verifyChecksum (receivePacket) == false) {
                    bfclient.logErr ("Checksum error: packet discarded");
                    continue;
                } else {
                    // handle packets
                    bfclient.logInfo ("incoming packet received");
                    bfclient_packet rcv = new bfclient_packet (receiveData);
                    packetProcessor (rcv);
                }
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
        
        // if traceroute packet, need a stamp
        if (inc.getType () == bfclient_packet.M_TROUTE_REQ) {
            
            byte[] myAddrArray = myAddr.getAddress ();
            byte[] myPortArray = ByteBuffer.allocate(4).putInt (repo.getPort()).array ();
            byte[] userData = inc.getUserData ();
            int uDataLen;
            
            if (userData == null)
                uDataLen = 0;
            else
                uDataLen = userData.length;
            
            
            byte[] myStamp = new byte[myAddrArray.length + myPortArray.length];
            System.arraycopy (myAddrArray, 0, myStamp, 0, 4);
            System.arraycopy (myPortArray, 0, myStamp, 4, 4);
                
            byte[] newUserData = new byte[uDataLen + myStamp.length];
            
            if (userData != null)
                System.arraycopy (userData, 0, newUserData, 0, uDataLen);
                
            System.arraycopy (myStamp,  0, newUserData, uDataLen, 8);
                
            inc.setUserData (newUserData);
        }
        
        if (myAddr.equals (inc.getDstAddr ()) && repo.getPort () == inc.getDstPort ()) {
            
            // I am the destination
            if (inc.getType () == bfclient_packet.M_PING_REQ) {
                bfclient.logInfo ("Receiving ping request");
                bfclient_msg msg = new bfclient_msg (bfclient_msg.M_RCV_PING_REQ);
                msg.enqueue (inc.getSrcAddr ().getHostAddress ());
                msg.enqueue (Integer.toString (inc.getSrcPort ()));
                bfclient_proc.getMainProc ().enqueueMsg (msg);
            } else if (inc.getType () == bfclient_packet.M_PING_RSP) {
                
                bfclient_msg msg = new bfclient_msg (bfclient_worker.M_RCV_PING_RSP);
                msg.enqueue (inc.getSrcAddr ().getHostAddress ());
                msg.enqueue (Integer.toString (inc.getSrcPort ()));
                bfclient_worker.getWorker ().enqueueMsg (msg);
                    
            } else if (inc.getType () == bfclient_packet.M_ROUTER_UPDATE) {
                bfclient.logInfo ("Receiving Router Update");
                bfclient_msg msg = new bfclient_msg (bfclient_msg.M_RCV_REMOTE_VEC);
                msg.setUserData ((Object)inc);
                bfclient_proc.getMainProc ().enqueueMsg (msg);
            } else if (inc.getType () == bfclient_packet.M_TROUTE_REQ) {
                bfclient.logInfo ("Receiving trace route req");
                bfclient_msg msg = new bfclient_msg (bfclient_msg.M_RCV_TROUTE_REQ);
                msg.setUserData ((Object)inc);
                bfclient_proc.getMainProc ().enqueueMsg (msg);
            } else if (inc.getType () == bfclient_packet.M_TROUTE_RSP_OK) {
                bfclient.logInfo ("Receiving trace route rsp ok");
                bfclient_msg msg = new bfclient_msg (bfclient_msg.M_RCV_TROUTE_RSP);
                msg.setUserData ((Object)inc);
                bfclient_proc.getMainProc ().enqueueMsg (msg);
            } else if (inc.getType () == bfclient_packet.M_LINK_UP) {
                bfclient.logInfo ("Receiving link up");
                bfclient_msg msg = new bfclient_msg (bfclient_msg.M_RCV_LINK_UP);
                msg.setUserData ((Object)inc);
                bfclient_proc.getMainProc ().enqueueMsg (msg);
            } else if (inc.getType () == bfclient_packet.M_LINK_DOWN) {
                bfclient.logInfo ("Receiving link down");
                bfclient_msg msg = new bfclient_msg (bfclient_msg.M_RCV_LINK_DOWN);
                msg.setUserData ((Object)inc);
                bfclient_proc.getMainProc ().enqueueMsg (msg);
            } else if (inc.getType () == bfclient_packet.M_USER_BASIC_TRANS) {
                bfclient.logInfo ("Receiving simple transmission packet");
                bfclient_msg msg = new bfclient_msg (bfclient_msg.M_RCV_SMPL_TRANS_DATA);
                msg.setUserData ((Object)inc);
                bfclient_proc.getMainProc ().enqueueMsg (msg);
            } else if (inc.getType () == bfclient_packet.M_USER_BASIC_TRANS_ACK) {
                bfclient.logInfo ("Data ACK received.");
                bfclient_msg msg = new bfclient_msg (bfclient_msg.M_RCV_SMPL_TRANS_ACK);
                msg.setUserData ((Object)inc);
                bfclient_proc.getMainProc ().enqueueMsg (msg);
            } else if (inc.getType () == bfclient_packet.M_HOST_NOT_REACHABLE) {
                bfclient.printMsg ("packet sent unreachable: " + inc.getSrcAddr ());
            } else if (inc.getType () == bfclient_packet.M_HOST_UNKNOWN_PACKET) {
                // debuggin - stop now
                bfclient.logInfo ("Receiving unknown packet error");
                System.exit (0);
            } else {
                // debuggin - stop now
                bfclient.logInfo ("Receiving unknown packet: " + inc.getType ());
                System.exit (0);
                // unknown type - drop and return an error msg
                bfclient_msg msg = new bfclient_msg (bfclient_msg.M_RCV_UNKNOWN_PKT);
                msg.enqueue (inc.getSrcAddr ().getHostAddress ());
                msg.enqueue (Integer.toString (inc.getSrcPort ()));
                bfclient_proc.getMainProc ().enqueueMsg (msg);
            }
        } else {
            bfclient.logInfo ("packet to forward");
            
            bfclient_msg msg = new bfclient_msg (bfclient_msg.M_DO_FORWARD);
            msg.setUserData (inc);
            bfclient_proc.getMainProc ().enqueueMsg (msg);
        }
    }
}