import java.util.*;
import java.net.*;
import java.io.*;
import java.util.concurrent.*;

public class bfclient_proc implements Runnable {

    static bfclient_proc sm_proc;
    
    LinkedBlockingQueue<bfclient_msg> m_queue;
    Timer m_updateTimer;
    
    static {
        sm_proc = null;
    }

    public static bfclient_proc getMainProc () {
        
        if (sm_proc == null)
            sm_proc = new bfclient_proc ();
            
        return sm_proc;
    }

    private bfclient_proc () {
        m_queue = new LinkedBlockingQueue<bfclient_msg> ();
        m_updateTimer = new Timer ("UpdateTimer");
    }
    
    public void enqueueMsg (bfclient_msg m) {
        try {
            m_queue.put (m);
        } catch (Exception e) {
            bfclient.logExp (e, true);
        }
    }
    
    public void run () {
        
        bfclient.logInfo ("Main thread starting");
        bfclient_repo repo = bfclient_repo.getRepo ();
        
        // start update timer
        m_updateTimer.scheduleAtFixedRate (
            new TimerTask () {
                public void run () {
                    bfclient_msg msg = new bfclient_msg ();
                    msg.pushStr ("UPDATE_TIMER_TO");
                    bfclient_proc.getMainProc ().enqueueMsg (msg);
                }
            }, 1000, repo.getTimeout () * 1000);
        
        while (true) {
            try {
                bfclient_msg msg = m_queue.take ();
                msg.showHeader ();
            } catch (Exception e) {
                bfclient.logExp (e, false);
            }
        }
    }
}