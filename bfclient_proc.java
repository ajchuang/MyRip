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
            // debug code
            if (m.getType () == 0)
                throw new Exception ();
                
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
                    bfclient_msg msg = new bfclient_msg (bfclient_msg.M_UPDATE_TIMER_TO);
                    bfclient_proc.getMainProc ().enqueueMsg (msg);
                }
            }, 1000, repo.getTimeout () * 1000);
        
        // enter message loop
        while (true) {
            try {
                bfclient_msg msg = m_queue.take ();
                msg.showHeader ();
                
                //String type = msg.dequeue ();
                int type = msg.getType ();
                switch (type) {
                    
                    case bfclient_msg.M_DO_FORWARD:
                        processForward (msg);
                    break;
                    
                    case bfclient_msg.M_RCV_REMOTE_VEC:
                        processRemoteVec (msg);
                    break;
                    
                    case bfclient_msg.M_UPDATE_TIMER_TO:
                        processUpdateTimeout (msg);
                    break;
                    
                    case bfclient_msg.M_SEND_PING_REQ:
                        processPing (msg);
                    break;
                    
                    case bfclient_msg.M_RCV_PING_REQ:
                        processPingRsp (msg);
                    break;
                    
                    case bfclient_msg.M_SEND_TROUTE_REQ:
                        processTroute (msg);
                    break;
                    
                    case bfclient_msg.M_RCV_TROUTE_REQ:
                        processRxTrouteRsp (msg);
                    break;
                    
                    case bfclient_msg.M_RCV_TROUTE_RSP:
                        processTrouteRsp (msg);
                    break;
                    
                    case bfclient_msg.M_RCV_UNKNOWN_PKT:
                        processUnknownPkt (msg);
                    break;
                    
                    case bfclient_msg.M_LINK_DOWN:
                        processLinkDown (msg);
                    break;
                    
                    case bfclient_msg.M_LINK_UP:
                        processLinkUp (msg);
                    break;
                    
                    default:
                        bfclient.logErr ("Unknown msg received: " + type);
                        System.exit (0);
                    break;
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
        
        int localIfCnt = repo.getAllLocalIntfCnt ();
        
        for (int i=0; i<localIfCnt; ++i) {
            bfclient.logInfo ("processUpdateTimeout - 1");
            bfclient_rentry lent = repo.getLocalIntfEntry (i);
            
            if (lent.getOn () == false)
                continue;
            
            byte[] rtb = repo.getFlatRoutingTable (lent);
            
            bfclient_packet pkt = new bfclient_packet ();
            pkt.setDstAddr (lent.getAddr ());
            pkt.setDstPort (lent.getPort ());
            pkt.setSrcAddr (repo.getLocalAddr ());
            pkt.setSrcPort (repo.getPort ());
            pkt.setType (bfclient_packet.M_ROUTER_UPDATE);
            pkt.setUserData (rtb);
            
            sendPacket (pkt.pack (), lent);
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
            
            bfclient_packet pkt = new bfclient_packet ();
            pkt.setDstAddr (addr);
            pkt.setDstPort (port);
            pkt.setSrcAddr (hostAddr);
            pkt.setSrcPort (hostPort);
            pkt.setType (bfclient_packet.M_PING_REQ);
            
            bfclient_rentry nextHop = repo.searchRoutingTable (addr, port);
            sendPacket (pkt.pack (), nextHop);
            
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
            
            bfclient_packet pkt = new bfclient_packet ();
            pkt.setDstAddr (addr);
            pkt.setDstPort (port);
            pkt.setSrcAddr (hostAddr);
            pkt.setSrcPort (hostPort);
            pkt.setType    (bfclient_packet.M_PING_RSP); 
            
            bfclient_rentry nextHop = repo.searchRoutingTable (addr, port);
            sendPacket (pkt.pack (), nextHop);
            
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
            
            bfclient_packet pkt = new bfclient_packet ();
            pkt.setDstAddr (addr);
            pkt.setDstPort (port);
            pkt.setSrcAddr (hostAddr);
            pkt.setSrcPort (hostPort);
            pkt.setType    (bfclient_packet.M_HOST_UNKNOWN_PACKET);
            
            bfclient_rentry nextHop = repo.searchRoutingTable (addr, port);
            sendPacket (pkt.pack (), nextHop);
            
        } catch (Exception e) {
            bfclient.logExp (e, false);
        }
    }
    
    void processRemoteVec (bfclient_msg msg) {
        bfclient.logInfo ("processRemoteVec - enter");
        
        Object obj = msg.getUserData ();
        
        if (obj instanceof bfclient_packet == false) {
            bfclient.logErr ("processRemoteVec: bad input");
            return;
        }
        
        bfclient_repo repo = bfclient_repo.getRepo ();
        bfclient_packet inc = (bfclient_packet) obj;
        InetAddress srcAddr = inc.getSrcAddr ();
        int srcPort = inc.getSrcPort ();
        
        byte[] incTable = inc.getUserData ();
        int numEntry = incTable.length / bfclient_rentry.M_DEFLATE_SIZE;
        bfclient.logInfo ("processRemoteVec: total entry size: " + numEntry);
        
        // prepare nexthop & linkcost
        bfclient_rentry nextHop = bfclient_rentry.rentryFactory (srcAddr, srcPort);
        bfclient_rentry srcEntry = repo.searchRoutingTable (srcAddr, srcPort);
        
        // if the link is off, just ignore the link
        if (srcEntry == null || srcEntry.getOn () == false) {
            bfclient.logInfo ("Receiving an UPDATE from DOWN LINK");
            return;
        }
        
        float linkCost = srcEntry.getCost ();
    
        //  for every entry in the packet
        //  if this entry is me 
        //      just ignore that.
        //
        //  find matching entry other than myself
        //      if found,
        //          compare the current cost and remote cost + link cost
        //              if the new link is smaller
        //                  add to table
        //              else
        //                  forget it
        //      if not found
        //          just add to the rtable
        
        for (int i=0; i<numEntry; ++i) {
            
            // inflate the received entry
            byte[] curEntry = new byte[bfclient_rentry.M_DEFLATE_SIZE];
            System.arraycopy (
                incTable, i*bfclient_rentry.M_DEFLATE_SIZE, curEntry, 0, 
                bfclient_rentry.M_DEFLATE_SIZE);
            bfclient_rentry newEnt = bfclient_rentry.rentryFactory (curEntry);
            
            if (newEnt.getAddr ().equals (repo.getLocalAddr ()) && 
                newEnt.getPort () == repo.getPort ()) {
                // skip the entry - this is me
                continue;
            }
            
            float newCost = linkCost + newEnt.getCost ();
            
            // search routing table
            bfclient_rentry rEnt = 
                repo.searchRoutingTable (newEnt.getAddr (), newEnt.getPort ());
            
            if (rEnt != null) {
                if (newCost < linkCost) {
                    // put it into the rtable
                    newEnt.setCost (newCost);
                    newEnt.setNextHop (nextHop);
                    newEnt.setIntfIdx (srcEntry.getIntfIdx ());
                    newEnt.setOn (true);
                    repo.addRoutingEntry (newEnt);
                } else {
                    // drop it
                }
            } else {
                 // just add to the rtable
                newEnt.setCost (newCost);
                newEnt.setNextHop (nextHop);
                newEnt.setIntfIdx (srcEntry.getIntfIdx ());
                newEnt.setOn (true);
                repo.addRoutingEntry (newEnt);
            }
        }
    }
    
    void processForward (bfclient_msg msg) {
        
        bfclient_packet pkt = (bfclient_packet) msg.getUserData ();
        bfclient_repo repo = bfclient_repo.getRepo ();
        
        InetAddress dstAddr = pkt.getDstAddr ();
        int dstPort = pkt.getDstPort ();
        bfclient_rentry rent = repo.searchRoutingTable (dstAddr, dstPort);
        sendPacket (pkt.pack (), rent);
    }
    
    void processTroute (bfclient_msg msg) {
        
        try {
            bfclient_repo repo = bfclient_repo.getRepo ();
            
            String destAddrStr = msg.dequeue ();
            String destPortStr = msg.dequeue ();
            InetAddress hostAddr = repo.getLocalAddr ();
            int hostPort = repo.getPort ();
        
            // find next step
            InetAddress addr = InetAddress.getByName (destAddrStr);
            int port = Integer.parseInt (destPortStr);
            
            bfclient_packet pkt = new bfclient_packet ();
            pkt.setDstAddr (addr);
            pkt.setDstPort (port);
            pkt.setSrcAddr (hostAddr);
            pkt.setSrcPort (hostPort);
            pkt.setType    (bfclient_packet.M_TROUTE_REQ); 
            
            bfclient_rentry nextHop = repo.searchRoutingTable (addr, port);
            sendPacket (pkt.pack (), nextHop);
            
        } catch (Exception e) {
            bfclient.logExp (e, false);
        }
    }
    
    void processRxTrouteRsp (bfclient_msg msg) {
        
        bfclient.logInfo ("processRxTrouteRsp");
        
        bfclient_repo repo = bfclient_repo.getRepo ();
        bfclient_packet inc = (bfclient_packet)msg.getUserData (); 
        byte[] uData = inc.getUserData ();
        
        InetAddress myAddr = repo.getLocalAddr ();
        int myPort = repo.getPort ();
        InetAddress dstAddr = inc.getSrcAddr ();
        int dstPort = inc.getSrcPort ();
        
        bfclient_packet pkt = new bfclient_packet ();
        pkt.setDstAddr  (dstAddr);
        pkt.setDstPort  (dstPort);
        pkt.setSrcAddr  (myAddr);
        pkt.setSrcPort  (myPort);
        pkt.setUserData (uData);
        pkt.setType     (bfclient_packet.M_TROUTE_RSP_OK);
            
        bfclient_rentry nextHop = repo.searchRoutingTable (dstAddr, dstPort);
        sendPacket (pkt.pack (), nextHop);
    }
    
    void processTrouteRsp (bfclient_msg msg) {
        
        try {
            bfclient.logInfo ("processTrouteRsp");
            bfclient_packet inc = (bfclient_packet)msg.getUserData (); 
            byte[] uData = inc.getUserData ();
        
            int nHop = uData.length / 8;
            byte[] hopData = new byte[8];
            byte[] hopAddr = new byte[4];
            byte[] hopPort = new byte[4];
            
            for (int i=0; i<nHop; ++i) {
                System.arraycopy (uData, i*8,     hopAddr, 0, 4);
                System.arraycopy (uData, i*8 + 4, hopPort, 0, 4);
                InetAddress add = InetAddress.getByAddress (hopAddr);
                int port = ByteBuffer.wrap (hopPort).getInt ();
            
                bfclient.printMsg ("Hop: " + add + ":" + port);
            }
        } catch (Exception e) {
            bfclient.logExp (e, false);
        }
    }
    
    void processLinkDown (bfclient_msg msg) {
        try {
            bfclient.logInfo ("processLinkDown");
            
            String destAddrStr = msg.dequeue ();
            String destPortStr = msg.dequeue ();
            InetAddress addr = InetAddress.getByName (destAddrStr);
            int port = Integer.parseInt (destPortStr);
        
            bfclient_repo repo = bfclient_repo.getRepo ();
            repo.diableLocalLink (addr, port);
            
        } catch (Exception e) {
            bfclient.logExp (e, false);
        }
    }
    
    void processLinkUp (bfclient_msg msg) {
        try {
            bfclient.logInfo ("processLinkDown");
            
            String destAddrStr = msg.dequeue ();
            String destPortStr = msg.dequeue ();
            InetAddress addr = InetAddress.getByName (destAddrStr);
            int port = Integer.parseInt (destPortStr);
        
            bfclient_repo repo = bfclient_repo.getRepo ();
            repo.enableLocalLink (addr, port);
            
        } catch (Exception e) {
            bfclient.logExp (e, false);
        }
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
            } catch (java.io.IOException ioe) {
                bfclient.logErr ("Sending pkt error: " + ioe);
            } catch (Exception e) {
                bfclient.logExp (e, false);
            }
            
        } else {
            bfclient.printMsg ("Host unreachable");
        }
    }
    

}