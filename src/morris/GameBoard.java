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
        rule.initializeAdjacencyMatrix();
    }

    public void updatePiece(int index, Piece pieceType) {
        this.nodes[index]=pieceType;
    }

    public boolean placePiece(int index,Piece currentPlayer) {
        /*if (piecesPlaced >= 18 || nodes[index] != Piece.NONE) {
            return false;
        }*/
        nodes[index] = currentPlayer;
        piecesPlaced++;
        System.out.println("Piece placed: " + index);
        changeTurn();
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
    }

    public boolean movingPiece(int fromIndex, int toIndex) {
        nodes[toIndex] = nodes[fromIndex];
        nodes[fromIndex] = Piece.NONE;
        changeTurn();
        return true;
    }
    public boolean isCurrentPlayerPiece(int index) {
        return nodes[index] == currentPlayer;
    }
    public boolean remove(int index) {
            nodes[index] = Piece.NONE;
            changeTurn();
            return true;
    }

    public boolean jumpingPiece(int fromIndex,int toIndex) {
        nodes[toIndex] = nodes[fromIndex];
        nodes[fromIndex] = Piece.NONE;
        changeTurn();
        return true;
    }
}



