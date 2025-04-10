import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

public class TetrisGame extends JPanel implements ActionListener {
    private final int WIDTH = 10; // ���� ������ �ʺ�
    private final int HEIGHT = 20; // ���� ������ ����
    private int NORMAL_DELAY = 500; // �Ϲ� �ӵ� ���� �ð�
    private final int DROP_DELAY = 50; // ����� �ٴڿ� ���� �� ���� �ð�

    private Timer timer; // ����� �������� Ÿ�̸�
    private Timer dropTimer; // ��� ��ġ ������ ���� Ÿ�̸�
    private Timer gameTimeTimer; // ���� �ð� Ÿ�̸�
    private boolean isFallingFinished = false; // ����� �������� ���� �Ϸ� ����
    private boolean isStarted = false; // ���� ���� ����
    private boolean isPaused = false; // ���� �Ͻ� ���� ����
    private boolean isFullScreen = false; // ��ü ȭ�� ��� ����
    private boolean isGameOver = false; // ���� ���� ����
    private int numLinesRemoved = 0; // ���ŵ� �� ��
    private int curX = 0; // ���� ����� X ��ġ
    private int curY = 0; // ���� ����� Y ��ġ
    private JLabel scoreLabel; // ���� ǥ�� ��
    private JLabel timeLabel; // �ð� ǥ�� ��
    private JLabel nextLabel; // ���� ��� ǥ�� ��
    private JLabel controlsLabel; // ���۹� ǥ�� ��
    private Shape curPiece; // ���� ��� ���
    private Shape nextPiece1; // ���� ��� ���
    private Shape nextPiece2; // �ٴ��� ��� ���
    private Shape.Tetrominoes[] board; // ���� ���� �迭
    private JFrame frame; // ���� ������
    private long startTime; // ���� ���� �ð�
    private long lastSpeedIncreaseTime; // ������ �ӵ� ���� �ð�
    private long lastGrayLineTime; // ������ ȸ�� ���� �߰� �ð�
    private long pausedTime; // �Ͻ� ���� ���� �ð�
    private long totalPausedDuration; // �� �Ͻ� ���� �ð�

    public TetrisGame(JFrame frame) {
        this.frame = frame;
        initBoard();
    }

    private void initBoard() {
        setFocusable(true);
        setLayout(null);

        // �ð� �� ����
        timeLabel = new JLabel("Time: 00:00");
        timeLabel.setForeground(Color.WHITE);
        timeLabel.setBounds(10, 10, 100, 30);
        add(timeLabel);

        // ���� �� ����
        scoreLabel = new JLabel("Score: 0");
        scoreLabel.setForeground(Color.WHITE);
        scoreLabel.setBounds(190, 10, 100, 30);
        add(scoreLabel);

        // ���� ��� �� ����
        nextLabel = new JLabel("Next:");
        nextLabel.setForeground(Color.WHITE);
        nextLabel.setBounds(290, 10, 100, 30);
        add(nextLabel);

        // ���۹� �� ����
        controlsLabel = new JLabel("<html>��� ����Ű : �¿� �̵�<br>�� ����Ű : ������ ��������<br>�� ����Ű : �� ȸ��<br>space bar : �� �ٷ� ��ġ<br>F key: ��üȭ�� ���<br>R key: �ٽ� ����<br>Esc key: ���� ����<br>P key: �Ͻ� ����</html>");
        controlsLabel.setForeground(Color.WHITE);
        controlsLabel.setBounds(290, 400, 200, 150);
        add(controlsLabel);

        // ���� ���� �ʱ�ȭ
        board = new Shape.Tetrominoes[WIDTH * HEIGHT];
        addKeyListener(new TAdapter());
        timer = new Timer(NORMAL_DELAY, this);
        dropTimer = new Timer(DROP_DELAY, e -> pieceDropped()); // ��� ��ġ ���� Ÿ�̸�
        dropTimer.setRepeats(false); // �ݺ����� �ʵ��� ����

        clearBoard();
        nextPiece1 = new Shape();
        nextPiece2 = new Shape();
        nextPiece1.setRandomShape();
        nextPiece2.setRandomShape();
        newPiece();
        timer.start();

        // ���� ���� �ð� ����
        startTime = System.currentTimeMillis();
        lastSpeedIncreaseTime = startTime; // ������ �ӵ� ���� �ð� �ʱ�ȭ
        lastGrayLineTime = startTime; // ������ ȸ�� ���� �߰� �ð� �ʱ�ȭ
        totalPausedDuration = 0; // �� �Ͻ� ���� �ð� �ʱ�ȭ
        gameTimeTimer = new Timer(1000, e -> {
            updateGameTime();
            increaseSpeed();
            addGrayLine();
        });
        gameTimeTimer.start();
    }

    private void updateGameTime() {
        long elapsed = System.currentTimeMillis() - startTime - totalPausedDuration;
        SimpleDateFormat df = new SimpleDateFormat("mm:ss");
        timeLabel.setText("Time: " + df.format(new Date(elapsed)));
    }

    private void increaseSpeed() {
        long elapsed = System.currentTimeMillis() - lastSpeedIncreaseTime;
        if (elapsed >= 5000) { // 5�ʸ��� �ӵ� ����
            NORMAL_DELAY = Math.max(10, NORMAL_DELAY - 10); // �ּ� 10ms���� �ӵ� ����
            timer.setDelay(NORMAL_DELAY);
            lastSpeedIncreaseTime = System.currentTimeMillis(); // ������ �ӵ� ���� �ð� ����
        }
    }

    private void addGrayLine() {
        long elapsed = System.currentTimeMillis() - startTime - totalPausedDuration;
        if (elapsed >= 60000) { // 1�� �ĺ��� ȸ�� ���� �߰�
            long timeSinceLastGrayLine = System.currentTimeMillis() - lastGrayLineTime - totalPausedDuration;
            if (timeSinceLastGrayLine >= 15000) { // 15�ʸ��� ȸ�� ���� �߰�
                lastGrayLineTime = System.currentTimeMillis();

                // ���� ������ ��ϵ��� �� �� ���� �̵�
                for (int y = HEIGHT - 2; y >= 0; y--) {
                    for (int x = 0; x < WIDTH; x++) {
                        board[(y + 1) * WIDTH + x] = board[y * WIDTH + x];
                    }
                }

                // ���ο� ȸ�� ���� ����, �����ϰ� �� ĭ ����
                Random rand = new Random();
                int emptyIndex = rand.nextInt(WIDTH);
                for (int x = 0; x < WIDTH; x++) {
                    if (x == emptyIndex) {
                        board[x] = Shape.Tetrominoes.NoShape;
                    } else {
                        board[x] = Shape.Tetrominoes.GrayShape;
                    }
                }
                repaint(); // ���� ����
            }
        }
    }

    private int squareWidth() {
        return Math.min((int) getSize().getWidth() / (WIDTH + 10), (int) getSize().getHeight() / (HEIGHT + 2)); // ���� �߰�
    }

    private int squareHeight() {
        return squareWidth();
    }

    private Shape.Tetrominoes shapeAt(int x, int y) {
        return board[(y * WIDTH) + x];
    }

    private void clearBoard() {
        for (int i = 0; i < HEIGHT * WIDTH; i++) {
            board[i] = Shape.Tetrominoes.NoShape;
        }
    }

    private void dropDown() {
        int newY = curY;
        while (newY > 0) {
            if (!tryMove(curPiece, curX, newY - 1)) {
                break;
            }
            newY--;
        }
        pieceDropped(); // �����ð� ���� �ٷ� ��ġ
    }

    private void oneLineDown() {
        if (!tryMove(curPiece, curX, curY - 1)) {
            dropTimer.start(); // ��� ��ġ ���� Ÿ�̸� ����
        }
    }

    private void pieceDropped() {
        for (int i = 0; i < 4; i++) {
            int x = curX + curPiece.x(i);
            int y = curY - curPiece.y(i);
            board[(y * WIDTH) + x] = curPiece.getShape();
        }
        removeFullLines();
        if (!isFallingFinished) {
            newPiece();
        }
        // ����� ����� �Ѿ����� Ȯ��
        if (isGameOver()) {
            timer.stop();
            gameTimeTimer.stop();
            isStarted = false;
            isGameOver = true; // ���� ���� ���·� ����
            scoreLabel.setText("Game Over");
            repaint(); // ���� ���� �޽��� ǥ��
        }
    }

    private boolean isGameOver() {
        for (int i = 0; i < WIDTH; i++) {
            if (shapeAt(i, HEIGHT - 4) != Shape.Tetrominoes.NoShape) {
                return true;
            }
        }
        return false;
    }

    private void removeFullLines() {
        int numFullLines = 0;
        for (int i = HEIGHT - 1; i >= 0; i--) {
            boolean lineIsFull = true;
            for (int j = 0; j < WIDTH; j++) {
                if (shapeAt(j, i) == Shape.Tetrominoes.NoShape) {
                    lineIsFull = false;
                    break;
                }
            }
            if (lineIsFull) {
                numFullLines++;
                for (int k = i; k < HEIGHT - 1; k++) {
                    for (int j = 0; j < WIDTH; j++) {
                        board[(k * WIDTH) + j] = shapeAt(j, k + 1);
                    }
                }
            }
        }
        if (numFullLines > 0) {
            numLinesRemoved += numFullLines;
            scoreLabel.setText("Score: " + numLinesRemoved);
            isFallingFinished = true;
            curPiece.setShape(Shape.Tetrominoes.NoShape);
            repaint();
        }
    }

    private void newPiece() {
        curPiece = nextPiece1;
        nextPiece1 = nextPiece2;
        nextPiece2 = new Shape();
        nextPiece2.setRandomShape();
        curX = WIDTH / 2 + 1;
        curY = HEIGHT - 1 + curPiece.minY();
        if (!tryMove(curPiece, curX, curY)) {
            curPiece.setShape(Shape.Tetrominoes.NoShape);
            timer.stop();
            gameTimeTimer.stop();
            isStarted = false;
            isGameOver = true; // ���� ���� ���·� ����
            scoreLabel.setText("Game Over");
            repaint(); // ���� ���� �޽��� ǥ��
        }
    }

    private boolean tryMove(Shape newPiece, int newX, int newY) {
        for (int i = 0; i < 4; i++) {
            int x = newX + newPiece.x(i);
            int y = newY - newPiece.y(i);
            if (x < 0 || x >= WIDTH || y < 0 || y >= HEIGHT) {
                return false;
            }
            if (shapeAt(x, y) != Shape.Tetrominoes.NoShape) {
                return false;
            }
        }
        curPiece = newPiece;
        curX = newX;
        curY = newY;
        repaint();
        return true;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!isPaused) {
            if (isFallingFinished) {
                isFallingFinished = false;
                newPiece();
            } else {
                oneLineDown();
            }
        }
    }

    private void doDrawing(Graphics g) {
        Dimension size = getSize();
        int boardTop = 50; // ���� ���� �߰�
        int boardLeft = 10; // �⺻ ��忡���� ���� ���� ����
        if (isFullScreen)
            boardLeft = 500;

        // �׵θ� �׸���
        g.setColor(Color.white);
        g.fillRect(boardLeft - 5, boardTop - 5, (WIDTH * squareWidth()) + 10, 5); // ��� �׵θ�
        g.fillRect(boardLeft - 5, boardTop + HEIGHT * squareHeight(), (WIDTH * squareWidth()) + 10, 5); // �ϴ� �׵θ�
        g.fillRect(boardLeft - 5, boardTop - 5, 5, HEIGHT * squareHeight() + 10); // ���� �׵θ�
        g.fillRect(boardLeft + WIDTH * squareWidth(), boardTop - 5, 5, HEIGHT * squareHeight() + 10); // ������ �׵θ�

        // �׸��� �׸���
        for (int i = 1; i <= HEIGHT; i++) {
            for (int j = 0; j <= WIDTH; j++) {
                g.setColor(Color.DARK_GRAY);
                g.drawLine(boardLeft + j * squareWidth(), boardTop, boardLeft + j * squareWidth(), boardTop + HEIGHT * squareHeight());
                g.drawLine(boardLeft, boardTop + i * squareHeight(), boardLeft + WIDTH * squareWidth(), boardTop + i * squareHeight());
            }
        }

        // ���� ���� ���� �׸���
        g.setColor(Color.RED);
        g.drawLine(boardLeft, boardTop + 3 * squareHeight(), boardLeft + WIDTH * squareWidth(), boardTop + 3 * squareHeight());

        // ��� �׸���
        for (int i = 0; i < HEIGHT; i++) {
            for (int j = 0; j < WIDTH; j++) {
                Shape.Tetrominoes shape = shapeAt(j, HEIGHT - i - 1);
                if (shape != Shape.Tetrominoes.NoShape) {
                    drawSquare(g, boardLeft + j * squareWidth(), boardTop + i * squareHeight(), shape);
                }
            }
        }
        if (!isGameOver) {
            if (curPiece.getShape() != Shape.Tetrominoes.NoShape) {
                for (int i = 0; i < 4; i++) {
                    int x = curX + curPiece.x(i);
                    int y = curY - curPiece.y(i);
                    drawSquare(g, boardLeft + x * squareWidth(), boardTop + (HEIGHT - y - 1) * squareHeight(), curPiece.getShape());
                }
            }
        }

        // ���� ���� �޽��� �׸���
        if (isGameOver) {
            if (isFullScreen) {
                g.setColor(Color.white);
                g.setFont(new Font("���� ���", Font.BOLD, 60));
                g.drawString("Game Over", boardLeft + WIDTH * squareWidth() / 2 - 163, boardTop + HEIGHT * squareHeight() / 2 - 18);
                g.setFont(new Font("���� ���", Font.BOLD, 48));
                g.drawString("Score: " + numLinesRemoved, boardLeft + WIDTH * squareWidth() / 2 - 88, boardTop + HEIGHT * squareHeight() / 2 + 62);
                g.setColor(Color.RED);
                g.setFont(new Font("���� ���", Font.BOLD, 60));
                g.drawString("Game Over", boardLeft + WIDTH * squareWidth() / 2 - 165, boardTop + HEIGHT * squareHeight() / 2 - 20);
                g.setFont(new Font("���� ���", Font.BOLD, 48));
                g.drawString("Score: " + numLinesRemoved, boardLeft + WIDTH * squareWidth() / 2 - 90, boardTop + HEIGHT * squareHeight() / 2 + 60);
            } else {
                g.setColor(Color.white);
                g.setFont(new Font("���� ���", Font.BOLD, 36));
                g.drawString("Game Over", boardLeft + WIDTH * squareWidth() / 2 - 99, boardTop + HEIGHT * squareHeight() / 2 - 19);
                g.setFont(new Font("���� ���", Font.BOLD, 24));
                g.drawString("Score: " + numLinesRemoved, boardLeft + WIDTH * squareWidth() / 2 - 59, boardTop + HEIGHT * squareHeight() / 2 + 21);
                g.setColor(Color.RED);
                g.setFont(new Font("���� ���", Font.BOLD, 36));
                g.drawString("Game Over", boardLeft + WIDTH * squareWidth() / 2 - 100, boardTop + HEIGHT * squareHeight() / 2 - 20);
                g.setFont(new Font("���� ���", Font.BOLD, 24));
                g.drawString("Score: " + numLinesRemoved, boardLeft + WIDTH * squareWidth() / 2 - 60, boardTop + HEIGHT * squareHeight() / 2 + 20);
            }
        }

        // ���� ��� �׸���
        int nextPieceX = (WIDTH + 2) * squareWidth();
        if (isFullScreen)
            nextPieceX += 500;
        int nextPieceY1 = 50;
        int nextPieceY2 = 200;
        if (isFullScreen)
            nextPieceY2 = 300;
        for (int i = 0; i < 4; i++) {
            int x1 = nextPieceX + (nextPiece1.x(i) + 1) * squareWidth(); // nextPiece1.x(i) + 1 �߾� ����
            int y1 = nextPieceY1 + (nextPiece1.y(i) + 1) * squareHeight(); // nextPiece1.y(i) + 1 �߾� ����
            drawSquare(g, x1, y1, nextPiece1.getShape());

            int x2 = nextPieceX + (nextPiece2.x(i) + 1) * squareWidth(); // nextPiece2.x(i) + 1 �߾� ����
            int y2 = nextPieceY2 + (nextPiece2.y(i) + 1) * squareHeight(); // nextPiece2.y(i) + 1 �߾� ����
            drawSquare(g, x2, y2, nextPiece2.getShape());
        }
    }

    private void drawSquare(Graphics g, int x, int y, Shape.Tetrominoes shape) {
        Color colors[] = {
                new Color(0, 0, 0), new Color(204, 102, 102),
                new Color(102, 204, 102), new Color(102, 102, 204),
                new Color(204, 204, 102), new Color(204, 102, 204),
                new Color(102, 204, 204), new Color(218, 170, 0),
                new Color(128, 128, 128) // ȸ�� ��� ���� �߰�
        };
        Color color = colors[shape.ordinal()];
        g.setColor(color);
        g.fillRect(x + 1, y + 1, squareWidth() - 2, squareHeight() - 2);
        g.setColor(color.brighter());
        g.drawLine(x, y + squareHeight() - 1, x, y);
        g.drawLine(x, y, x + squareWidth() - 1, y);
        g.setColor(color.darker());
        g.drawLine(x + 1, y + squareHeight() - 1, x + squareWidth() - 1, y + squareHeight() - 1);
        g.drawLine(x + squareWidth() - 1, y + squareHeight() - 1, x + squareWidth() - 1, y + 1);
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        setBackground(Color.BLACK);
        doDrawing(g);
    }

    private class TAdapter extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            int keycode = e.getKeyCode();
            if (keycode == 'p' || keycode == 'P') {
                pause();
                return;
            }
            if (keycode == KeyEvent.VK_F) {
                toggleFullScreen();
                return;
            }
            if (keycode == 'r' || keycode == 'R') {
                start();
                return;
            }
            if (keycode == KeyEvent.VK_ESCAPE) { // ESC Ű�� ������ â ����
                System.exit(0);
                return;
            }
            if (!isStarted || curPiece.getShape() == Shape.Tetrominoes.NoShape) {
                return;
            }
            if (isPaused) {
                return;
            }
            switch (keycode) {
                case KeyEvent.VK_LEFT:
                    tryMove(curPiece, curX - 1, curY);
                    break;
                case KeyEvent.VK_RIGHT:
                    tryMove(curPiece, curX + 1, curY);
                    break;
                case KeyEvent.VK_DOWN:
                    oneLineDown();
                    break;
                case KeyEvent.VK_UP:
                    tryMove(curPiece.rotateRight(), curX, curY);
                    break;
                case KeyEvent.VK_SPACE:
                    dropDown();
                    break;
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                timer.setDelay(NORMAL_DELAY);
            }
        }
    }

    private void pause() {
        if (!isStarted) {
            return;
        }
        isPaused = !isPaused;
        if (isPaused) {
            scoreLabel.setText("Paused");
            timer.stop();
            gameTimeTimer.stop();
            pausedTime = System.currentTimeMillis();
        } else {
            scoreLabel.setText("Score: " + numLinesRemoved);
            totalPausedDuration += System.currentTimeMillis() - pausedTime;
            timer.start();
            gameTimeTimer.start();
        }
        repaint();
    }

    private void toggleFullScreen() {
        GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        isFullScreen = !isFullScreen;
        frame.dispose();
        frame.setUndecorated(isFullScreen);
        if (isFullScreen) {
            device.setFullScreenWindow(frame);
        } else {
            device.setFullScreenWindow(null);
            frame.setSize(500, 600); // ���� ũ��� ����
            frame.setLocationRelativeTo(null);
        }
        frame.setVisible(true);

        // ��ü ȭ�� ��忡 ���� �� ��ġ ����
        Font fullScreenFont = new Font("���� ���", Font.BOLD, 18);
        Font normalFont = new Font("���� ���", Font.BOLD, 12);

        if (isFullScreen) {
            timeLabel.setBounds(500, 10, 100, 30);
            scoreLabel.setBounds(835, 10, 100, 30);
            nextLabel.setBounds(1000, 10, 100, 30);
            controlsLabel.setBounds(1000, 650, 400, 300);

            timeLabel.setFont(fullScreenFont);
            scoreLabel.setFont(fullScreenFont);
            nextLabel.setFont(fullScreenFont);
            controlsLabel.setFont(fullScreenFont);
        } else {
            timeLabel.setBounds(10, 10, 100, 30);
            scoreLabel.setBounds(190, 10, 100, 30);
            nextLabel.setBounds(290, 10, 100, 30);
            controlsLabel.setBounds(290, 400, 200, 150);

            timeLabel.setFont(normalFont);
            scoreLabel.setFont(normalFont);
            nextLabel.setFont(normalFont);
            controlsLabel.setFont(normalFont);
        }
    }

    public void start() {
        if (isPaused) {
            return;
        }
        isStarted = true;
        isFallingFinished = false;
        numLinesRemoved = 0;
        isGameOver = false;
        clearBoard();
        newPiece();
        timer.stop(); // ���� Ÿ�̸Ӹ� ����
        NORMAL_DELAY = 500;
        timer = new Timer(NORMAL_DELAY, this); // ���ο� Ÿ�̸� ����
        timer.start();
        gameTimeTimer.stop(); // ���� ���� �ð� Ÿ�̸Ӹ� ����
        timeLabel.setText("Time: 00:00");
        startTime = System.currentTimeMillis(); // ���� ���� �ð� �ʱ�ȭ
        lastSpeedIncreaseTime = startTime; // ������ �ӵ� ���� �ð� �ʱ�ȭ
        lastGrayLineTime = startTime; // ������ ȸ�� ���� �߰� �ð� �ʱ�ȭ
        totalPausedDuration = 0; // �� �Ͻ� ���� �ð� �ʱ�ȭ
        gameTimeTimer = new Timer(1000, e -> {
            updateGameTime();
            increaseSpeed();
            addGrayLine();
        }); // ���ο� ���� �ð� Ÿ�̸� ����
        gameTimeTimer.start();
        scoreLabel.setText("Score: 0");
    }

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            JFrame frame = new JFrame("Tetris");
            frame.setSize(500, 600);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            TetrisGame game = new TetrisGame(frame);
            frame.add(game);
            frame.setVisible(true);
            game.start();
        });
    }
}
