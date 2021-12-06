package ca.camosun.ICS226;

import java.io.*;
import java.net.*;
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

    protected Scanner scanner = new Scanner(System.in);


    public Client(String serverName, int serverPort, String message) {
        this.serverName = serverName;
        this.serverPort = serverPort;
        this.message = message;
    }

    protected String getSaltString() {
        String SALTCHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
        StringBuilder salt = new StringBuilder();
        Random rnd = new Random();
        while (salt.length() < KEY_LENGTH) { // length of the random string.
            int index = (int) (rnd.nextFloat() * SALTCHARS.length());
            salt.append(SALTCHARS.charAt(index));
        }
        String saltStr = salt.toString();
        return saltStr;

    }

    protected String getInput(){
        System.out.print("Enter new message: ");
        String input = scanner.nextLine();
        return input;
    }

    public String sendGetRequest(){

        String request;

        try (
            Socket socket = new Socket(serverName, serverPort);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream()));
        ) {

            //System.out.println("[SENDING GET]");
            request = GET_CMD.concat(key).concat("\n");
            out.println(request);
            return in.readLine();

        } catch (Exception e) {
            System.err.println(e);
            System.exit(-1);
        }

        return null;
    }

    public void handle_get() {
        String reply;
        String get_result;
        String msg = "";
        String request;

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

    public String sendPutRequest(String new_msg){

        String reply;
        String next_key;
        String og_msg;
        String request;

        System.out.println("[SENDING PUT]");
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
                        key = reply.substring(ERROR_RESPONSE.length(), ERROR_RESPONSE.length() + KEY_LENGTH);
                        next_key = getSaltString();
                        new_msg = next_key.concat(og_msg);
                        request = PUT_CMD.concat(key).concat(new_msg).concat("\n");
                    }
                    else{
                        return reply;
                    }
                

            } catch (Exception e) {
                System.err.println(e);
                System.exit(-1);
                
            }
            return null;
        }

    }

    public void handle_put() {
        
        String next_key;
        String new_msg;
        String reply;

        while (true) {
            next_key = getSaltString();
            new_msg = getInput();
            new_msg = next_key.concat(new_msg);
            reply = sendPutRequest(new_msg);
        }
    }
    
    public void handle_client(){
        try {
            Runnable runnable_put = () -> this.handle_put();
            Thread t_put = new Thread(runnable_put);
            t_put.start();

            Runnable runnable_get = () -> this.handle_get();
            Thread t_get = new Thread(runnable_get);
            t_get.start();

        } catch (Exception e) {
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
