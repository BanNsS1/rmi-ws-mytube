package myRESTwsWeb;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import javax.enterprise.context.RequestScoped;
import javax.naming.InitialContext;
import javax.sql.DataSource;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.json.simple.JSONObject;

@RequestScoped
@Path("")
@Produces({ "application/xml", "application/json" })
@Consumes({ "application/xml", "application/json" })
public class WebService {
	Statement statement;
	Connection connection;
	
	/* ERRORS */
	private Response dbError(SQLException e){
		System.out.println(e.toString());
		dbDisconnect();
		return Response.status(500).entity("Database error.").build();
	}
	
	/* DB Connection */
	public void dbConnect(){
		try {
			InitialContext cxt = new InitialContext();
			DataSource ds = (DataSource) cxt.lookup("java:/PostgresXADS");
			connection = ds.getConnection();
			statement = connection.createStatement();
		} catch (Exception e) {
			System.out.println("Couldn't establish a connection to the database. "+ e.toString());
			e.printStackTrace();
		}
	}
	
	public void dbDisconnect(){
		try {
			statement.close();
			connection.close();
		} catch (SQLException e) {
			
		}
	}
	
	///////////////////////////////////
	////////////// STATUS //////////////
	///////////////////////////////////	
	/* GET WS Status */ 
	@GET
	@Path("/status/ws")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getStatusWS(){
		return Response.status(200).entity("WS is working!").build();
	}
	
	/* GET DB Status */ 
	@GET
	@Path("/status/db")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getStatusDB(){	 
		try {
			dbConnect();
			statement.executeQuery("SELECT username FROM users WHERE username = ''");
			dbDisconnect();
			return Response.status(200).entity("Database is working!").build();
			
		} catch (SQLException e) {
			return Response.status(503).entity("Database is not available :(").build();
		}
	}
	
	///////////////////////////////////
	///////////// SERVERS /////////////
	///////////////////////////////////
	
	/* GET: Servers (online servers only) */ 
	@GET
	@Path("/servers")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getServers(){	 
		try {
			dbConnect();
			ResultSet rs = statement.executeQuery("SELECT * FROM servers WHERE status = 'true'");
			List<JSONObject> servers = new ArrayList<>();
			
			while(rs.next()){
	            JSONObject obj = new JSONObject();

	            obj.put("id", rs.getString("id"));
	            obj.put("address", rs.getString("address"));
	            obj.put("status", Boolean.toString(rs.getBoolean("status")));
				
				servers.add(obj);
			}
			dbDisconnect();
			return Response.status(200).entity(servers).build();
			
		} catch (SQLException e) {
			return this.dbError(e);
		}
	}
	
	/* GET: Get Server by id */ 
	@GET
	@Path("/server/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getServer(@PathParam("id") String id){	 
		try {
			dbConnect();
			ResultSet rs = statement.executeQuery("SELECT id,address,status FROM servers "
					+ "WHERE id='" + id + "';");
			
			if(!rs.isBeforeFirst())
				return Response.status(404).entity("Server not found").build();

			rs.next();
			
            JSONObject server = new JSONObject();
            
            server.put("id", rs.getString("id"));
            server.put("address", rs.getString("address"));
            server.put("status", rs.getBoolean("status"));
			

			dbDisconnect();

			return Response.status(200).entity(server).build();
			
		} catch (SQLException e) {
			return this.dbError(e);
		}
	}
	
	/* POST: Create a Server*/
	@POST
	@Path("/server")
	public Response serverRegister(JSONObject obj){
		try {
			String address = obj.get("address").toString();
			String status = obj.get("status").toString();

			dbConnect();
			
			//We don't know how to set AUTOINCREMENT in postgres.
			int id = 1;
			boolean idFree = false;
			while(!idFree){
				ResultSet rs = statement.executeQuery("SELECT id FROM servers "
						+ "WHERE id='" + id + "';");
				
				//checking id is free
				if (rs.isBeforeFirst())
					id += 1;
				else
					idFree = true;
			}
			
			statement.executeUpdate("INSERT INTO servers(id,address,status) VALUES("
				+ "'" + id + "'," 
				+ "'" + address + "'," 
				+ "'" + status +"');", Statement.RETURN_GENERATED_KEYS);

			dbDisconnect();
			return Response.status(201).entity(Integer.toString(id)).build();
			
		} catch (SQLException e) {
			return this.dbError(e);
		}
	}	
	
	/* PUT: Update a Server */
	@PUT
	@Path("/server/{id}")
	public Response updateServer(@PathParam("id") String id, JSONObject obj){
		try{
			String nid = obj.get("id").toString();
			String address = obj.get("address").toString();
			String status = obj.get("status").toString();

			dbConnect();
			statement.executeUpdate("UPDATE servers SET "
					+ "id = '" + nid + "',"
					+ "address = '" + address + "',"
					+ "status = '" + status + "'"
					+ " WHERE id = '" + id + "';");
			dbDisconnect();
			return Response.status(204).build();
		}catch(SQLException e){
			return this.dbError(e);
		}
	}
	
	
	
	///////////////////////////////////
	////////////// USERS //////////////
	///////////////////////////////////	
	/* GET: Users (username) */ 
	@GET
	@Path("/users")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getUsers(){	 
		try {
			dbConnect();
			ResultSet rs = statement.executeQuery("SELECT username FROM users");
			List<String> users = new ArrayList<>();
			String name;
			while(rs.next()){		
				name = rs.getString("username");
				users.add(name);
			}
			dbDisconnect();
			return Response.status(200).entity(users).build();
			
		} catch (SQLException e) {
			return this.dbError(e);
		}
	}
	
	/* POST: Create an User */
	@POST
	@Path("/user")
	public Response createUser(JSONObject obj){
		try {
			String id = obj.get("id").toString();
			String username = obj.get("username").toString();
			String password = obj.get("password").toString();

			dbConnect();
			ResultSet rs = statement.executeQuery("SELECT username FROM users "
					+ "WHERE username='" + username + "';");
			
			if (rs.isBeforeFirst())
				return Response.status(409).entity("Username already in use!").build();

			//MD5 encryption
			//REMOVED BECAUSE THIS STEP IS BEING DONE IN THE RMI.
			//CLIENT will encrypt it so there is not plain passwords
			//sent through the network.
			
			statement.executeUpdate("INSERT INTO users(id,username,password) VALUES("
							+ "'" + id + "',"
							+ "'" + username + "'," 
							+ "'" + password + "');");

			dbDisconnect();
			return Response.status(201).build();
			
		} catch (SQLException e) {
			return this.dbError(e);
		}
	}
	
	/* GET: Get User by Username */ 
	@GET
	@Path("/user/{username}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getUser(@PathParam("username") String username){	 
		try {
			dbConnect();
			ResultSet rs = statement.executeQuery("SELECT id,username,password FROM users "
					+ "WHERE username='" + username + "';");
			
			if(!rs.isBeforeFirst())
				return Response.status(404).entity("User not found").build();

			rs.next();
			User user = new User(rs.getString("username"), rs.getString("password"));
			user.setId(Integer.parseInt(rs.getString("id")));
			dbDisconnect();

			return Response.status(200).entity(user).build();
			
		} catch (SQLException e) {
			return this.dbError(e);
		}
	}
	

	///////////////////////////////////
	////////////// FILES //////////////
	///////////////////////////////////
	/* GET Files */
	@GET
	@Path("/files")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getFiles(){
		try{
			dbConnect();
			ResultSet rs = statement.executeQuery(
				"SELECT id,title,description,owner,server FROM files"
			);
			List<MyTubeFile> files = new ArrayList<>();
			while(rs.next()){	
				files.add(
					new MyTubeFile(
						Integer.parseInt(rs.getString("id")),
						rs.getString("title"),
						rs.getString("description"),
						Integer.parseInt(rs.getString("owner")),
						Integer.parseInt(rs.getString("server"))
					)
				);
			}
			dbDisconnect();
			return Response.status(200).entity(files).build();
			
		} catch (SQLException e) {
			return this.dbError(e);
		}
	}
	
	/* POST File */
	@POST
	@Path("/file")
	public Response createFile(JSONObject obj){
		try {
			String title = obj.get("title").toString();
			String description = obj.get("description").toString();
			String owner = obj.get("owner").toString();
			String server = obj.get("server").toString();
			

			dbConnect();
			int id = title.hashCode();
			String sid = Integer.toString(id);
			
			statement.executeUpdate(
				"INSERT INTO files (id,title,description,owner,server) VALUES ("+
				"'" + sid +"',"+
				"'" + title +"',"+
				"'" + description +"',"+
				"'" + owner +"',"+
				"'" + server +"');"
			);
			dbDisconnect();
			return Response.status(201).entity(sid).build();
			
		} catch (SQLException e) {
			return this.dbError(e);
		}
	}
	
	/* GET File by Id*/
	@GET
	@Path("/file/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getFileById(@PathParam("id") String id){	 
		try {
			dbConnect();
			ResultSet rs = statement.executeQuery(
				"SELECT title,description,owner,server "+
				"FROM files WHERE id = '" + id + "';"
			);
			
			if(!rs.isBeforeFirst())
				return Response.status(404).entity("File not found").build();
			
			rs.next();
			MyTubeFile file = new MyTubeFile(
				Integer.parseInt(id),
				rs.getString("title"),
				rs.getString("description"),
				Integer.parseInt(rs.getString("owner")),
				Integer.parseInt(rs.getString("server"))
			);
			dbDisconnect();
			return Response.status(200).entity(file).build();
			
		} catch (SQLException e) {
			return this.dbError(e);
		}
	}
	
	/* PUT: Update file by Id*/
	@PUT
	@Path("/file/{id}")
	public Response updateFile(@PathParam("id") String id, JSONObject obj){
		try{
			String title = obj.get("title").toString();
			String description = obj.get("description").toString();
			String nid = Integer.toString(title.hashCode());

			dbConnect();
			statement.executeUpdate("UPDATE files SET "
					+ "id = '" + nid + "',"
					+ "title = '" + title + "',"
					+ "description = '" + description + "'"
					+ " WHERE id = '" + id + "';");
			dbDisconnect();
			return Response.status(200).entity(nid).build();
		}catch(SQLException e){
			return this.dbError(e);
		}
	}
	
	/* DELETE: File by Id*/
	@DELETE
	@Path("/file/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response deleteVideo(@PathParam("id") String id){
		try{
			dbConnect();
			statement.executeUpdate("DELETE FROM files WHERE id = '" + id + "';");
			dbDisconnect();
			return Response.status(204).build();
			
		}catch(SQLException e){
			return this.dbError(e);
		}
	}
}