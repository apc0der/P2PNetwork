import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
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
            try {
                Socket skt = new Socket(p.get((new Random()).nextInt(p.size())), 6969);
                PrintWriter pw = new PrintWriter(new FileWriter("../machines.txt"));
                for (String x: p) {
                    pw.println(x);
                }
                pw.println(InetAddress.getLocalHost().getHostName());
                pw.close();
                Listener l = new Listener(skt, 6969);
                l.start();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}

enum MType {
    SEARCH, REPLY, TRANSFER, BRUH;
    public static MType query(char c) {
        if (c == 'S') {
            return SEARCH; // ID (4 dig) + hops + keyword
        } else if (c == 'R') {
            return REPLY; // ID + tabSepTupleList
        } else if (c  == 'T') {
            return TRANSFER; // ID + fileName
        } else {
            return BRUH;
        }
    }
}

class Input extends Thread {

    @Override
    public void run() {
        try {
            BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
            while (true) {
                String[] searchReq = stdIn.readLine().split(" ");
                // System.out.println(Arrays.toString(searchReq)); // del this
                if (searchReq[0].equals("Search")) {
                    for (int i = 1; i <= 16; i*=2) {
                        int cuh = (new Random()).nextInt(9000)+1000;
                        System.out.println("Initiating search " + cuh + " with a hop count of... " + i);
                        EdgeSender edgar = new EdgeSender(null, cuh + " " + (i-1) + " " + searchReq[1], MType.SEARCH, i-1);
                        Listener.id2s.put(cuh, edgar);
                        edgar.start();
                        try {
                            edgar.join();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                        if (edgar.replies.size() == 0) {
                            System.out.println("File/kwd not found :(");
                        } else {
                            for (int j = 0; j < edgar.replies.size(); j++) {
                                System.out.println((j+1) + ") " + edgar.replies.get(j));
                            }
                            break;
                        }
                    }
                    System.out.println("Search for " + searchReq[1] + " concluded...");
                } else {
                  System.out.println("Not a valid search!");
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

class EdgeSender extends Thread{
    private String toSend = "";
    private Socket from;
    private MType m;
    private int h;
    public List<String> replies;

    public EdgeSender(Socket s, String forward, MType t, int hl) {
        from = s;
        toSend = forward;
        // System.out.println("Got a message --> " + toSend);
        m = t;
        replies = new ArrayList<>();
        h = hl+1;
    }

    @Override
    public void run() {
        switch(m) {
            case SEARCH:
                for (EdgeReceiver p: Listener.peers) {
                    try {
                        // System.out.println("Sending the above message...");
                        PrintWriter pw = new PrintWriter(new OutputStreamWriter(p.src.getOutputStream()));
                        pw.println("S " + toSend);
                        pw.flush();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                try {
                    synchronized(this) {
                        this.wait((long)1000*h*2);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                // System.out.println("Got all my replies!");
                if (from == null) {
                    break;
                }
            case REPLY:
                try {
                    PrintWriter fromWriter = new PrintWriter(new OutputStreamWriter(from.getOutputStream()));
                    String gyat = "R " + toSend.substring(0, toSend.indexOf(" ")) + " " + String.join("\t", replies);
                    System.out.println(gyat);
                    fromWriter.println(gyat);
                    fromWriter.flush();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                break;
            case TRANSFER:
                // functionality needed
        }
    }
}

class EdgeReceiver extends Thread {
    private String msg;
    public Socket src;
    private MType m;

    public EdgeReceiver(Socket s) {
        src = s;
        msg = "";
    }

    @Override
    public void run() {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(src.getInputStream()));
            // System.out.println("Before the while(true)...");
            while(true) {
                // System.out.println("Before reading from buffered reader...");
                msg = br.readLine();
                // System.out.println("Received the message... " + msg);
                m = MType.query(msg.charAt(0));
                String[] pieces = msg.substring(2).split(" ");
                int ID = Integer.parseInt(pieces[0]);
                switch(m) {
                    case SEARCH:
                        List<String> founds = new ArrayList<>();
                        int hopsLeft = Integer.parseInt(pieces[1]);
                        String kwd = pieces[2];
                        System.out.println(kwd);
                        // include search functionality here
                        Scanner libReader = new Scanner(new File("lib.txt"));
                        while (libReader.hasNext()) {
                            String f = libReader.next();
                            Scanner fRead = new Scanner(new File(f));
                            String bruh = fRead.next();
                            if (bruh.equals(kwd) || f.equals(kwd)) {
                                founds.add("(" + kwd + "," + f + "," + InetAddress.getLocalHost().getHostName().substring(0, 4) + ")");
                            }
                        }
                        if(Listener.id2s.containsKey(ID)) {
                            break;
                        }
                        if (hopsLeft > 0) {
                            EdgeSender es = new EdgeSender(this.src, ID + " " + (hopsLeft - 1) + " " + kwd, m, hopsLeft - 1);
                            es.replies.addAll(founds);
                            Listener.id2s.put(ID, es);
                            es.start();
                        } else {
                            EdgeSender es = new EdgeSender(this.src, ID + " ", MType.REPLY, -1);
                            es.replies.addAll(founds);
                            es.start();
                        }
                        break;
                    case REPLY:
                        // System.out.println(Arrays.toString(pieces));

                        if (pieces.length > 1) {
                            for (String x : pieces[1].split("\\t")) {
                                Listener.id2s.get(ID).replies.add(x);
                            }
                        }
                        break;
                    case TRANSFER:
                        String fName = pieces[1];
                        // functionality required
                        break;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

class Listener extends Thread{
    public static HashMap<Integer, EdgeSender> id2s;
    private final int PortNumber;
    public static ArrayList<EdgeReceiver> peers = new ArrayList<>();
    public static HashSet<Integer> incoming = new HashSet<>();

    public Listener(int i) {
        PortNumber = i;
        id2s = new HashMap<>();
    }

    public Listener(Socket skt, int i) {
        peers.add(new EdgeReceiver(skt));
        peers.get(peers.size()-1).start();
        PortNumber = i;
        id2s = new HashMap<>();
    }

    public void printPeers() {
        System.out.println("Neighbors: ");
        int i = 1;
        for(EdgeReceiver peer: peers) {
            System.out.println((i++) + ") " + peer.src.getInetAddress().getHostName());
        }
        System.out.println("~~~");
    }

    @Override
    public void run() {
        try {
            ServerSocket serverSocket = new ServerSocket(PortNumber);
            System.out.println("New computer started on... " + InetAddress.getLocalHost().getHostName());
            if (peers.size()!= 0) {
                printPeers();
            }
            Input mrInput = new Input();
            mrInput.start();
            while (true) {
                Socket clientSocket = serverSocket.accept();
                peers.add(new EdgeReceiver(clientSocket));
                peers.get(peers.size()-1).start();
                printPeers();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
