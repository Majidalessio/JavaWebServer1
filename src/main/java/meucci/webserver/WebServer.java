package meucci.webserver;
/**
 * small java web server made in order to try some HTTP returning codes.
 * the class generates a new thread for each connection. 
 * @author Majid Alessio
 */
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

public class WebServer implements Runnable { 	
    static final File standardFile = new File("");
    static final String PATH = standardFile.getAbsolutePath();
    static final File WEB_ROOT = new File (PATH + "\\src\\main\\java\\meucci\\websources");
    static final int PORT = 8080;
    static final String METHOD_NOT_SUPPORTED = "not_supported.html";
    static final String FILE_MOVED = "301.html";
    static final String DEFAULT_FILE = "index.html";
    static final String FILE_NOT_FOUND = "404.html";
    static final boolean verbose = true;
    private Connection con;
    private Statement stmt;
    private Socket SC;
    
    /**
     * constructor method
     * @param c 
     */
    public WebServer(Socket c) 
    {
            SC = c;
    }

    /**
     * Main method
     * @param args 
     */
    public static void main(String[] args) 
    {
        try 
        {
            ServerSocket serverConnect = new ServerSocket(PORT);
            System.out.println("Server started.\nListening for connections on port : " + PORT + " ...\n");
            
            while (true)      // Always true, a loop to check for incoming connections
            {
                WebServer myServer = new WebServer(serverConnect.accept());
                if (verbose) 
                {
                    System.out.println("Connecton opened. (" + new Date() + ")");
                }
                
                // Once a user connects, create and
                // run a new thread
                Thread thread = new Thread(myServer);
                thread.start();
            }
            
        } 
        catch (IOException e) 
        {
            System.err.println("Server Connection error : " + e.getMessage());
        }
    }
    
    /**
     * Thread Run method
     */
    public void run() 
    {
        BufferedReader in = null;
        PrintWriter out = null;
        BufferedOutputStream dataOut = null;
        String fileRequested = null;
        
        // Serialization/deserialization
        ObjectMapper objectMapper = new ObjectMapper();
        XmlMapper xmlMapper = new XmlMapper();
        
        // Database connection
        try {
			this.con = DriverManager.getConnection("jdbc:mysql://localhost:3306/quintab_condominio?serverTimezone=UTC","root","passwordprova1");
			this.stmt = con.createStatement();  
		} catch (SQLException e1) {
			e1.printStackTrace();
		}
        
        
        try 
        {
        	// Input and Output
            in = new BufferedReader(new InputStreamReader(SC.getInputStream()));
            out = new PrintWriter(SC.getOutputStream());
            dataOut = new BufferedOutputStream(SC.getOutputStream());

            String input = in.readLine();
            StringTokenizer parse = new StringTokenizer(input);
            String method = parse.nextToken().toUpperCase(); 

            fileRequested = parse.nextToken().toLowerCase();
            if (!method.equals("GET") && !method.equals("HEAD")) 
            {
                if (verbose) 
                {
                    System.out.println("501 Not Implemented : " + method + " method.");
                }
                
                
                File file = new File(WEB_ROOT, METHOD_NOT_SUPPORTED);
                int fileLength = (int) file.length();
                String contentMimeType = "text/html";
                byte[] fileData = readFileData(file, fileLength);
                
                out.println("HTTP/1.1 501 Not Implemented");
                out.println("Server: Java HTTP Server from SSaurel : 1.0");
                out.println("Date: " + new Date());
                out.println("Content-Type: " + contentMimeType);
                out.println("Content-Length: " + fileLength);
                out.println();
                out.flush();
                
                dataOut.write(fileData, 0, fileLength);
                dataOut.flush();
            } 
            else 
            {
            	// Check ending character
                if (fileRequested.endsWith("/")) 
                {
                    fileRequested += DEFAULT_FILE;
                }
                
                File file = new File(WEB_ROOT, fileRequested);
                int fileLength = (int) file.length();
                String content = getContentType(fileRequested);
                
                if (fileRequested.contains("puntiVendita.xml")) {
                	
                	String jsonString = null;
                	
                	try {
						jsonString = readFileAsString("resources/puntiVendita.json");
					} catch (Exception e) {
						e.printStackTrace();
					}
                	
                	List<PuntoVendita> listPuntiVendita = objectMapper.readValue(jsonString, new TypeReference<List<PuntoVendita>>(){});
                	ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    xmlMapper.writeValue(byteArrayOutputStream, listPuntiVendita);
                    
                    file = new File("puntiVendita.xml");
                    fileLength = (int) file.length();
                    content = "text/xml";
                }
                
                if (fileRequested.endsWith("/db/json")) {
                	// Base ResultSet
                    ResultSet rs;
                    
					try {
						// Fill ResultSet with database information
						rs = stmt.executeQuery("SELECT nome,cognome FROM user");
	                	file = this.resultSetToJson(rs);
					} catch (SQLException e) {
						e.printStackTrace();
					} 
					
					fileLength = (int) file.length();
					content = "application/json";
                }
                
                if (fileRequested.endsWith("/db/xml")) {
                	// Base ResultSet
                    ResultSet rs;
                    
					try {
						// Fill ResultSet with database information
						rs = stmt.executeQuery("SELECT nome,cognome FROM user");
	                	file = this.resultSetToXml(rs);
					} catch (SQLException e) {
						e.printStackTrace();
					} 
					
					fileLength = (int) file.length();
					content = "text/xml";
                }

                
                if (method.equals("GET"))
                { 
                    byte[] fileData = readFileData(file, fileLength);
                    
                    // HTTP Response
                    out.println("HTTP/1.1 200 OK");
                    out.println("Server: Java HTTP Server from SSaurel : 1.0");
                    out.println("Date: " + new Date());
                    out.println("Content-Type: " + content);
                    out.println("Content-Length: " + fileLength);
                    out.println();
                    out.flush();
                    dataOut.write(fileData, 0, fileLength);
                    dataOut.flush();
                }
                if (verbose) 
                {
                    System.out.println("File " + fileRequested + " of type " + content + " returned");
                }
            }
        } 
        // Exception if the file does not exist
        catch (FileNotFoundException fnfe) 
        {
            try 
            {
            	// String array
                String[] path = fileRequested.split("/");
                int nPath = path.length;
                String choosenObj = path[nPath-1];
                if (choosenObj.lastIndexOf(".") == -1)
                {
                    movedPermanently(out,dataOut,fileRequested + "/");
                }
                else
                {
                    fileNotFound(out, dataOut, fileRequested);
                }
            } 
            catch (IOException ioe) 
            {
                System.err.println("Exception generated in input/output: " + ioe.getMessage());
            }
        }
        catch (IOException ioe)
        {
            System.err.println("Server error : " + ioe);
        }
        finally
        {
            try 
            {
                in.close();
                out.close();
                dataOut.close();
                SC.close();
            } 
            catch (Exception e) 
            {
                System.err.println("Error closing stream : " + e.getMessage());
            } 

            if (verbose) 
            {
                System.out.println("Connection closed.\n");
            }
        }
    }
    
    /**
     * Reads data from a file
     * @param file
     * @param fileLength
     * @return
     * @throws IOException 
     */
    private byte[] readFileData(File file, int fileLength) throws IOException
    {
        FileInputStream fileIn = null;
        byte[] fileData = new byte[fileLength];
        try 
        {
            fileIn = new FileInputStream(file);
            fileIn.read(fileData);
        } 
        finally 
        {
            if (fileIn != null)
            {
                fileIn.close();
            }
        }
        return fileData;
    }

    /**
     * Returns supported file types
     * @param fileRequested
     * @return 
     */
    private String getContentType(String fileRequested) 
    {
        if (fileRequested.endsWith(".htm")  ||  fileRequested.endsWith(".html"))
        {
            return "text/html";
        }
        else if (fileRequested.endsWith(".png"))
        {
            return "image/png";
        }
        else
        {
            return "text/plain";
        }
    }
    
    /**
     * Prints HTTP response 404
     * @param out
     * @param dataOut
     * @param fileRequested
     * @throws IOException 
     */
    private void fileNotFound(PrintWriter out, OutputStream dataOut, String fileRequested) throws IOException 
    {
        File file = new File(WEB_ROOT, FILE_NOT_FOUND);
        int fileLength = (int) file.length();
        String content = "text/html";
        byte[] fileData = readFileData(file, fileLength);
        
        // HTTP Request 404
        out.println("HTTP/1.1 404 File Not Found");
        out.println("Server: Java HTTP Server from SSaurel : 1.0");
        out.println("Date: " + new Date());
        out.println("Content-type: " + content);
        out.println("Content-length: " + fileLength);
        out.println();
        out.flush();
        dataOut.write(fileData, 0, fileLength);
        dataOut.flush();
        if (verbose)
        {
            System.out.println("File " + fileRequested + " not found");
        }
    }
    
    /**
     * Prints HTTP response 301
     * @param out
     * @param dataOut
     * @param directoryRequested
     * @throws IOException 
     */
    private void movedPermanently(PrintWriter out, OutputStream dataOut, String directory) throws IOException
    {
        File file = new File(WEB_ROOT, FILE_MOVED);
        int fileLength = (int) file.length();
        String content = "text/html";
        byte[] fileData = readFileData(file, fileLength);
        
        // Creation of the 301 response
        out.println("HTTP/1.1 301 Moved Permanently");
        out.println("Server: Java HTTP Server from SSaurel : 1.0");
        out.println("Date: " + new Date());
        out.println("Content-Type: " + content);
        out.println("Content-Length: " + fileLength);
        out.println("Location: " + directory);
        out.println();
        out.flush();
        dataOut.write(fileData, 0, fileLength);
        dataOut.flush();
        if (verbose) 
        {
            System.out.println("Directory " + directory + " hint sended");
        }
    }
    
    public static String readFileAsString(String file)throws Exception
    {
        return new String(Files.readAllBytes(Paths.get(file)));
    }
    
    public File resultSetToJson(ResultSet rs) throws SQLException, JsonGenerationException, JsonMappingException, IOException {
    	File file = new File("userList.json");
    	List<User> list = new ArrayList<User>();
    	rs = this.stmt.executeQuery("SELECT userid, username FROM USER");

    	// Fetch each row from the result set
    	while (rs.next()) {
    	  String nome = rs.getString("nome");
    	  String cognome = rs.getString("cognome");

    	  // Create user object to pass into the list
    	  User user = new User(nome, cognome);

    	  list.add(user);
    	}

    	ObjectMapper objectMapper = new ObjectMapper();
    	objectMapper.writeValue(file, list);
    	
    	return file;
  }
    
    public File resultSetToXml(ResultSet rs) throws SQLException, JsonGenerationException, JsonMappingException, IOException {
    	File file = new File("userList.xml");
        XmlMapper xmlMapper = new XmlMapper();
    	List<User> list = new ArrayList<User>();
    	rs = this.stmt.executeQuery("SELECT userid, username FROM USER");

    	// Fetch each row from the result set
    	while (rs.next()) {
    	  String nome = rs.getString("nome");
    	  String cognome = rs.getString("cognome");

    	  // Create user object to pass into the list
    	  User user = new User(nome, cognome);

    	  list.add(user);
    	}
    	
        xmlMapper.writeValue(file, list);
    	
    	return file;
  }
}