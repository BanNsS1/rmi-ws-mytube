package myRESTwsWeb;

import java.io.Serializable;

public class MyTubeFile implements Serializable{
	private int id;
    private String title;
    private String description;
    private int owner;
    private int server;
	
    public int matches;
    private String filename;
    
	//Client constructor
	public MyTubeFile(String title, String description, String filename){
		this.title = title;
		this.description = description;
		this.filename = filename;
		
		this.id = -1;
		this.owner = -1;
		this.server = -1;
	}
	
	//WS Constructor
    public MyTubeFile(int id, String title, String description, int owner, int server){
    	this.id = id;
    	this.title = title;
    	this.description = description;
    	this.owner = owner;
    	this.server = server;
    	
		this.filename = "";
    }
    
    public int matchesDescription(String description){
        String[] fdesc = this.description.split("\\s");
        String[] cdesc = description.split("\\s");
        
        matches = 0;
        
        //counts matching words
        for(String word1 : fdesc){
            for(String word2 : cdesc){
                if(word1.equals(word2)){
                    matches++;
                }
            }
        }        
        return matches;
    }
    
    //SETTERS
    public void setId(int id){
    	this.id = id;
    }
    public void setTitle(String title){
        this.title = title;
    }
    public void setDescription(String description){
        this.description = description;
    }
	public void setOwner(int owner){
        this.owner = owner;
    }
	public void setServer(int server){
		this.server = server;
	}
        
    //GETTERS
	public int getId(){
        return this.id;
    }
	
	public String getTitle(){
        return this.title;
    }
    
    public String getDescription(){
        return this.description;
    }
    
    public int getOwner(){
        return this.owner;
    }
	
	public int getServer(){
    	return this.server;
    }
	
    public String getFilename(){
        return this.filename;
    }
    
    public String toString(){
    	String str = "MyTubeFile {";
    	str += "\n\t id = '"+ id +"',";
    	str += "\n\t title = '"+ title +"',";
    	str += "\n\t description = '"+ description +"',";
    	str += "\n\t owner = '"+ owner +"',";
    	str += "\n\t server = '"+ server +"',";
    	str += "\n\t filename = '"+ filename +"',";
    	str += "\n\t matches = '"+ matches +"',";
    	str += "\n}";
    	return str;
    }
}