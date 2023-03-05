package server;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import remote.ControllerImp;

public class ServerController {
    Registry rg;
    
    public ServerController() {
        try{
            rg = LocateRegistry.createRegistry(5991);
            beginning();
        }catch(RemoteException ex){
            Logger.getLogger(ServerController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void beginning(){
        try{
            rg.rebind("Filter", new ControllerImp());
        }catch(RemoteException e){
            System.out.println(e.getMessage());
        }
    }
    
    public void end(){
        try{
            rg.unbind("Filter");
        }catch(RemoteException e){
            System.out.println(e.getMessage());
        }catch(NotBoundException ex){
            System.out.println(ex.getMessage());
        }
    }
    

    public static void main(String args[]) {
        var executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        executorService.execute(()->new ServerController());
        executorService.shutdown();
        executorService.close();
    }
}

