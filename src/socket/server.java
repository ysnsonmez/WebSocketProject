package socket;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.websocket.EncodeException;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

@ServerEndpoint("/chat") 
public class server extends Thread
{ 
	//DB
	Connection connect=null;
    PreparedStatement pr=null;
    String dbPath="jdbc:mysql://localhost/";
    String dbName="mysql";
    String dbUserName="root";
    String dbuserpassword=""; 
    
    public PreparedStatement conn(String query)
    {
        try {
           Class.forName("com.mysql.jdbc.Driver"); 
           connect=DriverManager.getConnection(dbPath + dbName,dbUserName,dbuserpassword);
           pr=connect.prepareStatement(query);
        }catch (Exception e) {
        	System.out.println("connect error" +e);
        }
        return pr;
    }
    
    public void connClose()
    {
        try {
            pr.close();
        } catch (Exception e) {
            System.out.println("connect close error :" +e);
        } 
    }
    
	public static final Set<Session> clients = Collections.synchronizedSet(new HashSet<Session>());
	
	String link = ""
	    		+ "<!DOCTYPE html><html>"
	    		+ "<head>"
	    		+ "<link href=\"operatorPageShow\">"
	    		+ "</head><body></body></html>";
	
	@OnOpen
    public void onOpen(Session session) {
        clients.add(session);
    } 
	
    @OnMessage
    public void onMessage(String message, Session client) throws IOException, EncodeException{
     	
        String username = (String) client.getUserProperties().get("username");
        String[] nameAndpass=null;
        String[] signupNameandPass=null;
         
        if (username == null)
        {
            try{
            	
            	signupNameandPass=message.split(" ",4);
	        	if(signupNameandPass[0].equals("*"))
	        	{
	            	System.out.println("kayıttayım"+signupNameandPass[0]+"|"+signupNameandPass[1]+"|"+signupNameandPass[2]+"|"+signupNameandPass[3]);
	        		
	    			PreparedStatement pr=conn("insert into users (username,password) values (?,?)");
	                pr.setString(1, signupNameandPass[1]);
	                pr.setString(2, signupNameandPass[2]);
	                pr.executeUpdate();
	                pr.close();
	                
	            	client.getUserProperties().put("username", signupNameandPass[1]);
	                client.getUserProperties().put("password",signupNameandPass[2]);
		            client.getBasicRemote().sendText("+"); 
	        	} 
            }
            catch(Exception e)
            {
            	System.out.println("Registration Error !");
            }
        	 
            // query initialize
            try {
            	
            	nameAndpass=message.split(" ", 2);
            	client.getUserProperties().put("username", nameAndpass[0]);
                client.getUserProperties().put("password",nameAndpass[1]);
            	
    	        PreparedStatement pr = conn("select * from users");
    	        
    			ResultSet rs=pr.executeQuery();
    			int flag=0;
    			
    			while(rs.next())
    			{                 	
    				//check db column name
    				if(nameAndpass[0].equals(rs.getString("username")) && nameAndpass[1].equals(rs.getString("password")))
    				{ 
    					System.out.println(" into first if "+rs.getString("username")+" "+rs.getString("password"));
    		            client.getBasicRemote().sendText(link);
    		            flag++;
    		            break;    
    		        }   
                 }
    			 
                 pr.close();
    				 
    		}
            catch (SQLException e) {
    			 System.out.println("query error :" +e);
    		} 
        }
        else 
        { 
        	clients.iterator();
            
        	synchronized(clients)
        	{
                 for(Session Client : clients)
                 {
                  if (!Client.equals(client))
                  {
                	  String text = username + " : " + message;
                	  Client.getBasicRemote().sendText(text);
                  }
                }
             }
        } 
    }  
    
    @OnClose
    public void onClose(Session session) {
        clients.remove(session);
    } 
}
