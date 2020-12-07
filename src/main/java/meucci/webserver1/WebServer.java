package meucci.webserver1;
/**
 * small java web server made in order to try some HTTP returning codes.
 * the class generates a new thread for each connection. 
 * @author Majid Alessio
 */
import java.io.*;
import java.net.*;
import java.util.*;

// Each Client Connection will be managed in a dedicated Thread
public class WebServer implements Runnable
{ 	
    static final File appo=new File("");
    static final String PATH=appo.getAbsolutePath();
    static final File WEB_ROOT = new File(PATH+"\\src\\main\\java\\meucci\\websources");
    static final String DEFAULT_FILE = "index.html";
    static final String FILE_NOT_FOUND = "404.html";
    static final String METHOD_NOT_SUPPORTED = "not_supported.html";
    static final String FILE_MOVED="301.html";
    static final int PORT = 8080;       //port to listen connection
    static final boolean verbose = true;       //verbose mode
    private Socket SC;     // Client Connection via Socket Class
    
    /**
     * constructor method
     * @param c 
     */
    public WebServer(Socket c) 
    {
            SC = c;
    }

    /**
     * main method that runs thread once a connection is established
     * @param args 
     */
    public static void main(String[] args) 
    {
        try 
        {
            ServerSocket serverConnect = new ServerSocket(PORT);
            System.out.println("Server started.\nListening for connections on port : " + PORT + " ...\n");
            // we listen until user halts server execution
            while (true)      //infinite loop
            {
                WebServer myServer = new WebServer(serverConnect.accept());
                if (verbose) 
                {
                    System.out.println("Connecton opened. (" + new Date() + ")");
                }
                //create dedicated thread to manage the client connection
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
     * run method, override of the run method in the Runnable class
     */
    @Override
    public void run() 
    {
        // we manage our particular client connection
        BufferedReader in = null;       //variable used for the client's input
        PrintWriter out = null;         //
        BufferedOutputStream dataOut = null;    //   
        String fileRequested = null;
        try 
        {
            //we read characters from the client via input stream on the socket
            in = new BufferedReader(new InputStreamReader(SC.getInputStream()));
            //we get character output stream to client (for headers)
            out = new PrintWriter(SC.getOutputStream());
            //get binary output stream to client (for requested data)
            dataOut = new BufferedOutputStream(SC.getOutputStream());
            //get first line of the request from the client
            String input = in.readLine();
            //we parse the request with a string tokenizer
            StringTokenizer parse = new StringTokenizer(input);
            //we get the HTTP method of the client
            String method = parse.nextToken().toUpperCase(); 
            //we get file requested
            fileRequested = parse.nextToken().toLowerCase();
            //we support only GET and HEAD methods, we check
            if (!method.equals("GET")  &&  !method.equals("HEAD")) 
            {
                if (verbose) 
                {
                    System.out.println("501 Not Implemented : " + method + " method.");
                }
                //we return the not supported file to the client
                File file = new File(WEB_ROOT, METHOD_NOT_SUPPORTED);
                int fileLength = (int) file.length();
                String contentMimeType = "text/html";
                byte[] fileData = readFileData(file, fileLength);   //read content to return to client
                //we send HTTP Headers with data to client
                out.println("HTTP/1.1 501 Not Implemented");
                out.println("Server: Java HTTP Server from SSaurel : 1.0");
                out.println("Date: " + new Date());
                out.println("Content-type: " + contentMimeType);
                out.println("Content-length: " + fileLength);
                out.println();  //blank line between headers and content, very important !
                out.flush();   //flush character output stream buffer
                //file data
                dataOut.write(fileData, 0, fileLength);
                dataOut.flush();
            } 
            else 
            {
                //GET or HEAD method
                if (fileRequested.endsWith("/")) 
                {
                    fileRequested += DEFAULT_FILE;
                }
                File file = new File(WEB_ROOT, fileRequested);
                int fileLength = (int) file.length();
                String content = getContentType(fileRequested);
                if (method.equals("GET")) // GET method so we return content
                { 
                    byte[] fileData = readFileData(file, fileLength);
                    //send HTTP Headers
                    out.println("HTTP/1.1 200 OK");
                    out.println("Server: Java HTTP Server from SSaurel : 1.0");
                    out.println("Date: " + new Date());
                    out.println("Content-type: " + content);
                    out.println("Content-length: " + fileLength);
                    out.println(); // blank line between headers and content, very important !
                    out.flush(); // flush character output stream buffer
                    dataOut.write(fileData, 0, fileLength);
                    dataOut.flush();
                }
                if (verbose) 
                {
                    System.out.println("File " + fileRequested + " of type " + content + " returned");
                }
            }
        } 
        catch (FileNotFoundException fnfe) 
        {
            try 
            {
                String[] path=fileRequested.split("/");     //array used in order to store the path
                int nPath=path.length;
                String choosenObj=path[nPath-1];
                if(choosenObj.lastIndexOf(".")==-1)         //if the lastIndexOf method return -1 then the noSlash method is called and the hint given to the client will be PATH+/
                {
                    noSlash(out,dataOut,fileRequested+"/");
                }
                else
                {
                    fileNotFound(out, dataOut, fileRequested);      //otherwise the called method will be file not found
                }
            } 
            catch (IOException ioe) 
            {
                System.err.println("Error with file not found exception : " + ioe.getMessage());
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
                SC.close();     //we close socket connection
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
     * method used in order to read data from a file
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
     * method used in order to return supported MIME Types
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
     * method used to print the file not found code 404 from the HTTP protocol
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
        out.println("HTTP/1.1 404 File Not Found");
        out.println("Server: Java HTTP Server from SSaurel : 1.0");
        out.println("Date: " + new Date());
        out.println("Content-type: " + content);
        out.println("Content-length: " + fileLength);
        out.println(); // blank line between headers and content, very important !
        out.flush(); // flush character output stream buffer
        dataOut.write(fileData, 0, fileLength);
        dataOut.flush();
        if (verbose)
        {
            System.out.println("File " + fileRequested + " not found");
        }
    }
    
    /**
     * method used in order to give an hint to the client if he requests a path without the ending /
     * @param out
     * @param dataOut
     * @param directoryRequested
     * @throws IOException 
     */
    private void noSlash (PrintWriter out, OutputStream dataOut,String directoryRequested) throws IOException
    {
        File file = new File(WEB_ROOT, FILE_MOVED);
        int fileLength = (int) file.length();
        String content = "text/html";
        byte[] fileData = readFileData(file, fileLength);
        out.println("HTTP/1.1 301 Moved Permanently");
        out.println("Server: Java HTTP Server from SSaurel : 1.0");
        out.println("Date: " + new Date());
        out.println("Content-type: " + content);
        out.println("Content-length: " + fileLength);
        out.println("Location: "+directoryRequested);   //path with the / added printed as an hint for the user in order to reach the right location
        out.println();  // blank line between headers and content, very important !
        out.flush();    // flush character output stream buffer
        dataOut.write(fileData, 0, fileLength);
        dataOut.flush();
        if (verbose) 
        {
            System.out.println("Directory " + directoryRequested + " hint sended");
        }
    }

}
