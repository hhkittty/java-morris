package morris.controller;


import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import morris.model.GameBoard;
import morris.model.GamePhase;
import morris.model.Piece;
import morris.model.RuleChecker;

public class MorrisServer {
    private static ServerSocket serverSocket;
    private static List<PlayerHandler> handlers = new CopyOnWriteArrayList<>();
    private static final int REQUIRED_PLAYERS = 2;
    private static volatile int readyCount = 0;
    private static final Object readyLock = new Object();
    private static Object restartLock = new Object();
    static volatile int currentPlayerID = 1;
    private static GameBoard masterGameBoard = new GameBoard();
    private static RuleChecker rule = new RuleChecker();
    private static Map<Integer, Integer> selectedNodes = new ConcurrentHashMap<>();
    private static int MaxPlacedPiece=8;
    private static Map<Integer, Boolean> playerReadyToRestart = new HashMap<>();
    private static int winner=0;
    private static int loser=0;

    public static void main(String[] args) throws IOException {
        serverSocket = new ServerSocket(9000);
        System.out.println("나인 맨스 모리스 서버 시작...");

        Socket player1 = serverSocket.accept(); // 첫 번째 플레이어 대기
        System.out.println("Player1 접속");
        PlayerHandler handler1 = new PlayerHandler(player1, 1);
        handlers.add(handler1);
        new Thread(handler1).start();

        Socket player2 = serverSocket.accept();
        System.out.println("Player2 접속");// 두 번째 플레이어 대기
        PlayerHandler handler2 = new PlayerHandler(player2, 2);
        handlers.add(handler2);
        new Thread(handler2).start();

        broadcastMessage("START");
        masterGameBoard.setPhase(GamePhase.PLACING);
    }

    private static void broadcastMessage(String message) throws IOException {
        for (PlayerHandler handler : handlers) {
            handler.sendMessage(message);

        }
    }
    private static void sendMessageToPlayer(int playerID,String message) throws IOException {
        handlers.get(playerID-1).sendMessage(message);
    }

    public static void clientIsReady() throws IOException {
        synchronized (readyLock) {
            readyCount++;
            System.out.println("클라이언트 READY 수신. 현재 readyCount: " + readyCount);
            if (readyCount == REQUIRED_PLAYERS) {
                System.out.println("모든 플레이어가 준비 완료. GO 명령 전송.");
                broadcastMessage("GO");
            }
        }
    }

    public static void switchTurn() throws IOException {
        currentPlayerID = 3 - currentPlayerID; // 1<->2 전환
        Piece currentPlayer=Piece.fromServerID(currentPlayerID);
        broadcastMessage("TURN:" + currentPlayer);
    }
    public static void setCurrentPlayer(int i){
        currentPlayerID=i;
    }

    public static synchronized void handleClientMove(GameMove move, int playerID) throws IOException {
        int index = move.clickIndex;
        GamePhase phase=masterGameBoard.getPhase();
        Piece currentPlayer = Piece.fromServerID(playerID);
        Piece opponentPlayer = Piece.fromServerID(3 - playerID);

        if (phase == GamePhase.PLACING) {
            boolean canPlace=rule.canPlace(
                    index,
                    masterGameBoard.getNodes(),
                    masterGameBoard.getpiecesPlaced(),
                    MaxPlacedPiece
            );
            if(!canPlace) {
                sendMessageToPlayer(playerID,"빈 공간을 선택해 주세요.");
                return;
            }
            masterGameBoard.placePiece(index,currentPlayer);
            broadcastMessage(currentPlayer + ";" + index + ";" + phase + " Success");
            if (rule.isMill(index, masterGameBoard.getNodes())) {
                masterGameBoard.setPhase(GamePhase.REMOVE);
                broadcastMessage("Mill 성공💣");
                return;
            }
            switchTurn();
            if (masterGameBoard.getpiecesPlaced() == MaxPlacedPiece && masterGameBoard.getPhase()!=GamePhase.REMOVE) {
                broadcastMessage("--말놓기 종료--");
                broadcastMessage("MOVING Phase");
                masterGameBoard.setPhase(GamePhase.MOVING);
            }
        }
        else if (phase == GamePhase.MOVING) {
            if (!selectedNodes.containsKey(playerID)) {
                if (!masterGameBoard.isCurrentPlayerPiece(index,currentPlayer)) {
                    sendMessageToPlayer(playerID,"본인 돌을 선택해 주세요.");
                    return;
                }
                sendMessageToPlayer(playerID,"Select Success:" + index);
                selectedNodes.put(playerID, index);
                masterGameBoard.setSelectedNode(index);
            } else {
                int fromIndex = selectedNodes.get(playerID);
                if (fromIndex == index) {
                    sendMessageToPlayer(playerID,"돌 선택을 취소합니다");
                    selectedNodes.remove(playerID);
                    masterGameBoard.setSelectedNode(-1);
                    return;
                }
                boolean canMove = rule.canMove(fromIndex, index, masterGameBoard.getNodes());
                if (!canMove) {
                    sendMessageToPlayer(playerID,"이동할 수 없습니다. 다시 선택해 주세요.");
                    return;
                }
                masterGameBoard.movingPiece(fromIndex, index);
                broadcastMessage("Move Success:" + fromIndex + "to" + index);
                selectedNodes.remove(playerID);
                Piece[] nodes = masterGameBoard.getNodes();
                if (rule.isMill(index, nodes)) {
                    masterGameBoard.setPhase(GamePhase.REMOVE);
                    broadcastMessage("Mill 성공💣");
                    return;
                }
                switchTurn();
                if (rule.isJump(opponentPlayer, nodes)) {
                    masterGameBoard.setPhase(GamePhase.JUMP);
                    broadcastMessage("JUMP Phase");
                }
                else {
                    broadcastMessage("MOVING Phase");
                    masterGameBoard.setPhase(GamePhase.MOVING);
                }
            }
        }
        else if (phase == GamePhase.REMOVE) {
            Piece[] nodes = masterGameBoard.getNodes();
            boolean canRemove=rule.canRemove(nodes,index,opponentPlayer);
            if (!canRemove){
                if (rule.isInMill(index, nodes) && opponentPlayer == nodes[index]) {
                    sendMessageToPlayer(playerID,"Mill에 포함되어있습니다. 다른 돌을 먼저 선택해주세요.");
                } else {
                    sendMessageToPlayer(playerID,"상대방 돌을 선택해주세요.");
                }
                return;
            }
            masterGameBoard.remove(index);
            broadcastMessage("REMOVE:" + index + ":" + playerID);
            nodes=masterGameBoard.getNodes();
            if(masterGameBoard.getpiecesPlaced()>=MaxPlacedPiece && rule.isDefeat(opponentPlayer, nodes)){
                broadcastMessage(opponentPlayer + "의 돌이 2개남았습니다. " + currentPlayer + " 승리🎉");
                winner=playerID;
                loser=3-playerID;
                masterGameBoard.setPhase(GamePhase.END);
                broadcastMessage("GAME END");
                return;
            }
            switchTurn();
            if (masterGameBoard.getpiecesPlaced()<MaxPlacedPiece){
                masterGameBoard.setPhase(GamePhase.PLACING);
                broadcastMessage("PLACING Phase");
            }
            else if (rule.isJump(opponentPlayer, nodes)) {
                broadcastMessage(opponentPlayer + "돌이 3개 남았습니다. 자유롭게 이동이 가능합니다.");
                masterGameBoard.setPhase(GamePhase.JUMP);
                broadcastMessage("JUMP Phase");
            }
            else{
                masterGameBoard.setPhase(GamePhase.MOVING);
                broadcastMessage("MOVING Phase");
            }
        }
        else if (phase == GamePhase.JUMP) {
            Piece[] nodes = masterGameBoard.getNodes();
            if (!selectedNodes.containsKey(playerID)) {
                if (!masterGameBoard.isCurrentPlayerPiece(index,currentPlayer)) {
                    sendMessageToPlayer(playerID,"본인 돌을 선택해 주세요.");
                    return;
                }
                sendMessageToPlayer(playerID,"Select Success:" + index);
                selectedNodes.put(playerID, index);
                masterGameBoard.setSelectedNode(index);
            } else {
                int fromIndex = selectedNodes.get(playerID);
                if (fromIndex == index) {
                    sendMessageToPlayer(playerID,"돌 선택을 취소합니다");
                    selectedNodes.remove(playerID);
                    masterGameBoard.setSelectedNode(-1);
                    return;
                }
                boolean canJump=rule.canJump(index,nodes);
                if (!canJump) {
                    sendMessageToPlayer(playerID,"이동할 수 없습니다. 다시 선택해 주세요.");
                    return;
                }
                masterGameBoard.jumpingPiece(fromIndex, index);
                broadcastMessage("Jump Success:" + selectedNodes.get(playerID) + "to" + index);
                selectedNodes.remove(playerID);
                nodes = masterGameBoard.getNodes();
                if (rule.isMill(index, nodes)) {
                    masterGameBoard.setPhase(GamePhase.REMOVE);
                    broadcastMessage("Mill 성공💣");
                    return;
                }
                switchTurn();
                if (rule.isJump(opponentPlayer, nodes)) {
                    masterGameBoard.setPhase(GamePhase.JUMP);
                    broadcastMessage("JUMP Phase");
                }
                else {
                    masterGameBoard.setPhase(GamePhase.MOVING);
                    broadcastMessage("MOVING Phase");
                    }
                }
            }
        else if (phase == GamePhase.END) {
            broadcastMessage("END");

        }
    }

    public static void handleClientCommand(String clientCommand, int playerID) throws IOException {
        String command = clientCommand;
        if (masterGameBoard.getPhase() == GamePhase.END) {

            if (command.equals("QUIT")) {
                stopServer();
                return;
            }

            if (command.equals("RESTART")) {
                synchronized (restartLock) {
                    playerReadyToRestart.put(playerID, true);
                    System.out.println("플레이어 " + playerID + " 재시작 동의.");

                    if (playerReadyToRestart.size() == 2 &&
                            playerReadyToRestart.values().stream().allMatch(ready -> ready)) {

                        broadcastMessage("GAME_RESTARTING");
                        resetGame();
                        System.out.println("게임 재시작 완료.");
                    } else {
                        sendMessageToPlayer(playerID, "대기 중: 상대방의 응답을 기다립니다.");
                    }
                }
            }
        }
    }

    private static void resetGame() throws IOException {
        masterGameBoard.initializeBoard();
        masterGameBoard.setPiecesPlaced(0);
        masterGameBoard.setPhase(GamePhase.PLACING);

        playerReadyToRestart.clear();
        final int STARTING_PLAYER_ID = 1;
        setCurrentPlayer(STARTING_PLAYER_ID);
        Piece startPlayer=Piece.fromServerID(STARTING_PLAYER_ID);

        System.out.println("게임 상태 초기화...");
        broadcastMessage("GAME_RESTARTING"); // 클라이언트 UI 초기화 트리거
        broadcastMessage("PLACING Phase");
        broadcastMessage("TURN:" +startPlayer);
    }
    private static void stopServer() {
        try {
            broadcastMessage("SERVER_SHUTDOWN");
            System.out.println("곧 서버를 종료합니다.");

            for (PlayerHandler handler : handlers) {
                handler.closeConnection();
            }
            handlers.clear();

            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
                System.out.println("서버 소켓이 닫혔습니다.");
            }

            System.out.println("서버 프로그램이 안전하게 종료됩니다.");
            System.exit(0);

        } catch (IOException e) {
            System.err.println("서버 종료 중 오류 발생: " + e.getMessage());
            System.exit(1);
        }
    }
}


