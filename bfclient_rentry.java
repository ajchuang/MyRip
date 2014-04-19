import java.net.*;
import java.util.*;
import java.nio.*;

public class bfclient_rentry {

    public final static int M_DEFLATE_SIZE = 20;

    InetAddress m_addr;
    int     m_port;
    float   m_linkCost;
    
    bfclient_rentry m_nextHop;
    
    int     m_intfIdx;  // which interface we send
    boolean m_isOn;     // if the interface is on
    boolean m_localIntf;
    
    public static bfclient_rentry rentryFactory (byte[] data) {
        bfclient_rentry ent = null;
        
        try { 
            ent = new bfclient_rentry ();
            ent.inflate (data);
        } catch (Exception e) {
            bfclient.logExp (e, false);
        }
        
        return ent;
    }
    
    public static bfclient_rentry rentryFactory (InetAddress addr, int port) {
        bfclient_rentry ent = null;
        
        try { 
            ent = 
                new bfclient_rentry (
                        addr,
                        port,
                        (float)0.0,
                        false);
        } catch (Exception e) {
            bfclient.logExp (e, false);
        }
        
        return ent;
    }
    
    public static bfclient_rentry rentryFactory (String addr, String port, String linkCost, boolean localIf) {
        
        bfclient_rentry ent = null;
        
        try { 
            ent = 
                new bfclient_rentry (
                        InetAddress.getByName (addr),
                        Integer.parseInt (port),
                        Float.parseFloat (linkCost),
                        localIf);
        } catch (Exception e) {
            bfclient.logExp (e, false);
        }
        
        return ent;
    }
    
    private bfclient_rentry (InetAddress addr, int port, float linkCost, boolean localIf) {
        
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
    
    private bfclient_rentry () {

        // conservative default value
        m_localIntf = false;
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
    
    public float getCost () {
        return m_linkCost;
    }
    
    public void setCost (float c) {
        m_linkCost = c;
    }
    
    public boolean getOn () {
        return m_isOn;
    }
    
    public void setOn (boolean on) {
        m_isOn = on;
    }
    
    public bfclient_rentry getNextHop () {
        return m_nextHop;
    }
    
    public void setNextHop (bfclient_rentry next) {
        m_nextHop = next;
    }
    
    public InetAddress getAddr () {
        return m_addr;
    }
    
    public void setAddr (InetAddress a) {
        m_addr = a;
    }
    
    public int getPort () {
        return m_port;
    }
    
    public void setPort (int p) {
        m_port = p;
    }
    
    @Override
    public String toString () {
        
        String nextHop;
        
        if (m_localIntf == true) {
            nextHop = "DirectLink\t";
        } else {
            nextHop = m_nextHop.getAddr ().getHostAddress() + ":" + m_nextHop.getPort ();
        }
        
        return m_addr.getHostAddress () + ":" + m_port + "\t" + 
               m_linkCost + "\t" + 
               nextHop    + "\t" +
               m_intfIdx;
    }
    
    public boolean compare (InetAddress addr, int port) {
        
        if (addr.equals (m_addr) && port == m_port) 
            return true;
        else
            return false;
    }
    
    public byte[] deflate () {
        
        byte[] out = new byte[M_DEFLATE_SIZE];
        
        byte[] addr = m_addr.getAddress ();
        byte[] port = ByteBuffer.allocate(4).putInt(m_port).array ();
        byte[] cost = ByteBuffer.allocate(4).putFloat (m_linkCost).array ();
        byte[] nextAddr;
        byte[] nextPort;
        
        if (m_nextHop == null) {
            nextAddr = ByteBuffer.allocate(4).putInt(0).array ();
            nextPort = ByteBuffer.allocate(4).putInt(0).array ();
        } else {
            nextAddr = m_nextHop.getAddr().getAddress();
            nextPort = ByteBuffer.allocate(4).putInt(m_nextHop.getPort()).array ();
        }
        
        System.arraycopy (addr,     0, out,  0, 4);
        System.arraycopy (port,     0, out,  4, 4);
        System.arraycopy (cost,     0, out,  8, 4);
        System.arraycopy (nextAddr, 0, out, 12, 4);
        System.arraycopy (nextPort, 0, out, 16, 4);
        
        return out;
    }
    
    public void inflate (byte[] raw) {
        
        try {
            byte[] addr = new byte[4];
            byte[] port = new byte[4];
            byte[] cost = new byte[4];
            byte[] nextAddr = new byte[4];
            byte[] nextPort = new byte[4];
        
            System.arraycopy (raw,  0, addr,      0, 4);
            System.arraycopy (raw,  4, port,      0, 4);
            System.arraycopy (raw,  8, cost,      0, 4);
            System.arraycopy (raw, 12, nextAddr,  0, 4);
            System.arraycopy (raw, 16, nextPort,  0, 4);
        
            m_addr = InetAddress.getByAddress (addr);
            m_port = ByteBuffer.wrap (port).getInt ();
            m_linkCost = ByteBuffer.wrap (cost).getFloat ();
        
            bfclient_rentry nextHop = new bfclient_rentry ();
            nextHop.setAddr (InetAddress.getByAddress (nextAddr));
            nextHop.setPort (ByteBuffer.wrap (nextPort).getInt ());
        } catch (Exception e) {
            bfclient.logExp (e, true);
        }
    }
}