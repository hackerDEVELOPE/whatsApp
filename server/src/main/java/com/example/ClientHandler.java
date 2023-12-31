package com.example;


import constants.Command;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class ClientHandler {
    private Server server;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private boolean isAuthenticated;
    private String nickname;
    private String login;

    public ClientHandler(Server server, Socket socket) {
        try {
            this.server = server;
            this.socket = socket;
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());


            new Thread(() -> {
                try {
                    socket.setSoTimeout(5000);
                    //цикл аутентификации
                    while (true) {
                        String str = in.readUTF();

                        if (str.startsWith("/")) {

                            if (str.equals(Command.END)) {
                                sendMsg(Command.END); // watafak
                                break;
                            }

                            if (str.startsWith(Command.AUTH)) {
                                String[] token = str.split(" ", 3);
                                if (token.length < 3) {
                                    continue;
                                }
                                String newNick = server.getAuthService()
                                        .getNicknameByLoginAndPassword(token[1], token[2]);
                                login = token[1];
                                if (newNick != null) {
                                    if (!server.isAccAuthenticated(login)) {
                                        nickname = newNick;
                                        isAuthenticated = true;
                                        sendMsg(Command.AUTH_OK+ " " + nickname + " есть контакт блять");
                                        server.subscribe(this);
                                        socket.setSoTimeout(0);
                                        break;
                                    } else {
                                        sendMsg("Аккаунт уже авторизован");
                                    }

                                } else {
                                    sendMsg("invalid login or password");
                                }

                            }
                            if (str.startsWith(Command.REG)) {
                                String[] token = str.split(" ");
                                if (token.length < 4) {
                                    continue;
                                }
                                if (server.getAuthService()
                                        .registration(token[1], token[2], token[3])) {

                                    sendMsg(Command.REG_OK);
                                } else {
                                    sendMsg(Command.REG_FAIL);
                                }
                            }
                        }


                    }

                    //цикл работы
                    while (isAuthenticated) {
//                        socket.setSoTimeout(0);
                        String str = in.readUTF();
                        if (str.startsWith("/")) {
                            if (str.equals(Command.END)) {
                                break;
                            }
                            if (str.startsWith(Command.W)) {
                                String[] token = str.split(" ", 3);
                                if (token.length < 3) {
                                    continue;
                                }
                                server.privateMsg(this, token[1], token[2]);
                            }
                        } else {
                            server.broadcastMsg(this, str);
                        }


                    }
                } catch (SocketTimeoutException e) {
                    sendMsg(Command.END);

                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    server.unsubscribe(this);
                    System.out.println("Client was disconnected");
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void sendMsg(String msg) {
        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getNickname() {
        return nickname;
    }

    public String getLogin() {
        return login;
    }
}
