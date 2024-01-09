

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

public class ChatClient {

    String serverAddress;
    Scanner in;
    PrintWriter out;
    JFrame frame = new JFrame("Chatter");
    JTextField textField = new JTextField(50);
    JTextPane messageArea = new JTextPane();
    JScrollPane scrollPane = new JScrollPane(messageArea);
    Map<String, Color> userColors = new HashMap<>();

    public ChatClient(String serverAddress) {
        this.serverAddress = serverAddress;

        textField.setEditable(false);
        messageArea.setEditable(false);
        messageArea.setPreferredSize(new Dimension(200, 300));
        frame.getContentPane().add(textField, BorderLayout.SOUTH);
        frame.getContentPane().add(new JScrollPane(messageArea), BorderLayout.CENTER);
        frame.pack();

        // Send on enter then clear to prepare for next message
        textField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                out.println(textField.getText());
                textField.setText("");
            }
        });
    }

    private String getName() {
        String userName = JOptionPane.showInputDialog(frame, "Choose a screen name:", "Screen name selection",
                JOptionPane.PLAIN_MESSAGE);
        Color userColor = getRandomColor();
        userColors.put(userName, userColor);
        return userName;
    }

    private void run() throws IOException {
        try {
            Socket socket = new Socket(serverAddress, 59001);
            in = new Scanner(socket.getInputStream());
            out = new PrintWriter(socket.getOutputStream(), true);

            while (in.hasNextLine()) {
                String line = in.nextLine();
                if (line.startsWith("SUBMITNAME")) {
                    out.println(getName());
                } else if (line.startsWith("NAMEACCEPTED")) {
                    this.frame.setTitle("Chatter - " + line.substring(13));
                    textField.setEditable(true);
                } else if (line.startsWith("MESSAGE")) {
                    String[] parts = line.substring(8).split(":");
                    String userName = "";
                    String message = "";
               
                    if (parts.length == 1) {
                        userName = parts[0];
                        appendMessage(null,line.substring(8));
                    } else if (parts.length >= 2) {
                        // Caso em que há pelo menos dois elementos no array parts, representando o nome de usuário e a mensagem
                        userName = parts[0];
                        message = String.join(":", Arrays.copyOfRange(parts, 1, parts.length));
                        appendMessage(userName, message);
                    }
                    
                }
            }
        } finally {
            frame.setVisible(false);
            frame.dispose();
        }
    }

    private void appendMessage(String userName, String message) {
    	  StyledDocument doc = messageArea.getStyledDocument(); // Obtém o StyledDocument do JTextPane
    	    try {
    	    	
    	    	if(userName != null) {
    	    		// Cria um AttributeSet com a cor do usuário
        	        AttributeSet userNameAttributeSet = getColorAttributeSet(getColorForUser(userName));
        	        doc.insertString(doc.getLength(), userName + ": ", userNameAttributeSet);
    	    	}
    	        
    	        // Insere a mensagem com o estilo padrão
    	        doc.insertString(doc.getLength(), message + "\n", null);
    	    } catch (BadLocationException e) {
    	        // Trata exceção de inserção de texto
    	        e.printStackTrace();
    	    }
    }
    
    private AttributeSet getColorAttributeSet(Color color) {
        SimpleAttributeSet attributeSet = new SimpleAttributeSet();
        StyleConstants.setForeground(attributeSet, color);
        return attributeSet;
    }
    
    private Color getColorForUser(String userName) {
        if (userColors.containsKey(userName)) {
            return userColors.get(userName);
        } else {
            // Se não houver uma cor associada ao usuário, gere uma nova cor aleatória e associe ao usuário
            Color newColor = getRandomColor();
            userColors.put(userName, newColor);
            return newColor;
        }
    }

    private Color getRandomColor() {
        Random random = new Random();
        int red = random.nextInt(256);
        int green = random.nextInt(256);
        int blue = random.nextInt(256);
        return new Color(red, green, blue);
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("Pass the server IP as the sole command line argument");
            return;
        }
        var client = new ChatClient(args[0]);
        
        client.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        client.frame.setVisible(true);
        client.run();
    }
}
