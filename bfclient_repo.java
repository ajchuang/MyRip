import java.io.*;
import java.net.*;
import java.util.*;

public class bfclient_repo {

    static bfclient_repo m_repo;
    static InetAddress m_hostAddr;
    
    int m_port;
    int m_timeout;
    String m_configFileName;
    
    // The routing table
    Vector<bfclient_rentry> m_rtable;
    Vector<bfclient_rentry> m_localEntryIdx;
    
    // synchronous lock
    Object m_lock;
    
    static {
        try {
            m_repo = null;
            m_hostAddr = InetAddress.getLocalHost ();
        } catch (Exception e) {
            bfclient.logExp (e, true);
        }
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
        m_rtable = new Vector<bfclient_rentry> ();
        m_localEntryIdx = new Vector<bfclient_rentry> ();
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
            
            int intf = 0;
            
            while ((line = br.readLine ()) != null) {
                String[] ln = line.split (":| ");
                bfclient.logInfo ("ip: " + ln[0] + " port: " + ln[1] + " weight: " + ln[2]);
                
                // insert into routing table
                bfclient_rentry ent = bfclient_rentry.rentryFactory (ln[0], ln[1], ln[2], true);
                ent.setOn (true);
                ent.setIntfIdx (intf++);
                m_rtable.add (ent);
                m_localEntryIdx.add (ent);
            }
            
            br.close ();
            
        } catch (Exception e) {
            bfclient.logExp (e, true);    
        } 
    }

    // getters & setters
    public final InetAddress getLocalAddr () {
        return m_hostAddr;
    }
    
    public void setPort (int port) {
        m_port = port;
    }
    
    public int getPort () {
        return m_port;
    }
    
    // @lfred: unit in seconds
    public void setTimeout (int to) {
        m_timeout = to;
    } 
    
    // @lfred: unit in second
    public int getTimeout () {
        return m_timeout;
    }
    
    // not thread safe printing.
    public void showRouteTable () {
        synchronized (m_lock) {
            System.out.println ("Current Routing Table: ");
            System.out.println ("Destination\t\tCost\tInterface");
            for (bfclient_rentry ent:m_rtable) {
                System.out.println (ent);
            }
        }
    }
    
    public final bfclient_rentry searchRoutingTable (InetAddress addr, int port) {
        
        bfclient_rentry ret = null;
        
        synchronized (m_lock) {
            for (bfclient_rentry ent:m_rtable) {
                if (ent.compare (addr, port) == true) {
                    ret = ent;
                }
            }
        }
        
        return ret;
    }
    
    //public byte[] getRoutingTable () {
    //}
}