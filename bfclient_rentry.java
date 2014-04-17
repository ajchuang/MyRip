import java.net.*;
import java.util.*;

public class bfclient_rentry {

    InetAddress m_addr;
    int     m_port;
    double  m_linkCost;
    
    bfclient_rentry m_nextHop;
    
    int     m_intfIdx;  // which interface we send
    boolean m_isOn;     // if the interface is on
    boolean m_localIntf;
    
    public static bfclient_rentry rentryFactory (String addr, String port, String linkCost, boolean localIf) {
        
        bfclient_rentry ent = null;
        
        try { 
            ent = 
                new bfclient_rentry (
                        InetAddress.getByName (addr),
                        Integer.parseInt (port),
                        Double.parseDouble (linkCost),
                        localIf);
        } catch (Exception e) {
            bfclient.logExp (e, false);
        }
        
        return ent;
    }
    
    public bfclient_rentry (InetAddress addr, int port, double linkCost, boolean localIf) {
        
        // major fields
        m_addr = addr;
        m_port = port;
        m_linkCost = linkCost;
        m_localIntf = localIf;
        
        // conservative default value
        m_nextHop = null;
        m_intfIdx = -1;
        m_isOn = false;
    }
    
    public boolean isLocalIf () {
        return m_localIntf;
    }
    
    public int getIntfIdx () {
        return m_intfIdx;
    }
    
    public void setIntfIdx (int idx) {
        m_intfIdx = idx;
    }
    
    public boolean getOn () {
        return m_isOn;
    }
    
    public void setOn (boolean on) {
        m_isOn = on;
    }
    
    public void setNextHop (bfclient_rentry next) {
        m_nextHop = next;
    }
    
    public bfclient_rentry getNextHop () {
        return m_nextHop;
    }
    
    public InetAddress getAddr () {
        return m_addr;
    }
    
    public int getPort () {
        return m_port;
    }
    
    @Override
    public String toString () {
        return m_addr.getHostAddress () + ":" + m_port + "\t" + m_linkCost + "\t" + m_intfIdx;
    }
    
    public boolean compare (InetAddress addr, int port) {
        
        if (addr.equals (m_addr) && port == m_port) 
            return true;
        else
            return false;
    }
    
    //public static byte[] deflate (bfclient_rentry ent) {
    //}
    
    //public static bfclient_rentry inflate (byte[] raw) {
    //}
}