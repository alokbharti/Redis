package alok.redis;

import java.net.*; 
import java.io.*; 
import java.util.Scanner;
import alok.redis.Redis;

public class RedisServer{
	private Socket socket = null;
	private ServerSocket serverSocket = null;
	private DataInputStream dataInputStream = null;
	private DataOutputStream dataOutputStream = null;

	public RedisServer(int port){

		try{
			serverSocket = new ServerSocket(port);
			socket = serverSocket.accept();
			dataInputStream = new DataInputStream(
				new BufferedInputStream(socket.getInputStream()));
			dataOutputStream = new DataOutputStream(socket.getOutputStream()); 

			String line = ""; 
            Redis redis = new Redis(dataOutputStream);
            // reads message from client until "Over" is sent 
            while (!line.equals("QUIT")) 
            { 
                try
                { 
                    line = dataInputStream.readUTF(); 
                    System.out.println("Input from client: "+line);
                    redis.executeAction(line);
                } 
                catch(IOException i) 
                { 
                    System.out.println(i); 
                } 
            } 
            System.out.println("Closing connection"); 
  
            // close connection 
            socket.close(); 
            dataInputStream.close();
            dataOutputStream.close(); 


		}catch(IOException i) 
        { 
            System.out.println(i); 
        } 
	}

	public static void main(String args[]) 
    { 
    	System.out.print("Enter socket number: ");
        Scanner scanner = new Scanner(System.in);
        int port = scanner.nextInt();
        RedisServer server = new RedisServer(port); 
    } 

}