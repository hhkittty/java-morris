package morris;

import java.util.Arrays;

public class RuleChecker {
    private static final int[][] CONNECTIONS = {
            {0, 1}, {1, 2}, {2, 3}, {3, 4}, {4, 5}, {5, 6}, {6, 7}, {7, 0},
            {8, 9}, {9, 10}, {10, 11}, {11, 12}, {12, 13}, {13, 14}, {14, 15}, {15, 8},
            {16, 17}, {17, 18}, {18, 19}, {19, 20}, {20, 21}, {21, 22}, {22, 23}, {23, 16},
            {1, 9}, {3, 11}, {5, 13}, {7, 15}, {9, 17}, {11, 19}, {13, 21}, {15, 23}
    };
    private static final int[][] MILL_CONNECTIONS = {
            {0, 1, 2}, {2, 3, 4}, {4, 5, 6}, {6, 7, 0},
            {8, 9, 10}, {10, 11, 12}, {12, 13, 14}, {14, 15, 8},
            {16, 17, 18}, {18, 19, 20}, {20, 21, 22}, {22, 23, 16},
            {1, 9, 17}, {3, 11, 19}, {5, 13, 21}, {7, 15, 23}
    };

    public boolean isMill(int lastIndex, Piece[] nodes) {
        Piece playerPiece = nodes[lastIndex];
        for (int[] connection : MILL_CONNECTIONS) {
            int i = connection[0];
            int j = connection[1];
            int k = connection[2];
            if (i != lastIndex && j != lastIndex && k != lastIndex) { //지금놓은돌과 관련없으면 건너뛰기
                continue;
            }

            if (nodes[i] == playerPiece &&
                    nodes[j] == playerPiece &&
                    nodes[k] == playerPiece) {
                System.out.println("Mill 성공 💣");
                return true;
            }
        }
        return false;
    }

    public boolean isInMill(int lastIndex, Piece[] nodes) {
        Piece playerPiece = nodes[lastIndex];
        for (int[] connection : MILL_CONNECTIONS) {
            int i = connection[0];
            int j = connection[1];
            int k = connection[2];
            if (i != lastIndex && j != lastIndex && k != lastIndex) { //지금놓은돌과 관련없으면 건너뛰기
                continue;
            }
            if (nodes[i] == playerPiece &&
                    nodes[j] == playerPiece &&
                    nodes[k] == playerPiece) {
                return true;
            }
        }
        return false;
    }

    public boolean allPiecesAreInMill(Piece currentPiece, Piece[] nodes) {
        for (int i = 0; i < nodes.length; i++) {
            if (nodes[i] == currentPiece) {
                if (!isInMill(i, nodes))
                    return false;
            }
        }
        return true;
    }

    public int countPieces(Piece currentPlayer, Piece[] nodes) {
        int countPieces = 0;
        for (Piece p : nodes) {
            if (p.equals(currentPlayer)) {
                countPieces++;
            }
        }
        return countPieces;
    }

    public boolean isJump(Piece currentPlayer, Piece[] nodes) {
        return countPieces(currentPlayer, nodes) <= 3;
    }

    public boolean isDefeat(Piece currentPlayer, Piece[] nodes) {
        return countPieces(currentPlayer, nodes) == 2;
    }

    public int[][] getConnections() {
        return Arrays.copyOf(CONNECTIONS, CONNECTIONS.length);
    }

    public int[][] getMillConnections() {
        return Arrays.copyOf(MILL_CONNECTIONS, MILL_CONNECTIONS.length);
    }
}
