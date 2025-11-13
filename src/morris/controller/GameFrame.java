package morris.controller;

import javax.swing.JFrame;
import javax.swing.*;
import morris.model.GameBoard;
import morris.view.BoardView;

public class GameFrame extends JFrame {
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
