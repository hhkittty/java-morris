package morris;

import java.net.Socket;
import javax.swing.JFrame;
import javax.swing.*;

public class GameFrame extends JFrame {
    private JTextArea messageArea;
    private BoardView boardView;
    private GameBoard gameBoard;
    private MorrisClient client;

    public GameFrame(MorrisClient client) {
        super("Nine Men's Morris"+client.playerID);
        this.client = client;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        gameBoard = new GameBoard();
        boardView = new BoardView(this.gameBoard, client);
        add(boardView);

        setSize(600, 600);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    public GameBoard getGameBoard() {
        return gameBoard;
    }
    public BoardView getBoardView() {
        return boardView;
    }
}
