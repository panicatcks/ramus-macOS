package qr;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

public final class QrPng {
    public static void main(String[] args) throws Exception {
        String text = args.length>0 ? args[0] : "https://t.me/denypanic";
        String out = args.length>1 ? args[1] : "qr.png";
        int size = args.length>2 ? Integer.parseInt(args[2]) : 360;
        QrCode qr = QrCode.encodeText(text, QrCode.Ecc.MEDIUM);
        BufferedImage img = render(qr, size, 4, Color.BLACK, Color.WHITE);
        File f = new File(out); f.getParentFile().mkdirs();
        ImageIO.write(img, "png", f);
    }

    private static BufferedImage render(QrCode qr, int size, int border, Color fg, Color bg){
        int n = qr.size;
        int scale = Math.max(1, (size - border*2) / n);
        int imgSize = Math.max(size, n*scale + border*2);
        BufferedImage img = new BufferedImage(imgSize, imgSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setColor(bg); g.fillRect(0,0,imgSize,imgSize);
        g.setColor(fg);
        for (int y=0;y<n;y++) for(int x=0;x<n;x++){
            if (qr.modules[y][x]) g.fillRect(border + x*scale, border + y*scale, scale, scale);
        }
        g.dispose();
        return img;
    }
}

