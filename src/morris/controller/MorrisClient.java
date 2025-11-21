package morris.controller;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import morris.model.Piece;

public class MorrisClient {
    private Socket socket;
    public ObjectOutputStream output;
    private ObjectInputStream input;
    private GameFrame gameFrame;
    int playerID = -1;
    private static String winnerMessage=null;

    public MorrisClient(String serverAddress, int port) throws IOException {
        System.out.println("서버에 연결 시도 중...");
        this.socket = new Socket(serverAddress, port); // localhost, 9000
        System.out.println("서버에 성공적으로 연결되었습니다.");

        output = new ObjectOutputStream(socket.getOutputStream());
        output.flush();
        input = new ObjectInputStream(socket.getInputStream());

        try {
            this.playerID = input.readInt();
            String playerType = (this.playerID == 1) ? "흑돌" : "백돌";
            System.out.println("당신은 " + playerType + " 입니다.");
        } catch (NullPointerException e) {
        }

    }

    public static void main(String[] args) {
        final String serverAddress;

        if (args.length > 0) {
            serverAddress = args[0];
        } else {
            serverAddress = "192.168.219.112";
        }

        SwingUtilities.invokeLater(() -> {
            MorrisClient client = null;
            try {
                client = new MorrisClient(serverAddress, 9000);
                if (client.playerID == 0) {
                    String serverMessage = client.readServerMessage();
                    System.out.println("<< 서버 메시지 >> " + serverMessage);
                }
                String serverMessage = client.readServerMessage();
                System.out.println("<< 서버 메시지 >> " + serverMessage);
                if (serverMessage.equals("START")) {
                    client.output.writeObject("READY"); // client.output 필드가 public이 아니라면 getOutput()이 필요
                    client.output.flush();
                    System.out.println("게임진행 준비가 되었습니다.");
                    String finalCommand = client.readServerMessage();
                    if (finalCommand.equals("GO")) {
                        System.out.println("게임이 시작되었습니다.");
                        GameFrame frame = new GameFrame(client);
                        client.setGameFrame(frame);
                        client.startListeningThread();
                    }
                }
            } catch (IOException e) {
                System.err.println("서버 연결 실패: " + e.getMessage());
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public String readServerMessage() throws IOException, ClassNotFoundException {
        return input.readObject().toString();
    }

    public void sendGameMove(GameMove move) {
        try {
            output.writeObject(move);
            output.flush();
            System.out.println("서버로 GameMove 전송:"+move.clickIndex);
        } catch (IOException e) {
            System.err.println("게임 이동 전송 실패: " + e.getMessage());
        }
    }
    public void sendMessage(String message) throws IOException {
        output.writeObject(message);
        output.flush();
        System.out.println(message+"전송");
    }

    private void startListeningThread() {
        new Thread(() -> {
            try {
                while (true) {
                    Object receivedData = input.readObject();

                    if (receivedData instanceof String) {
                        String serverMessage = (String) receivedData;
                        System.out.println("<< 서버 응답 >> " + serverMessage);
                        if (serverMessage.contains("PLACING Success")) {
                            String[] parts = serverMessage.split(";");
                            Piece piece = Piece.valueOf(parts[0]);
                            int index = Integer.parseInt(parts[1]);
                            updatePlacing(index, piece);
                        }
                        else if (serverMessage.contains("Select")) {
                            System.out.println("선택 성공");
                            String[] parts = serverMessage.split(":");
                            updateSelected(Integer.parseInt(parts[1]));
                        }
                        else if (serverMessage.contains("취소")) {
                            System.out.println("선택 취소");
                            updateCancle();
                        }
                        else if(serverMessage.contains("Move Success")){
                            System.out.println("이동 성공");
                            String[] parts = serverMessage.split(":");
                            String[]index=parts[1].split("to");
                            int fromIndex= Integer.parseInt(index[0]);
                            int toIndex= Integer.parseInt(index[1]);
                            updateMoving(fromIndex, toIndex);
                            updateCancle();
                        }
                        else if(serverMessage.contains("REMOVE")){
                            String[] parts = serverMessage.split(":");
                            updateRemove(Integer.parseInt(parts[1]));
                            if(Integer.parseInt(parts[2])==playerID){
                                System.out.println("상대방 돌 제거 !");
                            }
                            else{
                                System.out.println("😢돌이 제거 당했어요");
                            }
                        }
                        else if (serverMessage.contains("Jump Success")){
                            System.out.println("이동 성공");
                            String[] parts = serverMessage.split(":");
                            String[]index=parts[1].split("to");
                            int fromIndex= Integer.parseInt(index[0]);
                            int toIndex= Integer.parseInt(index[1]);
                            updateJumping(fromIndex, toIndex);
                            updateCancle();
                        }
                        else if(serverMessage.contains("END")){
                            System.out.println("END");
                            Object[] options = {"재시작", "종료"};
                            int choice = JOptionPane.showOptionDialog(
                                    null, // Parent component
                                    winnerMessage,
                                    "게임 종료",
                                    JOptionPane.YES_NO_OPTION,
                                    JOptionPane.INFORMATION_MESSAGE,
                                    null,
                                    options,
                                    options[0] // Default button
                            );

                            if (choice == JOptionPane.YES_OPTION) { // "재시작" 선택
                                sendMessage("RESTART");
                            } else if (choice == JOptionPane.NO_OPTION) { // "종료" 선택
                                sendMessage("QUIT");
                                closeConnection();
                            }
                        }
                        else if(serverMessage.contains("승리")){
                           winnerMessage=winnerMessage(serverMessage);
                        }
                        else if(serverMessage.contains("RESTART")){
                            resetGame();
                        }
                        else if(serverMessage.contains("QUIT")){
                            closeConnection();
                        }
                    }

                }
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("수신 스레드 종료: " + e.getMessage());
            }
        }).start();
    }

    public void closeConnection() {
        try {
            if (output != null) {
                output.close();
            }
            if (input != null) {
                input.close();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
                System.out.println("서버와의 연결이 성공적으로 종료되었습니다.");
            }
            gameFrame.dispose();
            System.exit(0);

        } catch (IOException e) {
            System.err.println("클라이언트 연결 종료 중 오류 발생: " + e.getMessage());
        }
    }

    private void updatePlacing(int index, Piece pieceType) {
        SwingUtilities.invokeLater(() -> {
            gameFrame.getGameBoard().updatePiece(index, pieceType);
            gameFrame.getBoardView().repaint();
        });
    }
    private void updateSelected(int index) {
        SwingUtilities.invokeLater(() -> {
            gameFrame.getGameBoard().setSelectedNode(index);
            gameFrame.getBoardView().repaint();
        });
    }
    private void updateMoving(int fromIndex, int toIndex) {
        SwingUtilities.invokeLater(() -> {
            gameFrame.getGameBoard().movingPiece(fromIndex, toIndex);
            gameFrame.getBoardView().repaint();
        });
    }
    private void updateJumping(int fromIndex, int toIndex) {
        SwingUtilities.invokeLater(() -> {
            gameFrame.getGameBoard().jumpingPiece(fromIndex, toIndex);
            gameFrame.getBoardView().repaint();
        });
    }
    private void updateCancle() {
        SwingUtilities.invokeLater(() -> {
            gameFrame.getGameBoard().setSelectedNode(-1);
            gameFrame.getBoardView().repaint();
        });
    }
    private void updateRemove(int index) {
        SwingUtilities.invokeLater(() -> {
            gameFrame.getGameBoard().remove(index);
            gameFrame.getBoardView().revalidate();
            gameFrame.getBoardView().repaint();
        });
    }
    private void setGameFrame(GameFrame gameFrame) {
        this.gameFrame = gameFrame;
    }
    private void resetGame(){
        SwingUtilities.invokeLater(() -> {
            for(int i=0;i<24;i++){
                gameFrame.getGameBoard().updatePiece(i,Piece.NONE);
                gameFrame.getBoardView().repaint();
            }
        });
    }
    private String winnerMessage(String serverMessage) {
        String[] winnerMessage=serverMessage.split("\\.");
        return winnerMessage[1];
    }
}
