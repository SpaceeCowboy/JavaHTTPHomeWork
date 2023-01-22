package ru.netology;

public class Main {
    public static void main(String[] args) {
        ServerHTTP serverHTTP = new ServerHTTP();
        serverHTTP.startServer(9999);
    }
}
