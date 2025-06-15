import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;

// Point sınıfı
class Point {
    double x;
    double y;

    public Point(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public java.awt.Point toAWTPoint() {
        return new java.awt.Point((int) x, (int) y);
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }
}

// GeometryUtils sınıfı: Çapraz çarpım ve dışbükeylik kontrolü algoritmaları
class GeometryUtils {
    public static double crossProduct(Point p1, Point p2, Point p3) {
        return (p2.x - p1.x) * (p3.y - p1.y) - (p2.y - p1.y) * (p3.x - p1.x);
    }

    public static boolean isPolygonConvex(List<Point> points) {
        int n = points.size();
        if (n < 3) {
            return true;
        }

        double initialSign = 0;
        boolean foundNonCollinear = false;
        for (int i = 0; i < n; i++) {
            Point p1 = points.get(i);
            Point p2 = points.get((i + 1) % n);
            Point p3 = points.get((i + 2) % n);

            double cp = crossProduct(p1, p2, p3);

            if (cp != 0) {
                initialSign = Math.signum(cp);
                foundNonCollinear = true;
                break;
            }
        }

        if (!foundNonCollinear) {
            return true;
        }

        for (int i = 0; i < n; i++) {
            Point p1 = points.get(i);
            Point p2 = points.get((i + 1) % n);
            Point p3 = points.get((i + 2) % n);

            double cp = crossProduct(p1, p2, p3);

            if (cp != 0 && Math.signum(cp) != initialSign) {
                return false;
            }
        }
        return true;
    }
}

// ConvexityCheckTask sınıfı: Paralel dışbükeylik kontrolü için ForkJoinTask
class ConvexityCheckTask extends RecursiveTask<Boolean> {
    private final List<Point> allPoints;
    private final int startIndex;
    private final int endIndex;
    private final double initialOverallSign;

    // Dinamik veya sabit olarak ayarlanacak bu eşik, küçük iş yükleri için önemli.
    // Çok küçük bir THRESHOLD, aşırı görev bölünmesine ve overhead'e yol açar.
    // Burayı 50'den 500'e çıkardık, bu da her alt görevin en az 500 nokta kontrol etmesini sağlar.
    private static final int THRESHOLD = 5000; 

    public ConvexityCheckTask(List<Point> points, int startIndex, int endIndex, double initialOverallSign) {
        this.allPoints = points;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.initialOverallSign = initialOverallSign;
    }

    @Override
    protected Boolean compute() {
        // Eğer aralık küçükse doğrudan hesapla (paralel overhead'den kaçın)
        if (endIndex - startIndex <= THRESHOLD) {
            return checkSubsetConvexity();
        } else {
            // Aralığı ikiye böl ve alt görevleri oluştur
            int mid = startIndex + (endIndex - startIndex) / 2;
            ConvexityCheckTask leftTask = new ConvexityCheckTask(allPoints, startIndex, mid, initialOverallSign);
            ConvexityCheckTask rightTask = new ConvexityCheckTask(allPoints, mid, endIndex, initialOverallSign);

            // Sol görevi asenkron olarak çalıştır, sağ görevi bu thread'de çalıştır (work stealing)
            leftTask.fork();
            boolean rightResult = rightTask.compute();
            boolean leftResult = leftTask.join();

            return leftResult && rightResult;
        }
    }

    private boolean checkSubsetConvexity() {
        int n = allPoints.size();
        for (int i = startIndex; i < endIndex; i++) {
            Point p1 = allPoints.get(i % n);
            Point p2 = allPoints.get((i + 1) % n);
            Point p3 = allPoints.get((i + 2) % n);

            double cp = GeometryUtils.crossProduct(p1, p2, p3);

            // Eğer çapraz çarpım sıfır değilse ve işaret genel işaretle farklıysa, dışbükey değildir
            if (cp != 0 && Math.signum(cp) != initialOverallSign) {
                return false;
            }
        }
        return true;
    }
}

// Ana Swing Uygulama Paneli
public class ConvexityCheckerSwingApp extends JPanel {

    private final List<Point> currentPoints = new ArrayList<>();
    private boolean drawingMode = true;
    private Boolean isCurrentPolygonConvex = null;

    private final JLabel resultLabel = new JLabel("Sonuç: Çokgen çizilmedi.");
    private final JLabel serialTimeLabel = new JLabel("Seri Süre: ");
    private final JLabel parallelTimeLabel = new JLabel("Paralel Süre: ");

    // Rastgele çokgenler
    private final List<List<Point>> predefinedPolygons = Arrays.asList(
            // Dışbükeyler
            Arrays.asList(new Point(100, 100), new Point(300, 100), new Point(350, 200), new Point(200, 300), new Point(50, 200)), // Beşgen
            Arrays.asList(new Point(100, 100), new Point(400, 100), new Point(400, 400), new Point(100, 400)), // Kare
            Arrays.asList(new Point(200, 50), new Point(450, 150), new Point(400, 400), new Point(150, 450), new Point(50, 200)), // Düzensiz Dışbükey
            Arrays.asList(new Point(250, 50), new Point(350, 150), new Point(300, 250), new Point(200, 250), new Point(150, 150)), // Dışbükey Beşgen
            Arrays.asList(new Point(50, 300), new Point(150, 100), new Point(300, 100), new Point(400, 300), new Point(250, 450), new Point(100, 450)), // Dışbükey Altıgen

            // İçbükeyler
            Arrays.asList(new Point(150, 50), new Point(250, 50), new Point(300, 150), new Point(400, 150), new Point(300, 250), new Point(200, 250), new Point(100, 150), new Point(0, 150)), // Yıldız benzeri
            Arrays.asList(new Point(100, 100), new Point(400, 100), new Point(400, 300), new Point(300, 300), new Point(300, 200), new Point(200, 200), new Point(200, 300), new Point(100, 300)), // U şekli
            Arrays.asList(new Point(100, 100), new Point(400, 100), new Point(350, 200), new Point(400, 300), new Point(100, 300), new Point(150, 200)), // İçbükey altıgen
            Arrays.asList(new Point(100, 100), new Point(200, 50), new Point(300, 100), new Point(250, 200), new Point(300, 300), new Point(200, 350), new Point(100, 300), new Point(150, 200)), // Karmaşık içbükey
            Arrays.asList(new Point(100, 100), new Point(400, 100), new Point(250, 200), new Point(400, 300), new Point(100, 300)) // Ok şekli
    );
    private int predefinedPolygonIndex = 0; // Hangi rastgele çokgeni ekleyeceğimizi takip etmek için

    public ConvexityCheckerSwingApp() {
        setPreferredSize(new Dimension(800, 600));
        setBackground(Color.DARK_GRAY);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (drawingMode) {
                    currentPoints.add(new Point(e.getX(), e.getY()));
                    repaint();
                }
            }
        });
    }

    public JLabel getResultLabel() { return resultLabel; }
    public JLabel getSerialTimeLabel() { return serialTimeLabel; }
    public JLabel getParallelTimeLabel() { return parallelTimeLabel; }

    public void finishDrawing() {
        if (currentPoints.size() < 3) {
            JOptionPane.showMessageDialog(this, "Çokgen en az 3 nokta olmalı!", "Hata", JOptionPane.ERROR_MESSAGE);
            return;
        }
        drawingMode = false;
        repaint();
        resultLabel.setText("Sonuç: Çizim tamamlandı. Kontrol edebilirsiniz.");
    }

    public void resetDrawing() {
        currentPoints.clear();
        drawingMode = true;
        isCurrentPolygonConvex = null;
        resultLabel.setText("Sonuç: Çokgen temizlendi.");
        clearTimeLabels();
        repaint();
    }

    public void checkSerial() {
        if (drawingMode) {
            JOptionPane.showMessageDialog(this, "Önce çokgeni bitiriniz!", "Uyarı", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (currentPoints.size() < 3) {
            resultLabel.setText("Sonuç: En az 3 nokta gerekli.");
            clearTimeLabels();
            return;
        }

        long startTime = System.nanoTime();
        isCurrentPolygonConvex = GeometryUtils.isPolygonConvex(currentPoints);
        long endTime = System.nanoTime();
        long durationMicroseconds = (endTime - startTime) / 1_000;

        resultLabel.setText("Sonuç (Seri): " + (isCurrentPolygonConvex ? "Dışbükey" : "İçbükey"));
        serialTimeLabel.setText("Seri Süre: " + durationMicroseconds + " µs");
        repaint();
    }

    // Paralel kontrol metodu
    public void checkParallel() {
        if (drawingMode) {
            JOptionPane.showMessageDialog(this, "Önce çokgeni bitiriniz!", "Uyarı", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (currentPoints.size() < 3) {
            resultLabel.setText("Sonuç: En az 3 nokta gerekli.");
            clearTimeLabels();
            return;
        }

        final int numPoints = currentPoints.size();
        // İyileştirme: Çok az sayıda nokta varsa, paralel başlatma yükü (overhead) seriyi geçebilir.
        // Bu eşik değerini sisteminize ve test senaryolarınıza göre ayarlayabilirsiniz.
        // Örneğin, 1000'den az nokta varsa doğrudan seri kontrole geç.
        final int MIN_POINTS_FOR_PARALLEL = 4; 

        if (numPoints < MIN_POINTS_FOR_PARALLEL) {
            System.out.println("Paralel kontrol çağrıldı ancak nokta sayısı düşük (" + numPoints + "). Seri kontrol kullanılıyor.");
            checkSerial(); // Doğrudan seri kontrolü çağır
            // Seri süresini yansıtmak için etiketi güncelle
            String serialTime = serialTimeLabel.getText().replace("Seri Süre: ", "");
            parallelTimeLabel.setText("Paralel Süre (az nokta): " + serialTime);
            return;
        }

        new SwingWorker<Boolean, Void>() {
            long startTime;
            long endTime;

            @Override
            protected Boolean doInBackground() throws Exception {
                // Mevcut işlemci çekirdeği sayısı kadar thread kullanmak genellikle en iyisidir.
                // Ancak, çok fazla I/O veya bloklama işlemi olmayan CPU-yoğun görevler için bu uygundur.
                ForkJoinPool pool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());

                double initialOverallSign = 0;
                boolean foundNonCollinear = false;
                int n = currentPoints.size();
                for (int i = 0; i < n; i++) {
                    Point p1 = currentPoints.get(i);
                    Point p2 = currentPoints.get((i + 1) % n);
                    Point p3 = currentPoints.get((i + 2) % n);
                    double cp = GeometryUtils.crossProduct(p1, p2, p3);
                    if (cp != 0) {
                        initialOverallSign = Math.signum(cp);
                        foundNonCollinear = true;
                        break;
                    }
                }

                if (!foundNonCollinear) {
                    return true;
                }

                startTime = System.nanoTime();
                ConvexityCheckTask mainTask = new ConvexityCheckTask(currentPoints, 0, n, initialOverallSign);
                boolean result = pool.invoke(mainTask);
                endTime = System.nanoTime();

                pool.shutdown();
                // Opsiyonel: Havuzun tamamen kapanmasını beklemek, ancak SwingWorker'da genellikle gerekli değil
                // try {
                //     pool.awaitTermination(1, TimeUnit.MINUTES);
                // } catch (InterruptedException e) {
                //     Thread.currentThread().interrupt();
                // }

                return result;
            }

            @Override
            protected void done() {
                try {
                    isCurrentPolygonConvex = get();
                    long durationMicroseconds = (endTime - startTime) / 1_000;
                    resultLabel.setText("Sonuç (Paralel): " + (isCurrentPolygonConvex ? "Dışbükey" : "İçbükey"));
                    parallelTimeLabel.setText("Paralel Süre: " + durationMicroseconds + " µs");
                    repaint();
                } catch (InterruptedException | ExecutionException e) {
                    resultLabel.setText("Hata: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }.execute();
    }

    public void addRandomPolygon() {
        resetDrawing();
        drawingMode = false;

        List<Point> polyToAdd = predefinedPolygons.get(predefinedPolygonIndex);
        currentPoints.addAll(polyToAdd);

        predefinedPolygonIndex = (predefinedPolygonIndex + 1) % predefinedPolygons.size();

        resultLabel.setText("Sonuç: Rastgele çokgen eklendi. Kontrol edebilirsiniz.");
        repaint();
    }

    private void clearTimeLabels() {
        serialTimeLabel.setText("Seri Süre: ");
        parallelTimeLabel.setText("Paralel Süre: ");
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        if (currentPoints.size() > 1) {
            Path2D path = new Path2D.Double();
            path.moveTo(currentPoints.get(0).x, currentPoints.get(0).y);
            for (int i = 1; i < currentPoints.size(); i++) {
                path.lineTo(currentPoints.get(i).x, currentPoints.get(i).y);
            }
            if (!drawingMode && currentPoints.size() >= 3) {
                path.closePath();
            }

            g2.setColor(new Color(100, 100, 100, 120));
            g2.fill(path);

            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(2));
            g2.draw(path);
        }

        g2.setColor(Color.BLUE);
        for (Point p : currentPoints) {
            g2.fillOval((int) p.x - 5, (int) p.y - 5, 10, 10);
        }

        g2.setColor(Color.WHITE);
        String infoText = drawingMode
                ? "Çokgen çizimi: Mouse ile tıkla. 'Çokgeni Bitir' butonuna bas."
                : "Çizim bitti. Kontrol butonlarını kullan.";
        g2.drawString(infoText, 10, 20);

        if (isCurrentPolygonConvex != null && currentPoints.size() >= 3) {
            g2.setColor(isCurrentPolygonConvex ? Color.GREEN : Color.RED);
            g2.setStroke(new BasicStroke(3));
            Path2D path = new Path2D.Double();
            path.moveTo(currentPoints.get(0).x, currentPoints.get(0).y);
            for (int i = 1; i < currentPoints.size(); i++) {
                path.lineTo(currentPoints.get(i).x, currentPoints.get(i).y);
            }
            path.closePath();
            g2.draw(path);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Dışbükey Çokgen Kontrolü (Swing)");
            ConvexityCheckerSwingApp panel = new ConvexityCheckerSwingApp();

            JButton finishDrawingBtn = new JButton("Çokgeni Bitir");
            JButton resetDrawingBtn = new JButton("Temizle");
            JButton serialCheckBtn = new JButton("Seri Kontrol");
            JButton parallelCheckBtn = new JButton("Paralel Kontrol");
            JButton randomPolygonBtn = new JButton("Rastgele Çokgen Ekle");

            finishDrawingBtn.addActionListener(e -> panel.finishDrawing());
            resetDrawingBtn.addActionListener(e -> panel.resetDrawing());
            serialCheckBtn.addActionListener(e -> panel.checkSerial());
            parallelCheckBtn.addActionListener(e -> panel.checkParallel());
            randomPolygonBtn.addActionListener(e -> panel.addRandomPolygon());

            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
            buttonPanel.add(finishDrawingBtn);
            buttonPanel.add(resetDrawingBtn);
            buttonPanel.add(serialCheckBtn);
            buttonPanel.add(parallelCheckBtn);
            buttonPanel.add(randomPolygonBtn);

            JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
            infoPanel.add(panel.getResultLabel());
            infoPanel.add(panel.getSerialTimeLabel());
            infoPanel.add(panel.getParallelTimeLabel());

            frame.setLayout(new BorderLayout());
            frame.add(panel, BorderLayout.CENTER);
            frame.add(buttonPanel, BorderLayout.SOUTH);
            frame.add(infoPanel, BorderLayout.NORTH);

            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}