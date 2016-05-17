package com.mp;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

public class Main {

    private static final int PORT = 1234;
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    static ConcurrentHashMap<String, String> locks = new ConcurrentHashMap<>();
    static volatile long locksAcquired = 0;
    static volatile long locksReleased = 0;
    private static ConcurrentHashMap<String, Socket> sockets = new ConcurrentHashMap<>();

    public static void main(String args[]) {

        ServerSocket serverSocket;
        Socket socket;

        try {
            serverSocket = new ServerSocket(PORT);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        scheduler.scheduleAtFixedRate(new Runnable() {
            public void run() {
                for (Map.Entry<String, Socket> e : sockets.entrySet()) {
                    try {
                        if (e.getValue().isClosed() || !e.getValue().isConnected()) {
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
                        e1.printStackTrace();
                    }
                }
            }
        }, 600, 600, TimeUnit.SECONDS);

        System.out.println("Ready to accept connections");

        while (true) {
            try {
                socket = serverSocket.accept();
                try {
                    sockets.put(socket.toString(), socket);
                    new SocketThread(socket).start();
                } catch (SocketException e) {
                    removeAllLocks(socket.toString());
                    e.printStackTrace();
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    static boolean acquire(String lockName, String owner, Integer wait) {
        do {
            if (locks.containsKey(lockName)) {
                try {
                    Thread.sleep(Math.min(250, wait));
                } catch (InterruptedException e) {
                    return false;
                }
                wait -= 250;
            } else {
                if (locks.put(lockName, owner) == null) {
                    synchronized (Main.class) {
                        locksAcquired++;
                    }
                    //success acquire
                    return true;
                }
                //lock already acquired
                return false;
            }
        } while (wait > 0);

        // timeout has left
        return false;
    }

    static boolean is_acquired(String lockName, String owner) {
        return locks.containsKey(lockName)
                && locks.get(lockName).equals(owner);
    }

    static void release(String lockName, String owner) {
        if (owner == null)
            return;
        if (locks.remove(lockName, owner)) {
            synchronized (Main.class) {
                locksReleased++;
            }
        }
    }

    static void removeAllLocks(String owner) {
        for (Map.Entry<String, String> e : locks.entrySet()) {
            if (e.getValue().equals(owner)) {
                release(e.getKey(), owner);
            }
        }
    }

}
