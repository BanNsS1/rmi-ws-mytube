package rmi;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import rmi.ClientInterface;
import rmi.MyTubeFile;

public class ServerImplementation extends UnicastRemoteObject implements ServerInterface{

	private static final long serialVersionUID = -4244756727996129130L;

	private static String WS_PATH = "http://localhost:8080/myRESTwsWeb/rest";
    
    private final ArrayList<ServerInterface> servers;
    private final ArrayList<ClientInterface> clients;
    private final ArrayList<MyTubeFile> files;
    private final String uploadsPath;
    private final String ip;
    private final int port;
    private int id;
    
    
    public ServerImplementation(String uploadsPath, String ip, int port) throws RemoteException{
        super();
        this.servers = new ArrayList<>();
        this.clients = new ArrayList<>();
        this.files = new ArrayList<>();
        this.uploadsPath = uploadsPath;
        this.ip = ip;
        this.port = port;
                
        new File(uploadsPath).mkdir();
    }
        
    @Override
    public int getId() throws RemoteException{
    	return this.id;
    }
    
    @Override
    public String getAdrress() throws RemoteException{
    	return "rmi://"+this.ip+":"+this.port+"/mytube";
    }
    
    //////////////////////////////////
    ////////// SERVER STATUS /////////
    //////////////////////////////////
    
    public boolean recoverServer() throws RemoteException{
    	File[] listOfFiles = new File(uploadsPath).listFiles();
    	List<String> fs_files = new ArrayList<>();
    	
    	if(listOfFiles.length == 0){
    		System.out.println("\nREGISTER: This server was detected as a new server. Registering in the database...");
    		return false;
    	}
    	System.out.println("\nRECOVER: Found existing files. Let's try to recover server data.");
    	
		for (int i = 0; i < listOfFiles.length; i++) {
			if (listOfFiles[i].isFile()) {
				String filename = listOfFiles[i].getName();
				String name = "";
				
				String[] parts = filename.split("\\.");
				name = parts[0];
				
				fs_files.add(name);
			}
		}
		
		MyTubeFile file = null;
		int index = -1;
		while(file == null){
			index += 1;
			file = getFile(Integer.parseInt(fs_files.get(index)));
		}
		
		this.id = file.getServer();
		updateServer(id, true);
		
		int correct = 0;
		
		List<MyTubeFile> db_files = getFiles();
		for(MyTubeFile f : db_files){
			String id = Integer.toString(f.getId());
			if(fs_files.contains(id)){
				files.add(f);
				fs_files.remove(id);
				System.out.println("RECOVER: Successfully recovered file "+ id +" with title '"+ f.getTitle() +"'");
				correct += 1;
			}else{
				System.out.println("RECOVER: Failed to recover file "+ id +". Removing from database...");
			}
		}
		
		System.out.println("RECOVER: Recover completed ("+correct+"/"+db_files.size()+").");
		
		
    	return true;
    }
    
    public void registerServer() throws RemoteException{
    	this.id = postServer();
    	if(this.id != -1){
    		System.out.println("REGISTER: Successfully registered.");
    	}else{
    		System.out.println("REGISTER: Failed to register. Stopping...");
    		System.exit(-1);
    	}
    }
    
    public void serverDisconnect() throws RemoteException{
    	//THIS SHOULD BE EXECUTED BY A SHUTDOWN HOOK
    	//BUT IT DOESN'T SEEM TO WORK SO WE'VE NO
    	//WAY TO RUN THIS FUNCTION BEFORE SHUTTING DOWN.
    	
    	System.out.println("SERVICE: Shutdown requested.");
    	updateServer(id, false);

    	System.out.println("SERVICE: Disconnecting clients...");
    	for(ClientInterface client : clients){
    		client.setError(true, "ERROR: Server closed the connection.");
    	}

    	System.out.println("SERVICE: Disconnecting servers...");
    	for(ServerInterface server : servers){
    		server.serverDisconnectFrom(server);
    	}
    	System.out.println("SERVICE: Shutdown successfully.");
    }
    
    //////////////////////////////////
    ////// SERVERS COMMUNICATION /////
    //////////////////////////////////
    
    private ServerInterface getServerById(int id) throws RemoteException{
    	for(ServerInterface sv : this.servers){
    		if(sv.getId() == id){
    			return sv;
    		}
    	}
    	return null;
    }
    
    @Override
    public void connectToOtherServers() throws RemoteException{
    	List<JSONObject> svs = getServers();

		System.out.println();
    	if(svs.size() == 1){ //itself
    		System.out.println("LOOKUP: No servers found. We're alone :(");
    	}else{
    		System.out.println("LOOKUP: Found "+ (svs.size() - 1) +" servers. Let's try to connect to them.");
	    	for(JSONObject sv : svs){
	    		int id = Integer.parseInt(sv.get("id").toString());
    			String address = sv.get("address").toString();
	    		if(id != this.getId()){
	    			//Connecting to server
	    			serverConnectTo(this, address, id);
	    			//Getting server impl
	    			ServerInterface server = getServerById(id);
	    			if(server != null){
	    				//Telling remote server to connect to us.
	    				if(server.serverConnectTo(this, this.getAdrress(), this.getId())){
	    					System.out.println("LOOKUP: Successfully connected to server #"+ server.getId());
	    					continue;
	    				}else{
	    					servers.remove(server);
	    				}
	    			}
	    			System.out.println("LOOKUP: Couldn't establish a bidirectional connection with the server #"+ id);
	    			//SINCE SHUTDOWN HOOK DOES NOT SEEM TO WORK
	    			//WE'RE UPDATING SERVER STATUS AFTER AN OTHER
	    			//SERVER TRIES TO CONNECT UNSUCCESSFULLY
	    			updateServer(id, false);
	    		}
	    	}
    	}
    }
    
    @Override
    public boolean serverConnectTo(ServerInterface from, String address, int id) throws RemoteException{
        try {
        	if(address.equals(this.getAdrress())){
        		return false;
        	}else if(this.getId() == id){
        		return false;
        	}
            
            ServerInterface server = (ServerInterface) Naming.lookup(address);
            this.servers.add(server);
            if(from.getId() != this.getId()){
            	System.out.println("LOOKUP: Server "+ from.getId() +" successfully connected to you.");
            }
            return true;
            
        } catch (NotBoundException | MalformedURLException | RemoteException e){
            return false;
        }
    }
    
    @Override
    public void serverDisconnectFrom(ServerInterface server) throws RemoteException{
    	System.out.println("LOOKUP: Server #"+ server.getId() +" closed the connection.");
    	this.servers.remove(server);
    }
    
    /////////////////////////////////
    ////////// FILE SYSTEM //////////
    /////////////////////////////////
    private String getExtension(String filename){
    	String[] parts = filename.split("\\.");
		return parts[parts.length - 1];
    }
    private String getFileName(String filename){
    	String[] parts = filename.split("\\.");
		return parts[0];
    }
    private String getFSFileName(MyTubeFile file){ 
    	File[] listOfFiles = new File(uploadsPath).listFiles();
    	int id = file.getId();
    	
    	
		for (int i = 0; i < listOfFiles.length; i++) {
			if (listOfFiles[i].isFile()) {
				String filename = listOfFiles[i].getName();
				
				String[] parts = filename.split("\\.");
				String name = parts[0];
				String extension = parts[1];
				
				if(id == Integer.parseInt(name)){
					return name + "." + extension;
				}
			}
		}
		return "";
    }
    
    private boolean saveFile(ClientInterface client, MyTubeFile file){
        try{
            byte[] content = client.readFile(file.getFilename());
            String sf = uploadsPath + "/"+ file.getId() + "." + getExtension(file.getFilename());
        	
            FileOutputStream fos = new FileOutputStream(sf);
            fos.write(content);
            fos.close();
            return true;
        }catch(IOException e){
            return false;
        }
    }
    private byte[] readFile(MyTubeFile file) throws IOException{
    	String rf = uploadsPath + "/" + getFSFileName(file);
    	return Files.readAllBytes(Paths.get(rf));
    }
    private void rmFile(MyTubeFile file){
        String path = new File("").getAbsolutePath();
        path += "/" + uploadsPath + "/" + getFSFileName(file);
        
        File f = new File(path);
        f.delete();
    }
    private boolean renameFile(MyTubeFile oldf, MyTubeFile newf){
    	File[] listOfFiles = new File(uploadsPath).listFiles();
    	int old_id = oldf.getId();
    	int new_id = newf.getId();
    	
    	
		for (int i = 0; i < listOfFiles.length; i++) {
			if (listOfFiles[i].isFile()) {
				String filename = listOfFiles[i].getName();
				
				String name = getFileName(filename);
				String extension = getExtension(filename);
				
				if(old_id == Integer.parseInt(name)){
					File oldfile = new File(uploadsPath + "/"+ old_id + "." + extension);
					File newfile = new File(uploadsPath + "/"+ new_id + "." + extension);
			        
			        if(oldfile.renameTo(newfile)){
			        	return true;
			        }
				}
			}
		}
		return false;
    }
    
    /////////////////////////////////
    ////////// Web Service //////////
    /////////////////////////////////
    //STATUS//
    public boolean statusWS() throws RemoteException{
		try {
			URL url = new URL(WS_PATH + "/status/ws");
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
            conn.setRequestProperty("Content-Type", "application/json");
		
			if(conn.getResponseCode() != 200)
				return false;
		
			return true;
		} catch (Exception e) { 
			return false;
		}
    }
    public boolean statusDB() throws RemoteException{
		try {
			URL url = new URL(WS_PATH + "/status/db");
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
            conn.setRequestProperty("Content-Type", "application/json");
		
			if(conn.getResponseCode() != 200)
				return false;
		
			return true;
		} catch (Exception e) { 
			return false;
		}
    }

    //USERS//
    private List<String> getUsers(){
		try {
			if(!statusDB()){
				return new ArrayList<String>();
			}
			
			URL url = new URL(WS_PATH + "/users");
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
            conn.setRequestProperty("Content-Type", "application/json");
		
			if(conn.getResponseCode() != 200)
				return new ArrayList<>();
			
			BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			String output = br.readLine();
			conn.disconnect();
			
			JSONParser parser = new JSONParser();
			Object obj = parser.parse(output);
			JSONArray array = (JSONArray)obj;
			
			//toArray
			List<String> users = new ArrayList<>();
			for(int i = 0; i < array.size(); i++){
				users.add(array.get(i).toString());
			}

			return users;
			
		} catch (Exception e) { 
			return null;
		}
	}
    private User getUser(String username){
		try {
			if(!statusDB()){
				return null;
			}
			
			URL url = new URL(WS_PATH + "/user/" + username);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
            conn.setRequestProperty("Content-Type", "application/json");
		
			if(conn.getResponseCode() != 200)
				return null;
			
			BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			String output = br.readLine();
			conn.disconnect();

			JSONParser parser = new JSONParser();
			Object obj = parser.parse(output);
			JSONObject obj2 = (JSONObject) obj;
			
			User user = new User(obj2.get("username").toString(), obj2.get("password").toString());
			user.setId(Integer.parseInt(obj2.get("id").toString()));
			
			return user;
			
		} catch (Exception e) {
			return null;
		}
    }
    private boolean postUser(User user){
        try {
			if(!statusDB()){
				return false;
			}
        	
            URL url = new URL(WS_PATH + "/user");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");

            JSONObject obj = new JSONObject();
            
            obj.put("id", user.getId());
            obj.put("username", user.getUsername());
            obj.put("password", user.getPassword());

            StringWriter out = new StringWriter();
            obj.writeJSONString(out);
            String jsonText = out.toString();  
            
            java.io.OutputStream os = conn.getOutputStream();
            os.write(jsonText.getBytes());
            os.flush();

            int status = conn.getResponseCode();
            conn.disconnect();
            
            if(status == 201){ 
            	return true;
            }
            return false;
        } catch (IOException ex) {
            return false;
        }
    }
    
    //FILES//
    private List<MyTubeFile> getFiles(){
    	try{
			if(!statusDB()){
				return null;
			}
			
			URL url = new URL(WS_PATH + "/files");
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
	        conn.setRequestProperty("Content-Type", "application/json");
		
			if(conn.getResponseCode() != 200)
				return new ArrayList<>();
			
			BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			String output = br.readLine();
			conn.disconnect();
			
			JSONParser parser = new JSONParser();
			Object obj = parser.parse(output);
			JSONArray array = (JSONArray)obj;
			
			//toArray
			List<MyTubeFile> files = new ArrayList<>();
			for(int i = 0; i < array.size(); i++){
				JSONObject obj2 = (JSONObject) array.get(i);
				files.add(new MyTubeFile(
			    	Integer.parseInt(obj2.get("id").toString()),
			    	obj2.get("title").toString(),
			    	obj2.get("description").toString(),
			    	Integer.parseInt(obj2.get("owner").toString()),
			    	Integer.parseInt(obj2.get("server").toString())
				));
			}
			return files;
			
		} catch (Exception e) { 
			return null;
		}    	
    }
    private MyTubeFile getFile(int id){
		try {
			if(!statusDB()){
				return null;
			}
			
			URL url = new URL(WS_PATH + "/file/" + id);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
            conn.setRequestProperty("Content-Type", "application/json");
		
			if(conn.getResponseCode() != 200)
				return null;
			
			BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			String output = br.readLine();
			conn.disconnect();

			JSONParser parser = new JSONParser();
			Object obj = parser.parse(output);
			JSONObject obj2 = (JSONObject) obj;
			
		    MyTubeFile file = new MyTubeFile(
		    	Integer.parseInt(obj2.get("id").toString()),
		    	obj2.get("title").toString(),
		    	obj2.get("description").toString(),
		    	Integer.parseInt(obj2.get("owner").toString()),
		    	Integer.parseInt(obj2.get("server").toString())
		    );
		    
			return file;
			
		} catch (Exception e) {
			return null;
		}
    }
    private boolean postFile(MyTubeFile file){
        try {
			if(!statusDB()){
				return false;
			}
        	
            URL url = new URL(WS_PATH + "/file");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");

            JSONObject obj = new JSONObject();

            obj.put("title", file.getTitle());
            obj.put("description", file.getDescription());
            obj.put("owner", file.getOwner());
            obj.put("server", this.id);

            StringWriter out = new StringWriter();
            obj.writeJSONString(out);
            String jsonText = out.toString();  
            
            java.io.OutputStream os = conn.getOutputStream();
            os.write(jsonText.getBytes());
            os.flush();

            int status = conn.getResponseCode();
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			String output = br.readLine();
			conn.disconnect();
            
			int id = Integer.parseInt(output);
            
            if(status == 201){
            	file.setId(id);
            	return true;
            }
            return false;
        } catch (IOException ex) {
            return false;
        }
    }
 	private boolean updateFile(MyTubeFile file){
        try {
			if(!statusDB()){
				return false;
			}
        	
            URL url = new URL(WS_PATH + "/file/"+ file.getId());
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("PUT");
            conn.setRequestProperty("Content-Type", "application/json");

            JSONObject obj = new JSONObject();

            obj.put("title", file.getTitle());
            obj.put("description", file.getDescription());
            
            StringWriter out = new StringWriter();
            obj.writeJSONString(out);
            String jsonText = out.toString();  
            
            java.io.OutputStream os = conn.getOutputStream();
            os.write(jsonText.getBytes());
            os.flush();

            int status = conn.getResponseCode();
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			String output = br.readLine();
			conn.disconnect();
            
			int id = Integer.parseInt(output);
            
            if(status == 200){
            	file.setId(id);
            	return true;
            }
            return false;
        } catch (IOException ex) {
            return false;
        }
 	}
    private boolean deleteFile(int id){
        try {
			if(statusDB()){
	            URL url = new URL(WS_PATH + "/file/"+ id);
	            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
	            conn.setDoOutput(true);
	            conn.setRequestMethod("DELETE");
	            conn.setRequestProperty("Content-Type", "application/json");
	            
	            int status = conn.getResponseCode();
	            if(status == 204){
	            	return true;
	            }
			}
            return false;
        } catch (IOException ex) {
            return false;
        }
    }
 	
    //SERVERS//
 	private List<JSONObject> getServers(){
    	try{
			if(!statusDB()){
				return null;
			}
			
			URL url = new URL(WS_PATH + "/servers");
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
	        conn.setRequestProperty("Content-Type", "application/json");
		
			if(conn.getResponseCode() != 200)
				return new ArrayList<>();
			
			BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			String output = br.readLine();
			conn.disconnect();
			
			JSONParser parser = new JSONParser();
			Object obj = parser.parse(output);
			JSONArray array = (JSONArray)obj;
			
			List<JSONObject> servers = new ArrayList<>();
			for(int i = 0; i < array.size(); i++){
				servers.add((JSONObject) array.get(i));
			}
			
			return servers;
			
		} catch (Exception e) { 
			return null;
		}    
 	}
    private int postServer(){
        try {
			if(!statusDB()){
				return -1;
			}
        	
            URL url = new URL(WS_PATH + "/server");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");

            JSONObject obj = new JSONObject();

            obj.put("address", "rmi://"+ip+":"+port+"/mytube");
            obj.put("status", "true");

            StringWriter out = new StringWriter();
            obj.writeJSONString(out);
            String jsonText = out.toString();  
            
            java.io.OutputStream os = conn.getOutputStream();
            os.write(jsonText.getBytes());
            os.flush();

            int status = conn.getResponseCode();
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			String output = br.readLine();
			conn.disconnect();
            
			int id = Integer.parseInt(output);
			
            if(status == 201){
            	return id;
            }
            return -1;
        } catch (IOException ex) {
            return -1;
        }
    	
    }
    private boolean updateServer(int id, boolean sv_status){
        try {
			if(!statusDB()){
				return false;
			}
        	
            URL url = new URL(WS_PATH + "/server/"+ id);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("PUT");
            conn.setRequestProperty("Content-Type", "application/json");

            JSONObject obj = new JSONObject();

            obj.put("id", id);
            obj.put("address", "rmi://"+ip+":"+port+"/mytube");
            obj.put("status", sv_status);

            StringWriter out = new StringWriter();
            obj.writeJSONString(out);
            String jsonText = out.toString();  
            
            java.io.OutputStream os = conn.getOutputStream();
            os.write(jsonText.getBytes());
            os.flush();

            int status = conn.getResponseCode();
            conn.disconnect();
            
            if(status == 204){
            	return true;
            }
            return false;
        } catch (IOException ex) {
            return false;
        }
    	
    }
    
    ///////////////////////////////////
    /////// CLIENT AUTHENTICATION /////
    ///////////////////////////////////
    private boolean private_login(ClientInterface client) throws RemoteException{
    	if(!global_userExists(client)){
			clients.add(client);
			client.setStatus(true);
			return true;
    	}else{
	    	client.sendMessage("LOGIN: This user is already logged in in the RMI servers.");
    	}
        return false;
    }
    @Override
    public boolean login(ClientInterface client) throws RemoteException {
    	List<String> users = getUsers();
    	
    	String dbUsername = "";
    	for(int i = 0; i < users.size(); i++){
    		if(users.get(i).equals(client.getUsername())){
    			dbUsername = users.get(i);
    			break;
    		}
    	}
    	
    	if(dbUsername.equals("")){
            client.sendMessage("LOGIN: The entered username does not exist.");
    		return false;
    	}
    	
    	User dbuser = getUser(dbUsername);
    	if(!client.getUser().getPassword().equals(dbuser.getPassword())){
            client.sendMessage("LOGIN: Wrong password.");
    		return false;
    	}
    	    	
    	client.setUser(dbuser);
    	if(private_login(client)){
	    	client.sendMessage("LOGIN: Successfully logged in. Welcome "+client.getUsername());
	        System.out.println("LOGIN: The client '"+client.getUsername()+"' just logged in");
	        return true;
    	}
    	return false;
    }
    @Override
    public boolean register(ClientInterface client) throws RemoteException {
    	List<String> users = getUsers();
    	
    	String dbUsername = "";
    	for(int i = 0; i < users.size(); i++){
    		if(users.get(i).equals(client.getUsername())){
				dbUsername = users.get(i);
    			break;
    		}
    	}
    	
    	if(!dbUsername.equals("")){
            client.sendMessage("REGISTER: The entered username is taken.");
    		return false;
    	}
    	
    	client.setId(client.getUsername().hashCode());
    	if(!postUser(client.getUser())){
    		return false;
    	}

        client.sendMessage("REGISTER: Successfully registered. Welcome "+client.getUsername());
        System.out.println("REGISTER: '"+client.getUsername()+"' just registered");
		
        private_login(client);
    	return true;
    }
    @Override
    public void disconnect(ClientInterface client) throws RemoteException {
        if(clients.contains(client)){
            clients.remove(client);
            client.sendMessage("LOGOUT: Successfully logged out.");
            System.out.println("LOGOUT: '"+ client.getUsername() +"' just logged out");
        }
    }

    ///////////////////////
    //////// FILES ////////
    ///////////////////////
    private MyTubeFile getFileById(int id){
    	for(MyTubeFile f : files){
    		if(f.getId() == id){
    			return f;
    		}
    	}
    	return null;
    }
    @Override
    public boolean uploadFile(ClientInterface client, MyTubeFile file) throws RemoteException {
    	if(global_userExists(client)){
        	file.setOwner(client.getId());
        	file.setServer(this.getId());
        	
        	if(postFile(file)){
        		int fid = file.getId();
        		if(fid != -1){
        			saveFile(client, file);
        			files.add(file);
                    global_notifyAll(client.getUsername(), file);

                    System.out.println("FILES: "+client.getUsername()+" uploaded a new file '"+ file.getTitle() +"' ("+ file.getId()+")");
                    client.sendMessage("FILES: File '"+ file.getTitle() +"' with id "+ file.getId() +" uploaded successfully");
                    return true;
        		}
        	}
            client.sendMessage("ERROR: The entered title already belongs to an other file");
            return false;
        }
        client.sendMessage("ERROR: You must login or register first");
        return false;
    }
    @Override
    public boolean deleteFile(ClientInterface client, int id) throws RemoteException{
    	if(global_userExists(client)){
        	MyTubeFile file = getFile(id);
        	if(file != null){
	        	int server_id = file.getServer();
	        	
	        	if(server_id == this.getId()){
	        		if(file.getOwner() == client.getId()){
	        			
	        			if(deleteFile(id)){
	        				MyTubeFile local = getFileById(id);
	        				files.remove(local);
	        				rmFile(local);
	        				System.out.println("FILES: "+client.getUsername()+" deleted his file '"+ file.getTitle() +"' ("+id+") ");
	                        client.sendMessage("FILES: File deleted successfully");
	                        return true;
	        			}
	        			
	        		}
	                client.sendMessage("ERROR: You're not the owner of this file");
	                return false;
	        	}else{
	        		//Redirecting request to other server.
	        		ServerInterface server = getServerById(server_id);
	        		if(server != null){
	        			return server.deleteFile(client, id);
	        		}
	                client.sendMessage("ERROR: The server where this file is located is not online at the moment");
	                return false;
	        	}
        	}
        	client.sendMessage("ERROR: File not found");
        	return false;
        }
        client.sendMessage("ERROR: You must login or register first");
        return false;
    }
    @Override
    public boolean modifyFileTitle(ClientInterface client, int id, String title) throws RemoteException{
    	if(global_userExists(client)){
        	MyTubeFile file = getFile(id);
        	if(file != null){
	        	int server_id = file.getServer();
	        	
	        	file.setTitle(title);
	        	
	        	if(server_id == this.getId()){
	        		if(file.getOwner() == client.getId()){
	        			
	        			if(updateFile(file)){
	        				MyTubeFile local = getFileById(id);
	        				files.remove(local);
	        				files.add(file);
	        				renameFile(local, file);
	        				System.out.println("FILES: "+client.getUsername()+" modified the title of his file '"+ local.getTitle() +"' to '"+ file.getTitle() +"' ("+file.getId()+")");
	                        client.sendMessage("FILES: File title edited successfully");
	                        return true;
	        			}
	        			
	        		}
	                client.sendMessage("ERROR: You're not the owner of this file");
	                return false;
	        	}else{
	        		//Redirecting request to other server.
	        		ServerInterface server = getServerById(server_id);
	        		if(server != null){
	        			return server.modifyFileTitle(client, id, title);
	        		}
	                client.sendMessage("ERROR: The server where this file is located is not online at the moment");
	                return false;
	        	}

	    	}
	    	client.sendMessage("ERROR: File not found");
	    	return false;
        }
        client.sendMessage("ERROR: You must login or register first");
        return false;
    }
    @Override
    public boolean modifyFileDescription(ClientInterface client, int id, String description) throws RemoteException{
    	if(global_userExists(client)){
        	MyTubeFile file = getFile(id);
        	if(file != null){
	        	int server_id = file.getServer();
	        	
	        	file.setDescription(description);
	        	
	        	if(server_id == this.getId()){
	        		if(file.getOwner() == client.getId()){
	        			
	        			if(updateFile(file)){
	        				MyTubeFile local = getFileById(id);
	        				files.remove(local);
	        				files.add(file);
	        				System.out.println("FILES: "+client.getUsername()+" modified the description of his file '"+ file.getTitle() +"' ("+file.getId()+")");
	        				System.out.println("\tFROM: "+ local.getDescription());
	        				System.out.println("\tTO: "+ file.getDescription());
	                        client.sendMessage("FILES: File description edited successfully");
	                        return true;
	        			}
	        			
	        		}
	                client.sendMessage("ERROR: You're not the owner of this file");
	                return false;
	        	}else{
	        		//Redirecting request to other server.
	        		ServerInterface server = getServerById(server_id);
	        		if(server != null){
	        			return server.modifyFileDescription(client, id, description);
	        		}
	                client.sendMessage("ERROR: The server where this file is located is not online at the moment");
	                return false;
	        	}
	    	}
	    	client.sendMessage("ERROR: File not found");
	    	return false;
        }
        client.sendMessage("ERROR: You must login or register first");
        return false;
    }
    @Override
    public List<MyTubeFile> getMyFiles(ClientInterface client) throws RemoteException {
    	if(userExists(client)){
        	List<MyTubeFile> allfiles = getFiles();
        	List<MyTubeFile> userfiles = new ArrayList<>();
        	
        	if(allfiles != null && allfiles.size() != 0){
        		for(MyTubeFile f : allfiles){
        			if(client.getId() == f.getOwner()){
        				userfiles.add(f);
        			}
        		}
        		if(userfiles.size() != 0){
        			return userfiles;
        		}
        	}
        	client.sendMessage("FILES: You have not uploaded any file yet.");
            return userfiles;
        }
        client.sendMessage("ERROR: You must login or register first");
        return null;
    } 
    public boolean downloadFile(ClientInterface client, int id) throws RemoteException{
        if(global_userExists(client)){
        	MyTubeFile file = getFile(id);
        	if(file != null){
	        	int server_id = file.getServer();
	        	
	        	if(server_id == this.getId()){
	           		byte[] content;
					try {
						content = readFile(file);
						if(client.writeFile(content, getFSFileName(file))){
							System.out.println("FILES: "+client.getUsername()+" downloaded the file '"+file.getTitle()+"' ("+file.getId()+")");
							client.sendMessage("FILES: File downloaded successfully.");
							return true;
						}else{
							client.sendMessage("FILES: Couldn't download that file.");
							return false;
						}
					} catch (IOException e) {
						client.sendMessage("ERROR: Couldn't read the file");
						return false;
					}
					
	        	}else{
	        		//Redirecting request to other server.
	        		ServerInterface server = getServerById(server_id);
	        		if(server != null){
	        			return server.downloadFile(client, id);
	        		}
	                client.sendMessage("ERROR: The server where this file is located is not online at the moment");
	                return false;
	        	}

	    	}
	    	client.sendMessage("ERROR: File not found");
	    	return false;
        }
        client.sendMessage("ERROR: You must login or register first");
        return false;
    	
    }
    public ArrayList<MyTubeFile> findFilesByTags(ClientInterface client, String tags) throws RemoteException{
    	if(userExists(client)){
        	List<MyTubeFile> files = getFiles();
            ArrayList<MyTubeFile> result = new ArrayList<>();
            
        	if(files != null && files.size() != 0){
        		//Filtering
                for(MyTubeFile f : files){
                    f.matchesDescription(tags);
                    if(f.matches > 0){
                        result.add(f);
                    }
                }
                
                if(result.size() == 0){
                	client.sendMessage("FILES: No files found by search: "+ tags);
                	return null;
                }
                
                
        		//Sorting
                ArrayList<MyTubeFile> sortedResult = new ArrayList<>();
                int maxMatches;
                MyTubeFile maxFile;

                while(!result.isEmpty()){
                	maxMatches = 0;
                	maxFile = null;
                	
                	//Getting max
                	for(MyTubeFile f: result){
                		if(f.matches >= maxMatches){
                			maxMatches = f.matches;
                			maxFile = f;
                		}                		
                	}
                	
                	//Taking maxFile
                	sortedResult.add(maxFile);
                	result.remove(maxFile);
                }
                
                
                
                return sortedResult;
        	}
        	client.sendMessage("FILES: There are not files uploaded yet");
            return result;
    	}
        client.sendMessage("ERROR: You must login or register first");
        return null;
    }

    ///////////////////////////////
    //CLIENT UPLOAD NOTIFICATIONS//
    ///////////////////////////////
    @Override
    public void notifyAll(String username, MyTubeFile file) throws RemoteException{
    	for(ClientInterface client : clients){
    		if(username != client.getUsername()){
    			client.sendMessage("UPLOADS: New file '"+ file.getTitle() +"' with id "+ file.getId() +" uploaded by "+ username);
    		}
    	}
    }
    private void global_notifyAll(String username, MyTubeFile file) throws RemoteException{
    	this.notifyAll(username, file);
    		
    	for(ServerInterface s : servers){
    		s.notifyAll(username, file);
    	}
    }
        
	//////////////////////////////////////////////
	// SERVERS USER EXISTS (isClientLoggedIn?) //
	//////////////////////////////////////////////
    @Override
    public boolean userExists(ClientInterface client) throws RemoteException {
    	if(clients.contains(client))
    		return true;
    		
    	for(ClientInterface c : clients){    			
    		if(c.getId() == id)
    			return true;
    	}
    	return false;
    }
    private boolean global_userExists(ClientInterface client) throws RemoteException {
    	if(this.userExists(client))
    		return true;
    		
    	for(ServerInterface s : servers){
    		if(s.userExists(client))
    			return true;
    	}
    	return false;
    }
    
}
