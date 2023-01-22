package ru.netology;

import java.io.*;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class ServerHTTP {
    private final static List<String> VALID_PATH = new ArrayList<>();
    private static final String DIR_NAME = "public\\";

    private static void getAllFileName(List<String> allFile, String nameFolder) {
        File dir = new File(nameFolder);
        List<File> list = Arrays.asList(dir.listFiles());
        for (File file : list) {
            allFile.add("/" + file.getName());
        }
    }

    private String notFound() {
        return "HTTP/1.1 404 Not Found\r\n" +
                "Content-Length: 0\r\n" +
                "Connection: close\r\n" +
                "\r\n";
    }

    private String successfulRequest(String mimeType, long length) {
        return "HTTP/1.1 200 OK\r\n" +
                "Content-Type: " + mimeType + "\r\n" +
                "Content-Length: " + length + "\r\n" +
                "Connection: close\r\n" +
                "\r\n";
    }

    private void specialCases(BufferedOutputStream out, String path, Path filePath, String mimeType) {
        try {
            if (path.equals("/classic.html")) {
                final var template = Files.readString(filePath);
                final var content = template.replace(
                        "{time}",
                        LocalDateTime.now().toString()
                ).getBytes();
                out.write((
                        successfulRequest(mimeType, content.length)
                ).getBytes());
                out.write(content);
                out.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void connection(ServerSocket serverSocket) {
        while (true) {
            try (final var socket = serverSocket.accept();
                 final var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 final var out = new BufferedOutputStream(socket.getOutputStream())
            ) {
                getAllFileName(VALID_PATH, DIR_NAME);
                final var requestLine = in.readLine();
                final var parts = requestLine.split(" ");

                if (parts.length != 3) {
                    continue;
                } else {
                    final var path = parts[1];
                    if (!VALID_PATH.contains(path)) {
                        out.write((
                                notFound()
                        ).getBytes());
                        out.flush();
                        continue;
                    }
                    final var filePath = Path.of(DIR_NAME, path);
                    final var mimeType = Files.probeContentType(filePath);
                    specialCases(out, path, filePath, mimeType);

                    final var length = Files.size(filePath);
                    out.write((
                            successfulRequest(mimeType, length)
                    ).getBytes());
                    Files.copy(filePath, out);
                    out.flush();

                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }


    public void startServer(int port) {
        try (final var serverSocket = new ServerSocket(port)) {
            while (true) {
                ExecutorService executorService = Executors.newFixedThreadPool(64);
                executorService.submit(new Thread(() -> {
                    connection(serverSocket);
                }));
                executorService.shutdown();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
