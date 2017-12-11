package com.mp;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class SocketThread extends Thread {
    private Socket socket;

    SocketThread(Socket clientSocket) throws SocketException {
        socket = clientSocket;
    }

    public void run() {
        try {
            InputStream inp;
            BufferedReader bufferedReader;
            DataOutputStream out;
            try {
                inp = socket.getInputStream();
                bufferedReader = new BufferedReader(new InputStreamReader(inp));
                out = new DataOutputStream(socket.getOutputStream());
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
            String line;

            Pattern patternLock = Pattern.compile("LOCK ([\\da-zA-Z_-]+) (\\d+)", Pattern.CASE_INSENSITIVE);
            Pattern patternILock = Pattern.compile("ILOCK ([\\da-zA-Z_-]+)", Pattern.CASE_INSENSITIVE);
            Pattern patternRelease = Pattern.compile("RELEASE ([\\da-zA-Z_-]+)", Pattern.CASE_INSENSITIVE);
            Pattern patternFree = Pattern.compile("FREE ([\\da-zA-Z_-]+)\\s+([\\S]+)", Pattern.CASE_INSENSITIVE);

            while (true) {
                try {
                    if (!socket.isConnected()) {
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
                        return;
                    } else if (line.toUpperCase().matches("LOCK .*")) {
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
                            out.writeBytes("WRONG_ARGS\n");
                        }
                        out.flush();
                    } else if (line.toUpperCase().matches("RELEASE .*")) {
                        Matcher matcher = patternRelease.matcher(line);
                        if (matcher.matches()) {
                            String lockName = matcher.group(1);
                            Main.release(lockName, socket.toString());
                        } else {
                            out.writeBytes("WRONG_ARGS\n");
                        }
                        out.flush();
                    } else if (line.equalsIgnoreCase("STATUS")) {
                        for (Map.Entry<String, String> e : Main.locks.entrySet()) {
                            out.writeBytes(e.getKey() + "\t" + e.getValue() + "\n");
                        }
                        out.writeBytes("Total lock acquired: " + Main.locksAcquired + " released: " + Main.locksReleased + "\n");
                        out.writeBytes("Total count: " + Main.locks.size());
                        out.writeBytes("\n");
                        out.writeBytes("\n");
                        out.flush();
                    } else if (line.toUpperCase().matches("ILOCK .*")) {
                        Matcher matcher = patternILock.matcher(line);
                        if (matcher.matches()) {
                            String lockName = matcher.group(1);
                            if (Main.is_acquired(lockName, socket.toString())) {
                                out.writeBytes("YES\n");
                            } else {
                                out.writeBytes("NO\n");
                            }
                        } else {
                            out.writeBytes("WRONG_ARGS\n");
                        }
                        out.flush();
                    } else if (line.equalsIgnoreCase("PING")) {
                        out.writeBytes("PONG\n");
                        out.flush();
                    } else if (line.toUpperCase().matches("^FREE .*")) {
                        Matcher matcher = patternFree.matcher(line);
                        if (matcher.matches()) {
                            String lockName = matcher.group(1);
                            String socketName = matcher.group(2);
                            if (Main.is_acquired(lockName, socketName)) {
                                Main.release(lockName, socketName);
                                out.writeBytes("SUCCESS\n");
                            } else {
                                out.writeBytes("NOT_FOUND\n");
                            }
                        } else {
                            out.writeBytes("WRONG_ARGS\n");
                        }
                        out.flush();
                    } else if (line.equalsIgnoreCase("HELP")) {
                        out.writeBytes("QUIT|LOCK|RELEASE|STATUS|ILOCK|PING|FREE|HELP\n");
                        out.flush();
                    } else {
                        out.writeBytes("Unknown request: " + line + "\n");
                        out.flush();
                    }
                } catch (IOException e) {
                    Main.removeAllLocks(socket.toString());
                    e.printStackTrace();
                    return;
                }
            }

        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}