package morris.model;

import java.util.Arrays;

public class GameBoard {
    private final Piece[] nodes = new Piece[24];
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

    public boolean placePiece(int index,Piece player) {
        nodes[index] = player;
        piecesPlaced++;
        System.out.println("Piece placed: " + index);
        return true;
    }

    public int getpiecesPlaced() {
        return piecesPlaced;
    }
    public void setPiecesPlaced(int num) {
        piecesPlaced = num;
    }

    public Piece getPieceAt(int index) {
        return nodes[index];
    }

    public boolean movingPiece(int fromIndex, int toIndex) {
        nodes[toIndex] = nodes[fromIndex];
        nodes[fromIndex] = Piece.NONE;
        return true;
    }
    public boolean isCurrentPlayerPiece(int index,Piece player) {
        return nodes[index] == player;
    }
    public boolean remove(int index) {
            nodes[index] = Piece.NONE;
            return true;
    }
    public boolean jumpingPiece(int fromIndex,int toIndex) {
        nodes[toIndex] = nodes[fromIndex];
        nodes[fromIndex] = Piece.NONE;
        return true;
    }
    public void initializeBoard(){
        Arrays.fill(nodes, Piece.NONE);
    }
}



