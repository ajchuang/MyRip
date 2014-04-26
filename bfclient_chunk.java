
public class bfclient_chunk implements Comparable<bfclient_chunk> {

    int m_chunkId;
    byte[]  m_chunkData;
    
    public bfclient_chunk (int id, byte[] data) {
        m_chunkId = id;
        m_chunkData = data;
    } 
    
    public int getId () {
        return m_chunkId;
    }
    
    public byte[] getData () {
        return m_chunkData;
    }
    
    @Override
    public int compareTo (bfclient_chunk comp) {
        return (m_chunkId - comp.getId ());
    }
}