import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import javax.imageio.ImageIO;

/** Original geometric assets. Run from the repository root with Java 17+. */
public class GenerateArtwork {
    public static void main(String[] args) throws Exception {
        Path out = Path.of("launcher/app/src/main/res/drawable-nodpi");
        Files.createDirectories(out);
        BufferedImage wallpaper = new BufferedImage(1920, 1080, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = wallpaper.createGraphics();
        g.setPaint(new GradientPaint(0, 0, new Color(8, 17, 43), 1920, 1080, new Color(65, 24, 77)));
        g.fillRect(0, 0, 1920, 1080);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        for (int i = 0; i < 7; i++) {
            g.setColor(new Color(70 + i * 9, 100, 180, 16));
            g.fillOval(1000 - i * 75, 60 + i * 30, 600 + i * 150, 600 + i * 150);
        }
        g.setColor(new Color(8, 14, 32, 180));
        Path2D hills = new Path2D.Double();
        hills.moveTo(0, 870); hills.curveTo(450, 620, 760, 1060, 1150, 750);
        hills.curveTo(1470, 500, 1650, 770, 1920, 620);
        hills.lineTo(1920, 1080); hills.lineTo(0, 1080); hills.closePath(); g.fill(hills);
        g.dispose(); ImageIO.write(wallpaper, "png", out.resolve("cyberpunk.png").toFile());
        BufferedImage pad = new BufferedImage(800, 480, BufferedImage.TYPE_INT_ARGB);
        g = pad.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Path2D body = new Path2D.Double();
        body.moveTo(220, 100); body.curveTo(270, 65, 530, 65, 580, 100);
        body.curveTo(660, 155, 750, 375, 660, 405);
        body.curveTo(615, 420, 555, 300, 520, 290); body.lineTo(280, 290);
        body.curveTo(245, 300, 185, 420, 140, 405); body.curveTo(50, 375, 140, 155, 220, 100);
        body.closePath();
        g.setPaint(new GradientPaint(0, 80, new Color(79, 103, 139), 0, 400, new Color(17, 25, 45)));
        g.fill(body); g.setStroke(new BasicStroke(4)); g.setColor(new Color(119, 177, 220)); g.draw(body);
        g.setColor(new Color(12, 19, 31)); g.fillRoundRect(205, 142, 34, 104, 8, 8); g.fillRoundRect(170, 177, 104, 34, 8, 8);
        for(int[] xy : new int[][]{{565,145},{610,190},{565,235},{520,190}}){
            g.setColor(new Color(136, 196, 220));g.fillOval(xy[0]-14,xy[1]-14,28,28);
        }
        for(int x : new int[]{310,455}){
            g.setColor(new Color(8,15,28)); g.fillOval(x-33,217,66,66);
            g.setColor(new Color(63,86,117)); g.fillOval(x-23,227,46,46);
        }
        g.dispose(); ImageIO.write(pad, "png", out.resolve("gamepad.png").toFile());
    }
}
