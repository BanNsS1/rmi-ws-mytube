package rmi;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;

public class ClientImplementation extends UnicastRemoteObject implements ClientInterface{
    private User user;
    private boolean connected;
    private boolean error;
    private String error_msg;
    private String msg;
    private String save_path;
    
    public ClientImplementation(String save_path) throws RemoteException{
        super();
        this.connected = false;
        this.error = false;
        this.error_msg = "";
        this.msg = "";
        this.save_path = save_path;
        
        new File(save_path).mkdir();
    }
    
    @Override
    public void setError(boolean error, String msg) throws RemoteException{
    	this.error = error;
    	this.error_msg = msg;
    }

    @Override
    public boolean getErrorStatus() throws RemoteException{
    	return this.error;
    }
    
    @Override
    public String getErrorMsg() throws RemoteException{
    	return this.error_msg;
    }
    
    @Override
    public void setStatus(boolean status) throws RemoteException{
    	this.connected = status;
    }
    
    @Override
    public boolean getStatus() throws RemoteException{
    	return this.connected;
    }

    @Override
    public void clearMessage() throws RemoteException{
    	this.msg = "";
    }
    
    @Override
    public void sendMessage(String msg) throws RemoteException {
        this.msg = msg;
    }
    
    @Override
    public String getMessage() throws RemoteException{
    	return this.msg;
    }
    
    @Override
    public byte[] readFile(String relpath) throws RemoteException, IOException{
        String path = new File("").getAbsolutePath();
        return Files.readAllBytes(Paths.get(path +"/"+ relpath));
    }
    
    @Override
    public boolean writeFile(byte[] content, String relpath) throws RemoteException{
        try{
            String filepath = save_path +"/"+ relpath;

            FileOutputStream fos = new FileOutputStream(filepath);
            fos.write(content);
            fos.close();
            return true;
        }catch(IOException e){
            return false;
        }
    }
    
    @Override
    public void setUser(User user) throws RemoteException{
    	this.user = user;
    }
    
    @Override
    public User getUser() throws RemoteException{
    	return this.user;
    }

    @Override
    public String getUsername() throws RemoteException {
        return this.user.getUsername();
    }
    
    @Override
    public void setId(int id) throws RemoteException {
        this.user.setId(id);
    }
    
    @Override
    public int getId() throws RemoteException {
        return this.user.getId();
    }
}
