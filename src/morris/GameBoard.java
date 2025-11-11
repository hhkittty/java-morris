package morris;

import java.util.Arrays;

public class GameBoard {
    private final Piece[] nodes = new Piece[24];
    private Piece currentPlayer = Piece.BLACK;
    private int piecesPlaced = 0;
    private int selectedNodeIndex = -1;
    private GamePhase currentPhase = GamePhase.PLACING;
    private RuleChecker rule=new RuleChecker();

    public GamePhase getPhase() {
        return currentPhase;
    }
    public void setPhase(GamePhase phase) {
        currentPhase = phase;
    }
    public Piece[] getNodes() {
        return nodes;
    }

    public int getSelectedNode() {
        return selectedNodeIndex;
    }

    public void setSelectedNode(int index) {
        this.selectedNodeIndex = index;
    }

    public GameBoard() {
        Arrays.fill(nodes, Piece.NONE);
        initializeAdjacencyMatrix();
    }

    public void updatePiece(int index, Piece pieceType) {
        this.nodes[index]=pieceType;
    }

    public boolean placePiece(int index,Piece currentPlayer) {
        if (piecesPlaced >= 18 || nodes[index] != Piece.NONE) {
            return false;
        }
        nodes[index] = currentPlayer;
        piecesPlaced++;
        System.out.println("Piece placed: " + index);
        if(rule.isMill(index,nodes)){
            currentPhase = GamePhase.REMOVE;
            return true;
        }

        if (piecesPlaced < 18) {
            changeTurn();
        } else {
            changeTurn();
        }
        return true;
    }

    public int getpiecesPlaced() {
        return piecesPlaced;
    }

    public Piece getPieceAt(int index) {
        return nodes[index];
    }

    private void changeTurn() {
        currentPlayer = (currentPlayer == Piece.BLACK) ? Piece.WHITE : Piece.BLACK;
        if(piecesPlaced==18){
            if(rule.isJump(currentPlayer,nodes)){
                if (currentPlayer == Piece.BLACK) {
                    currentPhase= GamePhase.JUMP_BLACK;
                }
                else{
                    currentPhase= GamePhase.JUMP_WHITE;
                }
            }
            else{ currentPhase= GamePhase.MOVING;}
        }
        else{
            currentPhase = GamePhase.PLACING;
        }
    }

    public boolean movingPiece(int fromIndex, int toIndex) {

        if (!isAdjacent(fromIndex, toIndex)) {
            System.out.println("연결된 칸을 선택해 주세요.");
            return false;
        }
        if (nodes[toIndex] != Piece.NONE) {
            System.out.println("비어있는 칸을 선택해 주세요.");
            return false;
        }
        nodes[toIndex] = nodes[fromIndex];
        nodes[fromIndex] = Piece.NONE;
        if(rule.isMill(toIndex,nodes)){
            currentPhase= GamePhase.REMOVE;
            return true;
        }
        changeTurn();
        return true;
    }
    private static final boolean[][] ADJACENCY_MATRIX = new boolean[24][24];
    public boolean isAdjacent(int fromIndex, int toIndex) {
        return ADJACENCY_MATRIX[fromIndex][toIndex];
    }

    private void initializeAdjacencyMatrix() {
        for (int[] connection : rule.getConnections()) {
            int i = connection[0];
            int j = connection[1];

            ADJACENCY_MATRIX[i][j] = true; // 0 -> 1 연결
            ADJACENCY_MATRIX[j][i] = true; // 1 -> 0 연결
        }
    }

    public boolean isCurrentPlayerPiece(int index) {
        return nodes[index] == currentPlayer;
    }

    public boolean remove(int index) {
        Piece opponentPlayer = (currentPlayer == Piece.BLACK) ? Piece.WHITE : Piece.BLACK;
        if (nodes[index] !=opponentPlayer) {
            return false;
        }
        if (rule.isInMill(index,nodes)) {
            return rule.allPiecesAreInMill(opponentPlayer, nodes);
        }
        nodes[index] = Piece.NONE;
        changeTurn();
        return true;
    }

    public boolean jumpingPiece(int fromIndex,int toIndex) {
        if (nodes[toIndex] != Piece.NONE) {
            return false;
        }
        nodes[toIndex] = nodes[fromIndex];
        nodes[fromIndex] = Piece.NONE;
        if(rule.isMill(toIndex,nodes)){
            currentPhase= GamePhase.REMOVE;
            return true;
        }
        changeTurn();
        return true;
    }
}



