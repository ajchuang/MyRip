import java.util.*;
import java.io.*;
import java.util.logging.*;


// @lfred: Thread analysis: 
// 1. UI thread (command line)
// 2. NW thread.
// 3. main thread.
// extra feature: ping, traceroute
public class bfclient {
    
    public final static String M_LINKDOWN  = "LINKDOWN";
    public final static String M_LINKUP    = "LINKUP";
    public final static String M_SHOWRT    = "SHOWRT";
    public final static String M_CLOSE     = "CLOSE";
    public final static String M_QUIT      = "QUIT";
    public final static String M_TRANSFER  = "TRANSFER";
    
    public final static String M_PING      = "PING";
    public final static String M_TROUTE    = "TRACEROUTE";
    public final static String M_LOG       = "LOG";
    public final static String M_UPDATE_TO = "UPDATE_TIMER_TO";
    
    final static String M_LOG_FILE         = "log.txt";
    
    static Logger sm_lgr;  
    static FileHandler sm_loggerFh;
    
    static {
        try {
            sm_lgr = Logger.getAnonymousLogger ();
            sm_loggerFh = new FileHandler (M_LOG_FILE);
            sm_lgr.addHandler (sm_loggerFh);
            sm_lgr.setUseParentHandlers (false);
            sm_loggerFh.setFormatter (new SimpleFormatter ());
            
        } catch (Exception e) {
            System.out.println ("Severe exception in system booting");
            System.exit (0);
        }        
    }
    
    public static void logErr (String s) {
        System.out.println ("[Err] " + s);
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
    
    public static void main (String[] args) {
        
        if (args.length != 1) {
            logErr ("Parameter setting is incorrect.");
            logErr ("Please set your config file correctly");
            return;
        }
        
        // create repo & parsing config
        String config = args[0];
        bfclient_repo repo = bfclient_repo.createRepo (config);
        repo.parseConfigFile ();
        
        // start main thread
        new Thread (bfclient_proc.getMainProc ()).start ();
        
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
        
            // processing input tokens
            String[] toks = userInput.split (" ");
            
            if (toks.length == 0)
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
                processTransfer (toks);
            } else if (cmd.equals (M_PING)) {
                processPing (toks);
            } else if (cmd.equals (M_TROUTE)) {
                processTroute (toks);
            } else if (cmd.equals (M_LOG)) {    
                processLog (toks);
            } else if (cmd.equals (M_CLOSE) || cmd.equals (M_QUIT)) {
                processClose ();
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
    }
    
    void processLinkUp (String[] toks) {
    }
    
    void processShowRt (String[] toks) {
        bfclient_repo.getRepo ().showRouteTable ();
    }
    
    void processClose () {
        System.exit (0);
    }
    
    void processTransfer (String[] toks) {
    }
    
    void processPing (String[] toks) {
        
        if (toks.length != 3) {
            printMsg ("Command format error.");
            printMsg ("[Usage] ping [ip] [port]");
            return;
        }
        
        bfclient_msg ping = new bfclient_msg ();
        ping.enqueue (M_PING);
        ping.enqueue (toks[1]);
        ping.enqueue (toks[2]);
        bfclient_proc.getMainProc ().enqueueMsg (ping);
    }
    
    void processTroute (String[] toks) {
    }
    
    void processLog (String[] toks) {
        
        try {
            BufferedInputStream reader = new BufferedInputStream (new FileInputStream (M_LOG_FILE));
            while (reader.available() > 0) {
                System.out.print ((char)reader.read ());
            }
        } catch (Exception e) {
            bfclient.logExp (e, false);
        }
        
        printMsg ("");
    }
}