import java.util.*;
import java.net.*;
import java.nio.*;

public class bfclient_packet {
    
    public final static int M_PKT_HEADER_SIZE = 24;
    
    // Packet types
    // control packets
    public final static byte M_PING_REQ = 0x01;
    public final static byte M_PING_RSP = 0x02;
    public final static byte M_TROUTE_REQ = 0x03;
    public final static byte M_TROUTE_RSP_OK = 0x04;
    public final static byte M_TROUTE_RSP_FAILED = 0x05;   
    
    // routing packets
    public final static byte M_ROUTER_UPDATE = 0x10;
    
    // user packet
    public final static byte M_USER_DATA = 0x20;
    public final static byte M_USER_BASIC_TRANS = 0x21;
    public final static byte M_USER_BASIC_TRANS_ACK = 0x22;
    
    
    public final static byte M_LINK_DOWN    = 0x30;
    public final static byte M_LINK_UP      = 0x31;
    
    // error msg
    public final static byte M_HOST_NOT_REACHABLE  = (byte)0xf0;
    public final static byte M_HOST_UNKNOWN_PACKET = (byte)0xf1;
    //------------ 

    InetAddress m_dstAddr;
    int         m_dstPort;
    InetAddress m_srcAddr;
    int         m_srcPort;
    byte        m_type;
    byte        m_chunkId;
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
            m_chunkId = control[1];
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
    
    public void setChunkId (byte cId) {
        m_chunkId = cId;
    }
    
    public byte getChunkId () {
        return m_chunkId;
    }
    
    public void setUserData (byte[] uData) {
        
        m_userData = new byte[uData.length];
        m_dataLen = uData.length;
        
        System.arraycopy (uData, 0, m_userData, 0, uData.length);
    }
    
    public byte[] getUserData () {
        return m_userData;
    }
    
    public byte[] pack () {
        byte[] destRawAddr = m_dstAddr.getAddress ();
        byte[] destRawPort = ByteBuffer.allocate(4).putInt(m_dstPort).array ();
        byte[] srcRawAddr  = m_srcAddr.getAddress ();
        byte[] srcRawPort  = ByteBuffer.allocate(4).putInt(m_srcPort).array ();
        byte[] control     = {m_type, (byte)0x00, (byte)0x00, m_pktId};
        
        byte[] rawDataLen;
        byte[] rawData;
        int packetTotalLen = M_PKT_HEADER_SIZE;
        
        if (m_userData != null) {
            rawData     = m_userData;
            rawDataLen  = ByteBuffer.allocate(4).putInt(m_userData.length).array ();
            packetTotalLen = M_PKT_HEADER_SIZE + m_userData.length;
        } else {
            rawData     = null;
            rawDataLen  = ByteBuffer.allocate(4).putInt(0x00000000).array ();
            packetTotalLen = M_PKT_HEADER_SIZE;
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
    
    @Override
    public String toString () {
        return new String ("PID: " + m_pktId + " TYPE: " + m_type);
    }
}