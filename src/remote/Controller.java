package remote;

import java.rmi.Remote;
import java.rmi.RemoteException;

import javax.swing.ImageIcon;

public interface Controller extends Remote{
    public ImageIcon image(ImageIcon icon,int selected) throws RemoteException;
    public void ping(int count) throws RemoteException;
    public void disconnect(ImageIcon image) throws RemoteException;
}