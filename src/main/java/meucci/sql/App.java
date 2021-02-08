/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package meucci.sql;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import meucci.webserver.User;
/**
 *
 * @author Majid Alessio
 */
public class App 
{
    public static void main( String[] args )
    {
      System.out.println("testing...");
    	// Estabilish connection to the SQL Database
        Connection con = null;
        try {
			con = DriverManager.getConnection("jdbc:mysql://localhost:3306/web_server?serverTimezone=UTC","root","telecomandonero");
        
		// Create statement and ResultSet from given query
        Statement stmt=con.createStatement();  
        ResultSet rs=stmt.executeQuery("SELECT nome,cognome FROM user");  
        
        // List with Java Objects
    	List<User> list = new ArrayList<User>();

    	// Fetch each row from the result set
    	while (rs.next()) {
    	  String nome = rs.getString("nome");
    	  String cognome = rs.getString("cognome");

    	  // Create user object to pass into the list
    	  User user = new User(nome, cognome);

    	  list.add(user);
    	}
    	
    	// Serialization/deserialization
    	ObjectMapper objectMapper = new ObjectMapper();
        XmlMapper xmlMapper = new XmlMapper();
        
        // Create both XML and JSON files from the Java List
    	try {
			objectMapper.writeValue(new File("target/userList.json"), list);
	        xmlMapper.writeValue(new File("target/userList.xml"), list);
		} catch (JsonGenerationException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
    	
    	// Print the query results
    	for (int i = 0; i < list.size(); i++) {
        	System.out.println(list.get(i).getNome() + " " + list.get(i).getCognome());
    	}
        
    	// Close the database connection
        con.close();
        
		} catch (SQLException e) {
			e.printStackTrace();
		}
    }
}