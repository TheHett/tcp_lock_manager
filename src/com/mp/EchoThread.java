package com.mp;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EchoThread extends Thread {
    protected Socket socket;

    public EchoThread(Socket clientSocket) throws SocketException {
        socket = clientSocket;
    }

    public void run() {
        InputStream inp;
        BufferedReader bufferedReader;
        DataOutputStream out;
        try {
            inp = socket.getInputStream();
            bufferedReader = new BufferedReader(new InputStreamReader(inp));
            out = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            return;
        }
        String line;

        Pattern patternLock = Pattern.compile("LOCK ([\\da-zA-Z_-]+) (\\d+)", Pattern.CASE_INSENSITIVE);
        Pattern patternRelease = Pattern.compile("RELEASE ([\\da-zA-Z_-]+)", Pattern.CASE_INSENSITIVE);

        while (true) {
            try {
                if(!socket.isConnected()) {
                    Main.removeAllLocks(socket.toString());
                    return;
                }
                try {
                    line = bufferedReader.readLine();
                } catch (Exception e) {
                    Main.removeAllLocks(socket.toString());
                    return;
                }

                if (line == null || line.equalsIgnoreCase("QUIT")) {
                    Main.removeAllLocks(socket.toString());
                    socket.close();
                    return;
                } else if(line.toUpperCase().matches("LOCK .*")) {
                    Matcher matcher = patternLock.matcher(line);
                    if (matcher.matches()) {
                        String lockName = matcher.group(1);
                        Integer lockWait = Integer.parseInt(matcher.group(2));
                        if (Main.acquire(lockName, socket.toString(), lockWait)) {
                            out.writeBytes("SUCCESS\n");
                        } else {
                            out.writeBytes("BUSY\n");
                        }
                    } else {
                        out.writeBytes("WRONG_ARGS\n\r");
                    }
                    out.flush();
                } else if(line.toUpperCase().matches("RELEASE .*")) {
                    Matcher matcher = patternRelease.matcher(line);
                    if(matcher.matches()) {
                        String lockName = matcher.group(1);
                        Main.release(lockName, socket.toString());
                    } else {
                        out.writeBytes("WRONG_ARGS\n\r");
                    }
                    out.flush();
                } else if(line.equalsIgnoreCase("STATUS")) {
                    for (Map.Entry<String, String> e : Main.locks.entrySet()) {
                        out.writeBytes(e.getKey() + "\t" + e.getValue() + "\n\r");
                    }
                    out.writeBytes("Total count: " + Main.locks.size());
                    out.flush();
                } else if(line.equalsIgnoreCase("PING")) {
                    out.writeBytes("PONG\n\r");
                    out.flush();
                } else {
                    out.writeBytes("Unknown request: " + line + "\n\r");
                    out.flush();
                }
            } catch (SocketException e) {
                e.printStackTrace();
                Main.removeAllLocks(socket.toString());
                try {
                    socket.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }

                return;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}