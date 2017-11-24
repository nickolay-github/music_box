package book_exs;
import java.net.*;
import java.io.*;
import java.util.*;

public class MusicServer {

    ArrayList<ObjectOutputStream> clientOutputsStreams;

    public static void main(String[] args) {
        new MusicServer().go();
    }
    public class ClientHandler implements Runnable {
        ObjectInputStream in;
        Socket clientSocket;

        public ClientHandler(Socket socket) {
            try {
                clientSocket = socket;
                in = new ObjectInputStream(clientSocket.getInputStream());

            } catch (Exception ex) {ex.printStackTrace();}
        }

        public void run() {
            Object o1 = null;
            Object o2 = null;

            try {
                while ((o1 = in.readObject()) != null) {
                    o2 = in.readObject();
                    System.out.println("read two objects");
                    tellEveryone(o1, o2);
                }
            } catch (Exception ex) {ex.printStackTrace();}
        }
    }



    public void go() {
        clientOutputsStreams = new ArrayList();
        try {
            ServerSocket serverSocket = new ServerSocket(4242);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
                clientOutputsStreams.add(out);

                Thread t = new Thread(new ClientHandler(clientSocket));
                t.start();

                System.out.println("got a connection");
            }
        } catch (Exception ex) {ex.printStackTrace();}
    }

    public void tellEveryone(Object o1, Object o2) {

        Iterator it = clientOutputsStreams.iterator();
        while (it.hasNext()) {
            try {
                ObjectOutputStream out = (ObjectOutputStream) it.next();
                out.writeObject(o1);
                out.writeObject(o2);
            } catch (Exception ex) {ex.printStackTrace();}
        }
    }
}

