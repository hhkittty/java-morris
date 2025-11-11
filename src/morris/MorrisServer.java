package morris;


import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class MorrisServer {
    private static List<PlayerHandler> handlers = new CopyOnWriteArrayList<>();
    private static final int REQUIRED_PLAYERS = 2;
    private static volatile int readyCount = 0;
    private static final Object readyLock = new Object();
    static volatile int currentPlayerID = 1;
    private static GameBoard masterGameBoard = new GameBoard();
    private static RuleChecker rule = new RuleChecker();
    private static Map<Integer, Integer> selectedNodes = new ConcurrentHashMap<>();
    private static int MaxPlacedPiece=18;

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(9000); // 9000번 포트
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

    public static void clientIsReady() throws IOException {
        synchronized (readyLock) {
            readyCount++;
            System.out.println("클라이언트 READY 수신. 현재 readyCount: " + readyCount);
            if (readyCount == REQUIRED_PLAYERS) {
                // 4. 모든 클라이언트가 READY 상태이므로 최종 시작 명령 전송
                System.out.println("모든 플레이어가 준비 완료. GO 명령 전송.");
                broadcastMessage("GO");
            }
        }
    }

    public static void switchTurn() throws IOException {
        currentPlayerID = 3 - currentPlayerID; // 0 <-> 1 전환
        // 🌟 모든 클라이언트에게 새로운 턴 ID를 메시지로 전송
        broadcastMessage("TURN:" + currentPlayerID);
    }

    public static synchronized void handleClientMove(GameMove move, int playerID) throws IOException {
        int index = move.clickIndex;
        GamePhase phase=masterGameBoard.getPhase();
        System.out.println(phase);

        Piece currentPlayer = Piece.fromServerID(playerID);

        if (phase == GamePhase.PLACING) {
            boolean isPlace=masterGameBoard.placePiece(index, currentPlayer);
            if(!isPlace) {
                handlers.get(playerID-1).sendMessage("빈 공간을 선택해 주세요.");
            }
            else {
                broadcastMessage(currentPlayer + ";" + index + ";" + phase + " Success");
                if (!rule.isMill(index, masterGameBoard.getNodes())) {
                    switchTurn();
                    System.out.println(masterGameBoard.getpiecesPlaced());
                }
                else {
                    masterGameBoard.setPhase(GamePhase.REMOVE);
                    broadcastMessage("Mill");
                }
                if (masterGameBoard.getpiecesPlaced() == MaxPlacedPiece && masterGameBoard.getPhase()!=GamePhase.REMOVE) {
                    broadcastMessage("--말놓기 종료--");
                    broadcastMessage("MOVING Phase");
                    masterGameBoard.setPhase(GamePhase.MOVING);
                }
            }
        }
        else if (phase == GamePhase.MOVING) {
            if (!selectedNodes.containsKey(playerID)) {
                if (masterGameBoard.isCurrentPlayerPiece(index)) {
                    handlers.get(playerID - 1).sendMessage("Select Success:" + index);
                    selectedNodes.put(playerID, index);
                    masterGameBoard.setSelectedNode(index);
                } else {
                    handlers.get(playerID - 1).sendMessage("본인 돌을 선택해 주세요.");
                }
            }
            else {
                if (selectedNodes.get(playerID) == index) {
                    handlers.get(playerID - 1).sendMessage("돌 선택을 취소합니다");
                    selectedNodes.remove(playerID);
                    masterGameBoard.setSelectedNode(-1);
                }
                else {
                    boolean isMoving = masterGameBoard.movingPiece(selectedNodes.get(playerID), index);
                    Piece opponentPlayer = Piece.fromServerID(3 - playerID);
                    if (!isMoving) {
                        broadcastMessage("이동할 수 없습니다. 다시 선택해 주세요.");
                    }
                    else {
                        broadcastMessage("Move Success:" + selectedNodes.get(playerID) + "to" + index);
                        selectedNodes.remove(playerID);
                        Piece[] nodes = masterGameBoard.getNodes();
                        if (rule.isMill(index, nodes)) {
                            masterGameBoard.setPhase(GamePhase.REMOVE);
                            broadcastMessage("Mill");
                        } else if (rule.countPieces(opponentPlayer, nodes) == 3) {
                            masterGameBoard.setPhase(GamePhase.JUMP);
                            broadcastMessage("JUMP Phase");
                            switchTurn();
                        } else {
                            switchTurn();
                            broadcastMessage("MOVING Phase");
                            masterGameBoard.setPhase(GamePhase.MOVING);
                        }
                    }
                }
            }
        }
        else if (phase == GamePhase.REMOVE) {
            boolean isRemove = masterGameBoard.remove(index);
            Piece[] nodes = masterGameBoard.getNodes();
            if (isRemove) {
                broadcastMessage("REMOVE:" + index + ":" + playerID);
                masterGameBoard.setPhase(GamePhase.MOVING);
                broadcastMessage("MOVING Phase");
                nodes[index] = Piece.NONE;
                Piece opponentPlayer = (currentPlayer == Piece.BLACK) ? Piece.WHITE : Piece.BLACK;
                if(masterGameBoard.getpiecesPlaced()<MaxPlacedPiece){
                    masterGameBoard.setPhase(GamePhase.PLACING);
                    broadcastMessage("PLACING Phase");
                }
                if (rule.countPieces(opponentPlayer, nodes) == 3 && masterGameBoard.getpiecesPlaced()==MaxPlacedPiece) {
                    broadcastMessage(opponentPlayer + "돌이 3개 남았습니다. 자유롭게 이동이 가능합니다.");
                    masterGameBoard.setPhase(GamePhase.JUMP);
                    switchTurn();
                    if (currentPlayer == Piece.BLACK) {
                        broadcastMessage("JUMP Phase for WHITE");
                    } else {
                        broadcastMessage("JUMP Phase for BLACK");
                    }
                }
                else if (rule.isDefeat(opponentPlayer, nodes)&& masterGameBoard.getpiecesPlaced()==MaxPlacedPiece) {
                    broadcastMessage(opponentPlayer + "의 돌이 2개남았습니다. " + currentPlayer + " 승리🎉");
                    masterGameBoard.setPhase(GamePhase.END);
                    broadcastMessage("GAME END");
                }
                else{
                    switchTurn();
                }
            } else {
                Piece opponent = Piece.fromServerID(3 - playerID);

                if (rule.isInMill(index, nodes) && opponent == nodes[index]) {
                    handlers.get(playerID - 1).sendMessage("Mill에 포함되어있습니다. 다른 돌을 먼저 선택해주세요.");
                } else {
                    handlers.get(playerID - 1).sendMessage("상대방 돌을 선택해주세요.");
                }
            }
        }
        else if (phase == GamePhase.JUMP) {
            Piece opponentPlayer = currentPlayer == Piece.BLACK ? Piece.WHITE : Piece.BLACK;
            Piece[] nodes = masterGameBoard.getNodes();
            if (!selectedNodes.containsKey(playerID)) {
                if (masterGameBoard.isCurrentPlayerPiece(index)) {
                    handlers.get(playerID - 1).sendMessage("Select Success:" + index);
                    selectedNodes.put(playerID, index);
                    masterGameBoard.setSelectedNode(index);
                } else {
                    handlers.get(playerID - 1).sendMessage("본인 돌을 선택해 주세요.");
                }
            } else {
                if (selectedNodes.get(playerID) == index) {
                    handlers.get(playerID - 1).sendMessage("돌 선택을 취소합니다");
                    selectedNodes.remove(playerID);
                    masterGameBoard.setSelectedNode(-1);
                } else {
                    boolean isJuming = masterGameBoard.jumpingPiece(selectedNodes.get(playerID), index);
                    if (!isJuming) {
                        broadcastMessage("이동할 수 없습니다. 다시 선택해 주세요.");
                    } else {
                        broadcastMessage("Jump Success:" + selectedNodes.get(playerID) + "to" + index);
                        selectedNodes.remove(playerID);
                        nodes = masterGameBoard.getNodes();
                        if (rule.isMill(index, nodes)) {
                            masterGameBoard.setPhase(GamePhase.REMOVE);
                            broadcastMessage("Mill");
                        } else {
                            switchTurn();
                            int countPieces = rule.countPieces(opponentPlayer, nodes);
                            if (countPieces == 3) {
                                masterGameBoard.setPhase(GamePhase.JUMP);
                                broadcastMessage("JUMP Phase");
                            } else {
                                masterGameBoard.setPhase(GamePhase.MOVING);
                                broadcastMessage("MOVING Phase");
                            }
                        }
                    }
                }


            }
        }
        else if (phase == GamePhase.END) {
            broadcastMessage("END");
        }
    }
}


