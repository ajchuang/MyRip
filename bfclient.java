import java.util.*;
import java.io.*;
import java.util.logging.*;
import java.security.*;
import java.math.*;


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
    
    static String M_LOG_FILE;
    
    static Logger sm_lgr;  
    static FileHandler sm_loggerFh;
    
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
        
        if (toks.length != 3) {
            printMsg ("Command format error.");
            printMsg ("[Usage] LINKDOWN [ip] [port]");
            return;
        }
        
        bfclient_msg ping = new bfclient_msg (bfclient_msg.M_LINK_DOWN);
        ping.enqueue (toks[1]);
        ping.enqueue (toks[2]);
        bfclient_proc.getMainProc ().enqueueMsg (ping);
    }
    
    void processLinkUp (String[] toks) {
        if (toks.length != 3) {
            printMsg ("Command format error.");
            printMsg ("[Usage] LINKUP [ip] [port]");
            return;
        }
        
        bfclient_msg ping = new bfclient_msg (bfclient_msg.M_LINK_UP);
        ping.enqueue (toks[1]);
        ping.enqueue (toks[2]);
        bfclient_proc.getMainProc ().enqueueMsg (ping);
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
        
        bfclient_msg ping = new bfclient_msg (bfclient_msg.M_SEND_PING_REQ);
        ping.enqueue (toks[1]);
        ping.enqueue (toks[2]);
        bfclient_proc.getMainProc ().enqueueMsg (ping);
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
        
        bfclient_msg ping = new bfclient_msg (bfclient_msg.M_SEND_TROUTE_REQ);
        ping.enqueue (toks[1]);
        ping.enqueue (toks[2]);
        bfclient_proc.getMainProc ().enqueueMsg (ping);
    }
    
    //  @lfred: 
    //      this function prints the log file we are running.
    //      toks[] are not used actually.
    void processLog (String[] toks) {
        try {
            
            BufferedReader reader = new BufferedReader(new FileReader (M_LOG_FILE));
            String line;
            int toss = 0;
            
            while ((line = reader.readLine ()) != null) {
                if (toss % 2 == 1)
                    System.out.println (line);
                    
                toss++;
            }
            
        } catch (Exception e) {
            bfclient.logExp (e, false);
        }
        
        printMsg ("");
    }
}