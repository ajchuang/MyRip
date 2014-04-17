import java.util.*;
import java.net.*;
import java.nio.*;

public class bfclient_packet {
    
    public final static byte M_PING_REQ = 0x01;
    public final static byte M_PING_RSP = 0x02;
    public final static byte M_TROUTE_REQ = 0x03;
    public final static byte M_TROUTE_RSP = 0x04;    
    public final static byte M_ROUTER_UPDATE = 0x10;

    InetAddress m_dstAddr;
    int         m_dstPort;
    InetAddress m_srcAddr;
    int         m_srcPort;
    byte        m_type;
    byte        m_reserve_01;
    byte        m_reserve_02;
    byte        m_pktId;
    int         m_dataLen;
    
    byte[]      m_userData;
    
    // create an empty packet (new packet)
    public bfclient_packet () {
        byte[] id = new byte[1];
        new Random ().nextBytes (id);
        m_pktId = id[0];
    }
    
    // inflate an incoming packet (for incoming packet)
    public bfclient_packet (byte[] rawPacket) {
        
        try {
            byte[] destRawAddr = new byte[4];
            byte[] destRawPort = new byte[4];
            byte[] srcRawAddr  = new byte[4];
            byte[] srcRawPort  = new byte[4];
            byte[] control     = new byte[4];
        
            byte[] rawDataLen  = new byte[4];
            
            System.arraycopy (rawPacket,   0, destRawAddr, 0, 4);
            System.arraycopy (rawPacket,   4, destRawPort, 0, 4);
            System.arraycopy (rawPacket,   8, srcRawAddr,  0, 4);
            System.arraycopy (rawPacket,  12, srcRawPort,  0, 4);
            System.arraycopy (rawPacket,  16, control,     0, 4);
            System.arraycopy (rawPacket,  20, rawDataLen,  0, 4);
        
            m_dstAddr = InetAddress.getByAddress (destRawAddr);
            m_dstPort = ByteBuffer.wrap (destRawPort).getInt ();
            m_srcAddr = InetAddress.getByAddress (srcRawAddr);
            m_srcPort = ByteBuffer.wrap (srcRawPort).getInt ();
            m_type    = control[0];
            m_pktId   = control[3];
            m_dataLen = ByteBuffer.wrap (rawDataLen).getInt ();
            
            if (m_dataLen != 0) {
                m_userData = new byte [m_dataLen];
                System.arraycopy (rawPacket,  24, m_userData,  0, m_dataLen);
            } else {
                m_userData = null;
            }
            
        } catch (Exception e) {
            bfclient.logExp (e, false);
        }
    }
    
    public final InetAddress getDstAddr () {
        return m_dstAddr;
    }
    
    public void setDstAddr (InetAddress addr) {
        m_dstAddr = addr;
    }
    
    public int getDstPort () {
        return m_dstPort; 
    }
    
    public void setDstPort (int port) {
        m_dstPort = port; 
    }
    
    public final InetAddress getSrcAddr () {
        return m_srcAddr;
    }
    
    public void setSrcAddr (InetAddress addr) {
        m_srcAddr = addr;
    }
    
    public int getSrcPort () {
        return m_srcPort; 
    }
    
    public void setSrcPort (int port) {
        m_srcPort = port; 
    }
    
    public byte getType () {
        return m_type;
    }
    
    public void setType (byte type) {
        m_type = type;
    }
    
    public byte getPktId () {
        return m_pktId;
    }
    
    public void setUserData (byte[] uData) {
        
        m_userData = new byte[uData.length];
        m_dataLen = uData.length;
        
        System.arraycopy (uData, 0, m_userData, 0, uData.length);
    }
    
    public byte[] getUserData () {
        return m_userData;
    }
    
    @Override
    public String toString () {
        return new String ("PID: " + m_pktId + " TYPE: " + m_type);
    }
}