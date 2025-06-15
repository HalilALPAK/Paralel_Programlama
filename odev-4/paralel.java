import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class paralel extends JPanel {

    private final List<Point> polygon = new ArrayList<>();
    private final List<Point> testPoints = new ArrayList<>();
    private final Map<Point, Boolean> parallelResults = new ConcurrentHashMap<>();
    private final Map<Point, Boolean> serialResults = new HashMap<>();

    private boolean drawingPolygon = true;

    private final JLabel timeLabel = new JLabel(" ");

    public paralel() {
        setPreferredSize(new Dimension(600, 600));
        setBackground(Color.BLACK);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (drawingPolygon) {
                    polygon.add(e.getPoint());
                } else {
                    testPoints.add(e.getPoint());
                }
                repaint();
            }
        });
    }

    public JLabel getTimeLabel() {
        return timeLabel;
    }

    public void finishPolygon() {
        if (polygon.size() < 3) {
            JOptionPane.showMessageDialog(this, "Çokgen en az 3 nokta olmalı!");
            return;
        }
        drawingPolygon = false;
        repaint();
    }

    public void resetPolygon() {
        polygon.clear();
        testPoints.clear();
        parallelResults.clear();
        serialResults.clear();
        drawingPolygon = true;
        timeLabel.setText(" ");
        repaint();
    }

    public void clearTestPoints() {
        testPoints.clear();
        parallelResults.clear();
        serialResults.clear();
        timeLabel.setText(" ");
        repaint();
    }

    // Bir noktanın poligonun içinde olup olmadığını kontrol eden Ray Döküm Algoritması
    private boolean isInsidePolygon(Point p, List<Point> poly) {
        int n = poly.size();
        boolean result = false;
        for (int i = 0, j = n - 1; i < n; j = i++) {
            Point pi = poly.get(i);
            Point pj = poly.get(j);

            if (pi.y == pj.y) {
                continue;
            }

            if (((pi.y <= p.y) && (pj.y > p.y)) || ((pj.y <= p.y) && (pi.y > p.y))) {
                double intersectX = (double)(pj.x - pi.x) * (p.y - pi.y) / (pj.y - pi.y) + pi.x;
                if (p.x < intersectX) {
                    result = !result;
                }
            }
        }
        return result;
    }

    public void checkPointsSerial() {
        if (drawingPolygon) {
            JOptionPane.showMessageDialog(this, "Önce çokgeni bitiriniz!");
            return;
        }
        serialResults.clear();

        long start = System.nanoTime();

        for (Point p : testPoints) {
            boolean inside = isInsidePolygon(p, polygon);
            serialResults.put(p, inside);
        }

        long end = System.nanoTime();
        long durationUs = (end - start) / 1000;

        String msg = "Seri süre: " + durationUs + " µs";
        System.out.println(msg);
        timeLabel.setText(msg);

        repaint();
    }

    // --- Yeni RecursiveTask Sınıfı ---
    private class PolygonCheckTask extends RecursiveTask<Map<Point, Boolean>> {
        private final List<Point> pointsToCheck;
        private final List<Point> currentPolygon;
        private final int threshold = 1000; // Bir görevin parçalanmadan önce işleyeceği minimum nokta sayısı

        public PolygonCheckTask(List<Point> points, List<Point> polygon) {
            this.pointsToCheck = points;
            this.currentPolygon = polygon;
        }

        @Override
        protected Map<Point, Boolean> compute() {
            if (pointsToCheck.size() <= threshold) {
                // Görev küçükse doğrudan hesapla
                Map<Point, Boolean> localResults = new HashMap<>();
                for (Point p : pointsToCheck) {
                    localResults.put(p, isInsidePolygon(p, currentPolygon));
                }
                return localResults;
            } else {
                // Görev büyükse ikiye böl ve paralel olarak çalıştır
                int mid = pointsToCheck.size() / 2;
                PolygonCheckTask leftTask = new PolygonCheckTask(pointsToCheck.subList(0, mid), currentPolygon);
                PolygonCheckTask rightTask = new PolygonCheckTask(pointsToCheck.subList(mid, pointsToCheck.size()), currentPolygon);

                // Sol görevi asenkron olarak çalıştır, sağ görevi bu thread'de çalıştır (work stealing)
                leftTask.fork(); // Sol görevi havuza gönder
                Map<Point, Boolean> rightResult = rightTask.compute(); // Sağ görevi bu thread'de hesapla
                Map<Point, Boolean> leftResult = leftTask.join(); // Sol görevin bitmesini bekle ve sonucunu al

                // Sonuçları birleştir
                leftResult.putAll(rightResult);
                return leftResult;
            }
        }
    }
    // --- Yeni RecursiveTask Sınıfı Sonu ---

    public void checkPointsParallel() {
        if (drawingPolygon) {
            JOptionPane.showMessageDialog(this, "Önce çokgeni bitiriniz!");
            return;
        }

        // --- OPTİMİZASYON 1: Küçük nokta sayıları için seri metoda yönlendirme ---
        // Bu eşik değerini sisteminize göre ayarlayın.
        if (testPoints.size() < 30000) { // Daha yüksek bir eşik belirledik (deneme yanılma ile ayarla)
            checkPointsSerial();
            String serialTime = timeLabel.getText().split(": ")[1];
            timeLabel.setText("Paralel çağrıldı (az nokta): " + serialTime);
            System.out.println("Paralel çağrıldı ancak az sayıda nokta için seri işlem kullanıldı.");
            return;
        }
        // --- OPTİMİZASYON 1 SONU ---

        parallelResults.clear();

        long start = System.nanoTime();

        // --- OPTİMİZASYON 2: ForkJoinPool ve RecursiveTask kullanarak manuel paralel işleme ---
        // Kaç çekirdeğiniz varsa o kadar thread kullanmak genelde en iyisidir.
        int parallelism = Runtime.getRuntime().availableProcessors();
        ForkJoinPool customThreadPool = new ForkJoinPool(parallelism);

        try {
            PolygonCheckTask mainTask = new PolygonCheckTask(testPoints, polygon);
            Map<Point, Boolean> results = customThreadPool.invoke(mainTask);
            parallelResults.putAll(results);
        } finally {
            customThreadPool.shutdown(); // Havuzu kullanımdan sonra kapat
            // İsteğe bağlı: Havuzun tamamen kapanmasını beklemek
            // try {
            //     customThreadPool.awaitTermination(1, TimeUnit.MINUTES);
            // } catch (InterruptedException e) {
            //     Thread.currentThread().interrupt();
            //     e.printStackTrace();
            // }
        }
        // --- OPTİMİZASYON 2 SONU ---

        long end = System.nanoTime();
        long durationUs = (end - start) / 1000;

        String msg = "Paralel süre: " + durationUs + " µs";
        System.out.println(msg);
        timeLabel.setText(msg);

        repaint();
    }

    public void addRandomPoints(int count) {
        if (drawingPolygon) {
            JOptionPane.showMessageDialog(this, "Önce çokgeni bitiriniz!");
            return;
        }
        Random rand = new Random();
        int w = getWidth();
        int h = getHeight();

        for (int i = 0; i < count; i++) {
            testPoints.add(new Point(rand.nextInt(w), rand.nextInt(h)));
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;

        if (polygon.size() > 1) {
            Path2D path = new Path2D.Double();
            path.moveTo(polygon.get(0).x, polygon.get(0).y);
            for (int i = 1; i < polygon.size(); i++) {
                path.lineTo(polygon.get(i).x, polygon.get(i).y);
            }
            if (!drawingPolygon) path.closePath();

            g2.setColor(new Color(255, 255, 255, 120));
            g2.fill(path);

            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(2));
            g2.draw(path);
        }

        for (Point p : testPoints) {
            Boolean inside = parallelResults.get(p);
            if (inside == null) inside = serialResults.get(p);

            if (inside == null) {
                g2.setColor(Color.YELLOW);
            } else {
                g2.setColor(inside ? Color.GREEN : Color.RED);
            }
            g2.fillOval(p.x - 4, p.y - 4, 8, 8);
        }

        g2.setColor(Color.WHITE);
        String infoText = drawingPolygon
            ? "Çokgen çizimi: mouse ile tıkla. 'Çokgeni Bitir' butonuna bas."
            : "Test noktaları: mouse ile tıkla veya 'Rasgele Nokta Yerleştir' kullan.";
        g2.drawString(infoText, 10, 40);
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Poligon Nokta Kontrol (Seri ve Paralel)");
        paralel panel = new paralel();

        JButton finishPolygonBtn = new JButton("Çokgeni Bitir");
        JButton resetPolygonBtn = new JButton("Çokgeni Kaldır");
        JButton clearTestPointsBtn = new JButton("Test Noktalarını Temizle");
        JButton serialCheckBtn = new JButton("Seri Kontrol");
        JButton parallelCheckBtn = new JButton("Paralel Kontrol");
        JButton randomPointsBtn = new JButton("Rasgele Nokta Yerleştir");

        finishPolygonBtn.addActionListener(e -> panel.finishPolygon());
        resetPolygonBtn.addActionListener(e -> panel.resetPolygon());
        clearTestPointsBtn.addActionListener(e -> panel.clearTestPoints());
        serialCheckBtn.addActionListener(e -> panel.checkPointsSerial());
        parallelCheckBtn.addActionListener(e -> panel.checkPointsParallel());
        randomPointsBtn.addActionListener(e -> {
            String input = JOptionPane.showInputDialog(frame, "Kaç tane rasgele nokta eklemek istiyorsunuz?");
            if (input == null) return;
            try {
                int count = Integer.parseInt(input);
                if (count < 0) throw new NumberFormatException();
                panel.addRandomPoints(count);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(frame, "Geçerli bir sayı giriniz!");
            }
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        buttonPanel.add(finishPolygonBtn);
        buttonPanel.add(resetPolygonBtn);
        buttonPanel.add(clearTestPointsBtn);
        buttonPanel.add(serialCheckBtn);
        buttonPanel.add(parallelCheckBtn);
        buttonPanel.add(randomPointsBtn);

        frame.setLayout(new BorderLayout());
        frame.add(panel, BorderLayout.CENTER);
        frame.add(buttonPanel, BorderLayout.SOUTH);
        frame.add(panel.getTimeLabel(), BorderLayout.NORTH);

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}