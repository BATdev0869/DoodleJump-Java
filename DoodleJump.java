import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

public class DoodleJump extends JPanel implements ActionListener, KeyListener {
    // Window
    static final int WIDTH = 400, HEIGHT = 600;

    // Player & Platform sizes
    final int PLATFORM_WIDTH = 60, PLATFORM_HEIGHT = 10;
    final int PLAYER_WIDTH = 30, PLAYER_HEIGHT = 30;

    // Game variables
    int playerX, playerY;
    double velocityY = 0;
    boolean left = false, right = false;
    boolean gameOver = false;

    int score = 0, maxHeight = 0;

    ArrayList<Platform> platforms = new ArrayList<>();
    Random rand = new Random();

    Rectangle monster = new Rectangle(100, 300, 40, 40);
    boolean monsterActive = true;

    javax.swing.Timer timer;

    // States
    enum State { MENU, GAME, LEADERBOARD }
    State currentState = State.MENU;

    // Leaderboard
    final String SCORE_FILE = "highscores.dat";
    ArrayList<Integer> highscores = new ArrayList<>();

    public DoodleJump() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);

        loadScores();

        timer = new javax.swing.Timer(16, this);
        timer.start();
    }

    void resetGame() {
        platforms.clear();
        score = 0;
        maxHeight = 0;
        velocityY = 0;
        gameOver = false;

        for (int i = 0; i < 8; i++) {
            int x = rand.nextInt(WIDTH - PLATFORM_WIDTH);
            int y = HEIGHT - i * 80;
            boolean moving = rand.nextBoolean();
            Platform plat = new Platform(x, y, PLATFORM_WIDTH, PLATFORM_HEIGHT, moving);
            platforms.add(plat);

            if (i == 1) {
                playerX = x + PLATFORM_WIDTH / 2 - PLAYER_WIDTH / 2;
                playerY = y - PLAYER_HEIGHT;
            }
        }

        monster.setLocation(rand.nextInt(WIDTH - 40), HEIGHT / 2);
    }

    void loadScores() {
        highscores.clear();
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(SCORE_FILE))) {
            highscores = (ArrayList<Integer>) ois.readObject();
        } catch (Exception e) {
            // File might not exist yet, no worries
        }
    }

    void saveScores() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(SCORE_FILE))) {
            oos.writeObject(highscores);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void addScore(int sc) {
        highscores.add(sc);
        highscores.sort(Collections.reverseOrder());
        if (highscores.size() > 5) highscores.remove(highscores.size() - 1);
        saveScores();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (currentState == State.GAME) {
            if (!gameOver) {
                // Movement
                if (left) playerX -= 5;
                if (right) playerX += 5;

                if (playerX < -PLAYER_WIDTH) playerX = WIDTH;
                if (playerX > WIDTH) playerX = -PLAYER_WIDTH;

                velocityY += 0.4;
                playerY += velocityY;

                // Collision
                for (Platform plat : platforms) {
                    plat.update();

                    Rectangle futurePlayer = new Rectangle(playerX, playerY + (int) velocityY, PLAYER_WIDTH, PLAYER_HEIGHT);

                    if (futurePlayer.intersects(plat) && velocityY > 0 &&
                            playerY + PLAYER_HEIGHT <= plat.y + velocityY) {
                        playerY = plat.y - PLAYER_HEIGHT;
                        velocityY = -10;
                    }
                }

                // Scroll platforms
                if (playerY < HEIGHT / 2) {
                    int dy = HEIGHT / 2 - playerY;
                    playerY = HEIGHT / 2;
                    score += dy;
                    maxHeight += dy;

                    for (int i = 0; i < platforms.size(); i++) {
                        Platform plat = platforms.get(i);
                        plat.y += dy;

                        if (plat.y > HEIGHT) {
                            platforms.remove(i);
                            platforms.add(new Platform(
                                    rand.nextInt(WIDTH - PLATFORM_WIDTH),
                                    0,
                                    PLATFORM_WIDTH,
                                    PLATFORM_HEIGHT,
                                    rand.nextBoolean()));
                        }
                    }

                    if (monsterActive) {
                        monster.y += dy;
                        if (monster.y > HEIGHT) {
                            monster.setLocation(rand.nextInt(WIDTH - 40), 0);
                        }
                    }
                }

                if (playerY > HEIGHT) gameOver = true;

                if (monsterActive && playerX + PLAYER_WIDTH > monster.x &&
                        playerX < monster.x + monster.width &&
                        playerY + PLAYER_HEIGHT > monster.y &&
                        playerY < monster.y + monster.height) {
                    gameOver = true;
                }
            } else {
                // Save highscore once
                if (!highscores.contains(score) && score > 0) {
                    addScore(score / 10);
                }
            }
        }
        repaint();
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        switch (currentState) {
            case MENU:
                drawMenu(g);
                break;
            case GAME:
                drawGame(g);
                break;
            case LEADERBOARD:
                drawLeaderboard(g);
                break;
        }
    }

    void drawMenu(Graphics g) {
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 48));
        drawCenteredString(g, "DOODLE JUMP", WIDTH, HEIGHT / 3);

         g.setColor(Color.ORANGE);
        g.setFont(new Font("Arial", Font.BOLD, 24));
        drawCenteredString(g, "Java Port!", WIDTH, HEIGHT / 2 - 80);

g.setColor(Color.WHITE);        
g.setFont(new Font("Arial", Font.PLAIN, 20));
        drawCenteredString(g, "Press ENTER to Start", WIDTH, HEIGHT / 2);
        drawCenteredString(g, "Press L to Leaderboard", WIDTH, HEIGHT / 2 + 30);
        drawCenteredString(g, "Press Q to Quit", WIDTH, HEIGHT / 2 + 60);
    }

    void drawGame(Graphics g) {
        // Player
        g.setColor(Color.GREEN);
        g.fillOval(playerX, playerY, PLAYER_WIDTH, PLAYER_HEIGHT);

        // Platforms
        for (Platform plat : platforms) {
            g.setColor(plat.moving ? Color.CYAN : Color.WHITE);
            g.fillRect(plat.x, plat.y, plat.width, plat.height);
        }

        // Monster
        if (monsterActive) {
            g.setColor(Color.RED);
            g.fillRect(monster.x, monster.y, monster.width, monster.height);
        }

        // Score
        g.setColor(Color.YELLOW);
        g.setFont(new Font("Monospaced", Font.BOLD, 18));
        g.drawString("Score: " + (maxHeight / 10), 10, 20);

        if (gameOver) {
            g.setColor(Color.RED);
            g.setFont(new Font("Arial", Font.BOLD, 36));
            g.drawString("Game Over", WIDTH / 2 - 100, HEIGHT / 2);
            g.setFont(new Font("Arial", Font.PLAIN, 16));
            g.drawString("Press R to Restart or M for Menu", WIDTH / 2 - 110, HEIGHT / 2 + 40);
        }
    }

    void drawLeaderboard(Graphics g) {
        g.setColor(Color.ORANGE);
        g.setFont(new Font("Arial", Font.BOLD, 36));
        drawCenteredString(g, "Leaderboard", WIDTH, 80);

        g.setFont(new Font("Monospaced", Font.PLAIN, 24));
        for (int i = 0; i < highscores.size(); i++) {
            String text = (i + 1) + ". " + highscores.get(i);
            g.drawString(text, WIDTH / 2 - 30, 130 + i * 30);
        }

        g.setFont(new Font("Arial", Font.PLAIN, 16));
        drawCenteredString(g, "Press M to return to Menu", WIDTH, HEIGHT - 50);
    }

    void drawCenteredString(Graphics g, String text, int width, int y) {
        FontMetrics fm = g.getFontMetrics();
        int x = (width - fm.stringWidth(text)) / 2;
        g.drawString(text, x, y);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        switch (currentState) {
            case MENU -> {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    resetGame();
                    currentState = State.GAME;
                }
                if (e.getKeyCode() == KeyEvent.VK_L) {
                    currentState = State.LEADERBOARD;
                }
                if (e.getKeyCode() == KeyEvent.VK_Q) {
                    System.exit(0);
                }
            }
            case GAME -> {
                if (e.getKeyCode() == KeyEvent.VK_LEFT) left = true;
                if (e.getKeyCode() == KeyEvent.VK_RIGHT) right = true;
                if (e.getKeyCode() == KeyEvent.VK_R && gameOver) {
                    resetGame();
                }
                if (e.getKeyCode() == KeyEvent.VK_M) {
                    currentState = State.MENU;
                }
            }
            case LEADERBOARD -> {
                if (e.getKeyCode() == KeyEvent.VK_M) {
                    currentState = State.MENU;
                }
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (currentState == State.GAME) {
            if (e.getKeyCode() == KeyEvent.VK_LEFT) left = false;
            if (e.getKeyCode() == KeyEvent.VK_RIGHT) right = false;
        }
    }

    @Override public void keyTyped(KeyEvent e) {}

    public static void main(String[] args) {
        JFrame frame = new JFrame("Doodle Jump Java Port");
        DoodleJump game = new DoodleJump();
        frame.add(game);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    static class Platform extends Rectangle {
        boolean moving;
        int dx = 2;

        public Platform(int x, int y, int w, int h, boolean moving) {
            super(x, y, w, h);
            this.moving = moving;
        }

        public void update() {
            if (moving) {
                x += dx;
                if (x <= 0 || x >= WIDTH - width) dx *= -1;
            }
        }
    }
}
