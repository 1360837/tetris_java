import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

public class TetrisGame extends JPanel implements ActionListener {
    private final int WIDTH = 10; // 게임 보드의 너비
    private final int HEIGHT = 20; // 게임 보드의 높이
    private int NORMAL_DELAY = 500; // 일반 속도 지연 시간
    private final int DROP_DELAY = 50; // 블록이 바닥에 닿은 후 지연 시간

    private Timer timer; // 블록이 떨어지는 타이머
    private Timer dropTimer; // 블록 설치 지연을 위한 타이머
    private Timer gameTimeTimer; // 게임 시간 타이머
    private boolean isFallingFinished = false; // 블록이 떨어지는 동작 완료 여부
    private boolean isStarted = false; // 게임 시작 여부
    private boolean isPaused = false; // 게임 일시 중지 여부
    private boolean isFullScreen = false; // 전체 화면 모드 여부
    private boolean isGameOver = false; // 게임 오버 여부
    private int numLinesRemoved = 0; // 제거된 줄 수
    private int curX = 0; // 현재 블록의 X 위치
    private int curY = 0; // 현재 블록의 Y 위치
    private JLabel scoreLabel; // 점수 표시 라벨
    private JLabel timeLabel; // 시간 표시 라벨
    private JLabel nextLabel; // 다음 블록 표시 라벨
    private JLabel controlsLabel; // 조작법 표시 라벨
    private Shape curPiece; // 현재 블록 모양
    private Shape nextPiece1; // 다음 블록 모양
    private Shape nextPiece2; // 다다음 블록 모양
    private Shape.Tetrominoes[] board; // 게임 보드 배열
    private JFrame frame; // 게임 프레임
    private long startTime; // 게임 시작 시간
    private long lastSpeedIncreaseTime; // 마지막 속도 증가 시간
    private long lastGrayLineTime; // 마지막 회색 라인 추가 시간
    private long pausedTime; // 일시 정지 시작 시간
    private long totalPausedDuration; // 총 일시 정지 시간

    public TetrisGame(JFrame frame) {
        this.frame = frame;
        initBoard();
    }

    private void initBoard() {
        setFocusable(true);
        setLayout(null);

        // 시간 라벨 설정
        timeLabel = new JLabel("Time: 00:00");
        timeLabel.setForeground(Color.WHITE);
        timeLabel.setBounds(10, 10, 100, 30);
        add(timeLabel);

        // 점수 라벨 설정
        scoreLabel = new JLabel("Score: 0");
        scoreLabel.setForeground(Color.WHITE);
        scoreLabel.setBounds(190, 10, 100, 30);
        add(scoreLabel);

        // 다음 블록 라벨 설정
        nextLabel = new JLabel("Next:");
        nextLabel.setForeground(Color.WHITE);
        nextLabel.setBounds(290, 10, 100, 30);
        add(nextLabel);

        // 조작법 라벨 설정
        controlsLabel = new JLabel("<html>←→ 방향키 : 좌우 이동<br>↓ 방향키 : 빠르게 내려오기<br>↑ 방향키 : 블럭 회전<br>space bar : 블럭 바로 설치<br>F key: 전체화면 모드<br>R key: 다시 시작<br>Esc key: 게임 종료<br>P key: 일시 정지</html>");
        controlsLabel.setForeground(Color.WHITE);
        controlsLabel.setBounds(290, 400, 200, 150);
        add(controlsLabel);

        // 게임 보드 초기화
        board = new Shape.Tetrominoes[WIDTH * HEIGHT];
        addKeyListener(new TAdapter());
        timer = new Timer(NORMAL_DELAY, this);
        dropTimer = new Timer(DROP_DELAY, e -> pieceDropped()); // 블록 설치 지연 타이머
        dropTimer.setRepeats(false); // 반복하지 않도록 설정

        clearBoard();
        nextPiece1 = new Shape();
        nextPiece2 = new Shape();
        nextPiece1.setRandomShape();
        nextPiece2.setRandomShape();
        newPiece();
        timer.start();

        // 게임 시작 시간 설정
        startTime = System.currentTimeMillis();
        lastSpeedIncreaseTime = startTime; // 마지막 속도 증가 시간 초기화
        lastGrayLineTime = startTime; // 마지막 회색 라인 추가 시간 초기화
        totalPausedDuration = 0; // 총 일시 정지 시간 초기화
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
        if (elapsed >= 5000) { // 5초마다 속도 증가
            NORMAL_DELAY = Math.max(10, NORMAL_DELAY - 10); // 최소 10ms까지 속도 증가
            timer.setDelay(NORMAL_DELAY);
            lastSpeedIncreaseTime = System.currentTimeMillis(); // 마지막 속도 증가 시간 갱신
        }
    }

    private void addGrayLine() {
        long elapsed = System.currentTimeMillis() - startTime - totalPausedDuration;
        if (elapsed >= 60000) { // 1분 후부터 회색 라인 추가
            long timeSinceLastGrayLine = System.currentTimeMillis() - lastGrayLineTime - totalPausedDuration;
            if (timeSinceLastGrayLine >= 15000) { // 15초마다 회색 라인 추가
                lastGrayLineTime = System.currentTimeMillis();

                // 현재 보드의 블록들을 한 줄 위로 이동
                for (int y = HEIGHT - 2; y >= 0; y--) {
                    for (int x = 0; x < WIDTH; x++) {
                        board[(y + 1) * WIDTH + x] = board[y * WIDTH + x];
                    }
                }

                // 새로운 회색 라인 생성, 랜덤하게 한 칸 비우기
                Random rand = new Random();
                int emptyIndex = rand.nextInt(WIDTH);
                for (int x = 0; x < WIDTH; x++) {
                    if (x == emptyIndex) {
                        board[x] = Shape.Tetrominoes.NoShape;
                    } else {
                        board[x] = Shape.Tetrominoes.GrayShape;
                    }
                }
                repaint(); // 보드 갱신
            }
        }
    }

    private int squareWidth() {
        return Math.min((int) getSize().getWidth() / (WIDTH + 10), (int) getSize().getHeight() / (HEIGHT + 2)); // 여백 추가
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
        pieceDropped(); // 지연시간 없이 바로 설치
    }

    private void oneLineDown() {
        if (!tryMove(curPiece, curX, curY - 1)) {
            dropTimer.start(); // 블록 설치 지연 타이머 시작
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
        // 블록이 상단을 넘었는지 확인
        if (isGameOver()) {
            timer.stop();
            gameTimeTimer.stop();
            isStarted = false;
            isGameOver = true; // 게임 오버 상태로 설정
            scoreLabel.setText("Game Over");
            repaint(); // 게임 오버 메시지 표시
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
            isGameOver = true; // 게임 오버 상태로 설정
            scoreLabel.setText("Game Over");
            repaint(); // 게임 오버 메시지 표시
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
        int boardTop = 50; // 위쪽 여백 추가
        int boardLeft = 10; // 기본 모드에서는 왼쪽 여백 제거
        if (isFullScreen)
            boardLeft = 500;

        // 테두리 그리기
        g.setColor(Color.white);
        g.fillRect(boardLeft - 5, boardTop - 5, (WIDTH * squareWidth()) + 10, 5); // 상단 테두리
        g.fillRect(boardLeft - 5, boardTop + HEIGHT * squareHeight(), (WIDTH * squareWidth()) + 10, 5); // 하단 테두리
        g.fillRect(boardLeft - 5, boardTop - 5, 5, HEIGHT * squareHeight() + 10); // 왼쪽 테두리
        g.fillRect(boardLeft + WIDTH * squareWidth(), boardTop - 5, 5, HEIGHT * squareHeight() + 10); // 오른쪽 테두리

        // 그리드 그리기
        for (int i = 1; i <= HEIGHT; i++) {
            for (int j = 0; j <= WIDTH; j++) {
                g.setColor(Color.DARK_GRAY);
                g.drawLine(boardLeft + j * squareWidth(), boardTop, boardLeft + j * squareWidth(), boardTop + HEIGHT * squareHeight());
                g.drawLine(boardLeft, boardTop + i * squareHeight(), boardLeft + WIDTH * squareWidth(), boardTop + i * squareHeight());
            }
        }

        // 게임 오버 라인 그리기
        g.setColor(Color.RED);
        g.drawLine(boardLeft, boardTop + 3 * squareHeight(), boardLeft + WIDTH * squareWidth(), boardTop + 3 * squareHeight());

        // 블록 그리기
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

        // 게임 오버 메시지 그리기
        if (isGameOver) {
            if (isFullScreen) {
                g.setColor(Color.white);
                g.setFont(new Font("맑은 고딕", Font.BOLD, 60));
                g.drawString("Game Over", boardLeft + WIDTH * squareWidth() / 2 - 163, boardTop + HEIGHT * squareHeight() / 2 - 18);
                g.setFont(new Font("맑은 고딕", Font.BOLD, 48));
                g.drawString("Score: " + numLinesRemoved, boardLeft + WIDTH * squareWidth() / 2 - 88, boardTop + HEIGHT * squareHeight() / 2 + 62);
                g.setColor(Color.RED);
                g.setFont(new Font("맑은 고딕", Font.BOLD, 60));
                g.drawString("Game Over", boardLeft + WIDTH * squareWidth() / 2 - 165, boardTop + HEIGHT * squareHeight() / 2 - 20);
                g.setFont(new Font("맑은 고딕", Font.BOLD, 48));
                g.drawString("Score: " + numLinesRemoved, boardLeft + WIDTH * squareWidth() / 2 - 90, boardTop + HEIGHT * squareHeight() / 2 + 60);
            } else {
                g.setColor(Color.white);
                g.setFont(new Font("맑은 고딕", Font.BOLD, 36));
                g.drawString("Game Over", boardLeft + WIDTH * squareWidth() / 2 - 99, boardTop + HEIGHT * squareHeight() / 2 - 19);
                g.setFont(new Font("맑은 고딕", Font.BOLD, 24));
                g.drawString("Score: " + numLinesRemoved, boardLeft + WIDTH * squareWidth() / 2 - 59, boardTop + HEIGHT * squareHeight() / 2 + 21);
                g.setColor(Color.RED);
                g.setFont(new Font("맑은 고딕", Font.BOLD, 36));
                g.drawString("Game Over", boardLeft + WIDTH * squareWidth() / 2 - 100, boardTop + HEIGHT * squareHeight() / 2 - 20);
                g.setFont(new Font("맑은 고딕", Font.BOLD, 24));
                g.drawString("Score: " + numLinesRemoved, boardLeft + WIDTH * squareWidth() / 2 - 60, boardTop + HEIGHT * squareHeight() / 2 + 20);
            }
        }

        // 다음 블록 그리기
        int nextPieceX = (WIDTH + 2) * squareWidth();
        if (isFullScreen)
            nextPieceX += 500;
        int nextPieceY1 = 50;
        int nextPieceY2 = 200;
        if (isFullScreen)
            nextPieceY2 = 300;
        for (int i = 0; i < 4; i++) {
            int x1 = nextPieceX + (nextPiece1.x(i) + 1) * squareWidth(); // nextPiece1.x(i) + 1 중앙 정렬
            int y1 = nextPieceY1 + (nextPiece1.y(i) + 1) * squareHeight(); // nextPiece1.y(i) + 1 중앙 정렬
            drawSquare(g, x1, y1, nextPiece1.getShape());

            int x2 = nextPieceX + (nextPiece2.x(i) + 1) * squareWidth(); // nextPiece2.x(i) + 1 중앙 정렬
            int y2 = nextPieceY2 + (nextPiece2.y(i) + 1) * squareHeight(); // nextPiece2.y(i) + 1 중앙 정렬
            drawSquare(g, x2, y2, nextPiece2.getShape());
        }
    }

    private void drawSquare(Graphics g, int x, int y, Shape.Tetrominoes shape) {
        Color colors[] = {
                new Color(0, 0, 0), new Color(204, 102, 102),
                new Color(102, 204, 102), new Color(102, 102, 204),
                new Color(204, 204, 102), new Color(204, 102, 204),
                new Color(102, 204, 204), new Color(218, 170, 0),
                new Color(128, 128, 128) // 회색 블록 색상 추가
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
            if (keycode == KeyEvent.VK_ESCAPE) { // ESC 키가 눌리면 창 종료
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
            frame.setSize(500, 600); // 원래 크기로 설정
            frame.setLocationRelativeTo(null);
        }
        frame.setVisible(true);

        // 전체 화면 모드에 따라 라벨 위치 조정
        Font fullScreenFont = new Font("맑은 고딕", Font.BOLD, 18);
        Font normalFont = new Font("맑은 고딕", Font.BOLD, 12);

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
        timer.stop(); // 기존 타이머를 정지
        NORMAL_DELAY = 500;
        timer = new Timer(NORMAL_DELAY, this); // 새로운 타이머 생성
        timer.start();
        gameTimeTimer.stop(); // 기존 게임 시간 타이머를 정지
        timeLabel.setText("Time: 00:00");
        startTime = System.currentTimeMillis(); // 게임 시작 시간 초기화
        lastSpeedIncreaseTime = startTime; // 마지막 속도 증가 시간 초기화
        lastGrayLineTime = startTime; // 마지막 회색 라인 추가 시간 초기화
        totalPausedDuration = 0; // 총 일시 정지 시간 초기화
        gameTimeTimer = new Timer(1000, e -> {
            updateGameTime();
            increaseSpeed();
            addGrayLine();
        }); // 새로운 게임 시간 타이머 생성
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
