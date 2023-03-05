package remote;

import java.rmi.Remote;
import java.rmi.RemoteException;

import javax.swing.ImageIcon;

public interface Filter extends Remote{
    public ImageIcon image(ImageIcon icon,int answer) throws RemoteException;
}
