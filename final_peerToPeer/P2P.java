//Programa consumer

// link jar org.json -> https://jar-download.com/artifacts/org.json
// tutorial json parse -> http://theoryapp.com/parse-json-in-java/


import java.io.*;
import java.net.*;
import java.util.concurrent.TimeUnit;

import java.awt.event.*;
import javax.swing.*;
import java.awt.*;

import javax.swing.text.AttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.JOptionPane;

import org.json.*;
import java.util.Scanner;

public class P2P {

    public static String output_msg = "";
    public static String input_msg = "";
    public static String nameNode;
    public static String source_input = "";

    public static String[] neighbors_name;
    public static boolean[] candSend;
    public static int num_neighbors;

    public static JTextPane ta = new JTextPane();
    

    public static void main(String args[]) throws IOException{

        String username = JOptionPane.showInputDialog("\nEnter your username: ");
        String jsonData = readFile("Users/"+username+".json");   
        jsonData = jsonData.replaceAll("\\s", "");
        
        JSONObject obj = new JSONObject(jsonData);
        
        ServerP2P serverp2p = new ServerP2P(obj);
        ClientP2P clientp2p = new ClientP2P(obj);
        
        serverp2p.start();
        // ----------------- G U I --------------------------
        //Creating the Frame
        JFrame frame = new JFrame("Fauti Chat");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 400);

        //Creating the head and adding components
        JPanel HeadPanel = new JPanel();
        JLabel usernameLabel = new JLabel(username);
        JButton clientStartButton = new JButton("Start Client");
        HeadPanel.add(usernameLabel);
        HeadPanel.add(clientStartButton);

        //Creating the panel at bottom and adding components
        JPanel panel = new JPanel(); // the panel is not visible in output
        
        JTextField tf = new JTextField(20); // accepts upto 10 characters
        JButton sendButton = new JButton("Send");
        panel.add(tf);
        panel.add(sendButton);
        
        clientStartButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                clientp2p.start();
                clientStartButton.setEnabled(false);
            }          
        });

        sendButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                output_msg = tf.getText();

                source_input = nameNode;
                appendToPane("\t\t\t"+output_msg+"\n", Color.RED);
                for(int i =0; i<num_neighbors; i++){
                    candSend[i] = true;
                }
                tf.setText("");
            }          
         });

        ta.setEditable(false); // set textArea non-editable
        JScrollPane scroll = new JScrollPane(ta);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        //Adding Components to the frame.
        frame.getContentPane().add(BorderLayout.SOUTH, panel);
        frame.getContentPane().add(BorderLayout.NORTH, HeadPanel);
        frame.getContentPane().add(BorderLayout.CENTER, scroll);
        frame.setVisible(true);
        
    }

    public static void appendToPane(String msg, Color c)
    {
        StyleContext sc = StyleContext.getDefaultStyleContext();
        AttributeSet aset = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, c);

        aset = sc.addAttribute(aset, StyleConstants.FontFamily, "Lucida Console");
        aset = sc.addAttribute(aset, StyleConstants.Alignment, StyleConstants.ALIGN_JUSTIFIED);

        int len = ta.getDocument().getLength();
        ta.setEditable(true);
        ta.setCaretPosition(len);
        ta.setCharacterAttributes(aset, false);
        ta.replaceSelection(msg);
        ta.setEditable(false);
    }

    static class ServerP2P extends Thread{
        Socket servicio = null;
        ServerSocket servidor = null;
        ThreadInput[] ti_consumer;

        public ServerP2P(JSONObject obj) throws IOException {
            JSONArray obj_neighbors_name = obj.getJSONArray("neighbors_names");
            nameNode = obj.getString("myself");
            num_neighbors = obj_neighbors_name.length();
            int port = obj.getInt("port");
            servidor = new ServerSocket(port);
            ti_consumer =  new ThreadInput[num_neighbors];

            neighbors_name = new String[num_neighbors];
            for (int i = 0; i < num_neighbors; i++){
                neighbors_name[i] = obj_neighbors_name.getString(i);
            }
        }

        public void run(){
            try {
                int id_count = 0;

                while( num_neighbors - id_count > 0 ){ 

                    try{
                        System.out.println("Watting Consumer...");
                        servicio = servidor.accept();
                        System.out.println("Consumer "+ servicio.getInetAddress()+" conected");
                        ti_consumer[id_count] = new ThreadInput(servicio);
                        ti_consumer[id_count].start();
                        id_count++;
                    }
                    catch(Exception e){
                        System.err.println("Error: " + e.getMessage());
                    }	
                }
                
            } 
            catch(Exception e){
				System.err.println("Error: " + e.getMessage());
			}
        }
    }

    static class ThreadInput extends Thread{

        Socket consumer = null;
		ObjectInputStream inputConsumer = null;

		public ThreadInput(Socket socket){
			consumer = socket;
		}

		public void run(){
			try{
                inputConsumer = new ObjectInputStream(consumer.getInputStream());
                String msg_recive, source_input_aux, input_msg_aux;

                while(true){	

                    msg_recive = (String) inputConsumer.readObject();    
                    JSONObject obj_recive = new JSONObject(msg_recive);
                    source_input_aux = obj_recive.getString("Source");
                    input_msg_aux = obj_recive.getString("Msg");

                    if(input_msg_aux.equals(input_msg) == false){
                        input_msg = input_msg_aux;
                        output_msg = input_msg;
                        source_input = source_input_aux;
                        System.out.println(source_input+" -> "+nameNode);
                        appendToPane(source_input+": "+input_msg+"\n", Color.BLUE);  
                        
                        for (int i = 0; i < num_neighbors; i++){
                            if(neighbors_name[i].equals(source_input) == false){
                                candSend[i] = true;
                            }
                        }

                    }

                    TimeUnit.MILLISECONDS.sleep(500);   // Not delete
                } 
			}	
			catch(Exception e){
				System.err.println("Error: " + e.getMessage());
			}
        }  
    }  

    static class ClientP2P extends Thread{
        Socket[] consumer;
        JSONObject neighbors;

        public ClientP2P(JSONObject obj) throws IOException {

            neighbors = obj.getJSONObject("neighbors");
            consumer = new Socket[num_neighbors];
            candSend = new boolean[num_neighbors];
        }

        public void run(){
            try {

                JSONObject neighbor;
                for (int i = 0; i < num_neighbors; i++){
                    String name_neighbor = neighbors_name[i];
                    neighbor = neighbors.getJSONObject(name_neighbor);
                    consumer[i] =  new Socket("localhost", neighbor.getInt("port"));
                    candSend[i] = false;
                    new ThreadOutput(consumer[i], i).start();
                }
            } 
            catch(Exception e){
				System.err.println("Error: " + e.getMessage());
			}
        }
    }  

    static class ThreadOutput extends Thread {

        Socket consumer = null;
		ObjectOutputStream outputConsumer = null;
        int id;
		public ThreadOutput(Socket socket, int id){
			consumer = socket;
            this.id = id;
        }

		public void run(){
			try{
                outputConsumer = new ObjectOutputStream(consumer.getOutputStream());

				while(true){
                    if(candSend[id]){

                        String msg_send = String.format("{\"Source\": \"%s\",\"Msg\":\"%s\"}", source_input, output_msg);
                        outputConsumer.writeObject(msg_send);
                        candSend[id] = false;
                    }
                    TimeUnit.MILLISECONDS.sleep(500);   // Not delete
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