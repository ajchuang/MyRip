import java.util.*;
import java.net.*;
import java.io.*;
import java.util.concurrent.*;
import java.nio.*;

public class bfclient_proc implements Runnable {
    
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
                        processUpdateTimeout (msg, false);
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
                    
                    case bfclient_msg.M_RCV_LINK_DOWN:
                        processRcvLinkDown (msg);
                    break;
                    
                    case bfclient_msg.M_RCV_LINK_UP:
                        processRcvLinkUp (msg);
                    break;
                    
                    case bfclient_msg.M_SND_SMPL_TRANS_DATA:
                        processSndSimpleTrans (msg);
                    break;
                    
                    case bfclient_msg.M_RCV_SMPL_TRANS_DATA:
                        processRcvSimpleTrans (msg);
                    break;
                    
                    case bfclient_msg.M_RCV_SMPL_TRANS_ACK:
                        processRcvSimpleTransAck (msg);
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
    
    public void processUpdateTimeout (bfclient_msg msg, boolean isUrgent) {
        
        bfclient.logInfo ("processUpdateTimeout");
        
        // send whole vector to neighbor
        bfclient_repo repo = bfclient_repo.getRepo ();
        InetAddress hostAddr = repo.getLocalAddr ();
        int hostPort = repo.getPort ();
        
        // Step 1. disable expired items
        repo.checkLastUpdateTime ();
        
        // Step 2. send good items
        int localIfCnt = repo.getAllLocalIntfCnt ();
        
        for (int i=0; i<localIfCnt; ++i) {
            bfclient.logInfo ("processUpdateTimeout - 1");
            
            // search local 'active' interface 
            bfclient_rentry lent = repo.getLocalIntfEntry (i);
            if (lent.getOn () == false)
                continue;
            
            byte[] rtb = repo.getFlatRoutingTable (lent);
            
            bfclient_packet pkt = new bfclient_packet ();
            pkt.setDstAddr (lent.getAddr ());
            pkt.setDstPort (lent.getPort ());
            pkt.setSrcAddr (repo.getLocalAddr ());
            pkt.setSrcPort (repo.getPort ());
            
            if (isUrgent)
                pkt.setType (bfclient_packet.M_ROUTER_UPDATE_URG);
            else
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

        boolean urgent = false;
        
        bfclient.logInfo ("processRemoteVec - enter");
        
        Object obj = msg.getUserData ();
        
        if (obj instanceof bfclient_packet == false) {
            bfclient.logErr ("processRemoteVec: bad input");
            return;
        }
        
        //String urg = msg.dequeue ();
        //if (urg.equals ("URGENT"))
        //    urgent = true;
        //else
        //    urgent = false;
        
        bfclient_repo repo = bfclient_repo.getRepo ();
        bfclient.logErr ("processRemoteVec Before");
        repo.printRoutingTable ();
        
        bfclient_packet inc = (bfclient_packet) obj;
        
        InetAddress srcAddr = inc.getSrcAddr ();
        int srcPort = inc.getSrcPort ();
        byte[] incTable = inc.getUserData ();
        
        int numEntry = incTable.length / bfclient_rentry.M_DEFLATE_SIZE;
        bfclient.logInfo ("processRemoteVec: total entry size: " + numEntry);
        
        // prepare nexthop & linkcost
        bfclient_rentry nextHop = bfclient_rentry.rentryFactory (srcAddr, srcPort);
        bfclient_rentry srcEntry = repo.searchAllRoutingTable (srcAddr, srcPort);
        
        // if the link is off, just ignore the link
        if (srcEntry == null) {
            bfclient.logInfo ("Receiving an UPDATE from an unknown link");
            return;
        } else if (srcEntry.getOn () == false) {
            bfclient.logInfo ("Receiving an UPDATE from DOWN LINK");
        }
    
        //  for every entry in the packet
        //  if this entry is me 
        //      just ignore that.
        //
        //  find matching entry other than myself
        //      if found,
        //          if it comes from the next hop -
        //              update accordingly
        //          else if I am the nexthop - do fucking care - dont update me
        //          else
        //              compare the current cost and remote cost + link cost
        //                  if the new link is smaller
        //                      add to table
        //                  else
        //                      forget it
        //      else
        //          just add to the rtable
        
        // begin of debug code
        bfclient.logInfo ("[Debug] " + srcAddr + ":" + srcPort);
        
        for (int j=0; j<numEntry; ++j) {
            byte[] curEntry = new byte[bfclient_rentry.M_DEFLATE_SIZE];
            System.arraycopy (
                incTable, j*bfclient_rentry.M_DEFLATE_SIZE, curEntry, 0, 
                bfclient_rentry.M_DEFLATE_SIZE);
            bfclient_rentry newEnt = bfclient_rentry.rentryFactory (curEntry);
            bfclient.logErr ("[Debug] " + newEnt.toString ());
        }
        // end of debug code
        
        for (int i=0; i<numEntry; ++i) {
            
            // inflate the received entry
            byte[] curEntry = new byte[bfclient_rentry.M_DEFLATE_SIZE];
            System.arraycopy (
                incTable, i*bfclient_rentry.M_DEFLATE_SIZE, curEntry, 0, 
                bfclient_rentry.M_DEFLATE_SIZE);
            bfclient_rentry newEnt = bfclient_rentry.rentryFactory (curEntry);
            bfclient.logErr (" receiving entry: " + newEnt.toString ());
            
            // even if its me, I still need to read the value and check if updated
            if (newEnt.getAddr ().equals (repo.getLocalAddr ()) && 
                newEnt.getPort () == repo.getPort ()) {
                bfclient.logErr ("new entry.0 - a link to me, just update cost & update");
                // this is me, I will not add to the routing table, but need to adjust cost
                srcEntry.setCost (newEnt.getCost ());
                srcEntry.setUpdate ();
                continue;
            }
            
            // search all routing tables including down links
            bfclient_rentry rEnt = 
                repo.searchAllRoutingTable (newEnt.getAddr (), newEnt.getPort ());
            
            if (rEnt != null) {
                float linkCost = rEnt.getCost ();
                float newCost = srcEntry.getCost () + newEnt.getCost ();
                
                if (rEnt.getNextHop () != null && 
                    rEnt.getNextHop ().getAddr ().equals (srcAddr) && 
                    rEnt.getNextHop ().getPort () == srcPort) {
                    
                    bfclient.logErr ("new entry.1 - next hop is sender, update cost as he says");
                    rEnt.setUpdate ();
                    
                    if (newCost >= bfclient_rentry.M_MAX_LINE_COST) {
                        rEnt.setCost (bfclient_rentry.M_MAX_LINE_COST);
                    } else {
                        rEnt.setCost (newCost);
                    }
                    
                } else if (newEnt.getNextHop () != null &&
                           newEnt.getNextHop ().getAddr ().equals (repo.getLocalAddr ()) && 
                           newEnt.getNextHop ().getPort () == repo.getPort ()) {
                    // ignore if the new entry's next hop is myself
                    bfclient.logErr ("new entry.2 - next hop is me, dont update - split horizon");
                } else {
                    
                    bfclient_rentry localEnt = 
                        repo.getLocalIntfEntry (
                            repo.getLocalIntfIdx (newEnt.getAddr (), newEnt.getPort ()));
                    
                    // if not the next hop 
                    if (newCost >= bfclient_rentry.M_MAX_LINE_COST) {
                        bfclient.logErr ("new entry.3 - this link is down");
                        
                        // search local entry first - incase a local link becomes better again
                        if (localEnt != null && localEnt.getCost () < bfclient_rentry.M_MAX_LINE_COST) {
                            rEnt.setCost (localEnt.getCost ());
                            rEnt.setNextHop (null);
                            rEnt.setIntfIdx (localEnt.getIntfIdx ());
                            rEnt.setUpdate ();
                        } else {
                            rEnt.setCost (bfclient_rentry.M_MAX_LINE_COST);
                        }
                    } else if (newCost < linkCost) {
                        // Update the rtable
                        bfclient.logErr ("new entry.4 - better link is found");
                        rEnt.setCost (newCost);
                        rEnt.setNextHop (nextHop);
                        rEnt.setIntfIdx (srcEntry.getIntfIdx ());
                        rEnt.setUpdate ();
                    } else {
                        // drop it (bad link or off link)
                        bfclient.logErr (
                            "new entry.5 - worse link, drop it: " + 
                            newCost + ":" + 
                            linkCost);
                    }
                }
            } else {
                bfclient.logErr ("new entry.6 - unforeseen link is found");
                float newCost = srcEntry.getCost () + newEnt.getCost ();
                
                if (newCost >= bfclient_rentry.M_MAX_LINE_COST)
                    newCost = bfclient_rentry.M_MAX_LINE_COST;

                newEnt.setCost (newCost);
                newEnt.setNextHop (nextHop);
                newEnt.setIntfIdx (srcEntry.getIntfIdx ());
                repo.addRoutingEntry (newEnt);
            }
        }
        
        bfclient.logErr ("processRemoteVec After");
        repo.printRoutingTable ();
        
        // fast change propagation
        if (urgent) {
            processUpdateTimeout (msg, true);
        }
    }
    
    void processForward (bfclient_msg msg) {
        
        bfclient_packet pkt = (bfclient_packet) msg.getUserData ();
        bfclient_repo repo = bfclient_repo.getRepo ();
        
        InetAddress dstAddr = pkt.getDstAddr ();
        int dstPort = pkt.getDstPort ();
        bfclient_rentry rent = repo.searchRoutingTable (dstAddr, dstPort);
        
        if (rent == null) {
            bfclient_packet ctl = new bfclient_packet ();
            ctl.setDstAddr (pkt.getSrcAddr ());
            ctl.setDstPort (pkt.getSrcPort ());
            ctl.setSrcAddr (repo.getLocalAddr ());
            ctl.setSrcPort (repo.getPort ());
            ctl.setType    (bfclient_packet.M_HOST_NOT_REACHABLE);
            bfclient_rentry initSrc = repo.searchRoutingTable (pkt.getSrcAddr (), repo.getPort ());
            sendPacket (ctl.pack (), initSrc);
        } else {
            sendPacket (pkt.pack (), rent);
        }
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
            
                bfclient.printMsg ("Hop " + i + ": " + add.getHostAddress () + ":" + port);
            }
        } catch (Exception e) {
            bfclient.logExp (e, false);
        }
    }
    
    void processLinkDown (bfclient_msg msg) {
        try {
            bfclient.logInfo ("processLinkDown");
            bfclient_repo repo = bfclient_repo.getRepo ();
            
            String destAddrStr = msg.dequeue ();
            String destPortStr = msg.dequeue ();
            InetAddress addr = InetAddress.getByName (destAddrStr);
            int port = Integer.parseInt (destPortStr);
            InetAddress myAddr = repo.getLocalAddr ();
            int myPort = repo.getPort ();
            
            // send link-down packet
            bfclient_packet pkt = new bfclient_packet ();
            pkt.setDstAddr  (addr);
            pkt.setDstPort  (port);
            pkt.setSrcAddr  (myAddr);
            pkt.setSrcPort  (myPort);
            pkt.setType     (bfclient_packet.M_LINK_DOWN);
            sendPacket (pkt.pack (), repo.searchAllRoutingTable (addr, port));
        
            // locally stopped
            repo.diableLocalLink (addr, port);
            
            // triggered update
            processUpdateTimeout (msg, false);
            
        } catch (Exception e) {
            bfclient.logExp (e, false);
        }
    }
    
    void processLinkUp (bfclient_msg msg) {
        try {
            bfclient.logInfo ("processLinkUp");
            
            bfclient_repo repo = bfclient_repo.getRepo ();
            
            String destAddrStr = msg.dequeue ();
            String destPortStr = msg.dequeue ();
            String linkCostStr = msg.dequeue ();
            
            InetAddress addr = InetAddress.getByName (destAddrStr);
            int port = Integer.parseInt (destPortStr);
            InetAddress myAddr = repo.getLocalAddr ();
            int myPort = repo.getPort ();
            float cost = Float.parseFloat (linkCostStr);
            byte[] cArray = ByteBuffer.allocate (4).putFloat (cost).array ();
            
            // enable local link
            repo.enableLocalLink (addr, port, cost);
            
            // send link-down packet
            bfclient_packet pkt = new bfclient_packet ();
            pkt.setDstAddr  (addr);
            pkt.setDstPort  (port);
            pkt.setSrcAddr  (myAddr);
            pkt.setSrcPort  (myPort);
            pkt.setType     (bfclient_packet.M_LINK_UP);
            pkt.setUserData (cArray);
            sendPacket (pkt.pack (), repo.searchAllRoutingTable (addr, port));
            
            // triggered update
            processUpdateTimeout (msg, false);
            
        } catch (Exception e) {
            bfclient.logExp (e, false);
        }
    }
    
    void processRcvLinkDown (bfclient_msg msg) {
        bfclient.logInfo ("processRcvLinkDown");
        
        bfclient_packet pkt = (bfclient_packet) msg.getUserData ();
        InetAddress srcAddr = pkt.getSrcAddr ();
        int srcPort = pkt.getSrcPort ();
        
        bfclient_repo repo = bfclient_repo.getRepo ();
        repo.diableLocalLink (srcAddr, srcPort);
        bfclient.logInfo ("Local link disabled");
    }
    
    void processRcvLinkUp (bfclient_msg msg) {
        bfclient.logInfo ("processRcvLinkUp");
        
        bfclient_packet pkt = (bfclient_packet) msg.getUserData ();
        InetAddress srcAddr = pkt.getSrcAddr ();
        int srcPort = pkt.getSrcPort ();
        float cost = ByteBuffer.wrap (pkt.getUserData ()).getFloat ();
        
        bfclient_repo repo = bfclient_repo.getRepo ();
        repo.enableLocalLink (srcAddr, srcPort, cost);
        bfclient.logInfo ("local link up");
    }
    
    void processSndSimpleTrans (bfclient_msg msg) {
        bfclient.logInfo ("processSndSimpleTrans");
      
        try {
            bfclient_repo repo = bfclient_repo.getRepo ();  
            String dstAddrStr = msg.dequeue ();
            String dstPortStr = msg.dequeue ();
            String fName = msg.dequeue ();
            String chunkNum = msg.dequeue ();
                
            InetAddress addr = InetAddress.getByName (dstAddrStr);
            int port = Integer.parseInt (dstPortStr);
            InetAddress myAddr = repo.getLocalAddr ();
            int myPort = repo.getPort ();
            byte cId = Byte.parseByte (chunkNum);
            
            // read file
            File f = new File (fName);
            InputStream insputStream = new FileInputStream (f);
            long fLen = f.length ();
            byte[] fData = new byte [(int)fLen];
            insputStream.read (fData);
            insputStream.close ();
            
            // random drop packets
            if (bfclient_repo.getRepo ().getReliableL2 () == false &&
                Math.random () % 2 == 0) {
                bfclient.logInfo ("Packet dropped - randomly.");
                return;
            } else {            
                bfclient_packet pkt = new bfclient_packet ();
                pkt.setDstAddr  (addr);
                pkt.setDstPort  (port);
                pkt.setSrcAddr  (myAddr);
                pkt.setSrcPort  (myPort);
                pkt.setType     (bfclient_packet.M_USER_BASIC_TRANS);
                pkt.setChunkId  (cId);
                pkt.setUserData (fData); 
                
                bfclient_rentry nextHop = repo.searchRoutingTable (addr, port);
                sendPacket (pkt.pack (), nextHop);
            }
            
        } catch (Exception e) {
            bfclient.logErr ("Error while sending packet");
            bfclient.logErr ("Wait for retry");
            
        }
        return;
    }
    
    void processRcvSimpleTrans (bfclient_msg msg) {
        
        bfclient.logInfo ("processRcvSimpleTrans");
        bfclient_repo repo = bfclient_repo.getRepo ();
        
        // random drop packets
        if (repo.getReliableL2 () == false &&
            Math.random () % 2 == 0) {
            bfclient.logInfo ("Packet dropped - randomly.");
            return;
        }
        
        bfclient_packet pkt = (bfclient_packet) msg.getUserData ();
        InetAddress srcAddr = pkt.getSrcAddr ();
        int srcPort = pkt.getSrcPort ();
        InetAddress dstAddr = pkt.getDstAddr ();
        int dstPort = pkt.getDstPort ();
        
        // 1. send ack back to the sender
        bfclient_packet ackPkt = new bfclient_packet ();
        ackPkt.setDstAddr  (srcAddr);
        ackPkt.setDstPort  (srcPort);
        ackPkt.setSrcAddr  (dstAddr);
        ackPkt.setSrcPort  (dstPort);
        ackPkt.setType     (bfclient_packet.M_USER_BASIC_TRANS_ACK);
        ackPkt.setChunkId  (pkt.getChunkId ());
            
        bfclient_rentry nextHop = repo.searchRoutingTable (srcAddr, srcPort);
        sendPacket (ackPkt.pack (), nextHop);
        
        // 2. send a report to the upper layer
        bfclient_msg report = new bfclient_msg (bfclient_worker.M_RCV_SIMPLE_TRANSFER);
        report.setBinData (pkt.getUserData ());
        report.enqueue (Byte.toString (pkt.getChunkId ()));
        bfclient_worker.getWorker ().enqueueMsg (report);
    }
    
    void processRcvSimpleTransAck (bfclient_msg msg) {
        
        bfclient.logInfo ("processRcvSimpleTransAck");
        
        bfclient_packet pkt = (bfclient_packet) msg.getUserData ();
        int id = pkt.getChunkId ();
        String idStr = Integer.toString (id);
        
        bfclient_msg rsp = new bfclient_msg (bfclient_worker.M_RCV_SIMPLE_TRANS_ACK);
        rsp.enqueue (idStr);
        bfclient_worker.getWorker ().enqueueMsg (rsp);
    }
    
    // msg is the packet to send, rentry is the matching routing table entry
    void sendPacket (byte[] msg, bfclient_rentry rentry) {
        
        if (rentry != null) {
            
            if (rentry.getCost () >= bfclient_rentry.M_MAX_LINE_COST) {
                bfclient.logErr ("Destination not reachable - " + rentry);
                return;
            }
            
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
            bfclient.logErr ("Host unreachable");
        }
    }
    

}