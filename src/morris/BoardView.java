package morris;

/*import static morris.GamePhase.END;
import static morris.GamePhase.JUMP_BLACK;
import static morris.GamePhase.JUMP_WHITE;
import static morris.GamePhase.REMOVE;*/  //enum은 static으로 import x unum만 import 해서쓰기

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JPanel;

public class BoardView extends JPanel {
    private final MorrisClient client;
    private int firstX, firstY, firstSize;
    private int secondX, secondY, secondSize;
    private int thirdX, thirdY, thirdSize;
    private final GameBoard gameBoard;

    private Point[] Node = new Point[24];
    private boolean metricsCalculated = false;

    private static final int CLICK_RADIUS = 20;

    private int findClickedNode(int clickX, int clickY) {
        if (Node == null)
            return -1;
        for (int i = 0; i < Node.length; i++) {
            Point nodeCenter = Node[i];

            boolean inXRange = Math.abs(clickX - nodeCenter.x) <= CLICK_RADIUS;
            boolean inYRange = Math.abs(clickY - nodeCenter.y) <= CLICK_RADIUS;
            if (inXRange && inYRange) {
                return i;
            }
        }
        return -1;
    }

    public BoardView(GameBoard gameBoard,MorrisClient client) {
        this.gameBoard = gameBoard;
        this.client= client;
        setPreferredSize(new Dimension(500, 500));
        addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                int clickX = e.getX();
                int clickY = e.getY();
                int nodeIndex = findClickedNode(clickX, clickY);
                if (nodeIndex == -1)
                    return;
                GamePhase currentPhase =gameBoard.getPhase();
                GameMove move = new GameMove(nodeIndex, currentPhase);
                client.sendGameMove(move);
            }
        });
    }

            private void calculateNodes() {
                int width = getWidth();
                //first rectangle
                int outerPadding = 50;
                this.firstX = outerPadding;
                this.firstY = outerPadding;
                this.firstSize = width - 2 * outerPadding;

                //secon rectangle
                int secondPadding = (firstSize - outerPadding) / 6;
                this.secondX = firstX + secondPadding;
                this.secondY = firstY + secondPadding;
                this.secondSize = firstSize - 2 * secondPadding;

                //third rectangle
                this.thirdX = secondX + secondPadding;
                this.thirdY = secondY + secondPadding;
                this.thirdSize = secondSize - 2 * secondPadding;

                int[] startX = {firstX, secondX, thirdX};
                int[] startY = {firstY, secondY, thirdY};
                int[] size = {firstSize, secondSize, thirdSize};
                int nodeIndexCounter = 0;

                for (int level = 0; level < 3; level++) {
                    int currentX = startX[level];
                    int currentY = startY[level];
                    int currentSize = size[level];
                    int midX = currentX + currentSize / 2;
                    int midY = currentY + currentSize / 2;

                    Node[nodeIndexCounter++] = new Point(currentX, currentY);       // 0: 좌상단
                    Node[nodeIndexCounter++] = new Point(midX, currentY);          // 1: 상단 중앙
                    Node[nodeIndexCounter++] = new Point(currentX + currentSize, currentY); // 2: 우상단
                    Node[nodeIndexCounter++] = new Point(currentX + currentSize, midY);    // 3: 우측 중앙
                    Node[nodeIndexCounter++] = new Point(currentX + currentSize, currentY + currentSize); //4: 우측 하단
                    Node[nodeIndexCounter++] = new Point(midX, currentY + currentSize); //5: 하단 중앙
                    Node[nodeIndexCounter++] = new Point(currentX, currentY + currentSize); //6: 좌측 하단
                    Node[nodeIndexCounter++] = new Point(currentX, midY); //7: 좌측 중앙
                }
                metricsCalculated = true;
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (!metricsCalculated) {
                    calculateNodes();
                }

                g.setColor(Color.BLACK);
                g.drawRect(firstX, firstY, firstSize, firstSize);
                g.drawRect(secondX, secondY, secondSize, secondSize);
                g.drawRect(thirdX, thirdY, thirdSize, thirdSize);

                //Line
                int endX = thirdX + thirdSize;
                int endY = thirdY + thirdSize;
                int midY = firstY + firstSize / 2;
                int midX = firstX + firstSize / 2;

                g.drawLine(firstX, midY, thirdX, midY);
                g.drawLine(endX, midY, firstSize + firstX, midY);
                g.drawLine(midX, firstY, midX, thirdY);
                g.drawLine(midX, endY, midX, firstSize + firstY);

                int radius = 5;
                int rockRadius = 15;

                for (Point center : Node) { //보드판 위 작은점
                    if (center != null) {
                        g.fillOval(center.x - radius, center.y - radius, radius * 2, radius * 2);
                    }
                }
                for (int i = 0; i < Node.length; i++) { //바둑돌 표시
                    Point center = Node[i];
                    Piece piece = gameBoard.getPieceAt(i);
                    if (piece != Piece.NONE) {
                        if (piece == Piece.BLACK) {
                            g.setColor(Color.BLACK);
                        } else {
                            g.setColor(Color.WHITE);
                        }
                        g.fillOval(center.x - rockRadius, center.y - rockRadius, rockRadius * 2, rockRadius * 2);
                        g.setColor(Color.BLACK);
                        g.drawOval(center.x - rockRadius, center.y - rockRadius, rockRadius * 2, rockRadius * 2);
                    }
                    //선택한 돌 표시
                    int selected = gameBoard.getSelectedNode();
                    if (i==selected){
                        g.setColor(Color.ORANGE);
                        g.drawOval(center.x - rockRadius, center.y - rockRadius, rockRadius * 2, rockRadius * 2);
                    }
                }

            }
}




