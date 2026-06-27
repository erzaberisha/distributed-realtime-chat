package client;

import shared.*;

import javafx.application.Application;

import javafx.application.Platform;

import javafx.scene.Scene;

import javafx.scene.control.*;

import javafx.scene.layout.VBox;

import javafx.stage.Stage;

import java.io.*;

import java.net.Socket;

public class ClientMain extends Application {

    ObjectOutputStream out;

    ObjectInputStream in;

    boolean loggedIn = false;

    String currentRoom = null;

    TextArea chat = new TextArea();

    TextField username = new TextField();

    TextField room = new TextField();

    TextField message = new TextField();

    public void start(Stage stage) {

        chat.setEditable(false);

        Button connect = new Button("Login");

        Button create = new Button("Create Room");

        Button join = new Button("Join Room");

        Button leave = new Button("Leave Room");

        Button send = new Button("Send");

        connect.setOnAction(e -> connect());

        create.setOnAction(e -> send(MessageType.CREATE_ROOM, room.getText()));

        join.setOnAction(e -> send(MessageType.JOIN_ROOM, room.getText()));

        leave.setOnAction(e -> send(MessageType.LEAVE_ROOM, ""));

        send.setOnAction(e -> send(MessageType.MESSAGE, message.getText()));

        VBox root = new VBox(8,

                new Label("Username"), username,

                connect,

                new Label("Room"), room,

                create, join, leave,

                chat,

                message,

                send

        );

        stage.setScene(new Scene(root, 420, 550));

        stage.show();

    }

    void connect() {

        try {

            Socket socket = new Socket("10.1.28.125", 8005);

            out = new ObjectOutputStream(socket.getOutputStream());

            out.flush();

            in = new ObjectInputStream(socket.getInputStream());

            String user = username.getText().trim();

            if (user.isEmpty()) return;

            out.writeObject(new Message(MessageType.LOGIN, user, ""));

            out.flush();

            loggedIn = true;

            listen();

        } catch (Exception e) {

            chat.appendText("Cannot connect\n");

        }

    }

    void listen() {

        new Thread(() -> {

            try {

                while (true) {

                    Message msg = (Message) in.readObject();

                    Platform.runLater(() ->

                            chat.appendText(msg.sender + ": " + msg.content + "\n")

                    );

                }

            } catch (Exception e) {

                Platform.runLater(() ->

                        chat.appendText("Disconnected\n")

                );

            }

        }).start();

    }

    void send(MessageType type, String content) {

        try {

            if (!loggedIn) {

                chat.appendText("Login first\n");

                return;

            }

            out.writeObject(new Message(type, username.getText(), content));

            out.flush();

        } catch (Exception e) {

            chat.appendText("Send error\n");

        }

    }

    public static void main(String[] args) {

        launch();

    }

}
