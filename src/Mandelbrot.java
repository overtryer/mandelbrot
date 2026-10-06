import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.imageio.ImageIO;

public class Mandelbrot {

    static final int WIDTH  = 1600;
    static final int HEIGHT = 1200;
    static final int MAX_ITER = 1000;

    static final double X_MIN = -2.5, X_MAX = 1.0;
    static final double Y_MIN = -1.25, Y_MAX = 1.25;

    static int mandelbrot(double cr, double ci) {
        double zr = 0, zi = 0;
        int iter = 0;
        while (iter < MAX_ITER && zr * zr + zi * zi <= 4.0) {
            double tmp = zr * zr - zi * zi + cr;
            zi = 2.0 * zr * zi + ci;
            zr = tmp;
            iter++;
        }
        return iter;
    }

    record Tile(int y0, int y1) {}

    static Callable<Void> renderTile(BufferedImage img, Tile tile) {
        return () -> {
            for (int py = tile.y0(); py < tile.y1(); py++) {
                double ci = Y_MAX - py * (Y_MAX - Y_MIN) / (HEIGHT - 1);
                for (int px = 0; px < WIDTH; px++) {
                    double cr = X_MIN + px * (X_MAX - X_MIN) / (WIDTH - 1);
                    int iter = mandelbrot(cr, ci);
                    img.setRGB(px, py, colorFor(iter));
                }
            }
            return null;
        };
    }

    static int colorFor(int iter) {
        if (iter >= MAX_ITER) return 0x000000; // внутри множества
        int t = iter * 255 / MAX_ITER;
        return (t << 16) | (t << 8) | t; // серый
    }

    public static void main(String[] args) throws Exception {
        BufferedImage img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);

        int threads = Runtime.getRuntime().availableProcessors();
        ExecutorService pool = Executors.newFixedThreadPool(threads);

        int tileHeight = 16;
        List<Callable<Void>> tasks = new ArrayList<>();
        for (int y = 0; y < HEIGHT; y += tileHeight) {
            int y1 = Math.min(y + tileHeight, HEIGHT);
            tasks.add(renderTile(img, new Tile(y, y1)));
        }

        long start = System.nanoTime();
        List<Future<Void>> futures = pool.invokeAll(tasks);
        for (Future<Void> f : futures) f.get();
        long elapsed = System.nanoTime() - start;

        pool.shutdown();

        ImageIO.write(img, "png", new File("mandelbrot.png"));
        System.out.printf("Готово за %.2f мс, потоков: %d%n",
                elapsed / 1e6, threads);
    }
}
