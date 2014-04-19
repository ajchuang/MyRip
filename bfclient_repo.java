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
    ArrayList<bfclient_rentry> m_rtable;
    ArrayList<bfclient_rentry> m_localEntryIdx;
    
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
        m_rtable = new ArrayList<bfclient_rentry> ();
        m_localEntryIdx = new ArrayList<bfclient_rentry> ();
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
    
    // bad method - just to simplify
    public void showRouteTable () {
        bfclient.printMsg ("Current Routing Table: ");
        bfclient.printMsg ("Destination\t\tCost\tNexthop\t\t\tInterface");
        
        synchronized (m_lock) {
            for (bfclient_rentry ent:m_rtable) {
                if (ent.getOn () == true) {
                    bfclient.printMsg (ent.toString ());
                }
            }
        }
    }
    
    public void addRoutingEntry (bfclient_rentry newEnt) {
        
        bfclient.logInfo ("addRoutingEntry: " + newEnt);
        
        synchronized (m_lock) {
            m_rtable.add (newEnt);
        }
    }
    
    public final bfclient_rentry searchRoutingTable (InetAddress addr, int port) {
        
        bfclient_rentry ret = null;
        
        synchronized (m_lock) {
            for (bfclient_rentry ent:m_rtable) {
                if (ent.getOn () == true && 
                    ent.compare (addr, port) == true) {
                    ret = ent;
                }
            }
        }
        
        return ret;
    }
    
    public final boolean diableLocalLink (InetAddress addr, int port) {
        
        bfclient_rentry localLink = searchRoutingTable (addr, port);
        int idx = getLocalIntfIdx (addr, port);
        
        if (localLink == null || localLink.isLocalIf () == false) {
            bfclient.logErr ("Local link not found: " + addr + ":" + port);
            return false;
        }
        
        synchronized (m_lock) {
            localLink.setOn (false);
            
            ListIterator<bfclient_rentry> lstItr =  m_rtable.listIterator ();
            
            // remove all entries starting from locallink
            while (lstItr.hasNext () == true) {
                
                bfclient_rentry ent = lstItr.next ();
                
                if (ent.isLocalIf () == false && 
                    ent.getIntfIdx () == idx) {
                    
                    // remove the entry
                    lstItr.remove ();
                }
            }
        }
        
        return true;
    }
    
    public final boolean enableLocalLink (InetAddress addr, int port) {
        bfclient_rentry localLink = searchRoutingTable (addr, port);
        int idx = getLocalIntfIdx (addr, port);
        
        synchronized (m_lock) {
            for (bfclient_rentry ent: m_localEntryIdx) {
                if (ent.getAddr ().equals (addr) && ent.getPort () == port)
                    ent.setOn (true);
            }
        }
        
        return true;
    }
    
    // get local i/f count
    public int getAllLocalIntfCnt () {
        
        int cnt = 0;
        
        synchronized (m_lock) {
            cnt = m_localEntryIdx.size ();
        }
        
        return cnt;
    }
    
    // get local i/f entry
    public bfclient_rentry getLocalIntfEntry (int idx) {
        
        bfclient_rentry ent = null;
        
        synchronized (m_lock) {
        if (idx < m_localEntryIdx.size ())
            ent = m_localEntryIdx.get (idx);
        else
            ent = null;
        }
        
        return ent;
    }
    
    public int getLocalIntfIdx (InetAddress addr, int port) {
        
        int cnt = 0;
        synchronized (m_lock) {
            for (bfclient_rentry ent:m_localEntryIdx) {
                if (ent.getAddr ().equals (addr) && ent.getPort () == port)
                    break;
                cnt++;
            }
        }
        
        return cnt;
    }
    
    // for given local entry, do 
    public byte[] getFlatRoutingTable (bfclient_rentry local_if) {
        
        if (local_if.getOn () == false) {
            return null;
        }
        
        byte[] out = new byte[m_rtable.size () * 20];
        int c_idx = 0;
        
        synchronized (m_lock) {
            for (bfclient_rentry ent: m_rtable) {
                byte[] current = ent.deflate ();
                System.arraycopy (current, 0, out, c_idx, 20);
                c_idx += 20;
            }
        }
        
        return out;
    }
}