import java.util.*;
import java.nio.charset.*;

public class bfclient_msg {

    LinkedList<String> m_data;
    byte[] m_binData;

    public bfclient_msg () {
        m_data = new LinkedList<String> ();
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
    
    public byte[] flatten () {
        
        String outString = new String ();
        
        for (String s: m_data) {
            outString += (s + '\n');
        }
        
        byte[] cmd = outString.getBytes (Charset.forName ("UTF-8"));
        
        if (m_binData != null) {
            byte[] out = new byte[cmd.length + m_binData.length];
            System.arraycopy (cmd, 0, out, 0, cmd.length);
            System.arraycopy (m_binData, 0, out, out.length, m_binData.length);
            return out;
        } else {
            return cmd;
        }
    }
    
    public void showHeader () {
        if (m_data.size () == 0)
            bfclient.logInfo ("[MSG HEADER] empty msg");
        else
            bfclient.logInfo ("[MSG HEADER] " + m_data.element ());
    }
} 