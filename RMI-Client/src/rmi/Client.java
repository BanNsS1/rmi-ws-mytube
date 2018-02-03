package rmi;

import java.io.File;
import java.io.IOException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import javax.xml.bind.DatatypeConverter;

import rmi.MyTubeFile;
import rmi.ServerInterface;

public class Client {
    private static final String IP = "localhost";
    private static final int PORT = 65535;
    private static final int[][] ALLOWED_ACTIONS = new int[][]{{1,2}, {1,2,3,4,5,6,7,8}};
    
    private static boolean connected = false;
    private static String PASSWORD_PREFIX_ENCRYPTION = "`3[8P8y+<?U[qT";
    private static boolean DEBUG = true;
    
    public static void main(String args[]) {
        try {
            System.out.println("Connect to the server.");
            String ip = askForIP();
            int port = askForPort();
            String save_path = askForSavePath();
            ClientInterface client = new ClientImplementation(save_path);
            
            String url = "rmi://"+ip+":"+port+"/mytube";
            ServerInterface server = (ServerInterface) Naming.lookup(url);
            System.out.println("\nCONNECT: Successfully connected to "+url);
            
            UI(client,server);
            
        } catch (RemoteException | NotBoundException e){
            if(DEBUG){
                System.out.println("\tException:"+e.toString());
            	e.printStackTrace();
            }
            System.out.println("ERROR: Couldn't connect to the server.");
            System.exit(-1);
        } catch (IOException e){
            if(DEBUG){
                System.out.println("\tException:"+e.toString());
            	e.printStackTrace();
            }
            System.out.println("ERROR: Couldn't access that file.");
            System.exit(-1);
        } catch (Exception e){
            if(DEBUG){
                System.out.println("\tException:"+e.toString());
            	e.printStackTrace();
            }
            System.out.println("ERROR: Unexpected exception.");
            System.exit(-1);
        }
    }
    
    private static void UI(ClientInterface client, ServerInterface server){
    	try{
	    	while(true){
	    		int action = askForAction(client);
	    		System.out.println();
	    		System.out.println();
	    		performAction(client, server, action);
	    		
	    		if(client.getErrorStatus()){
	    			System.out.println(client.getErrorMsg());
	    			System.exit(-1);
	    		}else{
	    			String msg = client.getMessage();
	    			if(!msg.equals("")){
	    				System.out.println(msg);
	    				System.out.println();
	    				System.out.println();
	    				client.clearMessage();
	    			}
	    		}
	    	}
    	}catch (Exception e) {
    		System.out.println("ERROR: Unexpected Error. Recovering...");
    		System.out.println("\tError Description: "+ e.toString());
    		UI(client,server);
		}
    }
    
    private static int askForAction(ClientInterface client) throws RemoteException{
    	if(!client.getStatus()){
        	System.out.println("");
        	System.out.println("SERVICE: Do you already have an account?");
            System.out.println("\t1: Login\t\t2: Register");
        	return askForOption(0);
    	}else{
            System.out.println("");
            System.out.println("SERVICE: What action would you like to perform?");
            System.out.println("\t1: Upload File			2: List My Files");
            System.out.println("\t3: Modify file Title 		4: Modify file Description");
            System.out.println("\t5: Delete File			6:Search File");
            System.out.println("\t7: Download File		8:Disconnect");
        	return askForOption(1);
    	}
    }
    
    private static void performAction(ClientInterface client, ServerInterface server, int action) throws RemoteException, NoSuchAlgorithmException{

		if(!client.getStatus()){
    		if(action == 1){
					login(client, server);
    		}else if(action == 2){
    			register(client, server);
    		}
    	}else{
    		switch(action){
    			case 1:
    				uploadFile(client, server);
    				break;
    			case 2:
    				listMyFiles(client, server);
    				break;
    			case 3:
    				modifyFileTitle(client, server);
    				break;
    			case 4:
    				modifyFileDescription(client, server);
    				break;
    			case 5:
    				deleteFile(client, server);
    				break;
    			case 6:
    				searchFile(client, server);
    				break;
    			case 7:
    				downloadFile(client, server);
    				break;
    			case 8:
    				disconnect(client, server);
    				break;
    		}    		
    	}

    }
    
    private static void login(ClientInterface client, ServerInterface server) throws NoSuchAlgorithmException, RemoteException{
    	System.out.println();
    	System.out.println("Alright, let's LOGIN");
    	
    	String username = askForUsername();
    	String password = PASSWORD_PREFIX_ENCRYPTION + askForPassword();
    		
		MessageDigest md5 = MessageDigest.getInstance("MD5");
	    md5.update(password.getBytes());
	    byte[] digest = md5.digest();
	    String encryptedPassword = DatatypeConverter.printHexBinary(digest).toUpperCase();
	        	    
	    client.setUser(new User(username, encryptedPassword));
	    server.login(client);
    }
    private static void register(ClientInterface client, ServerInterface server) throws NoSuchAlgorithmException, RemoteException{
    	System.out.println();
		System.out.println("Alright, let's REGISTER");
		String username = askForUsername();
		String password = askForPassword();
		String vpassword = askForPasswordVerification();
		
		while(!password.equals(vpassword)){
			System.out.println("\tThe password entered and its verification didn't match");
    		password = askForPassword();
    		vpassword = askForPasswordVerification();
		}
		
		password = PASSWORD_PREFIX_ENCRYPTION + password;
		
		MessageDigest md5 = MessageDigest.getInstance("MD5");
	    md5.update(password.getBytes());
	    byte[] digest = md5.digest();
	    String encryptedPassword = DatatypeConverter.printHexBinary(digest).toUpperCase();
	    
	    client.setUser(new User(username, encryptedPassword));
	    server.register(client);
    }
    
    private static void uploadFile(ClientInterface client, ServerInterface server) throws RemoteException{
        System.out.println("OPTION 1: Selected upload file.");
        String title = askForTitle();
        String description = askForDescription();
        String path = askForFilePath();
        MyTubeFile file = new MyTubeFile(title, description, path);
        server.uploadFile(client, file);
    }
    private static void listMyFiles(ClientInterface client, ServerInterface server) throws RemoteException{
        System.out.println("OPTION 2: List my files.");
        List<MyTubeFile> list_myfiles = server.getMyFiles(client);
        printFileList(list_myfiles, "OPTION 2: Listing your files");
    }
    private static void modifyFileTitle(ClientInterface client, ServerInterface server) throws RemoteException{
        System.out.println("OPTION 3: Modify file title");
        int id = askForInt("File id: ");
        String title_new = askForTitle();
        server.modifyFileTitle(client, id, title_new);
    }
    private static void modifyFileDescription(ClientInterface client, ServerInterface server) throws RemoteException{
        System.out.println("OPTION 4: Modify file description");
        int id = askForInt("\tFile id: ");
        String description = askForDescription();
        server.modifyFileDescription(client, id, description);
	}
    private static void deleteFile(ClientInterface client, ServerInterface server) throws RemoteException{
    	System.out.println("OPTION 5: Delete file");
        int id = askForInt("\tFile id: ");
        server.deleteFile(client, id);
    }
    private static void searchFile(ClientInterface client, ServerInterface server) throws RemoteException{
        System.out.println("OPTION 6: Selected search file.");
        String tags = askForDescription();
        List<MyTubeFile> list_search = server.findFilesByTags(client, tags);
        printFileList(list_search, "\tOPTION 6: Listing files matching '"+ tags +"' description.");
    	
    }    
    private static void downloadFile(ClientInterface client, ServerInterface server) throws RemoteException{
        System.out.println("OPTION 7: Download file.");
        int id = askForInt("\tFile id: ");
        server.downloadFile(client, id);
    }
    private static void disconnect(ClientInterface client, ServerInterface server) throws RemoteException{
        System.out.println("OPTION 8: Disconnect");
        server.disconnect(client);
        System.exit(0);
    }
    
    private static void printFileList(List<MyTubeFile> list, String header){
        if(list != null && !list.isEmpty()){
            System.out.println(header);
            for(MyTubeFile f : list){
                System.out.println("\t- File: "+f.getId()+" - Title:"+f.getTitle() +" - Description:"+ f.getDescription());
            }
        }
    }

    private static String askForTitle(){
        String title = askForInput("\tFile Title:");
        if(!title.equals("")){
            return title;
        }
        return askForTitle();
    }
    
    private static String askForDescription(){
        String description = askForInput("\tFile Description:");
        if(!description.equals("")){
            return description;
        }
        return askForDescription();
    }
    
    private static String askForFilePath(){
        String abspath = new File("").getAbsolutePath();
        String path = askForInput("\tFile path (must be relative to "+abspath+"): ");
        if(!path.equals("")){
            File f = new File(path);
            if(f.exists() && !f.isDirectory()) { 
                return path;
            }
        }
        return askForFilePath();
    }
    
    private static String askForUsername(){
        String username = askForInput("\tUsername: ");
        if(!username.equals("")){
            return username;
        }
        return askForUsername();
    }
    
    private static String askForPassword(){
        String password = askForInput("\tPassword: ");
        if(!password.equals("")){
            return password;
        }
        return askForPassword();
    }    
    
    private static String askForPasswordVerification(){
        String password = askForInput("\tPassword (again): ");
        if(!password.equals("")){
            return password;
        }
        return askForPasswordVerification();
    }
    
    private static int askForInt(String msg){
    	try{
	        String value_str = askForInput("\tFile ID: ");
	        if(!value_str.equals("")){
	            int value = Integer.parseInt(value_str);
	            return value;
	        }
        	return askForInt(msg);
		}catch(Exception e){
	        return askForInt(msg);
		}
    }
    
    private static String askForIP(){
        String ip = askForInput("\tSERVER IP (Default: "+IP+"): ");
        if(!ip.equals("")){
            return ip;
        }
        return IP;
    }
    
    private static int askForPort(){
        String port = askForInput("\tSERVER PORT (Default: "+PORT+"): ");
        if(!port.equals("")){
            return Integer.parseInt(port);
        }
        return PORT;
    }
        
    private static String askForSavePath(){
    	String save_path = askForInput("\tDownloads save path (added to avoid creating 2 workspaces): ");
        if(!save_path.equals("")){
            return save_path;
        }
        return askForSavePath();
    }
    
    private static int askForOption(int type){
    	try{
	        String option = askForInput("Type the option code here: ");
	        if(!option.equals("")){
	            int value = Integer.parseInt(option);
	            for(int action : ALLOWED_ACTIONS[type]){
	                if(action == value)
	                    return value;
	            }
	        }
        	return askForOption(type);
		}catch(Exception e){
	        return askForOption(type);
		}
    }
    
    private static String askForInput(String feed){
        Scanner scn = new Scanner (System.in);
        String input;
        
        System.out.print(feed);
        input = scn.nextLine();
        
        return input;
    }
}
