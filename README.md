# P2PNetwork
## Context
The code simulates a peer-to-peer network, in which new nodes randomly join a single pre-existing node in the network as the network grows.

## Set Up
1. SSH into the machine dc01 and create a folder, "F", in your directory somewhere.
2. Upload machines.txt and P2P.java to "F".
3. Ensure that machines.txt is completely empty (no whitespaces or text characters).
4. Navigate to "X" and then type 'javac P2P.java' to compile the Java program into .class files.
5. Type 'ls' and ensure the class files have been created.
6. Create 15 subdirectories (for example, "sub01" to "sub15") under "F".
7. Repeatedly copy the files P2P.class and Listener.class into each subdirectory like so: type 'cp {P2P.class,Listener.class} sub01' and repeat for "sub02" to "sub15".

## Populating the Network
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
