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

B) Running environment:
1. Well, I develop this based on Java 7 @OS X. However, I think 
   I avoid the use of Java 7 APIs. (I hope I did).
2. In order to provide some operation sugar, you have to execute
   this program with internet access. (Loopback only isn't enough)
3. log folder has to be created. Sometimes it may cause problems.

C) How to run:
Basically, I follow the requirements. 
To provide a more modular testing mechanism, I organize the test 
cases in the ./config folder. In the cofig folder, there are a few
well-written test cases. You can make use of them to test the pro-
gram. 

Therefore, an example command would be
java bfclient ./config/line/config_01




