package remote;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.net.InetAddress;

import javax.swing.ImageIcon;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Color;


public class FilterSer_Imp extends UnicastRemoteObject implements Filter{

    public FilterSer_Imp() throws RemoteException{
        super();
    }

    @Override
    public ImageIcon image(ImageIcon icon,int answer) throws RemoteException {
        long init= System.currentTimeMillis();
        BufferedImage buff_original;
        float[] data=new float[9];
        buff_original = imageIconToBufferedImage(icon);
        if(answer<4){
            switch (answer) {
                case 0 -> data=new float[]{ 1f/9f, 1f/9f, 1f/9f,1f/9f, 1f/9f, 1f/9f, 1f/9f, 1f/9f, 1f/9f };
                case 1 -> data=new float[]{ 0, -1, 0,-1, 4, -1, 0, -1, 0 };
                case 2 -> data=new float[]{ 0, -1, 0,1, 5, -1, 0, -1, 0 };
                case 3 -> data=new float[]{ -2, -1, 0,-1, 1, 1, 0, 1, 2 };
            }
            var kernel = new Kernel(3, 3,data);
            var ConOp = new ConvolveOp(kernel);
            buff_original = ConOp.filter(buff_original, null);
            buff_original = removeFrame(buff_original);
            return result(buff_original, init);
        }else{
            int color;
            int newRgb=0;
            if(answer==7){
                var buff_original_2 = new BufferedImage(buff_original.getWidth(), buff_original.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
                var g = buff_original_2.getGraphics();
                g.drawImage(buff_original, 0, 0, null);
                g.dispose();
                return result(buff_original_2, init);
            }
            var buff_original_2 = new BufferedImage(buff_original.getWidth(), buff_original.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics g = buff_original_2.getGraphics();
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, buff_original_2.getWidth(), buff_original_2.getHeight());
            g.dispose();

            for (int x = 0; x < buff_original.getWidth(); x++) {
                for (int y = 0; y < buff_original.getHeight(); y++) {
                    int rgb = buff_original.getRGB(x, y);
                    switch (answer) {
                        case 4 -> {
                            color = (rgb >> 16) & 0xFF;
                            newRgb = (color << 16);
                        }
                        case 5 -> {
                            color =  (rgb >> 8) & 0xFF;
                            newRgb = (color << 8);
                        }
                        case 6 -> {
                            color =  rgb & 0xFF;
                            newRgb = color;
                        }
                    }
                    buff_original_2.setRGB(x, y, newRgb);
                }
            }
            return result(buff_original_2, init);
        }
    }

    private ImageIcon result(BufferedImage result, long init){
        while(!netIsAvailable())
        try {
            Thread.sleep(25);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("time spent : "+(System.currentTimeMillis()-init));
        return new ImageIcon(result);
    }

    private boolean netIsAvailable() {
        try {
            InetAddress address = InetAddress.getByName("www.google.com");
            if (address.isReachable(3000))
            return true;
        } catch (Exception e) {
            System.out.println("Error checking internet connection: " + e.getMessage());
        }
        return false;
    }
    private static BufferedImage imageIconToBufferedImage(ImageIcon icon) {
        BufferedImage bufferedImage = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(),
                BufferedImage.TYPE_INT_ARGB);
        Graphics graphics = bufferedImage.createGraphics();
        icon.paintIcon(null, graphics, 0, 0);
        graphics.dispose();
        return bufferedImage;
    }
    private BufferedImage removeFrame(BufferedImage image){
        BufferedImage result = new BufferedImage(
          image.getWidth(), image.getHeight(), //work these out
          BufferedImage.TYPE_INT_RGB);
        Graphics2D g = result.createGraphics();
        g.drawImage(image, -1, -1, result.getWidth()+2, result.getHeight()+2, null);
        return result;
    }
}
