package client;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.spec.SecretKeySpec;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.EtchedBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;

import remote.Controller;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Toolkit;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.HashMap;
import java.util.Optional;
import java.util.Stack;
import java.util.Timer;
import java.util.TimerTask;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

public class Client extends JFrame{
    Registry r;
    Controller i;
    String path;
    int choice, priority=0;
    JFileChooser file_I;
    File selFile;
    JLabel label_Q, label_R;
    JPanel result;
    ImageIcon resultI;
    private Stack<HashMap<ImageIcon,HashMap<ImageIcon,String>>> Undo=new Stack<>();
    private Stack<HashMap<ImageIcon,HashMap<ImageIcon,String>>> Redo=new Stack<>();
    private static int count=0;
    private boolean received=true;
    public Client(String title){
        super(title);
        JMenuBar menubar=new JMenuBar();
        JMenu menu=new JMenu("File");
        JMenu edit=new JMenu("Edit");
        JMenuItem e0=new JMenuItem("Open");
        JMenuItem e1=new JMenuItem("Save");
        JMenuItem e2=new JMenuItem("Encryption");
        JMenuItem e3=new JMenuItem("Decryption");
        JMenuItem e4=new JMenuItem("Undo");
        JMenuItem e5=new JMenuItem("Redo");
        menu.add(e0);
        menu.add(e1);
        menu.add(e2);
        menu.add(e3);
        edit.add(e4);
        edit.add(e5);
        menubar.add(menu);
        menubar.add(edit);
        setJMenuBar(menubar);
        e1.addActionListener(Event->selectFolder(resultI,0));
        e2.addActionListener(Event->selectFolder(resultI,1));
        e3.addActionListener(Event->selectFolder(resultI,2));
        e4.addActionListener(Event->edit(label_Q,label_R,0));
        e5.addActionListener(Event->edit(label_Q,label_R,1));
        var question=new JPanel(new BorderLayout());
        e0.addActionListener(Event->file(question));
        result= new JPanel(new BorderLayout());
        var header= new JPanel(new GridLayout(1,2,5,5));
        var footer= new JPanel(new GridLayout(1,3,5,5));
        var filter=new JLabel("  Filters ");
        filter.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
        var choice=new JComboBox<>(new String[]{"blurry","contour","accentuation","pushback","red Image","green Image","blue Image","grey Image"});
        var send=new JButton(" apply ");
        CustomButton(send);
        send.addActionListener(Event->{
            if (netIsAvailable() && isAlive("Localhost",5991) && received)
            new Thread(()->result(choice.getSelectedIndex())).start();
            else
            JOptionPane.showMessageDialog(this, "Internet connection is not available");
        });
        footer.add(filter);
        footer.add(choice);
        footer.add(send);
        label_Q= new JLabel();
        label_Q.setSize(485, 210);
        label_Q.setBackground(Color.blue);
        question.add(header, BorderLayout.NORTH);
        question.add(label_Q, BorderLayout.CENTER);
        question.add(footer, BorderLayout.SOUTH);
        label_R= new JLabel();
        label_R.setSize(485, 210);
        var sep = new JSplitPane(SwingConstants.VERTICAL, question, result);
        sep.setOrientation(SwingConstants.VERTICAL); 
        var tool= Toolkit.getDefaultToolkit();
        setIconImage(tool.getImage("src\\client\\logo.PNG"));

        add(sep);
        setSize(500, 250);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(rootPaneCheckingEnabled);
    }

    private void edit(JLabel label_Q2, JLabel label_R2, int choice) {
        switch (choice) {
            case 0 -> {
                if (!Undo.isEmpty()) {
                    label_R2.setIcon(Undo.peek().entrySet().iterator().next().getKey());
                    label_Q2.setIcon(Undo.peek().values().iterator().next().entrySet().iterator().next().getKey());
                    path=Undo.peek().values().iterator().next().values().iterator().next();
                    Redo.push(Undo.pop());
                }
            }
            case 1 -> {
                if (!Redo.isEmpty()) {
                    label_R2.setIcon(Redo.peek().entrySet().iterator().next().getKey());
                    label_Q2.setIcon(Redo.peek().values().iterator().next().entrySet().iterator().next().getKey());
                    path=Redo.peek().values().iterator().next().values().iterator().next();
                    Undo.push(Redo.pop());
                }
            }
        }
    }

    private void selectFolder(ImageIcon resultI, int choice) {
        switch (choice) {
            case 0 -> {
                Optional<ImageIcon> isNull= Optional.ofNullable(resultI);
                if(isNull.isPresent()){
                    JFileChooser fileChooser = new JFileChooser();
                    fileChooser.setDialogTitle(" Select destination folder ");
                    fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

                    int userSelection = fileChooser.showSaveDialog(this);

                    if (userSelection == JFileChooser.APPROVE_OPTION) {
                        File fileToSave = fileChooser.getSelectedFile();
                        try {
                            var image = imageIconToBufferedImage(resultI);
                            ImageIO.write(image, "JPG", new File(fileToSave.getPath()+"\\result.jpg"));
                        } catch (IOException e) {
                            JOptionPane.showMessageDialog(null, e.getMessage());
                        }
                    }
                }
            }
            case 1 -> {
                var file = new JFileChooser();
                var filter = new FileNameExtensionFilter("Image","JPG","BMP","PNG");
                file.setFileFilter(filter);
                choice= file.showOpenDialog(this);
                if(choice == JFileChooser.APPROVE_OPTION){
                    selFile = file.getSelectedFile();
                    path = selFile.getParent();
                    JFileChooser fileChooser = new JFileChooser();
                    fileChooser.setDialogTitle(" Select destination folder ");
                    fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

                    int userSelection = fileChooser.showSaveDialog(this);

                    if (userSelection == JFileChooser.APPROVE_OPTION) {
                        File fileToSave = fileChooser.getSelectedFile();
                        encryption(selFile,fileToSave);
                    }
                }
            }
            case 2 -> {
                var file = new JFileChooser();
                var filter = new FileNameExtensionFilter("Image","JPG","BMP","PNG");
                file.setFileFilter(filter);
                choice= file.showOpenDialog(this);
                if(choice == JFileChooser.APPROVE_OPTION){
                    selFile = file.getSelectedFile();
                    path = selFile.getParent();
                    JFileChooser fileChooser = new JFileChooser();
                    fileChooser.setDialogTitle(" Select destination folder ");
                    fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

                    int userSelection = fileChooser.showSaveDialog(this);

                    if (userSelection == JFileChooser.APPROVE_OPTION) {
                        File fileToSave = fileChooser.getSelectedFile();
                        decryption(selFile,fileToSave);
                    }
                }
            }
        }
    }

    private void file(JPanel question) {
        file_I = new JFileChooser();
        var filter = new FileNameExtensionFilter("Image","JPG","BMP","PNG");
        file_I.setFileFilter(filter);
        choice= file_I.showOpenDialog(question);
        if(choice == JFileChooser.APPROVE_OPTION){
            selFile = file_I.getSelectedFile();
            path = selFile.getAbsolutePath();
            label_Q.setIcon(resize(path));
        }
    }

    private void result(int selected) {
        Optional<String> isNull= Optional.ofNullable(path);
        if(!isNull.isPresent()) return;
        if(choice== JFileChooser.APPROVE_OPTION && selected!=-1){
            createConnection(result, label_R, selFile, selected);
            priority++;
            revalidate();
            repaint();
            pack();
        }    
    }

    private ImageIcon resize(String imgPath){
        var path = new ImageIcon(imgPath);
        var img = path.getImage();
        var newImg = img.getScaledInstance(label_Q.getWidth(), label_Q.getHeight(), Image.SCALE_SMOOTH);
        var image = new ImageIcon(newImg);
        return image;
    }

    private void CustomButton(JButton btn){
        btn.setBackground(Color.CYAN);
        btn.setBorderPainted(false);
    }

    private void createConnection(JPanel panel, JLabel label, File icon, int selected){
        try {
            received=false;
            r = LocateRegistry.getRegistry("127.0.0.1", 5991);
            try {
                i = (Controller) r.lookup("Filter");
                isConnected(i);
                if(priority!=0){
                    result.remove(label);
                    label.setIcon(null);
                }
                ImageIcon Image_send=new ImageIcon(path);
                resultI=i.image(Image_send,selected);
                i.disconnect(Image_send);
                Optional<ImageIcon> isNull= Optional.ofNullable(resultI);
                if(!isNull.isPresent())
                JOptionPane.showMessageDialog(null, " Image is null, Try again");
                else{
                    label.setIcon(resultI);
                    panel.add(label);
                    var map=new HashMap<ImageIcon,String>();
                    map.put(resize(icon.getPath()), icon.getPath());
                    var mapPath= new HashMap<ImageIcon,HashMap<ImageIcon,String>>();
                    mapPath.put(resultI,map);
                    Undo.push(mapPath);
                }
                received=true;
            } catch (NotBoundException | AccessException ex) {
                JOptionPane.showMessageDialog(null, ex.getMessage());
            }

        }catch(RemoteException ex){
            error();
        }
    }

    private boolean netIsAvailable() {
        try {
            InetAddress address = InetAddress.getByName("www.google.com");
            if (address.isReachable(3000))
            return true;
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,"Error checking internet connection: " + e.getMessage());
        }
        return false;
      }

    private void error(){
        java.util.logging.Logger.getLogger(Client.class.getName()).log(java.util.logging.Level.SEVERE, null,new ClassNotFoundException());
    }

    private boolean isAlive(String hostName, int port) {
        boolean isAlive = false;
        // Creates a socket address from a hostname and a port number
        SocketAddress socketAddress = new InetSocketAddress(hostName, port);
        Socket socket = new Socket();
        // Timeout required - it's in milliseconds
        int timeout = 3000;
        try {
            socket.connect(socketAddress, timeout);
            socket.close();
            isAlive = true;
        } catch (SocketTimeoutException exception) {
            JOptionPane.showMessageDialog(null," Connection timed out, Try again"+ exception.getMessage());
        } catch (IOException exception) {
            JOptionPane.showMessageDialog(null," Unable to connect to Server, try again " + exception.getMessage());
        }
        return isAlive;
    }

    private void isConnected(Controller service){
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
          public void run() {
            try {
              service.ping(count);
            } catch (RemoteException e) {
              //Client disconnected
              timer.cancel();
            }
          }
        }, 0, 3000);
    }

    public static void encryption(File file, File destination){
        try{
            Cipher cipher=Cipher.getInstance("DES");
            SecretKeySpec keySpec = new SecretKeySpec("MOADEXAM".getBytes(), "DES");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            CipherInputStream cipt=new CipherInputStream(new FileInputStream(file), cipher);
            String filename=file.getName();
            String extension = filename.substring(filename.lastIndexOf(".")+1);
            String[] name=filename.split(extension);
            FileOutputStream file_ip=new FileOutputStream(destination+"\\"+name[0]+".png");
            int i;
            while((i=cipt.read())!=-1){
                file_ip.write(i);
            }
            file_ip.close();
        }
        catch(Exception e)
        {
            JOptionPane.showMessageDialog(null, e.getMessage());
        }
    }
    public static void decryption(File file, File destination){
        try{
            Cipher cipher=Cipher.getInstance("DES");
            SecretKeySpec keySpec = new SecretKeySpec("MOADEXAM".getBytes(), "DES");
            cipher.init(Cipher.DECRYPT_MODE, keySpec);            
            CipherInputStream ciptt=new CipherInputStream(new FileInputStream(file), cipher);
            String filename=file.getName();
            String extension = filename.substring(filename.lastIndexOf(".")+1);
            String[] name=filename.split(extension);
            FileOutputStream file_op=new FileOutputStream(destination+"\\"+name[0]+".png");
            int j;
            while((j=ciptt.read())!=-1)
            {
                file_op.write(j);
            }
            file_op.close();
        }
        catch(Exception e)
        {
            JOptionPane.showMessageDialog(null, e.getMessage());
        }
    }
    private BufferedImage imageIconToBufferedImage(ImageIcon icon) {
        BufferedImage bufferedImage = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(),
                BufferedImage.TYPE_INT_RGB);
        Graphics graphics = bufferedImage.createGraphics();
        icon.paintIcon(null, graphics, 0, 0);
        graphics.dispose();
        return bufferedImage;
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel( new NimbusLookAndFeel() );
        } catch (UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }
        new Client("image");
    }
}
