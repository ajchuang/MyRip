import java.io.*;
import java.net.*;

public class bfclient_repo {

    static bfclient_repo m_repo;

    int m_port;
    int m_timeout;
    String m_configFileName;
    Object m_lock;
    
    static {
        m_repo = null;
    }
    
    public static bfclient_repo createRepo (String fname) {
        
        if (m_repo != null) {
            bfclient.logErr ("double creation");
            System.exit (0);
            return null;
        }
        
        m_repo = new bfclient_repo (fname);
        return m_repo;
    }
    
    public static bfclient_repo getRepo () {
        
        if (m_repo == null) {
            bfclient.logErr ("getRepo before creation.");
            System.exit (0);
            return null;
        }

        return m_repo;
    }
    
    private bfclient_repo (String fname) {
        m_configFileName = fname;
        m_lock = new Object ();
    }
    
    public void parseConfigFile () {
    
        BufferedReader br = null;
        String line = null;
        
        try {    
            File f = new File (m_configFileName);
            
            if (f.exists () == false) {
                bfclient.logErr ("The configuration file, " + m_configFileName + ", does not exist");
                System.exit (0);
                return;
            }
            
            br = new BufferedReader (new FileReader (m_configFileName));
            
            // Ln1 is different from others
            line = br.readLine ();
            if (line != null) {
                String[] lns = line.split (" ");
                
                if (lns.length == 2) {
                    bfclient.logInfo ("Port: " + lns[0] + " timeout: " + lns[1]);
                    m_port = Integer.parseInt (lns[0]);
                    m_timeout = Integer.parseInt (lns[1]);
                } else if (lns.length == 4) {
                    bfclient.logInfo ("Port: " + lns[0] + " timeout: "  + lns[1]);
                    bfclient.logInfo ("File: " + lns[2] + " sequence: " + lns[3]);
                    m_port = Integer.parseInt (lns[0]);
                    m_timeout = Integer.parseInt (lns[1]);
                } else {
                    bfclient.logErr ("Incorrect config file line 1");
                    System.exit (0);
                }
                
            } else {
                bfclient.logErr ("Empty file.");
                System.exit (0);
            }
            
            while ((line = br.readLine ()) != null) {
                String[] ln = line.split (":| ");
                bfclient.logInfo ("ip: " + ln[0] + " port: " + ln[1] + " weight: " + ln[2]);
            }
            
            br.close ();
            
        } catch (Exception e) {
            bfclient.logExp (e, true);    
        } 
    }

    // getters & setters
    public void setPort (int port) {
        m_port = port;
    }
    
    public int getPort () {
        return m_port;
    }
    
    public void setTimeout (int to) {
        m_timeout = to;
    } 
    
    public int getTimeout () {
        return m_timeout;
    }
}