import java.io.*;
import java.net.*;
import java.util.*;

public class P2P {
    public static void main(String[] args) throws IOException {
        Scanner sc = new Scanner(new File("../machines.txt"));
        ArrayList<String> p = new ArrayList<>();
        while (sc.hasNext()) {
            p.add(sc.next());
        }
        if (p.size()==0) {
            PrintWriter pw = new PrintWriter(new FileWriter("../machines.txt"));
            pw.println(InetAddress.getLocalHost().getHostName());
            pw.close();
            Listener l = new Listener(6969);
            l.start();
        } else {
            Random q = new Random();
            try (Socket skt = new Socket(p.get(q.nextInt(p.size())), 6969)) {
                PrintWriter pw = new PrintWriter(new FileWriter("../machines.txt"));
                for (String x: p) {
                    pw.println(x);
                }
                pw.println(InetAddress.getLocalHost().getHostName());
                pw.close();
                Listener l = new Listener(skt, 6969);
                l.start();
            }
        }
    }
}

class Listener extends Thread{
    private final int PortNumber;
    private ArrayList<Socket> peers = new ArrayList<>();

    public Listener(int i) {
        PortNumber = i;
    }

    public Listener(Socket skt, int i) {
        peers.add(skt);
        PortNumber = i;
    }

    public void printPeers() {
        System.out.println("Neighbors: ");
        int i = 1;
        for(Socket peer: peers) {
            System.out.println((i++) + ") " + peer.getInetAddress().getHostName());
        }
        System.out.println("~~~");
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(PortNumber);) {
            System.out.println(": " + InetAddress.getLocalHost().getHostName());
            if (peers.size()!= 0) {
                printPeers();
            }
            while (true) {
                Socket clientSocket = serverSocket.accept();
                peers.add(clientSocket);
                printPeers();
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
