package rmi;

import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ClientInterface extends Remote{
    
	public void clearMessage() throws RemoteException;
	public void sendMessage(String msg) throws RemoteException;
	public String getMessage() throws RemoteException;
	
    public void setError(boolean error, String msg) throws RemoteException;
    public boolean getErrorStatus() throws RemoteException;
    public String getErrorMsg() throws RemoteException;
    
    public void setStatus(boolean status) throws RemoteException;
    public boolean getStatus() throws RemoteException;
    
    public byte[] readFile(String relpath) throws RemoteException, IOException;
    public boolean writeFile(byte[] content, String relpath) throws RemoteException;
    
    public void setUser(User user) throws RemoteException;
    public User getUser() throws RemoteException;
    public String getUsername() throws RemoteException;
    public void setId(int id) throws RemoteException;
    public int getId() throws RemoteException;
}
