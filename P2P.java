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
        if (p.isEmpty()) {
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
    SEARCH, REPLY, TRANSFER, EXIT;
    public static MType query(char c) {
        if (c == 'S') {
            return SEARCH; // ID (4 dig) + hops + keyword
        } else if (c == 'R') {
            return REPLY; // ID + tabSepTupleList
        } else if (c  == 'T') {
            return TRANSFER; // ID + fileName
        } else {
            return EXIT; // ID + newHostName
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
                if (searchReq[0].equals("Peers")) {
                    System.out.println("Neighbors: ");
                    int i = 1;
                    for(EdgeReceiver peer: Listener.peers) {
                        System.out.println((i++) + ") " + peer.src.getInetAddress().getHostName());
                    }
                    System.out.println("~~~");
                } else if (searchReq[0].equals("Leave")) {
                    if (!Listener.peers.isEmpty()) {
                        EdgeReceiver replace = Listener.peers.get(0);
                        String msg = InetAddress.getLocalHost().getHostName() + " " + replace.src.getInetAddress().getHostName();
                        for (EdgeReceiver er: Listener.peers) {
                            EdgeSender rip = new EdgeSender(er.src, msg, MType.EXIT, -1);
                            rip.start();
                        }
                    }
                    Scanner mr = new Scanner(new File("../machines.txt"));
                    ArrayList<String> p = new ArrayList<>();
                    while (mr.hasNext()) { p.add(mr.next()); }
                    p.remove(InetAddress.getLocalHost().getHostName());
                    PrintWriter mw = new PrintWriter(new File("../machines.txt"));
                    for (String q: p) { mw.println(q); }
                    mr.close();
                    mw.close();
                    System.exit(0);
                } else if (searchReq[0].equals("Search")) {
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
                                System.out.println((j+1) + ") (" + edgar.replies.get(j) + ")");
                            }
                            System.out.println("Choose a peer (1 - " + edgar.replies.size() + " to download from, or hit 0 to cancel... ");
                            int c = Integer.parseInt(stdIn.readLine());
                            if (c != 0) {
                                String[] pcs = edgar.replies.get(c-1).split(",");
                                System.out.println(Arrays.toString(pcs));
                                try {
                                    EdgeSender req = new EdgeSender(null, cuh + " " + pcs[2] + ".utdallas.edu " + pcs[1], MType.TRANSFER, -1);
                                    req.start();
                                    try {
                                        req.join();
                                    } catch (Exception e) {
                                        throw new RuntimeException(e);
                                    }
                                    System.out.println("File received!");
                                } catch (Exception e) {
                                    throw new StreamCorruptedException();
                                }
                            } else {
                                System.out.println("Download cancelled...");
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
    private String toSend;
    private Socket from;
    private MType m;
    private int h;
    public List<String> replies;

    public EdgeSender(Socket s, String forward, MType t, int hl) {
        from = s;
        toSend = forward;
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
                        this.wait((long)1000*h);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (from == null) {
                    break;
                }
            case REPLY:
                try {
                    PrintWriter fromWriter = new PrintWriter(new OutputStreamWriter(from.getOutputStream()));
                    String gyat = "R " + toSend.substring(0, toSend.indexOf(" ")) + " " + String.join("\t", replies);
                    fromWriter.println(gyat);
                    fromWriter.flush();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                break;
            case TRANSFER:
                try {
                    String[] x = toSend.split(" ");
                    System.out.println(Arrays.toString(x)); // dbg
                    Socket ftp = new Socket(x[1], 6969);
                    PrintWriter fw = new PrintWriter(new OutputStreamWriter(ftp.getOutputStream()));
                    BufferedReader br = new BufferedReader(new InputStreamReader(ftp.getInputStream()));
                    String humbleRequest = "T " + x[0] + " " + x[2];
                    System.out.println(humbleRequest); // dbg
                    fw.println(humbleRequest);
                    fw.flush();
                    String[] dt = br.readLine().split("\\t");
                    PrintWriter monkey = new PrintWriter(new FileWriter(new File(x[2])));
                    for (String d: dt) { monkey.println(d); }
                    monkey.close();
                    ArrayList<String> fList = new ArrayList<>();
                    Scanner libber = new Scanner(new File("lib.txt"));
                    while (libber.hasNextLine()) { fList.add(libber.nextLine()); }
                    libber.close();
                    PrintWriter libWrit = new PrintWriter(new FileWriter(new File("lib.txt")));
                    for (String f: fList) { libWrit.println(f); }
                    libWrit.println(x[2]);
                    libWrit.close();
                    ftp.close();
                } catch (IOException e) {
                    throw new RuntimeException();
                }
                break;
            case EXIT:
                try {
                    PrintWriter fromWriter = new PrintWriter(new OutputStreamWriter(from.getOutputStream()));
                    fromWriter.println("E 1000 " + toSend);
                    fromWriter.flush();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                break;
        }
    }

    @Override
    public void interrupt() {
        super.interrupt();
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
            while(true) {
                msg = br.readLine();
                if (msg == null) { break; }
                System.out.println("Recv msg is... " + msg);
                m = MType.query(msg.charAt(0));

                String[] pieces = msg.substring(2).split(" ");
                int ID = Integer.parseInt(pieces[0]);
                switch(m) {
                    case SEARCH:
                        List<String> founds = new ArrayList<>();
                        int hopsLeft = Integer.parseInt(pieces[1]);
                        String kwd = pieces[2];

                        Scanner libReader = new Scanner(new File("lib.txt"));
                        while (libReader.hasNext()) {
                            String f = libReader.next();
                            Scanner fRead = new Scanner(new File(f));
                            String bruh = fRead.next();
                            if (bruh.equals(kwd) || f.equals(kwd)) {
                                founds.add(kwd + "," + f + "," + InetAddress.getLocalHost().getHostName().substring(0, 4));
                            }
                            fRead.close();
                        }
                        libReader.close();
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
                        if (pieces.length > 1) {
                            for (String x : pieces[1].split("\\t")) {
                                Listener.id2s.get(ID).replies.add(x);
                            }
                        }
                        break;
                    case TRANSFER:
                        System.out.println("File to read: " + pieces[1]); //dbg
                        Scanner sc = new Scanner(new File(pieces[1]));
                        ArrayList<String> data = new ArrayList<>();
                        while (sc.hasNextLine()) {
                            data.add(sc.nextLine());
                        }
                        String compressed = String.join("\t", data);
                        System.out.println(compressed + "EOL");
                        PrintWriter pw = new PrintWriter(new OutputStreamWriter(src.getOutputStream()));
                        pw.println(compressed);
                        pw.flush();
                        break;
                    case EXIT:
                        String leaver = pieces[1];
                        String replacer = pieces[2];

                        Iterator<EdgeReceiver> it = Listener.peers.iterator();
                        while (it.hasNext()) {
                            EdgeReceiver neighbor = it.next();
                            if (neighbor.src.getInetAddress().getHostName().equals(leaver)) {
                                try {
                                    neighbor.src.close();
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                                it.remove();
                                break;
                            }
                        }
                        if (!replacer.equals(InetAddress.getLocalHost().getHostName())) {
                            try {
                                Socket repSock = new Socket(replacer, 6969);
                                EdgeReceiver newP = new EdgeReceiver(repSock);
                                Listener.peers.add(newP);
                                newP.start();
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }
                }
            }
        } catch (IOException e) {
            System.out.println("EdgeReceiver closed!");
        }
    }
}

class Listener extends Thread{
    public static HashMap<Integer, EdgeSender> id2s;
    public static ArrayList<EdgeReceiver> peers = new ArrayList<>();

    public Listener(int i) {
        id2s = new HashMap<>();
    }

    public Listener(Socket skt, int i) {
        peers.add(new EdgeReceiver(skt));
        peers.get(peers.size()-1).start();
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
            ServerSocket serverSocket = new ServerSocket(6969);
            System.out.println("New computer started on... " + InetAddress.getLocalHost().getHostName());
            if (peers.size()!= 0) {
                printPeers();
            }
            Input mrInput = new Input();
            mrInput.start();
            while (true) {
                Socket clientSocket = serverSocket.accept();
                boolean found = false;
                for (EdgeReceiver p: peers) {
                    if (p.src.getInetAddress().getHostName().equals(clientSocket.getInetAddress().getHostName())) {
                        found = true;
                    }
                }
                if (!found) {
                    peers.add(new EdgeReceiver(clientSocket));
                    peers.get(peers.size()-1).start();
                    printPeers();
                } else {
                    EdgeReceiver er = new EdgeReceiver(clientSocket);
                    er.start();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
