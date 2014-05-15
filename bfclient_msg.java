import java.util.*;
import java.nio.charset.*;

public class bfclient_msg {
    
    // msg type
    public final static int M_SEND_PING_REQ     = 1;
    public final static int M_RCV_PING_REQ      = 2;
    
    public final static int M_SEND_PING_RSP     = 3;
    public final static int M_RCV_PING_RSP      = 4;
    
    public final static int M_SEND_TROUTE_REQ   = 11;
    public final static int M_RCV_TROUTE_REQ    = 12;
    
    public final static int M_SEND_TROUTE_RSP   = 13;
    public final static int M_RCV_TROUTE_RSP    = 14;
    
    public final static int M_RCV_REMOTE_VEC    = 20;

    public final static int M_RCV_UNKNOWN_PKT   = 30;
    public final static int M_DO_FORWARD        = 31;
    public final static int M_UPDATE_TIMER_TO   = 32;
    
    public final static int M_LINK_DOWN         = 40;
    public final static int M_RCV_LINK_DOWN     = 41;
    public final static int M_LINK_UP           = 42;
    public final static int M_RCV_LINK_UP       = 43;
    
    public final static int M_SND_SMPL_TRANS_DATA  = 70;
    public final static int M_RCV_SMPL_TRANS_DATA  = 71;
    public final static int M_SND_SMPL_TRANS_ACK   = 72;
    public final static int M_RCV_SMPL_TRANS_ACK   = 73;
    
    public final static int M_SND_DISCOVER_REQ    = 80;
    public final static int M_RCV_DISCOVER_REQ    = 81;
    public final static int M_RCV_DISCOVER_ACK    = 82;
    public final static int M_RCV_DISCOVER_NAK    = 83;
    
    
    int m_type;
    LinkedList<String> m_data;
    byte[] m_binData;
    Object m_userData;

    public bfclient_msg (int type) {
        
        m_type = type;
        m_data = new LinkedList<String> ();
    }
    
    public int getType () {
        return m_type;
    }
    
    public void enqueue (String s) {
        m_data.addLast (s);
    } 
    
    public String dequeue () {
        return m_data.removeFirst ();
    }
    
    public String peek (int idx) {
        return m_data.get (idx);
    }
    
    public int getCnt () {
        return m_data.size ();
    }
    
    public void setUserData (Object o) {
        m_userData = o;
    }
    
    public Object getUserData () {
        return m_userData;
    }
    
    public void setBinData (byte[] binData) {
        m_binData = binData;
    }
    
    public byte[] getBinData () {
        return m_binData;
    }
    
    public void showHeader () {
        bfclient.logInfo ("[MSG TYPE] " + m_type);
    }
} 