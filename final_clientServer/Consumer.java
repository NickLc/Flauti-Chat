//Programa consumer
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

public class Consumer {

    public static String output_msg = "";
    public static String input_msg = "";
    public static String username = "";
    public static JTextPane ta = new JTextPane();

    public static void main(String args[]) throws IOException{

        username = JOptionPane.showInputDialog("\nEnter your username: ");
        String jsonData = readFile("Users/"+username+".json");   
        jsonData = jsonData.replaceAll("\\s", "");
        JSONObject obj = new JSONObject(jsonData);
        int server_port = obj.getInt("server_port");
        Socket consumer =  new Socket("localhost",server_port);

        new ThreadInput(consumer).start();
        new ThreadOutput(consumer).start();

        // ----------------- G U I --------------------------
        //Creating the Frame
        JFrame frame = new JFrame("Fauti Chat");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 400);

         //Creating the head and adding components
         JPanel HeadPanel = new JPanel();
         JLabel usernameLabel = new JLabel(username);
         HeadPanel.add(usernameLabel);

        //Creating the panel at bottom and adding components
        JPanel panel = new JPanel(); // the panel is not visible in output
        JLabel label = new JLabel("Text");
        JTextField tf = new JTextField(20); // accepts upto 10 characters
        JButton send = new JButton("Send");
        panel.add(label); // Components Added using Flow Layout
        panel.add(tf);
        panel.add(send);

        send.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                output_msg = tf.getText();
                appendToPane("\t\t"+output_msg+"\n", Color.RED);
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

    static class ThreadInput extends Thread{

        Socket consumer = null;
		ObjectInputStream inputConsumer = null;

		public ThreadInput(Socket socket){
			consumer = socket;
		}

		public void run(){
			try{
                inputConsumer = new ObjectInputStream(consumer.getInputStream());
                String source_input, msg_recive;
				while(true){	
                    msg_recive = (String) inputConsumer.readObject();    
                    
                    JSONObject obj_recive = new JSONObject(msg_recive);
                    source_input = obj_recive.getString("Source");
                    input_msg = obj_recive.getString("Msg");

                    appendToPane(source_input+": "+input_msg+"\n", Color.BLUE);
                    TimeUnit.MILLISECONDS.sleep(500);   // Not delete
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
		
		public ThreadOutput(Socket socket){
			consumer = socket;
        }

		public void run(){
			try{
                outputConsumer = new ObjectOutputStream(consumer.getOutputStream());                
				while(true){
                    if(output_msg != ""){
                        String msg_send = String.format("{\"Source\": \"%s\",\"Msg\":\"%s\"}", username, output_msg);
                        outputConsumer.writeObject(msg_send);
                        output_msg = "";
                        
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