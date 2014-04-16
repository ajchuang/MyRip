import java.util.*;
import java.io.*;


// @lfred: Thread analysis: 
// 1. UI thread (command line)
// 2. NW thread.
// 3. main thread.
// extra feature: ping, traceroute
public class bfclient {
    
    final static String M_LINKDOWN  = "LINKDOWN";
    final static String M_LINKUP    = "LINKUP";
    final static String M_CLOSE     = "CLOSE";
    final static String M_QUIT      = "QUIT";
    final static String M_TRANSFER  = "TRANSFER";
    final static String M_PING      = "PING";
    final static String M_TROUTE    = "TRACEROUTE";
    
    public static void logErr (String s) {
        System.out.println ("[Err] " + s);
    }
    
    public static void logInfo (String s) {
        System.out.println ("  [Info] " + s);
    }
    
    public static void logDetail (String s) {
        System.out.println ("    [DTL] " + s);
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
        
        String config = args[0];
        
        // create repo & parsing config
        bfclient_repo repo = bfclient_repo.createRepo (config);
        repo.parseConfigFile ();
        
        // start main thread
        new Thread (bfclient_proc.getMainProc ()).start ();
        
        // start server
        new Thread (bfclient_listener.getListener ()).start (); 
        
        // start UI
        bfclient bfc = new bfclient ();
        bfc.startConsole ();
        System.exit (0);
    }
    
    public bfclient () {
    }
    
    public void startConsole () {
        
        Scanner scn = new Scanner (System.in);
        System.out.println ("Welcome to bfclient console!");
        
        while (true) {
            System.out.print ("$ ");
            String userInput = scn.nextLine ().trim ();
            
            if (userInput.equals (M_CLOSE)) {
                
            }
            
            String[] toks = userInput.split (" ");
            
            if (toks.length == 0)
                return;
            
            // we just take lower and upper case
            String cmd = toks[0].toUpperCase ();
            
            if (cmd.equals (M_LINKDOWN)) {
            } else if (cmd.equals (M_LINKUP)) {
                System.out.println ("echo > " + cmd);
            } else if (cmd.equals (M_CLOSE) || cmd.equals (M_QUIT)) {
                // should we flush the routing table?
                System.out.println ("Bye-bye");
                return;
            } else if (cmd.equals (M_TRANSFER)) {
                System.out.println ("echo > " + cmd);
            } else if (cmd.equals (M_PING)) {
                System.out.println ("echo > " + cmd);
            } else if (cmd.equals (M_TROUTE)) {
                System.out.println ("echo > " + cmd);
            } else {
                System.out.println ("Unknown command: " + userInput);
            }
        }
    }
}