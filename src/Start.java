import javax.swing.*;
import java.awt.*;

public class Start extends JPanel {
    private final Color lineColor = new Color(60, 60, 70);
    // client side drawn animation (don't change)
    private int[] x1, y1, x2, y2;

    public static void main(String[] args) {
        // Client starts normally without any native library checks
    }

    public void updateLines(int[] x1, int[] y1, int[] x2, int[] y2) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (x1 == null) {
            return;
        }

        Graphics2D g2d = (Graphics2D) g.create();

        try {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setColor(lineColor);

            for (int i = 0; i < x1.length; i++) {
                g2d.drawLine(x1[i], y1[i], x2[i], y2[i]);
            }
        } finally {
            g2d.dispose();
        }
    }
}
