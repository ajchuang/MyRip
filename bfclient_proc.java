import java.util.*;
import java.net.*;
import java.io.*;
import java.util.concurrent.*;
import java.nio.*;

public class bfclient_proc implements Runnable {
    
    final static byte M_PKT_TYPE_PING   = 0x01;

    static bfclient_proc sm_proc;
    
    LinkedBlockingQueue<bfclient_msg> m_queue;
    Timer m_updateTimer;
    
    static {
        sm_proc = null;
    }

    public static bfclient_proc getMainProc () {
        
        if (sm_proc == null)
            sm_proc = new bfclient_proc ();
            
        return sm_proc;
    }

    private bfclient_proc () {
        m_queue = new LinkedBlockingQueue<bfclient_msg> ();
        m_updateTimer = new Timer ("UpdateTimer");
    }
    
    public void enqueueMsg (bfclient_msg m) {
        try {
            m_queue.put (m);
        } catch (Exception e) {
            bfclient.logExp (e, true);
        }
    }
    
    public void run () {
        
        bfclient.logInfo ("Main thread starting");
        bfclient_repo repo = bfclient_repo.getRepo ();
        
        // start update timer
        m_updateTimer.scheduleAtFixedRate (
            new TimerTask () {
                public void run () {
                    bfclient_msg msg = new bfclient_msg ();
                    msg.enqueue (bfclient_msg.M_UPDATE_TO);
                    bfclient_proc.getMainProc ().enqueueMsg (msg);
                }
            }, 1000, repo.getTimeout () * 1000);
        
        // enter message loop
        while (true) {
            try {
                bfclient_msg msg = m_queue.take ();
                msg.showHeader ();
                
                String type = msg.dequeue ();
                
                if (type.equals (bfclient_msg.M_PING_REQ)) {
                    processPing (msg);
                } else if (type.equals (bfclient_msg.M_PING_RSP)) {
                    processPingRsp (msg);
                } else if (type.equals (bfclient_msg.M_UPDATE_TO)) {
                    processUpdateTimeout (msg);
                } else if (type.equals (bfclient_msg.M_UNKNOWN_PKT)) {
                    processUnknownPkt (msg);
                } else {
                    // unknown message - drop it
                }
                
            } catch (Exception e) {
                bfclient.logExp (e, false);
            }
        }
    }
    
    public void processUpdateTimeout (bfclient_msg msg) {
        
        bfclient.logInfo ("processUpdateTimeout");
        
        // send whole vector to neighbor
        bfclient_repo repo = bfclient_repo.getRepo ();
        InetAddress hostAddr = repo.getLocalAddr ();
        int hostPort = repo.getPort ();
        
        int localIfCnt = repo.getLocalIntfCnt ();
        
        for (int i=0; i<localIfCnt; ++i) {
            bfclient.logInfo ("processUpdateTimeout - 1");
            bfclient_rentry lent = repo.getLocalIntfEntry (i);
            byte[] rtb = repo.getFlatRoutingTable (lent);
            byte[] pkt = 
                packPacket (
                    rtb, lent.getAddr (), lent.getPort (), repo.getLocalAddr (), repo.getPort (), 
                    bfclient_packet.M_ROUTER_UPDATE, (byte)0x01, (byte)0x01);
            
            sendPacket (pkt, lent);
        }
    }
    
    void processPing (bfclient_msg msg) {
        
        try {
            bfclient_repo repo = bfclient_repo.getRepo ();
            
            String destAddrStr = msg.dequeue ();
            String destPortStr = msg.dequeue ();
            InetAddress hostAddr = repo.getLocalAddr ();
            int hostPort = repo.getPort ();
        
            // find next step
            InetAddress addr = InetAddress.getByName (destAddrStr);
            int port = Integer.parseInt (destPortStr);
            byte[] rawPacket = 
                packPacket (
                    null, addr, port, hostAddr, hostPort, 
                    bfclient_packet.M_PING_REQ, (byte)0x01, (byte)0x01);
            bfclient_rentry nextHop = repo.searchRoutingTable (addr, port);
            sendPacket (rawPacket, nextHop);
            
        } catch (Exception e) {
            bfclient.logExp (e, false);
        }
    }
    
    void processPingRsp (bfclient_msg msg) {
        try {
            bfclient_repo repo = bfclient_repo.getRepo ();
            
            String destAddrStr = msg.dequeue ();
            String destPortStr = msg.dequeue ();
            InetAddress hostAddr = repo.getLocalAddr ();
            int hostPort = repo.getPort ();
        
            // find next step
            InetAddress addr = InetAddress.getByName (destAddrStr);
            int port = Integer.parseInt (destPortStr);
            byte[] rawPacket = 
                packPacket (
                    null, addr, port, hostAddr, hostPort, 
                    bfclient_packet.M_PING_RSP, (byte)0x01, (byte)0x01);
            bfclient_rentry nextHop = repo.searchRoutingTable (addr, port);
            sendPacket (rawPacket, nextHop);
            
        } catch (Exception e) {
            bfclient.logExp (e, false);
        }
    }
    
    // when receiving unknown packet, just ack with an error
    void processUnknownPkt (bfclient_msg msg) {
        try {
            bfclient_repo repo = bfclient_repo.getRepo ();
            
            String destAddrStr = msg.dequeue ();
            String destPortStr = msg.dequeue ();
            InetAddress hostAddr = repo.getLocalAddr ();
            int hostPort = repo.getPort ();
        
            // find next step
            InetAddress addr = InetAddress.getByName (destAddrStr);
            int port = Integer.parseInt (destPortStr);
            byte[] rawPacket = 
                packPacket (
                    null, addr, port, hostAddr, hostPort, 
                    bfclient_packet.M_HOST_UNKNOWN_PACKET, (byte)0x01, (byte)0x01);
            bfclient_rentry nextHop = repo.searchRoutingTable (addr, port);
            sendPacket (rawPacket, nextHop);
            
        } catch (Exception e) {
            bfclient.logExp (e, false);
        }
    }
    
    void createCntlPacket (InetAddress dstAddr, int dstPort, byte type) {
        
        bfclient_repo repo = bfclient_repo.getRepo ();
        InetAddress myAddr = repo.getLocalAddr ();
        repo.getPort ();
    }
    
    // pack the msg into a valid packet
    byte[] packPacket (
        byte[] userData, 
        InetAddress destAddr, 
        int destPort, 
        InetAddress srcAddr, 
        int srcPort,
        byte type,
        byte total_parts,
        byte current_part) {
        
        byte[] destRawAddr = destAddr.getAddress ();
        byte[] destRawPort = ByteBuffer.allocate(4).putInt(destPort).array ();
        byte[] srcRawAddr  = srcAddr.getAddress ();
        byte[] srcRawPort  = ByteBuffer.allocate(4).putInt(srcPort).array ();
        byte[] control     = {type, total_parts, current_part, 0x00};
        
        byte[] rawDataLen;
        byte[] rawData;
        int packetTotalLen = 24;
        
        if (userData != null) {
            rawData     = userData;
            rawDataLen  = ByteBuffer.allocate(4).putInt(userData.length).array ();
            packetTotalLen = 24 + rawData.length;
        } else {
            rawData     = null;
            rawDataLen  = ByteBuffer.allocate(4).putInt(0x00000000).array ();
            packetTotalLen = 24;
        }
        
        if (rawData != null)
            bfclient.logInfo ("rawData: " + rawData.length);
        
        byte[] packet = new byte[packetTotalLen];
        
        System.arraycopy (destRawAddr, 0, packet,  0, destRawAddr.length);
        System.arraycopy (destRawPort, 0, packet,  4, destRawPort.length);
        System.arraycopy (srcRawAddr,  0, packet,  8, srcRawAddr.length);
        System.arraycopy (srcRawPort,  0, packet, 12, srcRawPort.length);
        System.arraycopy (control,     0, packet, 16, control.length);
        System.arraycopy (rawDataLen,  0, packet, 20, rawDataLen.length);
        
        if (rawData != null)
            System.arraycopy (rawData,  0, packet, 24, rawData.length);
        
        return packet;
    }
    
    // msg is the packet to send, rentry is the matching routing table entry
    void sendPacket (byte[] msg, bfclient_rentry rentry) {
        
        if (rentry != null) {
            bfclient.logInfo ("Packet is forwarding to " + rentry);
            
            InetAddress nextAddr;
            int nextPort;
            
            if (rentry.isLocalIf ()) {
                nextAddr = rentry.getAddr ();
                nextPort = rentry.getPort ();
                
            } else {
                // forwarding
                bfclient_rentry nextHop = rentry.getNextHop ();
                
                if (nextHop == null) {
                    bfclient.logErr ("Bad routing table next hop");
                    System.exit (0);
                } 
                
                nextAddr = nextHop.getAddr ();
                nextPort = nextHop.getPort ();
            }
            
            try {
                DatagramPacket packet = new DatagramPacket (msg, msg.length, nextAddr, nextPort); 
                DatagramSocket socket = new DatagramSocket ();
                socket.send (packet);                       
                socket.close ();
            } catch (Exception e) {
                bfclient.logExp (e, false);
            }
            
        } else {
            bfclient.printMsg ("Host unreachable");
        }
    }
    

}