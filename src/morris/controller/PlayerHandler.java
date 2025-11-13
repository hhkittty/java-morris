package morris.controller;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class PlayerHandler implements Runnable {
    private Socket clientSocket;
    private int playerID;
    private ObjectInputStream input;
    private ObjectOutputStream output;

    public PlayerHandler(Socket player, int playerID) {
        this.clientSocket = player;
        this.playerID = playerID;
        try {
            this.output = new ObjectOutputStream(clientSocket.getOutputStream());
            output.writeInt(playerID);
            output.flush();
            this.input = new ObjectInputStream(clientSocket.getInputStream());
        } catch (IOException e) {
            System.out.println("핸들러 초기화 중 오류 발생" + e.getMessage());
        }
    }
    public void sendMessage(String message) throws IOException {
        output.writeObject(message);
        output.flush();
    }
    public void closeConnection() throws IOException {
        clientSocket.close();
    }
    @Override
    public void run() {
        try {
            if (this.playerID == 0) {
                output.writeObject("상대를 기다리는 중입니다...");
                output.flush();
            }
            while (true) {
                Object receivedData = input.readObject();
                if (receivedData instanceof String) {
                    String clientCommand = (String) receivedData;

                    if (clientCommand.equals("READY")) {
                        MorrisServer.clientIsReady();
                        break;
                    }
                }
            }
            while(true) {
                Object receivedObject = input.readObject();
                if (receivedObject instanceof GameMove) {
                    GameMove move = (GameMove) receivedObject;

                    if (MorrisServer.currentPlayerID != this.playerID) {
                        sendMessage("ERROR: 상대 차례입니다.");
                        continue;
                    }

                    MorrisServer.handleClientMove(move, this.playerID);
                }
                else if(receivedObject instanceof String) {
                    String clientCommand = (String) receivedObject;
                    MorrisServer.handleClientCommand(clientCommand, playerID);
                }
            }

        } catch (EOFException e) {
            System.out.println("클라이언트 연결 종료: " + clientSocket.getInetAddress());
        } catch (IOException e) {
            System.out.println("핸들러 통신 오류: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (clientSocket != null)
                    clientSocket.close();
            } catch (IOException ignored) {
            }
        }
    }
}


