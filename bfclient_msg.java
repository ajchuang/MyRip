import java.util.*;

public class bfclient_msg {

    LinkedList<String> m_data;

    public bfclient_msg () {
        m_data = new LinkedList<String> ();
    }
    
    public void pushStr (String s) {
        m_data.push (s);
    } 
    
    public String popStr () {
        return m_data.pop ();
    }
    
    public int getCnt () {
        return m_data.size ();
    }
    
    public void showHeader () {
        
        if (m_data.size () == 0)
            bfclient.logDetail ("[MSG HEADER] empty msg");
        else
            bfclient.logDetail ("[MSG HEADER] " + m_data.peek ());
    }
} 