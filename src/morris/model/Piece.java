package morris.model;

public enum Piece {
    NONE(0),
    BLACK(1),
    WHITE(2)
    ;
    private final int serverID;
    Piece(int serverID) {
        this.serverID = serverID;
    }

    public int getID(Piece piece) {
        return serverID;
    }

    public static Piece fromServerID(int serverID) {
        for (Piece piece : Piece.values()) {
            if (piece.serverID == serverID) {
                return piece;
            }
        }
        throw new IllegalArgumentException("Invalid server ID: " + serverID);
    }
}
