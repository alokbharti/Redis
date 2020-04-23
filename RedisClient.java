package alok.redis;

import java.net.*; 
import java.io.*; 
import java.util.Scanner;

public class RedisClient 
{ 
    // initialize socket and input output streams 
    private Socket socket            = null; 
    private DataInputStream  input, inputFromServer   = null; 
    private DataOutputStream out     = null; 
  
    // constructor to put ip address and port 
    public RedisClient(String address, int port) 
    { 
        // establish a connection 
        try
        { 
            socket = new Socket(address, port); 
            System.out.println("Connected");
            System.out.print("redis>");
  
            // takes input from terminal 
            input  = new DataInputStream(System.in); 
            inputFromServer = new DataInputStream(
                new BufferedInputStream(socket.getInputStream()));
  
            // sends output to the socket 
            out    = new DataOutputStream(socket.getOutputStream()); 
        } 
        catch(UnknownHostException u) 
        { 
            System.out.println(u); 
        } 
        catch(IOException i) 
        { 
            System.out.println(i); 
        } 
  
        // string to read message from input 
        String line = ""; 
  
        // keep reading until "Over" is input 
        while (!line.equals("QUIT")) 
        { 
            try
            { 
                line = input.readLine(); 
                out.writeUTF(line); 
                Thread.sleep(500);
                while(inputFromServer.available()!= 0){
                    String outputFromServer = inputFromServer.readUTF();
                    System.out.println(outputFromServer);
                }
                System.out.print("redis>");
            } 
            catch(IOException i) 
            { 
                System.out.println(i); 
            } catch(InterruptedException i){
                System.out.println(i); 
            }
        } 
  
        // close the connection 
        try
        { 
            input.close(); 
            out.close(); 
            socket.close(); 
        } 
        catch(IOException i) 
        { 
            System.out.println(i); 
        } 
    } 
  
    public static void main(String args[]) 
    { 
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter IP Address of the server: ");
        String ipAddress = scanner.nextLine();
        System.out.print("Enter port number: ");
        int port = scanner.nextInt();
        RedisClient client = new RedisClient(ipAddress, port); 
    } 
} 