/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package clientchat;
/**
 *
 * @author Shwetha V
 */
import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class myFrame extends JFrame{
    
    /** Creates a new instance of myFrame */
    private final JTextArea ChatBox=new JTextArea(10,45);
    private final JScrollPane myChatHistory=new JScrollPane(ChatBox,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
            JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
    private JTextArea UserText = new JTextArea(5,40);
    private final JScrollPane myUserHistory=new JScrollPane(UserText,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    private final JButton Send = new JButton("Send");
    private final JButton Start = new JButton("Connect");
    private Client ChatClient;
    private final ReadThread myRead=new ReadThread();
    private final JTextField Server=new JTextField(20);
    private final JLabel myLabel=new JLabel("Server Name :");
    private final JTextField User=new JTextField(20);
    private String ServerName;
    private String UserName;
    
    
    public myFrame() {
        setResizable(false);
        setTitle("Client");
        setSize(560,400);
        Container cp=getContentPane();
        cp.setLayout(new FlowLayout());
        cp.add(new JLabel("Chat History"));
        cp.add(myChatHistory);
        cp.add(new JLabel("Chat Box : "));
        cp.add(myUserHistory);
        cp.add(Send);
        Component Component; /*add*/
        Component = cp.add(Start);
        cp.add(myLabel);
        cp.add(Server);
        cp.add(User);
        Send.addActionListener((ActionEvent e) -> {
            if(ChatClient!=null) {
                
                System.out.println(UserText.getText());
                ChatClient.SendMassage(UserText.getText());
            }
        });
        Start.addActionListener((ActionEvent e) -> {
            ChatClient=new Client();
            ChatClient.start();
        });
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
        
        
    }
    public static void main(String[] args) {
        // TODO code application logic here
        myFrame myFrame = new myFrame();
    }
    
    
    public class Client extends Thread {
        private static final int PORT=9999;
        private final LinkedList Clients;
        private final ByteBuffer ReadBuffer;
        private final ByteBuffer writeBuffer;
        private SocketChannel SChan;
        private Selector ReadSelector;
        private final CharsetDecoder asciiDecoder;
        
        public Client() {
            Clients=new LinkedList();
            ReadBuffer=ByteBuffer.allocateDirect(300);
            writeBuffer=ByteBuffer.allocateDirect(300);
            asciiDecoder = Charset.forName( "US-ASCII").newDecoder();
        }
        
        public void run() {
            
            ServerName=Server.getText();
            System.out.println(ServerName);
            UserName=User.getText();
            
            Connect(ServerName);
            myRead.start();
            while (true) {
                
                ReadMassage();
                
                try {
                    Thread.sleep(30);
                } catch (InterruptedException ie){
                }
            }
            
        }
        public void Connect(String hostname) {
            try {
                ReadSelector = Selector.open();
                InetAddress addr = InetAddress.getByName(hostname);
                SChan = SocketChannel.open(new InetSocketAddress(addr, PORT));
                SChan.configureBlocking(false);
                
                SChan.register(ReadSelector, SelectionKey.OP_READ, new StringBuffer());
            }
            
            catch (IOException e) {
            }
        }
        public void SendMassage(String messg) {
            prepareBuffer(UserName+" says: "+messg);
            channelWrite(SChan);
        }
        
        
        public void prepareBuffer(String massg) {
            writeBuffer.clear();
            writeBuffer.put(massg.getBytes());
            writeBuffer.putChar('\n');
            writeBuffer.flip();
        }
        
        public void channelWrite(SocketChannel client) {
            long num=0;
            long len=writeBuffer.remaining();
            while(num!=len) {
                try {
                    num+=SChan.write(writeBuffer);
                    
                    Thread.sleep(5);
                } catch (IOException ex) {
                } catch(InterruptedException ex) {
                    
                }
                
            }
            writeBuffer.rewind();
        }
        
        public void ReadMassage() {
            
            try {
                
                ReadSelector.selectNow();
                
                Set readyKeys = ReadSelector.selectedKeys();
                
                Iterator i = readyKeys.iterator();
                
                while (i.hasNext()) {
                    
                    SelectionKey key = (SelectionKey) i.next();
                    i.remove();
                    SocketChannel channel = (SocketChannel) key.channel();
                    ReadBuffer.clear();
                    
                    
                    long nbytes = channel.read(ReadBuffer);
                    
                    if (nbytes == -1) {
                        ChatBox.append("You logged out !\n");
                        channel.close();
                    } else {
                        
                        StringBuffer sb = (StringBuffer)key.attachment();
                        
                        ReadBuffer.flip( );
                        String str = asciiDecoder.decode( ReadBuffer).toString( );
                        sb.append( str );
                        ReadBuffer.clear( );
                        
                        
                        String line = sb.toString();
                        if ((line.contains("\n")) || (line.indexOf("\r") != -1)) {
                            line = line.trim();
                            
                            ChatBox.append("> "+ line);
                            ChatBox.append(""+'\n');
                            sb.delete(0,sb.length());
                        }
                    }
                }
            } catch (IOException ioe) {
            } catch (Exception e) {
            }
        }
    }
    class ReadThread extends Thread {
        @Override
        public void run() {
            ChatClient.ReadMassage();
        }
    }
}
