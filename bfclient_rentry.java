import java.net.*;
import java.util.*;

public class bfclient_rentry {

    InetAddress m_addr;
    int     m_port;
    double  m_linkCost;
    
    bfclient_rentry m_nextHop;
    
    int     m_intfIdx;  // which interface we send
    boolean m_isOn;     //
    
    public static bfclient_rentry rentryFactory (String addr, String port, String linkCost) {
        
        bfclient_rentry ent = null;
        
        try { 
            ent = 
                new bfclient_rentry (
                        InetAddress.getByName (addr),
                        Integer.parseInt (port),
                        Double.parseDouble (linkCost));
        } catch (Exception e) {
            bfclient.logExp (e, false);
        }
        
        return ent;
    }
    
    public bfclient_rentry (InetAddress addr, int port, double linkCost) {
        
        // major fields
        m_addr = addr;
        m_port = port;
        m_linkCost = linkCost;
        
        // conservative default value
        m_nextHop = null;
        m_intfIdx = -1;
        m_isOn = false;
    }
}