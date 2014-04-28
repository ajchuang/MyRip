import java.net.*;
import java.util.*;
import java.nio.*;

public class bfclient_rentry {

    public final static int M_DEFLATE_SIZE = 20;
    public final static float M_MAX_LINE_COST = (float)999.0;
    
    InetAddress m_addr;
    int     m_port;
    float   m_linkCost;
    
    bfclient_rentry m_nextHop;
    
    int     m_intfIdx;  // which interface we send
    //boolean m_isOn;     // if the interface is on
    //boolean m_localIntf;
    
    long    m_lastUpdateTime;
    
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
            ent = new bfclient_rentry (addr, port, (float)0.0, false);
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
        //m_localIntf = localIf;
        
        // conservative default value
        m_nextHop = null;
        m_intfIdx = -1;
        //m_isOn = false;
        
        m_lastUpdateTime = System.currentTimeMillis ();
    }
    
    private bfclient_rentry () {

        // conservative default value
        //m_localIntf = false;
        m_nextHop = null;
        m_intfIdx = -1;
        //m_isOn = false;
        
        m_lastUpdateTime = System.currentTimeMillis ();
    }
    
    // @lfred: copy constructor for convinience
    public bfclient_rentry (bfclient_rentry copyEnt) {
        
        m_addr = copyEnt.getAddr ();
        m_port = copyEnt.getPort ();
        m_linkCost = getCost ();
        m_intfIdx = copyEnt.getIntfIdx ();
        //m_localIntf = copyEnt.isLocalIf ();
        m_lastUpdateTime = copyEnt.getLastUpdateTime ();
        
        if (m_nextHop == null)
            m_nextHop = null;
        else
            m_nextHop = new bfclient_rentry (copyEnt.getNextHop ());
    }
    
    public boolean isLocalIf () {
        if (m_nextHop == null) {
            return true;
        } else
            return false;
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
        if (m_linkCost >= M_MAX_LINE_COST)
            return false;
        else
            return true;
            
        //return m_isOn;
    }
    
    //public void setOn (boolean on) {
        //m_isOn = on;
    //}
    
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
        String linkCost;
        
        /*
        if (m_localIntf == true) {
            nextHop = "DirectLink\t";
        } else {
            if (m_nextHop != null)
                nextHop = m_nextHop.getAddr ().getHostAddress() + ":" + m_nextHop.getPort ();
            else
                nextHop = "Unknown next hop";
        }
        */
        
        if (m_nextHop == null) {
            nextHop = "DirectLink\t";
        } else {
            nextHop = m_nextHop.getAddr ().getHostAddress() + ":" + m_nextHop.getPort (); 
        } 
        
        if (m_linkCost >= M_MAX_LINE_COST) {
            linkCost = "Inf";
        } else {
            linkCost = Float.toString (m_linkCost);
        }
        
        return (m_addr.getHostAddress () + ":" + m_port + "\t" + 
                linkCost     + "\t" + 
                nextHop      + "\t" +
                m_intfIdx    + "\t" +
                (m_lastUpdateTime/1000));
    }
    
    public boolean compare (InetAddress addr, int port) {
        
        if (addr.equals (m_addr) && port == m_port) 
            return true;
        else
            return false;
    }
    
    public long getLastUpdateTime () {
        return m_lastUpdateTime;
    }
    
    public void setUpdate () {
        m_lastUpdateTime = System.currentTimeMillis ();
    }
    
    public byte[] deflate (boolean isSetToInf) {
        
        byte[] out = new byte[M_DEFLATE_SIZE];
        
        byte[] addr = m_addr.getAddress ();
        byte[] port = ByteBuffer.allocate(4).putInt(m_port).array ();
        byte[] cost;
        byte[] nextAddr;
        byte[] nextPort;
        
        if (isSetToInf)
            cost = ByteBuffer.allocate(4).putFloat (M_MAX_LINE_COST).array ();
        else
            cost = ByteBuffer.allocate(4).putFloat (m_linkCost).array ();
        
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
            
            int nextPortNum = ByteBuffer.wrap (nextPort).getInt ();
        
            if (nextPortNum == 0 && ByteBuffer.wrap (nextAddr).getInt () == 0) {
                m_nextHop = null;
            } else {
                m_nextHop = new bfclient_rentry ();
                m_nextHop.setAddr (InetAddress.getByAddress (nextAddr));
                m_nextHop.setPort (nextPortNum);
            }
        } catch (Exception e) {
            bfclient.logExp (e, true);
        }
    }
}