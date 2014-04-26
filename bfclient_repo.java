import java.io.*;
import java.net.*;
import java.util.*;

public class bfclient_repo {

    static String sm_simpleTransFileName = "./fs/output";
    
    static bfclient_repo m_repo;
    static InetAddress m_hostAddr;
    
    int m_port;
    int m_timeout;
    String m_configFileName;
    
    // for simple trans
    String m_transFileName;
    int    m_chuckNum;
    LinkedList<bfclient_chunk> m_simpleTranChunks;
    
    // The routing table
    ArrayList<bfclient_rentry> m_rtable;
    ArrayList<bfclient_rentry> m_localEntryIdx;
    bfclient_rentry m_loopback;
    
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
        m_simpleTranChunks = new LinkedList<bfclient_chunk> (); 
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
                    m_transFileName = lns[2];
                    m_chuckNum = Integer.parseInt (lns[3]);
                } else {
                    bfclient.logErr ("Incorrect config file line 1");
                    System.exit (0);
                }
                
                // init loopback - testing sugar
                m_loopback = 
                    bfclient_rentry.rentryFactory (
                        m_hostAddr.getHostAddress (), 
                        Integer.toString (m_port), "0.0", true);
                m_loopback.setNextHop (null);
                
                // loading mychunks
                sm_simpleTransFileName += m_port;
                
                if (m_transFileName != null) {                    
                    try {
                        // read file
                        File cf = new File (m_transFileName);
                        InputStream insputStream = new FileInputStream (cf);
                        long fLen = cf.length ();
                        byte[] fData = new byte [(int)fLen];
                        insputStream.read (fData);
                        insputStream.close ();
                    
                        bfclient_chunk myChunk = new bfclient_chunk (m_chuckNum, fData);
                        m_simpleTranChunks.add (myChunk);
                    } catch (Exception e) {
                        bfclient.printMsg ("Fatal: Chunk file does not exist.");
                        bfclient.logExp (e, true);
                    }
                }
                
            } else {
                bfclient.logErr ("Empty file.");
                System.exit (0);
            }
            
            int intf = 0;
            
            while ((line = br.readLine ()) != null) {
                
                String[] ln = line.split (":| ");
                String nAddr = ln[0];
                String nPort = ln[1];
                String nWeight = ln[2];
                bfclient.logInfo ("ip: " + nAddr + " port: " + nPort + " weight: " + nWeight);
                
                // a sugar that allows testing script using local host - local host translation
                InetAddress neighbor = InetAddress.getByName (nAddr);
                
                if (neighbor.isLoopbackAddress () == true) {
                    nAddr = InetAddress.getLocalHost ().getHostAddress ();
                }
                
                // insert into routing table
                bfclient_rentry ent = bfclient_rentry.rentryFactory (nAddr, nPort, nWeight, true);
                //ent.setOn (true);
                ent.setIntfIdx (intf++);
                m_rtable.add (ent);
                m_localEntryIdx.add (ent);
            }
            
            br.close ();
            
        } catch (Exception e) {
            bfclient.logExp (e, true);    
        } 
    }
    
    public final String getFileName () {
        return m_transFileName;
    }
    
    public int getChunkNum () {
        return m_chuckNum;
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
                //if (ent.getOn () == true) {
                    bfclient.printMsg (ent.toString ());
                //}
            }
        }
    }
    
    public void showLocalInterfaces () {
        bfclient.printMsg (
            "Local address:port = " + m_hostAddr.getHostAddress () + ":" + m_port);
    }
    
    public void addRoutingEntry (bfclient_rentry newEnt) {
        
        bfclient.logInfo ("addRoutingEntry: " + newEnt);
        
        synchronized (m_lock) {
            m_rtable.add (newEnt);
        }
    }
    
    public final bfclient_rentry searchRoutingTable (InetAddress addr, int port) {
        
        bfclient_rentry ret = null;
        
        // you're sending to yourself.
        if (addr.equals (m_hostAddr) && port == m_port) {
            return m_loopback;
        }
        
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
    
    public final bfclient_rentry searchAllRoutingTable (InetAddress addr, int port) {
        
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
    
    public final boolean diableLocalLink (InetAddress addr, int port) {
        
        bfclient_rentry localLink = searchRoutingTable (addr, port);
        int idx = getLocalIntfIdx (addr, port);
        
        if (localLink == null || localLink.isLocalIf () == false) {
            bfclient.logErr ("Local link not found: " + addr + ":" + port);
            return false;
        }
        
        synchronized (m_lock) {
            
            //localLink.setOn (false);
            localLink.setCost (bfclient_rentry.M_MAX_LINE_COST);
            
            ListIterator<bfclient_rentry> lstItr =  m_rtable.listIterator ();
            
            // remove all entries starting from locallink
            while (lstItr.hasNext () == true) {
                
                bfclient_rentry ent = lstItr.next ();
                
                if (ent.isLocalIf () == false && 
                    ent.getIntfIdx () == idx) {
                    
                    // remove the entry
                    // lstItr.remove ();
                    // ent.setOn (false);
                    ent.setCost (bfclient_rentry.M_MAX_LINE_COST);
                }
            }
        }
        
        return true;
    }
    
    public final boolean enableLocalLink (InetAddress addr, int port, float cost) {
        bfclient_rentry localLink = searchAllRoutingTable (addr, port);
        
        if (localLink != null)
            localLink.setCost (cost);
        
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
    
    public void checkLastUpdateTime () {
        long now = System.currentTimeMillis ();
        
        synchronized (m_lock) {
            for (bfclient_rentry ent: m_rtable) {
                
                if ((ent.getOn ()) && 
                    (now - ent.getLastUpdateTime ()) > 3 * 1000 * m_timeout) {
                    bfclient.logErr ("Link: " + ent + " expired.");
                    ent.setCost (bfclient_rentry.M_MAX_LINE_COST);
                } 
            }
        }
    }
    
    public void onRcvSimpleChunk (bfclient_chunk chk) {
        
        m_simpleTranChunks.add (chk);
        Collections.sort (m_simpleTranChunks);
        
        // check missing part.
        for (int i=0; i<m_simpleTranChunks.size(); ++i) {
            if (m_simpleTranChunks.get (i).getId () != i+1) {
                // @lfred: still missing part
                return;
            }
        }
        
        try {
            // if the list is done, dump the data to file.
            FileOutputStream fos = new FileOutputStream (new File (sm_simpleTransFileName));
        
            for (int i=0; i<m_simpleTranChunks.size(); ++i) {
                fos.write (m_simpleTranChunks.get(i).getData ());
            } 
            
            fos.flush ();
            fos.close ();
        } catch (Exception e) {
            bfclient.logExp (e, false);
        }
        
        return;
    }
    
    // for given local entry, do 
    public byte[] getFlatRoutingTable () { //bfclient_rentry local_if) {
        
        //if (local_if.getOn () == false) {
        //    return null;
        //}
        
        byte[] out = new byte[m_rtable.size () * bfclient_rentry.M_DEFLATE_SIZE];
        int c_idx = 0;
        
        synchronized (m_lock) {
            for (bfclient_rentry ent: m_rtable) {
                byte[] current = ent.deflate ();
                System.arraycopy (current, 0, out, c_idx, bfclient_rentry.M_DEFLATE_SIZE);
                c_idx += bfclient_rentry.M_DEFLATE_SIZE;
            }
        }
        
        return out;
    }
    
    
}