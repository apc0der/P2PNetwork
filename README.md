# P2PNetwork
### Context
The code simulates a peer-to-peer network, in which new nodes randomly join a single pre-existing node in the network as the network grows. Also note that for Deliverable 1 of this assignment, machines.txt will be empty. For later deliverables, it will be used as a master document to note which machines have access to which files in addition to which machines are participating in the network.

### Set Up
1. SSH into the machine dc01 and create a folder, "F", in your directory somewhere.
2. Upload machines.txt and P2P.java to "F".
3. Ensure that machines.txt is completely empty (no whitespaces or text characters).
4. Navigate to "X" and then type 'javac P2P.java' to compile the Java program into .class files.
5. Type 'ls' and ensure the class files have been created.
6. Create 15 subdirectories (for example, "sub01" to "sub15") under "F".
7. Repeatedly copy the files P2P.class and Listener.class into each subdirectory like so: type 'cp {P2P.class,Listener.class} sub01' and repeat for "sub02" to "sub15".

### Populating the Network
1. For the sake of the simulation, let us use only machines dc01 through dc15 (so that they correspond to "sub01" to "sub15").
2. SSH into whichever machine dcXX you would like to initiate the network, where XX is between 01 and 15.
3. Navigate to the subfolder of "F" named "subXX".
4. Type 'ls' and verify that there are only two files in "subXX": P2P.class and Listener.class.
5. Now type 'java P2P' and verify that the host name is printed out.
6. Now open a second terminal and SSH into another machine dcYY, where YY is between 01 and 15 and YY does not equal XX.
7. Navigate to the subfolder of "F" named "subYY".
8. Type 'ls' and verify that there are only two files in "subYY": P2P.class and Listener.class.
9. Now type 'java P2P' and verify that along with the host name being printed out, a neighbor list consisting of just the first host name of dcXX is also printed out.
10. Switch over to the first terminal (of dcXX) and verify that a neighbor list was printed after 'java P2P' was run on dcYY.
11. Repeat this process with other machines and verify that the neighbor picked by each incoming machine is random.
12. When you are done with a particular machine, simply hit Ctrl+C to exit the process.

### Searching for Files
1. Once in, you can simply type 'Search <X>' where X represents a keyword (first token in a file) or an actual file name.
2. After waiting for a bit, you can see all the search results. The search will terminate at the hop count where it found at least 1 result. If the final hop count of 16 fails, an error output will be produced.

### File Transfer
1. If the file you searched for was found, a numbered list of tuples will show up in the format ([search term],[file],[machine]).
2. Type out the number of the option you want, or alternatively type 0 to cancel the file transfer.
3. If you do not type 0, then a one-time socket instance is created between the requester and the machine containing the file.
4. At the end, a success notification should appear.

### Leaving the Network
1. To leave the network, simply type Leave. The program on the machine that leaves should simply end without fuss.
2. On the other machines that were once neighbors of this machine, type the command 'Peers' to get a list of Peers.
3. If there were only two peers it is nigh impossible to determine which peer was chosen as the replacement machine for connections.
4. If there are more than 2 peers, you can use the 'Peers' command to tell which neighbor of the leaving machine had the other neighbors added as neighbors of its own. 
