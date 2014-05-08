MyRip (Jen-Chieh Huang, jh3478)
=====
A funny RIP implementation for CompNet PA2

A) Code architecture:
The bfclient is designed in a multi-thread and event-based arch.
I'll introduce each thread.
1. main thread. The main thread is used to read the user input. 
2. main processing thread. The main processing thread is a real
   work horse. All major logic is done here. Also, to maintain 
   the system synchronization, almost all routing table writing
   operations are performed here. 
3. listener thread. The thread is used to listen the incoming 
   UDP packets in the designated port defined in the configuration
   file. Most of time, the thread is blocked by the socket func.
4. worker thread. The worker thread is used to perform inter-lock
   mechanism. To provide the blocking feeling in the user console,
   the thread will execute the user command asynchronously while
   blocking the main thread. Besides, command timeouts are also
   processed in this command.

Moreover, I would also like to introduce the folders in the program.
1. config. all the test config files are here
2. log. all testing log will be saved here. You can check the detail
   of the operations if you like.
3. fs. It is the file system of the client. You should be the file
   chunk here. The resulting complete file will also be here. The
   client can only access the files in the folder.

Besides, the packet format of the bfclient is -
word 01: destination ip
word 02: destination port
word 03: source ip
word 04: source port
word 05: TYPE (1 byte)   | CHUNKID (1 byte) | 
         CHKSUM (1 byte) | IP_ID (1 byte)
word 06: DATA LENGTH
word 07: USER DATA...

The packet type includes various data and control categories.
For control packets,
    1. Ping utility,
    - M_PING_REQ = 0x01
    - M_PING_RSP = 0x02

    2. traceroute utility,
    - M_TROUTE_REQ = 0x03
    - M_TROUTE_RSP_OK = 0x04
    - M_TROUTE_RSP_FAILED = 0x05   
    
    3. Error message when failed to route or other errors
    - M_HOST_NOT_REACHABLE = 0xf0
    - M_HOST_UNKNOWN_PACKET = 0xf1
                        
For routing information packets,
    - M_ROUTER_UPDATE = 0x10
    - M_ROUTER_UPDATE_URG = 0x11
                                        
For user data/ack packets,
    - M_USER_BASIC_TRANS = 0x21;
    - M_USER_BASIC_TRANS_ACK = 0x22;
                                                            
For L2 link control,                                            
    - M_LINK_DOWN = 0x30
    - M_LINK_UP = 0x31
                                                                            
The CHUNKID is used when data transfer is in progress. The id will be
used to inform the receiving end about the id.

The checksum is implemented using simple XOR for all packet bytes.
The receiving end will discard the data if the CRC is not correct.

The IPID is a random number, and not used in the program. It's for
debugging purpose.

B) Running environment:
1. Well, I develop this based on Java 7 @OS X. However, I think 
   I avoid the use of Java 7 APIs. (I hope I did).
2. In order to provide some operation sugar, you have to execute
   this program with internet access. (Loopback only isn't enough)
3. log folder has to be created. Sometimes it may cause problems.

C) How to run:
Basically, I follow the requirements. 
1. To build the whole thing. Just use ./make.sh @Unix/Linux/OS X.
2. If you're using windows, just need 'javac *.java' and mkdir log.

3. To provide a more modular testing mechanism, I organize the test 
   cases in the ./config folder. In the cofig folder, there are a few
   well-written test cases. You can make use of them to test the pro-
   gram. Therefore, an example command would be

** java bfclient ./config/[Your config folder]/[Your config file]
    
D)  Supported commands
1. All required commands are supported. (Both upper and lower cases
   are supported.)
   - LINKDOWN [ip] [port]
   - LINKUP [ip] [port] [weight]
   - SHOWRT 
   - CLOSE
   - TRANSFER [ip] [port]

2. Besides, a set of bonus commands are implemented including
   - PING [ip] [port] 
     It's a debug utility that allows you to see if a certain client
     is reachable. A timeout of 1 seconds is also implemented. If 
     destination is not reachable, a control packet will also be 
     received.
   - TRACEROUTE [ip] [port]
     it's also a debug utility, and it's a "REAL" traceroute. In my
     implementation, every router that forwards the packet will 
     attach its own signature on the packet. Therefore, when the
     TRACEROUTE is returned to the sending host, an accurate path
     can be retrieved. This is very useful for you to see if the
     router works as expected.
   - RELIABLE [on/off] 
     This command will change the underlying user data transferring
     behaviors. When the reliable transfer is turned off, the data
     packet will be dropped intentionally at the probability of 50%.
     This is useful to test the reliable transfer functionality.

3. Further, a set of bonus system functions is also implemented.
   - LOG [keyword]
     The user can make use of this command to check log with an optional
     filter. Convient for quick log check.
   - HISTORY
     The command history will be displayed with a sequence number.
     The user can input the history command by using ![number].
   - LS
     A traditional listing command. Let you check if the files in the
     ./fs/ folder. You can use this to check if the file transfer 
     is done.
   - CAT [file name]
     A utility allows the user to check the content of the file.
   - RM [file name]
     A utility allows the user to delete the existing file.

E) More things to say
The Bellman-ford routing program is well-designed in most cases including
   - Dynamic link up/down, 
   - local link retrieveal, 
   - poisoned reverse, 

As for the data transfer function, upto 256 chunks are supported.
   - The program will sort the input, and output when it is capable.
   - The resulting file will be put in the ./fs/output[port num]
   - A reliable transfer mechanism is also designed. When the packet
     arrived correctly, an ACK will be sent back. Otherwise, a retrans-
     mission will be performed upto 3 tries.

F) KNOWN issue
As the moment, I've tested most cases I can think of.
All cases mentioned in the requirement are met.
If you find any issue, mail to jh3478@columbia.edu please. 






