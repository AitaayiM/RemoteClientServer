package remote;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.rmi.server.ServerNotActiveException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.RemoteServer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Optional;
import java.util.Scanner;
import java.util.Stack;

import javax.swing.ImageIcon;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.awt.Graphics;
import java.awt.Graphics2D;

public class ControllerImp extends UnicastRemoteObject implements Controller{

  Registry r;
  Filter i;
  static ArrayList<Stack<Request>> requests=new ArrayList<>();
  static Stack<Integer> actual=new Stack<>();
  private Request clientData;

  public ControllerImp() throws RemoteException {
    super();
  }

    @Override
    public ImageIcon image(ImageIcon icon, int selected) throws RemoteException {
      long in=System.currentTimeMillis();
      BufferedImage image=imageIconToBufferedImage(icon);
        try {
          System.out.println("Client Host:"+RemoteServer.getClientHost());
          System.out.println("The number of requests that have not yet been received by the owners :"+requests.size());
          clientData=new Request(RemoteServer.getClientHost(), image, selected,null);
          Iterator<Stack<Request>> iterator = requests.iterator();
          while (iterator.hasNext()) {
            var req1 = iterator.next();
            for(var req2: req1){
              if(req2.getIp().equals(clientData.getIp()) && req2.getSelected()==clientData.getSelected() && compareImage(image, req2.getImage())){
                System.out.println("Client Host:"+RemoteServer.getClientHost()+" The request already exists");
                Optional<ImageIcon> isNull= Optional.ofNullable(req2.getResult());
                if(isNull.isPresent())
                return ClientIsAvailable(req2.getResult(), in);
                else{
                System.out.println("The image of this request is null");
                iterator.remove();
                }  
              }
            }
          }
          boolean ClientExists = false;
          Iterator<Stack<Request>> Iterator = requests.iterator();
          while (iterator.hasNext()) {
            var req1 = Iterator.next();
            for(var req2: req1){
              if(req2.getIp().equals(clientData.getIp())){
                req1.push(clientData);
                ClientExists = true;
                break;
              }
            }
            if (ClientExists) {
              break;
            }
          }
          if(!ClientExists){
            var client=new Stack<Request>();
            client.push(clientData);
            requests.add(client);
          }
        } catch (ServerNotActiveException e) {
          e.printStackTrace();
        }
        var servers=readFile("src\\remote\\servers.txt");
        var serversAvailable=new ArrayList<Server>();
        int count=0;
        for (var server : servers){
          if(netIsAvailable() && isAlive(server.getIp(), server.getPort())){
            serversAvailable.add(server);
            count++;
          }
        }
        if (count==0){
          return ClientIsAvailable(null, in);
        }
        var parts=getSubImage(image, count);
        var images= new BufferedImage[parts.length];
        int index=0;
        for (var server : serversAvailable) {
          final int j=index;
          new Thread(()->{
            try {
              r = LocateRegistry.getRegistry(server.getIp(), server.getPort());
              i = (Filter) r.lookup("Filter");
              images[j]=imageIconToBufferedImage(i.image(new ImageIcon(parts[j]),selected));
              Optional<BufferedImage> isNull= Optional.ofNullable(parts[j]);
              if(!isNull.isPresent())
              System.out.println("The image received "+j+" is null");            
              } catch (IOException | NotBoundException e) {
                java.util.logging.Logger.getLogger(ControllerImp.class.getName()).log(java.util.logging.Level.SEVERE, null,new ClassNotFoundException());
            }
          }).start();
          index++; 
        }

        for (int morceau=0; morceau<images.length ; morceau++) {
          Optional<BufferedImage> isNull= Optional.ofNullable(images[morceau]);
          if(!isNull.isPresent()){
            try {
                Thread.sleep(25);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            morceau--;
          }     
        }
        image = mergeImage(images);
        var newImg = new ImageIcon(image.getScaledInstance(485, 210, Image.SCALE_SMOOTH));
        boolean clientExists = false;
        Iterator<Stack<Request>> iterator = requests.iterator();
        while (iterator.hasNext()) {
          var req1 = iterator.next();
          for(var req2: req1){
            if(req2.getIp().equals(clientData.getIp()) && req2.getSelected()==clientData.getSelected() && compareImage(image, clientData.getImage())){
              req2.setResult(newImg);
              clientExists = true;
              break;
            }
          }
          if (clientExists) {
            break;
          }
        }
        if (!clientExists) {
          Stack<Request> clientStack = new Stack<>();
          clientData.setResult(newImg);
          clientStack.push(clientData);
          requests.add(clientStack);
        }
      return ClientIsAvailable(clientData.getResult(), in);
    }

    private BufferedImage imageIconToBufferedImage(ImageIcon icon) {
        BufferedImage bufferedImage = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(),
                BufferedImage.TYPE_INT_ARGB);
        Graphics graphics = bufferedImage.createGraphics();
        icon.paintIcon(null, graphics, 0, 0);
        graphics.dispose();
        return bufferedImage;
    }

    private BufferedImage mergeImage(BufferedImage[] parts){
        int width=parts[0].getWidth()*parts.length;
        int height=parts[0].getHeight();
        int position = 0;
        BufferedImage concatImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = concatImage.createGraphics();
        for (BufferedImage bufferedImage : parts) {
            g2d.drawImage(bufferedImage, position, 0, null);
            position += bufferedImage.getWidth();
        }
        return concatImage;
      }
    
    private BufferedImage[] getSubImage(BufferedImage image, int cols){
        BufferedImage[] parts= new BufferedImage[cols];
        int index=0;
        int height=image.getHeight();
        int width=image.getWidth()/cols;
        for (int abscise = 0; abscise < image.getWidth() && index<cols; abscise+=width) {
            parts[index++]=image.getSubimage(abscise, 0, width, height);
        }
        return parts;
    }

    private static boolean compareImage(BufferedImage biA, BufferedImage biB) {        
      try {
          DataBuffer dbA = biA.getData().getDataBuffer();
          int sizeA = dbA.getSize();                      
          DataBuffer dbB = biB.getData().getDataBuffer();
          int sizeB = dbB.getSize();
          // compare data-buffer objects //
          if(sizeA == sizeB) {
              for(int i=0; i<sizeA; i++) { 
                  if(dbA.getElem(i) != dbB.getElem(i)) {
                      return false;
                  }
              }
              return true;
          }
          else {
              return false;
          }
      } 
      catch (Exception e) { 
          System.out.println("Failed to compare image files ...");
          return  false;
      }
    }

    private ArrayList<Server> readFile(String path){
        try {
          var file = new FileInputStream(path);   
          var scanner = new Scanner(file); 
          var servers = new ArrayList<Server>();
          while(scanner.hasNextLine())
          {
            var line=scanner.nextLine().split(" ");
            servers.add(new Server(line[0], line[1]));
          }
          scanner.close();
          return servers;
        } catch (FileNotFoundException e) {
          e.printStackTrace();
        }    
        return null;
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
          System.out.println(" Connection timed out [ "+hostName+" , "+port+" ] "+ exception.getMessage());
      } catch (IOException exception) {
          System.out.println(" Unable to connect to Server [ "+hostName+" , "+port+" ] "+ exception.getMessage());
      }
      return isAlive;
  }

  @Override
  public void ping(int count) throws RemoteException {
    actual.push(count);
  }

  private ImageIcon ClientIsAvailable(ImageIcon image, long in_Time){
    while(!netIsAvailable())
      try {
        Thread.sleep(25);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    long out=System.currentTimeMillis();
    long time_passed=out-in_Time;
    if(time_passed>3000){
      int do_if=actual.lastElement();
      while(do_if<actual.firstElement()*(time_passed/3000)){
        try {
          Thread.sleep(25);
          do_if=actual.lastElement();
          time_passed+=25;
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
      return image;
    }
    return image;
  }

  @Override
  public void disconnect(ImageIcon image) throws RemoteException {
    Iterator<Stack<Request>> iterator = requests.iterator();
    while (iterator.hasNext()) {
      var req1 = iterator.next();
      for (var req2 : req1) {
        if (req2.getIp().equals(clientData.getIp()) && req2.getSelected() == clientData.getSelected() && compareImage(imageIconToBufferedImage(image), clientData.getImage())) {
          iterator.remove();
        }
      }
    }
    System.out.println("Customer No. " + clientData.getIp() + " received his order");
  }
}

class Server{
    private String ip;
    private String port;
    public String getIp() {
        return ip;
    }
    public int getPort() {
        return Integer.parseInt(port);
    }
    public Server(String ip, String port) {
        this.ip = ip;
        this.port = port;
    }
}

class Request{
  private String ip;
  private BufferedImage image;
  private int selected;
  private ImageIcon result;

  public Request(String ip, BufferedImage image,int selected, ImageIcon result) {
    this.ip = ip;
    this.image=image;
    this.selected = selected;
    this.result=result;
  }
  public String getIp() {
    return ip;
  }
  public BufferedImage getImage() {
    return image;
  }
  public int getSelected() {
    return selected;
  }
  public ImageIcon getResult() {
    return result;
  }
  public void setResult(ImageIcon result) {
    this.result = result;
  }
}