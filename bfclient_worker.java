//  @lfred: design idea
//  command line --> worker (worker will now blocking the command line)
//  worker --> proc (worker will not be blocked)
//  proc --> report event to worker
//  Upon completion or failure, the worker will respond to the proc, and carry on
import java.util.*;
import java.net.*;
import java.io.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;
import java.nio.*;

public class bfclient_worker implements Runnable {
    
    // messages
    public final static int M_START_SIMPLE_TRANSFER = 0x0000;
    public final static int M_RCV_SIMPLE_TRANSFER   = 0x0001;
    public final static int M_RCV_SIMPLE_TRANS_TO   = 0x0002;
    public final static int M_RCV_SIMPLE_TRANS_ACK  = 0x0003;
    
    public final static int M_START_PING            = 0x0010;
    public final static int M_RCV_PING_RSP          = 0x0011;
    public final static int M_RCV_PING_TO           = 0x0012;
    
    public final static int M_START_TROUTE          = 0x0020;
    public final static int M_RCV_TROUTE_RSP        = 0x0021;
    public final static int M_RCV_TROUTE_TO         = 0x0022;
    
    public final static int M_START_TRACEROUTE      = 0x0020;
    
    // opcodes
    public final static int M_OP_NONE = 0x0000;
    public final static int M_OP_PING = 0x0001;
    public final static int M_OP_SIMPLE_TRANS = 0x0010;
    
    LinkedBlockingQueue<bfclient_msg> m_queue;
    java.util.Timer m_opTimer;
    int   m_opCode;
    int   m_opRetryCnt;
    
    // thread lock mechanism
    Lock m_lock = new ReentrantLock ();
    Condition m_threadCond = m_lock.newCondition ();
    
    // singleton
    static bfclient_worker sm_worker = null;
    
    public static bfclient_worker getWorker () {
        if (sm_worker == null) {
            sm_worker = new bfclient_worker ();
        }
        
        return sm_worker;
    }
    
    private bfclient_worker () {
        m_queue = new LinkedBlockingQueue<bfclient_msg> ();
        m_opTimer = null;
        m_opCode = M_OP_NONE;
        m_opRetryCnt = 0;
    }
    
    public void assignWork (bfclient_msg m) {
        try {
            m_lock.lock ();
            m_opRetryCnt = 0;
            m_queue.put (m);
            m_threadCond.await ();
        } catch (Exception e) {
            bfclient.logExp (e, true);
        } finally {
            m_lock.unlock ();
        }
    }
    
    // for underlying process only
    public void enqueueMsg (bfclient_msg m) {
        try {
            m_queue.put (m);
        } catch (Exception e) {
            bfclient.logExp (e, true);
        }
    }
    
    public void run () {
        
        while (true) {
            try {
                bfclient_msg msg = m_queue.take ();
                msg.showHeader ();
                
                switch (msg.getType ()) {
                    
                    case M_START_SIMPLE_TRANSFER:
                        processSimpleTrans (msg);
                    break;
    
                    // When data are received.
                    case M_RCV_SIMPLE_TRANSFER:
                        processSimpleTransData (msg);
                    break;
                    
                    // When ack is received.
                    case M_RCV_SIMPLE_TRANS_ACK:
                        processSimpleTransAck (msg);
                    break;
                    
                    // When data transder is 
                    case M_RCV_SIMPLE_TRANS_TO:
                        processSimpleTransTO (msg);
                    break;
                    
                    case M_START_PING:
                        processPing (msg);
                    break;
                    
                    case M_RCV_PING_RSP:
                        processPingRsp (msg);
                    break;
                    
                    case M_RCV_PING_TO:
                        processPingTo (msg);
                    break;
                    
                    case M_START_TROUTE:
                    break;
                    
                    case M_RCV_TROUTE_RSP:
                    break;
                    
                    case M_RCV_TROUTE_TO:
                    break;
                    
                    
                }
                
                
            } catch (Exception e) {
                bfclient.logExp (e, false);
                m_threadCond.notifyAll ();
            }
        }
    }
    
    void processSimpleTrans (bfclient_msg msg) {
        
        bfclient.logInfo ("Worker: processSimpleTrans");
        bfclient_repo repo = bfclient_repo.getRepo ();
        
        if (m_opTimer != null) {
            bfclient.logErr ("Worker: Timer is not NULL");
        } else {
            m_opTimer = new java.util.Timer ("SimpleTrans");
            m_opTimer.schedule (
                (new TimerTask () {
                    public void run () {
                        bfclient_msg msg = 
                            new bfclient_msg (
                                bfclient_worker.M_RCV_SIMPLE_TRANS_TO);
                        enqueueMsg (msg);
                    }
                }), (long) 1000);
                
            m_opCode = M_OP_SIMPLE_TRANS;
        }
        
        String addr = msg.dequeue ();
        String port = msg.dequeue ();
        String fName = repo.getFileName ();
        int chuckNum = repo.getChunkNum ();
        
        bfclient_msg sTrans = new bfclient_msg (bfclient_msg.M_SND_SMPL_TRANS_DATA);
        sTrans.enqueue (addr);
        sTrans.enqueue (port);
        sTrans.enqueue (fName);
        sTrans.enqueue (Integer.toString (chuckNum));
        bfclient_proc.getMainProc ().enqueueMsg (sTrans);
    }
    
    // @lfred: on receiving data
    void processSimpleTransData (bfclient_msg msg) {
        
        bfclient_repo repo = bfclient_repo.getRepo ();
        int chunkNum = Integer.parseInt (msg.dequeue ());
        byte[] data = msg.getBinData ();
        
        // save data to file 'output'
        repo.onRcvSimpleChunk (new bfclient_chunk (chunkNum, data));
    }
    
    // @lfred: on receiving ack of the data
    void processSimpleTransAck (bfclient_msg msg) {
        // this is danger...
        m_opTimer.cancel ();
        m_opTimer = null;
        m_opCode = M_OP_NONE;
        
        bfclient.logInfo ("Worker: Receiving ACK for simple ack");
        bfclient.printMsg ("Chunk transfer completed.");
        
        // start the CLI thread
        m_lock.lock ();
        m_threadCond.signal ();
        m_lock.unlock ();
    }
    
    // @lfred: on transmission timer timeout
    void processSimpleTransTO (bfclient_msg msg) {
        
        m_opRetryCnt++;
        
        if (m_opRetryCnt == 3) {
            
            bfclient.printMsg ("Chunk transfer failed - unable to transfer.");
        
            m_opTimer = null;
            m_opCode = M_OP_NONE;
        
            // start the CLI thread
            m_lock.lock ();
            m_threadCond.signal ();
            m_lock.unlock ();

        } else {
            // resend data and restart timer
        }
        
    }
    
    void processPing (bfclient_msg msg) {
        
        bfclient.logInfo ("Worker: sending ping request");
        
        if (m_opTimer != null)
            bfclient.logErr ("Worker: Timer is not NULL");
            
        m_opTimer = new java.util.Timer ("PingTimer");
        m_opTimer.schedule (
            (new TimerTask () {
                public void run () {
                    bfclient_msg msg = new bfclient_msg (bfclient_worker.M_RCV_PING_TO);
                    enqueueMsg (msg);
                }
            }), (long) 2000);
    
        m_opCode = M_OP_PING;
        bfclient_msg ping = new bfclient_msg (bfclient_msg.M_SEND_PING_REQ);
        ping.enqueue (msg.dequeue ());
        ping.enqueue (msg.dequeue ());
        bfclient_proc.getMainProc ().enqueueMsg (ping);
    }
    
    void processPingRsp (bfclient_msg msg) {
        
        // this is danger...
        m_opTimer.cancel ();
        m_opTimer = null;
        m_opCode = M_OP_NONE;
        
        bfclient.logInfo ("Worker: Receiving ping response");
        bfclient.printMsg (
            "> ping response from " + 
            msg.dequeue () + ":" + 
            msg.dequeue ());
        
        // start the CLI thread
        m_lock.lock ();
        m_threadCond.signal ();
        m_lock.unlock ();
    }
    
    void processPingTo (bfclient_msg msg) {
        
        m_opTimer = null;
        m_opCode = M_OP_NONE;
        
        bfclient.printMsg ("> ping request timeout. No reachable.");
        
        // start the CLI thread
        m_lock.lock ();
        m_threadCond.signal ();
        m_lock.unlock ();
    }
}