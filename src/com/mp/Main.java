package com.mp;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

public class Main {

    static final int PORT = 1234;
    static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    static volatile ConcurrentHashMap<String, String> locks = new ConcurrentHashMap<>();

    public static void main(String args[]) {

        ServerSocket serverSocket = null;
        Socket socket = null;

        try {
            serverSocket = new ServerSocket(PORT);
        } catch (IOException e) {
            e.printStackTrace();

        }

        scheduler.scheduleAtFixedRate(new Runnable() {
            public void run() {
                int total = 0;
                for (Map.Entry<String, String> e : Main.locks.entrySet()) {
                    System.out.println(e.getKey() + "\t" + e.getValue() + "\n\r");
                    total++;
                }
                System.out.println("Total active locks: " + total);
            }
        }, 1800, 1800, TimeUnit.SECONDS);

        System.out.println("Ready to accept connections");

        while (true) {
            try {
                assert serverSocket != null;
                socket = serverSocket.accept();

            } catch (IOException e) {
                System.out.println("I/O error: " + e);
            }
            try {
                new EchoThread(socket).start();
            } catch (SocketException e) {
                removeAllLocks(socket.toString());
                e.printStackTrace();
            }
        }
    }

    public static boolean acquire(String lockName, String owner, Integer wait)
    {
        do {
            if(locks.containsKey(lockName)) {
                try {
                    Thread.sleep(Math.min(250, wait));
                } catch (InterruptedException e) {
                    return false;
                }
                wait -= 250;
            } else {
                locks.put(lockName, owner);
                System.out.println("acquire " + lockName);
                return true;
            }
        } while (wait > 0);

        // timeout has left
        return false;
    }

    public static void release(String lockName, String owner)
    {
        if(locks.remove(lockName, owner)) {
            System.out.println("release " + lockName);
        }
    }

    public static void removeAllLocks(String owner)
    {
        if(owner == null)
            return;

        for (Map.Entry<String, String> e : locks.entrySet()) {
            if(e.getValue().equals(owner)) {
                locks.remove(e.getKey());
                System.out.println("release " + e.getKey());
            }
        }
    }

}
