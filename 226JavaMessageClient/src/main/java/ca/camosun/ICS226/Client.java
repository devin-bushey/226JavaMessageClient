package ca.camosun.ICS226;

import java.io.*;
import java.net.*;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Scanner;

public class Client {
    
    protected String serverName;
    protected int serverPort;
    protected String message;
    
    public static String key = "";

    protected final int KEY_LENGTH = 8;
    protected final int GET_POLL_TIME = 5000;
    protected final String ERROR_RESPONSE = "NO";
    protected final String NO_RESPONSE = "NO";
    protected final String OK_RESPONSE = "OK";
    protected final String GET_CMD = "GET";
    protected final String PUT_CMD = "PUT";
    protected final String SALTCHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";

    protected Scanner scanner = new Scanner(System.in);


    public Client(String serverName, int serverPort, String message) {
        this.serverName = serverName;
        this.serverPort = serverPort;
        this.message = message;
    }

    /*
    #
    # PURPOSE:
    # Validate key length
    #
    # RETURN/SIDE EFFECTS:
    # Terminates the program if validation fails
    #
    # NOTES:
    # IP address and port arguments are not validated
    #
    */
    protected void validateKey(){

        if (key.length() != KEY_LENGTH){
            System.err.print("Key length must be ");
            System.err.println(KEY_LENGTH);
            System.exit(-2);
        }

    }

    /*
    #
    # PURPOSE:
    # Return a random alphanumeric String of length KEY_LENGTH
    #
    */
    protected String getSaltString() {
        StringBuilder salt = new StringBuilder();
        Random rnd = new Random();
        while (salt.length() < KEY_LENGTH) {
            int index = (int) (rnd.nextFloat() * SALTCHARS.length());
            salt.append(SALTCHARS.charAt(index));
        }
        String saltStr = salt.toString();
        return saltStr;
    }

    /*
    #
    # PURPOSE:
    # Return a message provided by the user 
    #
    */
    protected String getInput(){

        System.out.print("Enter new message: ");
        String input = scanner.nextLine();
        
        return input;
    }

    /*
    #
    # PURPOSE:
    # Send a GET request to the server using the latest key 
    #
    # PARAMETERS:
    # 'key' is a string which is the key to be sent to the server.
    # key is a global variable because its needed between both PUT and GET's in the main function
    #
    # RETURN/SIDE EFFECTS:
    # Returns the decoded response by the server
    #
    # NOTES:
    # Opens/closes a new connection to the server
    #
    */
    protected String sendGetRequest(){

        String request;

        try (
            Socket socket = new Socket(serverName, serverPort);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream()));
        ) {

            request = GET_CMD.concat(key);
            out.println(request);
            return in.readLine();

        } catch (Exception e) {
            System.err.println(e);
            System.exit(-1);
        }

        return null;
    }

    /*
    #
    # PURPOSE:
    # Given a key from the command line, print all associated messages in the thread
    # Poll the server every 5 seconds with the latest key to retrieve the latest message
    #
    # PARAMETERS:
    # 'key' is a string, which is a global variable
    # Assume the key has been validated as an 8-digit string
    #
    # NOTES:
    # If there is a message returned from the server, assume that meesage consists of
    # of an 8-digit key and a message body
    #
    */
    protected void handle_get() {
        String get_result;
        String msg = "";

        while (true) {

            get_result = sendGetRequest();

            if (get_result != null && get_result.length() > 0){
                msg = get_result.substring(KEY_LENGTH);
            }

            while (msg.length() > 0){
                key = get_result.substring(0, KEY_LENGTH);
                System.out.print("Message: ");
                System.out.println(msg);
                get_result = sendGetRequest();
                if (get_result != null && get_result.length() > 0){
                    msg = get_result.substring(KEY_LENGTH);
                }
                else {
                    msg = "";
                }
            }

            try{
                Thread.sleep(GET_POLL_TIME);
            }
            catch (Exception e) {
                System.err.println(e);
                System.exit(-1);
            }
            
                
        }
    }

    /*
    #
    # PURPOSE:
    # Send a PUT request to the server using a given key and message 
    # If the server responds with a 'NO' then that means that a message with the given key already exists
    # therefore, this method retries a PUT request with the key that was returned by the server 
    #
    # PARAMETERS:
    # 'key' is a string which is the key to be sent to the server.
    # 'msg' is a string to be sent to the server
    #
    # RETURN/SIDE EFFECTS:
    # Returns the decoded response by the server
    #
    # NOTES:
    # Opens/closes a new connection to the server
    # Catches NullPointerException and NoSuchElementException and General Exceptions
    #
    */
    protected String sendPutRequest(String key, String new_msg){

        String reply;
        String next_key;
        String og_msg;
        String request;

        og_msg = new_msg.substring(KEY_LENGTH);
        request = PUT_CMD.concat(key).concat(new_msg).concat("\n");

        while (true){

            try (
                Socket socket = new Socket(serverName, serverPort);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));
            ) {

                    out.println(request);
                    reply = in.readLine();
                    
                    if (reply.substring(0, NO_RESPONSE.length()).equals(NO_RESPONSE)){
                        key = reply.substring(NO_RESPONSE.length(), NO_RESPONSE.length() + KEY_LENGTH);
                        next_key = getSaltString();
                        new_msg = next_key.concat(og_msg);
                        request = PUT_CMD.concat(key).concat(new_msg).concat("\n");
                    }
                    else{
                        return reply;
                    }
            } 
            catch (NullPointerException e) {
            }
            catch (NoSuchElementException e) {
            }
            catch (Exception e) {
                System.err.println(e);
                System.exit(-1);
            }
            
        }

    }

    /*
    #
    # PURPOSE:
    # Prompt the user for a new message. This new message will be paired with a randomly generated key
    # The new message and key are sent to the server by a PUT request
    #
    # PARAMETERS:
    # 'key' is a string, which is a global variable
    # Assume the key has been validated as an 8-digit string
    #
    # NOTES:
    #
    */
    protected void handle_put() {
        
        String next_key;
        String new_msg;
        String reply;

        while (true) {
            next_key = getSaltString();
            new_msg = getInput();
            new_msg = next_key.concat(new_msg);
            reply = sendPutRequest(key, new_msg);
        }
    }
    
    /*
    #
    # PURPOSE:
    # Use two threads to retrieve the latest message in a thread using a given key from the commandline and 
    # send a new message to the server with the latest key
    # The client does not quit
    # initial key is validated and if it fails validation, program terminates
    #
    # NOTES:
    # Each co-routine communicates the latest key by using the key as a global variable
    #
    */
    protected void handle_client(){

        validateKey();

        try {
            Runnable runnable_put = () -> this.handle_put();
            Thread t_put = new Thread(runnable_put);
            t_put.start();

            Runnable runnable_get = () -> this.handle_get();
            Thread t_get = new Thread(runnable_get);
            t_get.start();

        }
        catch (Exception e) {
            System.err.println(e);
            System.exit(-2);
        }
    }
    
    public static void main(String[] args) {
        
        if (args.length != 3) {
            System.err.println("Need <host> <port> <message>");
            System.exit(-2);
        }

        key = args[2];

        Client c = new Client(args[0], Integer.valueOf(args[1]), args[2]);
        c.handle_client();
    }
}
