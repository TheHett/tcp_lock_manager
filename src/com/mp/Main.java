package com.mp;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Main {

    private static final int PORT = 1234;
    private static final int LOCK_CHECKS_INTERVAL_MS = 125;
    static ConcurrentHashMap<String, String> locks = new ConcurrentHashMap<>();
    static volatile long locksAcquired = 0;
    static volatile long locksReleased = 0;

    public static void main(String args[]) {
        ServerSocket serverSocket;
        Socket socket;

        try {
            serverSocket = new ServerSocket(PORT);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        System.out.println("Ready to accept connections");

        while (true) {
            try {
                socket = serverSocket.accept();
                socket.setTcpNoDelay(true);
                socket.setKeepAlive(true);
                try {
                    new SocketThread(socket).start();
                } catch (SocketException e) {
                    removeAllLocks(socket.toString());
                    e.printStackTrace();
                    socket.close();
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    static boolean acquire(String lockName, String owner, int wait) {
        do {
            if (locks.putIfAbsent(lockName, owner) == null) {
                synchronized (Main.class) {
                    locksAcquired++;
                }

                //success acquire
                return true;
            } else {
                try {
                    Thread.sleep(Math.min(LOCK_CHECKS_INTERVAL_MS, wait));
                } catch (InterruptedException e) {

                    return false;
                }
            }
            wait -= LOCK_CHECKS_INTERVAL_MS;
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
