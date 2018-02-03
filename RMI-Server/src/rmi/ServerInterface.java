package rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import rmi.ClientInterface;
import rmi.MyTubeFile;

public interface ServerInterface extends Remote{
       
    //OTHER
    public int getId() throws RemoteException;
    public String getAdrress() throws RemoteException;
    
    //SERVER STATUS
    public boolean recoverServer() throws RemoteException;
    public void registerServer() throws RemoteException;
    public void serverDisconnect() throws RemoteException;
    
    //SERVERS COMMUNICATION
    public void connectToOtherServers() throws RemoteException;
    public boolean serverConnectTo(ServerInterface from, String address, int id) throws RemoteException;
    public void serverDisconnectFrom(ServerInterface server) throws RemoteException;
    
    //WS
    public boolean statusWS() throws RemoteException;
    public boolean statusDB() throws RemoteException;
    
    //CHECK IF CLIENT IS LOGGED IN IN AN OTHER SERVER
    public boolean userExists(ClientInterface client) throws RemoteException;
    //NOTIFICATION
    public void notifyAll(String username, MyTubeFile file) throws RemoteException;
    
    //CLIENT/SERVER INTERACTION FILES
    public boolean uploadFile(ClientInterface client, MyTubeFile file) throws RemoteException;
    public boolean deleteFile(ClientInterface client, int id) throws RemoteException;
    public boolean modifyFileTitle(ClientInterface client, int id, String title) throws RemoteException;
    public boolean modifyFileDescription(ClientInterface client, int id, String description) throws RemoteException;
    public List<MyTubeFile> getMyFiles(ClientInterface client) throws RemoteException;
    public boolean downloadFile(ClientInterface client, int id) throws RemoteException;
    public ArrayList<MyTubeFile> findFilesByTags(ClientInterface client, String tags) throws RemoteException;
    
    //CLIENT AUTHENTICATION
    public boolean login(ClientInterface client) throws RemoteException;
    public boolean register(ClientInterface client) throws RemoteException;
    public void disconnect(ClientInterface client) throws RemoteException;
    
}  