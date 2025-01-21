package com.cn2.communication;

import java.io.*;
import java.net.*;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.*;
import java.awt.event.*;
import java.lang.Thread;

public class App extends Frame implements WindowListener, ActionListener {

	/*
	 * Definition of the app's fields
	 */
	static TextField inputTextField;		
	static JTextArea textArea;				 
	static JFrame frame;					
	static JButton sendButton;				
	static JTextField meesageTextField;		  
	public static Color gray;				
	final static String newline="\n";		
	static JButton callButton;		
	
    private static boolean UDP;
    private static boolean TCP;
    public static Object[] userinput;
	/*
	 *  Variables for communication over UDP
	 */
	public static DatagramSocket socket;
	public static DatagramSocket chatSocket;
	public static DatagramSocket voipSocket;
	public static InetAddress targetAddress;
    public static int voipTargetPort;
    public static int voipLocalPort;
    public static int chatTargetPort;
    public static int chatLocalPort;
    public static InetAddress ip1;   
    public static AudioFormat format;
    private static TargetDataLine microphone;
    private static SourceDataLine speakers;
    private static boolean chat = true;
    private static boolean voip = false;
	/*
	 *  Variables for communication over TCP
	 */ 
    private static boolean server;
    private static boolean client;
    private static boolean samePorts;
    public static ServerSocket serverSocket;
    public static Socket chatSocketTCP;
    public static BufferedReader reader;
    public static PrintWriter writer;
    public static OutputStream output;
    public static InputStream input;
    /*
     * Threads to communicate  
     */
    public static Thread sendtextThread;
    public static Thread sendaudioThread;
    public static Thread audioreceiverThread;
    public static Thread textreceiverThread;
    public static Runnable textreceiverRunnable;
    public static Runnable audioreceiverRunnable;
    public static Runnable sendaudioRunnable;
    public static Runnable sendtextRunnable;
    
    public static Runnable textreceiverRunnableTCP;
    public static Thread textreceiverThreadTCP;
    public static int count = 0;
  
	/**
	 * Construct the app's frame and initialize important parameters
	 */
	public App(String title) {
		/*
		 * 1. Defining the components of the GUI
		 */
		// Setting up the characteristics of the frame
		super(title);									
		gray = new Color(254, 254, 254);		
		setBackground(gray);
		setLayout(new FlowLayout());			
		addWindowListener(this);	
		
		// Setting up the TextField and the TextArea
		inputTextField = new TextField();
		inputTextField.setColumns(20);
		
		// Setting up the TextArea.
		textArea = new JTextArea(10,40);			
		textArea.setLineWrap(true);				
		textArea.setEditable(false);	
	
		JScrollPane scrollPane = new JScrollPane(textArea);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		
		//Setting up the buttons
		sendButton = new JButton("Send");			
		callButton = new JButton("Call");			
		/*
		 * 2. Adding the components to the GUI
		 */
		add(scrollPane);								
		add(inputTextField);
		add(sendButton);
		add(callButton);
		/*
		 * 3. Linking the buttons to the ActionListener
		 */
		sendButton.addActionListener(this);			
		callButton.addActionListener(this);	
	}
	
	/**
	 * The main method of the application. It continuously listens for
	 * new messages.	
	 */
	public static void main(String[] args){
		/*
		 * 1. Show the input window until the input is valid
		 */
		do {
			userinput = showInputWindow();
			if (userinput != null) {
				if (userinput.length == 1) {
					// Exit if the user cancels
					return;
				}else {
					break;
				}	
			}	
		}while(true);
		
		/*
		 * 2. Create the app's window
		 */
		App app = new App("CN2 - AUTH");  // TODO: You can add the title that will displayed on the Window of the App here																		  
		app.setSize(500,250);				  
		app.setVisible(true);
		
		/*
		 * 3. Extract values form the returned object and 
		 *    initialize variables according to user's input	 
		 */
		
		// Text communication protocol
		UDP = "UDP".equalsIgnoreCase((String) userinput[0]);
		TCP = "TCP".equalsIgnoreCase((String) userinput[0]);
		
		// Peer's IP address (peer with server role waits for connection)
		if (!server) {
				targetAddress = (InetAddress) userinput[1];
		}
		
		// Peer's port for VoIP communication over UDP (send voice packages)
		voipTargetPort = (int) userinput[2];
		// Local port for VoIP communication over UDP  (receive voices packages)
		voipLocalPort = (int) userinput[3];
		// Peer's port for chat communication over TCP or UDP
		chatTargetPort = (int) userinput[4];
		// Local port for chat communication over TCP or UDP
		chatLocalPort = (int) userinput[5];
			
		// Server or client role for TCP connection
		server= (boolean) userinput[6];
		client = (boolean) userinput[7];
		// Communicate with VoIP and chat over UDP using the same socket
		samePorts = (boolean) userinput[8];	
		
		/*
		 * 4. Initialize speakers, microphone and audio format for VoIP communication
		 *    	 
		 */
	
		// Create a new AudioFormat object specifying the format of the audio data
		format = new AudioFormat(
				AudioFormat.Encoding.PCM_SIGNED,  // Encoding format for audio data (PCM signed encoding)
			    8000,                             // Sample rate: 8000 samples per second (8 kHz)
			    8,                                // Sample size: 8 bits per sample
			    1,                                // Number of channels: 1 (mono audio)
			    1,                                // Frame size: 1 byte per frame (8-bit sample for mono audio)
			    8000,                             // Frame rate: 8000 frames per second (matches sample rate)
			    false                             // Big-endian byte order (false = little-endian)
			    );

		// Create an instance of DataLine.Info for the SourceDataLine class, which is used for playback (output) of audio data
		DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
		// Create an instance of DataLine.Info for the TargetDataLine class, which is used for capturing (input) audio data from a microphone
		DataLine.Info microphoneInfo = new DataLine.Info(TargetDataLine.class, format);	
		try {
			// Get a SourceDataLine for audio playback using the specified format (info)
			speakers = (SourceDataLine) AudioSystem.getLine(info);
			// Get a TargetDataLine for audio input (microphone) using the specified format (microphoneInfo)
			microphone = (TargetDataLine) AudioSystem.getLine(microphoneInfo);		
		}catch (LineUnavailableException e) {
			e.printStackTrace();
		}
		
		/*
		 * 5. Create DatagramSockets bound to the local ports given for chat and VoIP over UDP protocol
		 *    	 
		 */

		// Create two separate sockets to handle VoIP and chat operations (UDP)
		if (UDP && !samePorts) {
			try {
				// Create a DatagramSocket bound to the local port intended for VoIP 
				voipSocket = new DatagramSocket(voipLocalPort);

			} catch (SocketException e) {
				e.printStackTrace();
			}
			try {
				// Create a DatagramSocket bound to the local port intended for chat
				chatSocket = new DatagramSocket(chatLocalPort);
			} catch (SocketException e) {
				e.printStackTrace();
			}
		}
		// If the local ports are equal or TCP protocol for text communication is chosen
		else{
			try {
				// Create only one DatagramSocket bound to the local port intended for VoIP 
				socket = new DatagramSocket(voipLocalPort);
			} catch (SocketException e) {
				e.printStackTrace();
			}
		}
		
		/*
		 * 6. Create Sockets to establish connection over TCP according to the role given
		 *    	 
		 */	
   
		if (TCP) {
			if (server) {
				// Server side
				try {
					// Create a ServerSocket for the server side and wait for connections
					serverSocket = new ServerSocket(chatLocalPort);
					chatSocketTCP = serverSocket.accept();
					// Retrieve the client's IP
					targetAddress = chatSocketTCP.getInetAddress();
					System.out.println("Success");
				} catch (IOException e) {
					e.printStackTrace();
				}
			}else if (client) {
				// Client side
				try {
					// Create a socket to connect to Server using the IP and Port
					chatSocketTCP = new Socket(targetAddress,chatTargetPort);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			// Input and Output streams for communication
	        try {
				reader = new BufferedReader(new InputStreamReader(chatSocketTCP.getInputStream()));
			} catch (IOException e) {
				e.printStackTrace();
			}
	        try {
				writer = new PrintWriter(chatSocketTCP.getOutputStream(), true);
	        } catch (IOException e) {
					e.printStackTrace();
			}
		}
		
			
        /*
         * 7. Create Runnable tasks that are designed to be executed on separate threads. 
         * 	  By running these tasks in parallel threads, the main application thread is kept free,
         * 	  ensuring a non-blocking, responsive user experience for real-time communication.
		 */
  
	    // Runnable for receiving text messages over TCP
	    // This task listens for incoming text messages on a socket (TCP)
        textreceiverRunnableTCP = new Runnable() {
            @Override
			public void run() {
            	try {
            		while(chat) {							
            			String response = reader.readLine();
            			if (response == null) {
            				textArea.append("The connection is closed.\nOpen the app again to reconnect.");
                            break; // Exit the inner loop to reconnect
                        }
            			if (chat == true) {
 						textArea.append("remote - "  + response + "\n");
 						}
 					}		
 	            } catch (IOException e) {
 	                    e.printStackTrace();
 	                }
            	 }
        };

        // Runnable for receiving text messages over UDP
	    // This task listens for incoming text messages on a socket (UDP)
        textreceiverRunnable = new Runnable() {
        	@Override
	        public void run() {
        		try {
        			while(chat) {	
        				byte [] buffer1 = new byte[1024];
						DatagramPacket packet = new DatagramPacket(buffer1,buffer1.length);
						if (samePorts) {
							socket.receive(packet);
						}else {
							chatSocket.receive(packet);
						}
						String response = new String(packet.getData());
						if (chat == true) {
							textArea.append("remote - "  + response + "\n");
						}
					}		
	             } catch (IOException e) {
	                    e.printStackTrace();
	                }
	        }
        };
			
		// Runnable for receiving audio data (VoIP audio) over UDP
		// This task listens for incoming audio packets, processes the data, 
		// and plays the audio back through the speakers (using SourceDataLine).
		audioreceiverRunnable = new Runnable() {
			@Override
	        public void run() {
				try {
					speakers.open(format);
	    		} catch (LineUnavailableException e) {
	    			e.printStackTrace();
	    		}
		        speakers.start();
	            try {
					do{
							byte [] buffer = new byte[1024];
							DatagramPacket packet = new DatagramPacket(buffer,buffer.length);
							if (samePorts) {
								socket.receive(packet);
							}else{
								voipSocket.receive(packet);
							}// Receive a packet
		                    byte[] audioresponse = packet.getData();  // Get the audio data
		                    int audiobytes = packet.getLength(); // Get the number of bytes in the packet
		                    // Play the audio data through speakers
		                    	speakers.write(audioresponse, 0, audiobytes);
	                }while(voip);
	            }catch (IOException e) {
	                    e.printStackTrace();
	                }
			}
		};
			
			// Runnable for sending audio data (VoIP audio) over UDP
			// This task continuously captures audio from the microphone and sends it
			// over the network to the specified address using UDP.
			sendaudioRunnable = new Runnable() {
	            @Override
	            public void run() {
	            	try {
						microphone.open(format);
					} catch (LineUnavailableException e) {
						e.printStackTrace();
					}  // Open the microphone line
	            	microphone.start(); 
	            	byte[] buffer = new byte[1024];      // Start capturing audio
	            	do {
	            		int audiobytes = microphone.read(buffer, 0, buffer.length);
	            		DatagramPacket packet = new DatagramPacket(buffer,audiobytes,targetAddress,voipTargetPort);
	            		if (samePorts) {
	            			try {
	            				socket.send(packet);
	            			} catch (IOException e) { 
	            			e.printStackTrace();
	            			}
	            		}else {
	            			try {
	            				voipSocket.send(packet);
	            			} catch (IOException e) {
	            				e.printStackTrace();
	            			}
	            		}
	            	}while(voip);	
			     }
	       };
	       
	       /*
	        * 8. Create threads to execute the above operations
	        * 	 and start on default the treads responsible to receive text messages. 
	        * 	 If the chat and VoIP functionalities are assigned to different sockets
	        * 	 the text receiver thread will always run on the background.
	        *    The VoIP threads responsible for sending and receiving voice are started only when the call button is clicked
	        *    and they stop after the second click. New threads are then created to handle a new call if the button is clicked again.
			*/
	       
	       // Start the appropriate thread to listen for text messages over UDP or TCP protocol
	       if(TCP) {
	    	   textreceiverThreadTCP = new Thread(textreceiverRunnableTCP);
	    	   textreceiverThreadTCP.start();
	       }else if(UDP){
	    	   textreceiverThread = new Thread(textreceiverRunnable);
	    	   textreceiverThread.start();
	       }
			}	
	
	/**
	 * The method that corresponds to the Action Listener. Whenever an action is performed
	 * (i.e., one of the buttons is clicked) this method is executed. 
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		/*
		 * Check which button was clicked.
		 */
		if (e.getSource() == sendButton){
			// The "Send" button was clicked
			// Read the message from the input field 
			String message = inputTextField.getText();
			chat = true;
			// Send the message over UDP protocol using datagramSocket and datagramPackage classes
			if (UDP) {
				byte[] buffer = message.getBytes();
				DatagramPacket packet = new DatagramPacket(buffer,buffer.length,targetAddress,chatTargetPort);
				if(samePorts) {
					try {
						socket.send(packet);
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}else {
					try {
						chatSocket.send(packet);
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}

			// Send the message over TCP protocol to the output stream
			}else if (TCP == true) {
				writer.println(message);
			}
			// Print the message with flag local to the text area
			textArea.append("local - " + message + "\n");	
			inputTextField.setText("");
		}else if(e.getSource() == callButton){
			// The "Call" button was clicked
				
				// If the button is clicked for the first time start a call:
				if (count == 0) {
					if (UDP && samePorts) {
						// If the user uses the same ports to communicate via text and voice
						// - Update the flag chat to false to escape the loop and terminate the thread that listens for text messages
						chat = false;
						textArea.append("\nA call is now in progress...\nPress the call button again to return to chat mode\n");
					}
					else {
					
						textArea.append("A call is now in progress...\nYou can still send/receive text messages.\n");
					}// - Start new voip threads for every new call
					audioreceiverThread = new Thread(audioreceiverRunnable);
					sendaudioThread = new Thread(sendaudioRunnable);
					audioreceiverThread.start();  
				    sendaudioThread.start();
				    count = count+1;
				}else if(count==1){
					// If the button is clicked for the second time end the call:
					// - Update the flag voip to escape the loop and terminate the voip threads
					// - close the speakers and the microphone 
					textArea.append("The call has been terminated");
					count = 0;
					voip = false;
					chat = true;
					microphone.stop();
	                microphone.close();
	                speakers.stop();
	                speakers.close();
	    			if (UDP && samePorts) {
	    				// If the user uses the same ports to communicate via text and voice
						// - Start a new thread that listens for text messages
	    				textreceiverThread = new Thread(textreceiverRunnable);
	    				textreceiverThread.start();
	    			}
				}
		}
			

	}



	// Impute Dialog to initialize info for communication
	public static Object[] showInputWindow(){
    // Components for the input dialog
    JCheckBox tcpCheckBox = new JCheckBox("TCP (Text Messages)");
    JCheckBox udpCheckBox = new JCheckBox("UDP (Text Messages)");
    JCheckBox serverCheckBox = new JCheckBox("Server");
    JCheckBox clientCheckBox = new JCheckBox("Client");
    JCheckBox samePortsCheckBox = new JCheckBox("Choose the same ports for chat communication (chat and VoIP cannot operate simultaneously)");
    JTextField ipField = new JTextField(15); // Set columns for consistent text field size
    
    JTextField voipPortField = new JTextField(15);
    JTextField chatPortField = new JTextField(15);
    JTextField voiplocalPortField = new JTextField(15);
    JTextField chatlocalPortField = new JTextField(15);
    
    ButtonGroup group1 = new ButtonGroup();
    group1.add(tcpCheckBox);
    group1.add(udpCheckBox);
    ButtonGroup group2 = new ButtonGroup();
    group2.add(serverCheckBox);
    group2.add(clientCheckBox);
    
    // Panel for input fields
    JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));	
    panel.setBackground(gray); 
    
    // Prompt the user to input the target and local port for VoIP communication (UDP)
    panel.add(new JLabel("VoIp operates over Udp protocol\n"));
   
    panel.add(new JLabel("Enter target Port for VoIP:"));
    panel.add(voipPortField);
    panel.add(new JLabel("Enter local Port for VoIP:"));
    panel.add(voiplocalPortField);
    
    panel.add(new JLabel("Select Protocol for chat:"));
    panel.add(tcpCheckBox);
    panel.add(udpCheckBox);
    panel.add(new JLabel("TCP Role:"));
    panel.add(serverCheckBox);
    panel.add(clientCheckBox);
    
    panel.add(samePortsCheckBox);
    panel.add(new JLabel("Enter target Port for chat:"));
    panel.add(chatPortField);
    panel.add(new JLabel("Enter local Port for chat:"));
    panel.add(chatlocalPortField);
    
    panel.add(new JLabel("Enter Address:"));
    panel.add(ipField);
    serverCheckBox.setEnabled(false);
    clientCheckBox.setEnabled(false);
    
    udpCheckBox.addActionListener(e -> {
        serverCheckBox.setEnabled(false);
        clientCheckBox.setEnabled(false);
        ipField.setEnabled(true);
        samePortsCheckBox.setEnabled(true);
    });

    tcpCheckBox.addActionListener(e -> {
        serverCheckBox.setEnabled(true);
        clientCheckBox.setEnabled(true);
        samePortsCheckBox.setEnabled(false);
 });
   
    serverCheckBox.addActionListener(e1 -> {
        ipField.setEnabled(false);
        chatPortField.setEnabled(false);
        chatlocalPortField.setEnabled(true);
    });
    
    clientCheckBox.addActionListener(e1 -> {
        ipField.setEnabled(true); 
        chatPortField.setEnabled(true);
        chatlocalPortField.setEnabled(false);
    });
    
    samePortsCheckBox.addActionListener(e2 -> {
    	chatPortField.setEnabled(!samePortsCheckBox.isSelected());
        chatlocalPortField.setEnabled(!samePortsCheckBox.isSelected());
    });
    // Show the dialog
    panel.revalidate();
    int result = JOptionPane.showConfirmDialog(null, panel, "Communication Setup", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
    if (result == JOptionPane.OK_OPTION) {
        // Initialize variables
        String targetip = null;
        int voipPort = -1;
        int voiplocalPort = -1;
        int chatPort = -1;
        int chatlocalPort = -1;
        InetAddress ip = null;
        boolean samePorts;
        boolean isClient ;
    	boolean isServer;
        
        // Save the choices of the user
    	boolean isTcp = tcpCheckBox.isSelected();
        boolean isUdp = udpCheckBox.isSelected();
        
        if (isUdp) {
        	samePorts = samePortsCheckBox.isSelected();
        	isClient = false;
        	isServer = false;
        }
        else
        {
        	samePorts = false;
        	isClient = clientCheckBox.isSelected();
        	isServer = serverCheckBox.isSelected();
        }

    	// Save the chat protocol selected
    	String protocol = isTcp ? "TCP" : isUdp ? "UDP" : "";
    	if (!voipPortField.getText().isEmpty() && !voiplocalPortField.getText().isEmpty()) {
    		voipPort = Integer.parseInt(voipPortField.getText());
			voiplocalPort = Integer.parseInt(voiplocalPortField.getText());
    	}
        
        if (isTcp && isClient) {
        	targetip = ipField.getText();
        	// Get the chat (TCP) ports
        	if (!chatPortField.getText().isEmpty()) {
        		chatPort = Integer.parseInt(chatPortField.getText());
        	}
        	//chatlocalPort = Integer.parseInt(chatlocalPortField.getText());
        	/* The client does not need to provide the local port for chat (TCP)
             * The local port is assigned automatically when a connection is established
             */
        }else if(isTcp && isServer) {
        	if (!chatlocalPortField.getText().isEmpty()) {
        		chatlocalPort = Integer.parseInt(chatlocalPortField.getText());
        	}
        	/* The server does not need to provide the target port for chat (TCP)
             * The server does not need to provide the target IP address
             * The target IP can be retrieved once a connection is accepted 
             */
        }else{
        	targetip = ipField.getText();
        	if (!samePorts) {
        		if (!chatPortField.getText().isEmpty() && !chatlocalPortField.getText().isEmpty()) {
        			chatPort = Integer.parseInt(chatPortField.getText());
        			chatlocalPort = Integer.parseInt(chatlocalPortField.getText());
        		}
        	}else {
        		/* If the user wants to use the same ports to communicate,
                 * the VoIP and chat functionalities won't work simultaneously
                 * The system switches between the two modes, allowing only one to be active at a time.
                 * The user will be able to send either voice or text packages to the target port,
                 * and listen correspondingly on the local port for either voice or text packages.
                 */
        		chatPort = voipPort;
                chatlocalPort = voiplocalPort;
        	}	
        }
        
        if (protocol.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Error: You must select a protocol for chat communication.");
            return null;
        }
        if (voipPort == -1  || voiplocalPort == -1) {
        	 JOptionPane.showMessageDialog(null, "Error: Udp ports (target and local) are required for voip communincation.");
             return null;
        }
        if (voipPort < 0 || voipPort > 65535 || voiplocalPort < 0 || voiplocalPort > 65535 ){
        	JOptionPane.showMessageDialog(null, "Error: Please provide valid ports from 0 to 65535.");
            return null;
        }
        if (isUdp && targetip.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Error: Target ip address is required for chat and voip communincation.");
            return null;
        }
        if (isUdp && (chatlocalPort < 0 || chatlocalPort > 65535 || chatPort < 0 || chatPort > 65535) ) {
            JOptionPane.showMessageDialog(null, "Error: PLease enter valide ports.");
            return null;
        }
        if (isTcp && isServer && chatlocalPort == -1) {
            JOptionPane.showMessageDialog(null, "Error: The server needs to provide a local port to accept connections over TCP.");
            return null;
        }
        if (isTcp && isServer && (chatlocalPort < 0 || chatlocalPort > 65535)) {
            JOptionPane.showMessageDialog(null, "Please provide valid local port for chat from 0 to 65535.");
            return null;
        }
        if (isTcp && isClient && targetip.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Error: Target ip adress is required to connect to the server.");
            return null;
        }
        if (isTcp && isClient && (chatPort == -1)) {
            JOptionPane.showMessageDialog(null, "Error: Tcp target port is required to connect to the server.");
            return null;
        }
        if (isTcp && isClient && (chatPort < 0 || chatPort > 65535)) {
            JOptionPane.showMessageDialog(null, "Error: Tcp target port is required to connect to the server.");
            return null;
        }
        if ((isTcp && isClient) || isUdp) {
            try {
            	ip = InetAddress.getByName(targetip); 
            } catch (UnknownHostException e) {
            	JOptionPane.showMessageDialog(null, "Error: Please enter a valid IP address.");
                return null; 
            }
        }
        // Return the collected inputs
        return new Object []{protocol,ip,voipPort,voiplocalPort,chatPort,chatlocalPort,isServer,isClient,samePorts};
    } else {
    	Object[] cancel = {""};
    	//cancel[0] = "cancel";
    	return cancel;

    }
	}


	/**
	 * These methods have to do with the GUI. You can use them if you wish to define
	 * what the program should do in specific scenarios (e.g., when closing the 
	 * window).
	 */
	@Override
	public void windowActivated(WindowEvent e) {
		// TODO Auto-generated method stub	
	}

	@Override
	public void windowClosed(WindowEvent e) {
		// TODO Auto-generated method stub	
	}

	@Override
	public void windowClosing(WindowEvent e) {
		// Close any open sockets
		if (UDP && !samePorts) {
			voipSocket.close();
			chatSocket.close();
		}else{					
			socket.close();
		}
		if (TCP && server) {
			try {
				chatSocketTCP.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			try {
				serverSocket.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			} 
		}else if (TCP && client) {
			try {
				chatSocketTCP.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}	
		dispose();
        	System.exit(0);
	}

	@Override
	public void windowDeactivated(WindowEvent e) {
		// TODO Auto-generated method stub	
	}

	@Override
	public void windowDeiconified(WindowEvent e) {
		// TODO Auto-generated method stub	
	}

	@Override
	public void windowIconified(WindowEvent e) {
		// TODO Auto-generated method stub	
	}

	@Override
	public void windowOpened(WindowEvent e) {
		// TODO Auto-generated method stub	
	}
}
