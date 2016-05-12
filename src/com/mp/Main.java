package com.mp;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

public class Main {

    static final int PORT = 1234;
    static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    static volatile ConcurrentHashMap<String, String> locks = new ConcurrentHashMap<>();
    static volatile int locksAcquired = 0;
    static volatile int locksReleased = 0;
    static HashMap<String, Socket> sockets = new HashMap<>();

    public static void main(String args[]) {

        ServerSocket serverSocket = null;
        Socket socket = null;

        try {
            serverSocket = new ServerSocket(PORT);
        } catch (IOException e) {
            e.printStackTrace();
        }

        assert serverSocket != null;

        scheduler.scheduleAtFixedRate(new Runnable() {
            public void run() {
                for (Map.Entry<String, Socket> e : sockets.entrySet()) {
                    try {
                        System.out.println("Check socket + " + e.getKey());
                        if(e.getValue().isClosed() || !e.getValue().isConnected()) {
                            throw new IOException("Socket is not reachable");
                        }
                    } catch (IOException e1) {
                        sockets.remove(e.getKey());
                        removeAllLocks(e.getKey());
                        try {
                            e.getValue().close();
                        } catch (IOException e2) {
                            e2.printStackTrace();
                        }
                    }
                }
            }
        }, 600, 600, TimeUnit.SECONDS);

        System.out.println("Ready to accept connections");

        while (true) {
            try {
                socket = serverSocket.accept();
                sockets.put(socket.toString(), socket);
            } catch (IOException e) {
                System.out.println("I/O error: " + e);
            }
            try {
                new SocketThread(socket).start();
            } catch (SocketException e) {
                if (socket != null) {
                    removeAllLocks(socket.toString());
                }
                e.printStackTrace();
            }
        }
    }

    public static boolean acquire(String lockName, String owner, Integer wait) {
        do {
            if (locks.containsKey(lockName)) {
                try {
                    Thread.sleep(Math.min(250, wait));
                } catch (InterruptedException e) {
                    return false;
                }
                wait -= 250;
            } else {
                locks.put(lockName, owner);
                locksAcquired++;
                return true;
            }
        } while (wait > 0);

        // timeout has left
        return false;
    }

    public static boolean is_acquired(String lockName, String owner) {

        return locks.containsKey(lockName)
                && locks.get(lockName).equals(owner);
    }

    public static void release(String lockName, String owner) {
        if (locks.remove(lockName, owner)) {
            locksReleased++;
        }
    }

    public static void removeAllLocks(String owner) {
        if (owner == null)
            return;
        for (Map.Entry<String, String> e : locks.entrySet()) {
            if(e.getValue().equals(owner)) {
                release(e.getKey(), owner);
            }
        }
    }

}
