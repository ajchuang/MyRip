import java.util.*;
import java.io.*;
import java.util.logging.*;
import java.security.*;
import java.math.*;
import java.text.*;
import java.net.*;


// @lfred: Thread analysis: 
// 1. UI thread (command line)
// 2. NW thread.
// 3. main thread.
// extra feature: ping, traceroute
public class bfclient {
    
    public final int M_MAX_FILE_SIZE = 61440;
    
    public final static String M_LINKDOWN  = "LINKDOWN";
    public final static String M_LINKUP    = "LINKUP";
    public final static String M_SHOWRT    = "SHOWRT";
    public final static String M_CLOSE     = "CLOSE";
    public final static String M_QUIT      = "QUIT";
    public final static String M_TRANSFER  = "TRANSFER";
    
    public final static String M_PING      = "PING";
    public final static String M_TROUTE    = "TRACEROUTE";
    public final static String M_LOG       = "LOG";
    public final static String M_HISTORY   = "HISTORY";
    public final static String M_IFCONFIG  = "IFCONFIG";
    public final static String M_LS        = "LS";
    public final static String M_CAT       = "CAT";
    public final static String M_RM        = "RM";
    public final static String M_RELIABE   = "RELIABLE";
    public final static String M_DISCOVER  = "DISCOVER";
    
    static String M_LOG_FILE;    
    static Logger sm_lgr;  
    static FileHandler sm_loggerFh;
    
    static Vector<String> m_history;
    static int            m_histIdx;
    
    static {
        try {
            // assign random log file name
            SecureRandom rdm = new SecureRandom ();
            M_LOG_FILE = "./log/" + new BigInteger (32, rdm).toString (16) + ".log";
            System.out.println ("Logging file: " + M_LOG_FILE);
                        
            sm_lgr = Logger.getAnonymousLogger ();
            sm_loggerFh = new FileHandler (M_LOG_FILE);
            sm_lgr.addHandler (sm_loggerFh);
            sm_lgr.setUseParentHandlers (false);
            sm_loggerFh.setFormatter (new SimpleFormatter ());
            
            // initialize history
            m_history = new Vector<String> ();
            m_histIdx = -1;
            
        } catch (Exception e) {
            System.out.println ("Severe exception in system booting");
            System.exit (0);
        }        
    }
    
    public static void logErr (String s) {
        sm_lgr.severe ("[Err] " + s);
    }
    
    public static void logInfo (String s) {
        sm_lgr.info ("[Info] " + s);
    }
    
    public static void logDetail (String s) {
        sm_lgr.info ("[DTL] " + s);
    }
    
    public static void printMsg (String s) {
        System.out.println (s);
    }
    
    public static void logExp (Exception e, boolean isSysDead) {
        System.out.println ("[Exp] " + e);
        e.printStackTrace ();
        
        if (isSysDead)
            System.exit (0);
    }
    
    public static void main (String[] args) throws Exception {
        
        if (args.length != 1) {
            logErr ("Parameter setting is incorrect.");
            logErr ("Please set your config file correctly");
            return;
        }
        
        // install the shutdown hook for knowing the log at the end
        Runtime.getRuntime().addShutdownHook (new Thread (new Runnable() {
            @Override
            public void run() {
                System.out.println ("\n*** Log file @ " + M_LOG_FILE + " ***\n");
            }
        }){});
        
        // create repo & parsing config
        String config = args[0];
        bfclient_repo repo = bfclient_repo.createRepo (config);
        repo.parseConfigFile ();
        
        // start main thread
        new Thread (bfclient_proc.getMainProc ()).start ();
        
        // start worker thread
        new Thread (bfclient_worker.getWorker ()).start ();
        
        // start server
        new Thread (bfclient_listener.getListener ()).start (); 
        
        // start UI
        bfclient bfc = new bfclient ();
        bfc.startConsole ();
    }
    
    public bfclient () {
    }
    
    public void startConsole () {
        
        Scanner scn = new Scanner (System.in);
        System.out.println ("Welcome to bfclient console!");
        
        while (true) {
            
            // read user input
            System.out.print ("$ ");
            String userInput = scn.nextLine ().trim ();
            
            // doing history check
            if (userInput.length () == 0) {
                continue;
            } else if (userInput.charAt (0) == '!') {
                try {
                    String histIdx = userInput.substring (1);
                    int idx = Integer.parseInt (histIdx);
                    userInput = m_history.elementAt (idx);
                } catch (Exception e) {
                    printMsg ("Invalid input");
                    continue;
                }
            } else {
                m_history.add (userInput);
            }
            
            // processing input tokens
            String[] toks = userInput.split (" ");
            
            if (toks.length == 0 || toks[0].length () == 0)
                continue;
            
            // we just take lower and upper case
            String cmd = toks[0].toUpperCase ();
            
            if (cmd.equals (M_LINKDOWN)) {
                processLinkDown (toks);
            } else if (cmd.equals (M_LINKUP)) {
                processLinkUp (toks);
            } else if (cmd.equals (M_SHOWRT)) {
                processShowRt (toks);
            } else if (cmd.equals (M_TRANSFER)) {
                processBasicTransfer (toks);
            } else if (cmd.equals (M_PING)) {
                processPing (toks);
            } else if (cmd.equals (M_TROUTE)) {
                processTroute (toks);
            } else if (cmd.equals (M_LOG)) {    
                processLog (toks);
            } else if (cmd.equals (M_CLOSE) || cmd.equals (M_QUIT)) {
                processClose ();
            } else if (cmd.equals (M_HISTORY)) {
                processHistory ();
            } else if (cmd.equals (M_IFCONFIG)) {
                processIfconfig ();
            } else if (cmd.equals (M_LS)) {
                processLs ();
            } else if (cmd.equals (M_CAT)) {
                processCat (toks);
            } else if (cmd.equals (M_RM)) {
                processRm (toks);
            } else if (cmd.equals (M_RELIABE)) {
                processReliable (toks);
            } else if (cmd.equals (M_DISCOVER)) {
                processDiscover (toks);
            } else {
                System.out.println ("Unknown command: " + userInput);
            }
            
            try {
                Thread.sleep (200);
            } catch (Exception e) {
                bfclient.logExp (e, false);
            }
        }
    }
    
    void processLinkDown (String[] toks) {
        
        if (toks.length != 3) {
            printMsg ("Command format error.");
            printMsg ("[Usage] LINKDOWN [ip] [port]");
            return;
        }
        
        bfclient.logInfo ("processLinkDown");
        
        String addr = toks[1];
        String port = toks[2];
        
        // syntax sugar
        addr = localhostTranslate (addr);
        
        bfclient_msg linkdown = new bfclient_msg (bfclient_msg.M_LINK_DOWN);
        linkdown.enqueue (addr);
        linkdown.enqueue (port);
        bfclient_proc.getMainProc ().enqueueMsg (linkdown);
    }
    
    void processLinkUp (String[] toks) {
        
        if (toks.length != 4) {
            printMsg ("Command format error.");
            printMsg ("[Usage] LINKUP [ip] [port] [weight]");
            return;
        }
        
        bfclient.logInfo ("processLinkUp");
        
        String addr = toks[1];
        String port = toks[2];
        String cost = toks[3];
        
        // syntax sugar
        addr = localhostTranslate (addr);
        
        bfclient_msg linkup = new bfclient_msg (bfclient_msg.M_LINK_UP);
        linkup.enqueue (addr);
        linkup.enqueue (port);
        linkup.enqueue (cost);
        bfclient_proc.getMainProc ().enqueueMsg (linkup);
    }
    
    void processShowRt (String[] toks) {
        printMsg ("Current time: " + (long)(System.currentTimeMillis ()/1000));
        bfclient_repo.getRepo ().showRouteTable ();
    }
    
    void processClose () {
        System.exit (0);
    }
    
    void processBasicTransfer (String[] toks) {
        
        bfclient_repo repo = bfclient_repo.getRepo ();
        String fName = repo.getFileName ();
        File f;
        
        try {
            f = new File (fName);
        } catch (Exception e) {
            printMsg ("Appointed file does not exist.");
            return;
        }
         
        if (toks.length != 3) {
            printMsg ("Command format error.");
            printMsg ("[Usage] transfer [ip] [port]");
            return;
        } else if (fName == null) {
            printMsg ("This client did not configure chunk");
            return;
        } else if (f.length () > 61440 || f.length () == 0) {
            printMsg ("The file size," + f.length () + ", is incorrect.");
            return;
        } 
        
        String addr = toks[1];
        String port = toks[2];
        printMsg ("Starting to transfer to " + addr + ":" + port);
        
        try {
            InetAddress dstAddr = InetAddress.getByName (addr);
            int dstPort = Integer.parseInt (port);
            bfclient_rentry rent = repo.searchRoutingTable (dstAddr, dstPort);
            
            if (rent != null) {
                bfclient_rentry next = rent.getNextHop ();
                
                if (next == null) {
                    printMsg ("The destination address is a direct link.");
                } else {
                    printMsg (
                        "The chunk, " + repo.getChunkNum () + 
                        " of the file, " + repo.getFileName () + 
                        " , is transferring to " + next.getAddr ().getHostAddress () + 
                        ":" + next.getPort ()); 
                }
                
            } else {
                printMsg ("The destination address is unreachable.");
                return;
            }
            
            // syntax sugar
            addr = localhostTranslate (addr);
            
            bfclient_msg req = new bfclient_msg (bfclient_worker.M_START_SIMPLE_TRANSFER);
            req.enqueue (addr);
            req.enqueue (port);
            bfclient_worker.getWorker ().assignWork (req);
        } catch (Exception e) {
            logExp (e, false);
        }
    }
    
    void processPing (String[] toks) {
        
        if (toks.length != 3) {
            printMsg ("Command format error.");
            printMsg ("[Usage] ping [ip] [port]");
            return;
        }
        
        String addr = toks[1];
        String port = toks[2];
        
        // syntax sugar
        addr = localhostTranslate (addr);
        
        bfclient_msg ping = new bfclient_msg (bfclient_worker.M_START_PING);
        ping.enqueue (addr);
        ping.enqueue (port);
        bfclient_worker.getWorker().assignWork (ping);
    }
    
    //  @lfred:
    //      Troute is used trace the routing path 
    //      input: dest_addr, dest_port
    void processTroute (String[] toks) {
        
        if (toks.length != 3) {
            printMsg ("Command format error.");
            printMsg ("[Usage] TRACEROUTE [ip] [port]");
            return;
        }
        
        String addr = toks[1];
        String port = toks[2];
        
        // syntax sugar
        addr = localhostTranslate (addr);
        
        bfclient_msg ping = new bfclient_msg (bfclient_msg.M_SEND_TROUTE_REQ);
        ping.enqueue (addr);
        ping.enqueue (port);
        bfclient_proc.getMainProc ().enqueueMsg (ping);
    }
    
    void processHistory () {
        
        printMsg ("History Commands");
        
        for (int i=0; i<m_history.size (); ++i) {
            printMsg (i + ")\t" + m_history.elementAt (i));
        }
    }
    
    void processIfconfig () {
        printMsg ("Local Interfaces:");
        bfclient_repo.getRepo ().showLocalInterfaces ();
    }
    
    //  @lfred: 
    //      this function prints the log file we are running.
    //      toks[] are not used actually.
    void processLog (String[] toks) {
        
        boolean isFilter = false;
        String  filter = null;
        
        if (toks.length >= 2) {
            filter = toks[1].toUpperCase (); 
            isFilter = true;
        }
        
        try {
            BufferedReader reader = new BufferedReader(new FileReader (M_LOG_FILE));
            String line;
            int toss = 0;
            
            while ((line = reader.readLine ()) != null) {
                if (toss % 2 == 1) {
                    if (isFilter == false)
                        System.out.println (line);
                    else if (isFilter == true) {
                        String log = line.toUpperCase ();
                        
                        if (log.indexOf (filter) != -1)
                            System.out.println (line);
                    }
                }
                    
                toss++;
            }
            
        } catch (Exception e) {
            bfclient.logExp (e, false);
        }
        
        printMsg ("Log file: " + M_LOG_FILE);
    }
    
    void processLs () {
        
        File dir = new File ("./fs");
        File listDir[] = dir.listFiles();
        
        for (int i = 0; i < listDir.length; i++) {
            File cur = listDir[i];
            if (cur.isFile ()) {
                SimpleDateFormat format = new SimpleDateFormat ("MM/dd/yyyy HH:mm:ss");
                printMsg (
                    cur.getName () + "\t" + 
                    "size: " + cur.length() + "\t" + 
                    "Last Update:" + format.format (cur.lastModified ()));
            }
        }
    }
    
    void processCat (String[] toks) {
        
        if (toks.length != 2) {
            printMsg ("Command error");
            printMsg ("cat [file name]");
            return;
        }
        
        int ci;
        byte c;
        
        try {
            String fName = "./fs/" + toks[1];
            File f = new File (fName);
            InputStream is = new FileInputStream (f);
        
            while ((ci = is.read ()) != -1) {
                c = (byte)ci;
                byte[] array = new byte[1];
                array[0] = c;
                System.out.print (new String (array)); 
            }
            
        } catch (Exception e) {
            //bfclient.logExp (e, false);
            printMsg ("\n The selected file does not exist");
        }
        
        printMsg ("");
        return;
    }
    
    void processRm (String[] toks) {
        
        if (toks.length != 2) {
            printMsg ("Command error");
            printMsg ("rm [file name]");
            return;
        }
        
        String fName = "./fs/" + toks[1];
        File f = new File (fName);
        
        if (f.delete ()) {
            printMsg ("File (" + toks[1] + ") deleted");
            return;
        } else {
            printMsg ("Error: File (" + toks[1] + ") NOT deleted");
        }
    }
    
    void processReliable (String[] toks) {
        
        if (toks.length != 2) {
            printMsg ("Command format error");
            printMsg ("reliable on or reliable off");
            return;
        }
        
        bfclient_repo repo = bfclient_repo.getRepo ();
        if (toks[1].equals ("on")) {
            repo.setReliableL2 (true);
        } else {
            repo.setReliableL2 (false);
        }
    }
    
    void processDiscover (String[] toks) {
        
        bfclient_repo repo = bfclient_repo.getRepo ();
        
        bfclient_msg discover = new bfclient_msg (bfclient_msg.M_SND_DISCOVER_REQ);
        bfclient_proc.getMainProc ().enqueueMsg (discover);
        
        // sleep more time
        try {
            Thread.sleep (500);
        } catch (Exception e) {
            bfclient.logExp (e, false);
        }
        
        return;
    }
    
    void processDnsAdd (String[] toks) {
    }
    
    String localhostTranslate (String addr) {
        if (addr.equals ("localhost"))
            return bfclient_repo.getRepo ().getLocalAddr ().getHostAddress ();
        else
            return addr;
    }
}