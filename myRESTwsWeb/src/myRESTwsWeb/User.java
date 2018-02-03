package myRESTwsWeb;

import java.io.Serializable;

public class User implements Serializable{
	private int id;
	private String username;
	private final String password;
	
	public User(String username, String password){
		this.username = username;
		this.password = password;
	}
	
	//Getters
	public int getId(){
		return this.id;
	}
	
	public void setId(int id){
		this.id = id;
	}
	
	public String getUsername(){
		return this.username;
	}
	
	public void setUsername(String username){
		this.username = username;
	}
	
	public String getPassword(){
		return this.password;
	}

}
