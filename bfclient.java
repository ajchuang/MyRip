import java.util.*;
import java.io.*;


// @lfred: Thread analysis: 
// 1. UI thread (command line)
// 2. NW thread.
// 3. main thread.
// extra feature: ping, traceroute
public class bfclient {
    
    String m_configFileName;
    
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
        
        bfclient bfc = new bfclient (args[0]);
        bfc.parseConfigFile ();
        bfc.startConsole ();
        
    }
    
    public bfclient (String fName) {
        m_configFileName = fName;
    }
    
    public void parseConfigFile () {
    
        BufferedReader br = null;
        String line = null;
        
        try {    
            br = new BufferedReader (new FileReader (m_configFileName));
            
            // Ln1 is different from others
            line = br.readLine ();
            if (line != null) {
                String[] lns = line.split (" ");
                
                if (lns.length == 2) {
                    bfclient.logInfo ("Port: " + lns[0] + " timeout: " + lns[1]);
                } else if (lns.length == 4) {
                    bfclient.logInfo ("Port: " + lns[0] + " timeout: "  + lns[1]);
                    bfclient.logInfo ("File: " + lns[2] + " sequence: " + lns[3]);
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
            logExp (e, true);    
        } 
    }
    
    public void startConsole () {
        
        Scanner scn = new Scanner (System.in);
        while (true) {
            System.out.print ("$ ");
            String userInput = scn.nextLine ();
            
            if (userInput.equals ("quit")) {
                System.out.println ("Bye-bye");
                return;
            } else {
                System.out.println ("echo > " + userInput);
            }
        }
    }
}