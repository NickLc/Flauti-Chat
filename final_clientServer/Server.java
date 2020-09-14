//Programa Servidor
import java.net.*;
import java.io.*;
import java.util.LinkedList; 
import java.util.Queue; 
import javax.swing.JOptionPane;

import org.json.*;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class Server{

    public static int num_consumer;
	
	public static Queue<String>[] queueTowardsConsumer;

	public static void main(String[] args) throws Exception{
		Socket servicio = null;
		
        String jsonData = readFile("Users/server.json");   
        jsonData = jsonData.replaceAll("\\s", "");
        JSONObject obj = new JSONObject(jsonData);
		int port = obj.getInt("port");

		JSONArray clients = obj.getJSONArray("clients");

		num_consumer = clients.length();
		queueTowardsConsumer  = new Queue[num_consumer];

		ServerSocket servidor = new ServerSocket(port);

        // --------------- Consumer---------------------------
		int id_count = 0;
        ThreadOutputToConsumer[] to_consumer = new ThreadOutputToConsumer[num_consumer];
        ThreadInputFromConsumer[] ti_consumer = new ThreadInputFromConsumer[num_consumer];
		
		for(int i=0; i<num_consumer; i++){
			queueTowardsConsumer[i] = new LinkedList<>();	
		}
		
        while( num_consumer - id_count > 0 ){ 

			try{
                System.out.println("Watting Consumer...");
                servicio = servidor.accept();
				System.out.println("Consumer "+ servicio.getInetAddress()+" conected");
				ti_consumer[id_count] = new ThreadInputFromConsumer(servicio, id_count);
				ti_consumer[id_count].start();
				to_consumer[id_count] = new ThreadOutputToConsumer(servicio, id_count);
				to_consumer[id_count].start();
                id_count++;
			}
			catch(Exception e){
				System.err.println("Error: " + e.getMessage());
			}	
        }

	}

	static class ThreadOutputToConsumer extends Thread{
		Socket servicio = null;
        ObjectOutputStream salidaServidor = null;
        String output = "";
		int id;
		public ThreadOutputToConsumer(Socket socket, int id_consumer){
			servicio = socket;
            id = id_consumer;
		}

		public void run(){
			try{
				salidaServidor = new ObjectOutputStream(servicio.getOutputStream());
		
				while(true){
                    if(queueTowardsConsumer[id].isEmpty()){
						TimeUnit.MILLISECONDS.sleep(4000);
					}
                    else{
						output = queueTowardsConsumer[id].remove();
						salidaServidor.writeObject(output);
						TimeUnit.MILLISECONDS.sleep(500);	// not delete
                    }			
				}
			}	
			catch(Exception e){
				System.err.println("Error: " + e.getMessage());
			}
		}
	
	}

	static class ThreadInputFromConsumer extends Thread{
		Socket servicio = null;
		ObjectInputStream entradaServidor = null;;
		String input = "0";
		int id;
		
		public ThreadInputFromConsumer(Socket socket, int id_consumer){
			servicio = socket;
			id = id_consumer;
		}

		public void run(){
			try{
			
				entradaServidor = new ObjectInputStream(servicio.getInputStream());
				
				while(true){	
					input = (String) entradaServidor.readObject();

					if(input.equals("") == false){
						
						for(int i=0; i<num_consumer; i++){
							if(i != id){
								queueTowardsConsumer[i].add(input);
							}
						}
						System.out.println("BroadCast mensaje del customer "+id+": "+ input);
					}
					
					TimeUnit.MILLISECONDS.sleep(500);	// not delete	
				}
			}	
			catch(Exception e){
				System.err.println("Error: " + e.getMessage());
			}
		}
	}	

	public static String readFile(String file) {
        String data = ""; 
        try {
            File f = new File(file);
			Scanner r = new Scanner(f);  			
            while (r.hasNextLine()) {
			  data += r.nextLine();
            }
            r.close();
            
          } catch (FileNotFoundException ex) {
            JOptionPane.showMessageDialog(null,"Username don't exist\n");
            System.out.print("FILE NOT FOUND");
            ex.printStackTrace();
          }
        return data;
	}

}