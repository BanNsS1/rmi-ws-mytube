package rmi;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class Server {
    private static final String IP = "localhost";
    private static final int PORT = 65535;
    
    private static final String YES = "y";
    private static final String NO = "n";
    
    public static void main(String args[]) {
        try{
            //Server url params.
            System.out.println("Setting up the server.");
            //Skipping askForIP, dettecting itself.
            String ip = askForIP();
            int port = askForPort();
            String save_path = askForSavePath();
            
            //Initializing Server object.
            ServerImplementation server = new ServerImplementation(save_path, ip, port);
            
            //Checking ws + database status.
            boolean wsOnline = false;
            while(!wsOnline){
	           	if(!server.statusWS()){
	        		System.out.println("\nERROR: Webservice is not available.");
	        	}else if(!server.statusDB()){
	        		System.out.println("\nERROR: Webservice is available but DB is not.");
	        	}else{
	        		wsOnline = true;
	        	}
	           	if(!wsOnline)
	           		askForInput("Press ENTER to try again:");
            }
            //Starting server.
            startRegistry(port);
            Naming.rebind(server.getAdrress(), server);
            
            //Trying to recover server data.
            if(!server.recoverServer()){
            	//If not success: Registering as new one.
            	server.registerServer();
            }
            System.out.println("\nSERVICE: Server running at: "+server.getAdrress());
            System.out.println("SERVICE: Mytube service server #"+ server.getId() +" is online!");
            
            server.connectToOtherServers();
        }catch(Exception e){
            System.out.println("ERROR: An error ocurred.");
            System.out.println("\t Exception: "+e.toString());
            e.printStackTrace();
        }
    }
    
    private static String askForIP() throws UnknownHostException{
        InetAddress i = InetAddress.getLocalHost();
        String ip = i.getHostAddress();
        System.out.println("\tSERVER IP (Default: localhost): "+ ip +" (using your current private ip)");
        return ip;
        /*String ip = askForInput("\tSERVER IP (Default: "+IP+"):");
        if(!ip.equals("")){
            return ip;
        }
        return IP;*/
    }
    
    private static int askForPort(){
    	String port = askForInput("\tSERVER PORT (Default: "+PORT+"): ");
        if(!port.equals("")){
            return Integer.parseInt(port);
        }
        return PORT;
    }
    
    private static String askForSavePath(){
    	String save_path = askForInput("\tUploads save path (added to avoid creating 2 workspaces): ");
        if(!save_path.equals("")){
            return save_path;
        }
        return askForSavePath();
    }
    
    private static String askForInput(String feed){
        Scanner scn = new Scanner (System.in);
        String input;
        
        System.out.print(feed);
        input = scn.nextLine();
        return input;
    }
    
    public static void startRegistry(int port) throws RemoteException{
        try{
            Registry registry = LocateRegistry.getRegistry(port);
            registry.list();
        }catch(RemoteException ex){
            Registry registry = LocateRegistry.createRegistry(port);
        }
    }
}
