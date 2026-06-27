package projekti;
import projekti.Message;

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

public class ServerMain {

    private static final int PORT = 7000;

    private static final Map<String, ClientHandler> clients = new ConcurrentHashMap<>();
    private static final Map<String, ChatRoom> rooms = new ConcurrentHashMap<>();
    private static final ReentrantLock lock = new ReentrantLock();

    public static void main(String[] args) throws Exception {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("Server running on port " + PORT);

        while (true) {
            Socket socket = serverSocket.accept();
            new Thread(new ClientHandler(socket)).start();
        }
    }

    // ================= CLIENT =================
    static class ClientHandler implements Runnable {

        Socket socket;
        ObjectInputStream in;
        ObjectOutputStream out;

        String username;
        ChatRoom currentRoom;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                out = new ObjectOutputStream(socket.getOutputStream());
                out.flush();
                in = new ObjectInputStream(socket.getInputStream());

                while (true) {
                    Message msg = (Message) in.readObject();
                    handle(msg);
                }

            } catch (Exception e) {
                disconnect();
            }
        }

        private void handle(Message msg) {

            if (msg == null) return;

            switch (msg.type) {

                // ================= LOGIN =================
                case "LOGIN":
                    lock.lock();
                    try {
                        if (msg.sender == null || msg.sender.trim().isEmpty()) {
                            send(new Message("ERROR", "SERVER", "Invalid username"));
                            return;
                        }

                        if (clients.containsKey(msg.sender)) {
                            send(new Message("ERROR", "SERVER", "Username exists"));
                        } else {
                            username = msg.sender;
                            clients.put(username, this);
                            send(new Message("SUCCESS", "SERVER", "Logged in"));
                            System.out.println(username + " joined");
                        }
                    } finally {
                        lock.unlock();
                    }
                    break;

                // ================= CREATE ROOM =================
                case "CREATE":
                    rooms.putIfAbsent(msg.content, new ChatRoom(msg.content));
                    send(new Message("INFO", "SERVER", "Room created: " + msg.content));
                    break;

                // ================= JOIN ROOM =================
                case "JOIN":
                    ChatRoom room = rooms.get(msg.content);
                    if (room != null) {

                        if (currentRoom != null)
                            currentRoom.removeClient(this);

                        currentRoom = room;
                        room.addClient(this);

                        send(new Message("INFO", "SERVER", "Joined room: " + msg.content));
                    }
                    break;

                // ================= LEAVE ROOM =================
                case "LEAVE":
                    if (currentRoom != null) {
                        currentRoom.removeClient(this);
                        currentRoom = null;
                        send(new Message("INFO", "SERVER", "Left room"));
                    }
                    break;

                // ================= MESSAGE =================
                case "MESSAGE":
                    if (currentRoom != null) {
                        currentRoom.enqueue(msg.sender, msg.content);
                    }
                    break;
            }
        }

        void send(Message msg) {
            try {
                out.writeObject(msg);
                out.flush();
            } catch (IOException e) {
                disconnect();
            }
        }

        void disconnect() {
            try {
                if (username != null) clients.remove(username);
                if (currentRoom != null) currentRoom.removeClient(this);
                socket.close();
            } catch (Exception ignored) {}
        }
    }

    // ================= CHAT ROOM =================
    static class ChatRoom {

        String name;
        Set<ClientHandler> clients = ConcurrentHashMap.newKeySet();
        BlockingQueue<Message> queue = new LinkedBlockingQueue<>();

        public ChatRoom(String name) {
            this.name = name;

            // Consumer thread (blocking queue processing)
            new Thread(() -> {
                while (true) {
                    try {
                        Message msg = queue.take();

                        for (ClientHandler c : clients) {
                            c.send(msg);
                        }

                    } catch (Exception ignored) {}
                }
            }).start();
        }

        void addClient(ClientHandler c) {
            clients.add(c);
        }

        void removeClient(ClientHandler c) {
            clients.remove(c);
        }

        void enqueue(String sender, String content) {
            String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
            queue.offer(new Message("MESSAGE", sender, "[" + time + "] " + content));
        }
    }
}
