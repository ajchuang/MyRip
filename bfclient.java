import java.util.*;
import java.io.*;


// @lfred: Thread analysis: 
// 1. UI thread (command line)
// 2. NW thread.
// 3. main thread.
// extra feature: ping, traceroute
public class bfclient {
    
    public static void logErr (String s) {
        System.out.println ("[Err] " + s);
    }
    
    public static void logInfo (String s) {
        System.out.println ("  [Info] " + s);
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
            return;
        }
        
        String config = args[0];
        
        bfclient_repo repo = bfclient_repo.createRepo (config);
        bfclient bfc = new bfclient ();
        repo.parseConfigFile ();
        
        // start server
        new Thread (bfclient_listener.getListener ()).start (); 
        
        // start UI
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
            String userInput = scn.nextLine ();
            
            if (userInput.equals ("quit")) {
                System.out.println ("Bye-bye");
                return;
            }
            
            System.out.println ("echo > " + userInput);
        }
    }
}