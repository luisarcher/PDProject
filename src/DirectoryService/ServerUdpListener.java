package DirectoryService;

import FileServer.ServerHeartbeat;
import common.Heartbeat;
import common.Msg;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

public class ServerUdpListener extends Thread{
    public static final int MAX_SIZE = 1000;
    public static final String LIST = "LIST";
    public static final String MSG = "MSG";
    public static final int TIMEOUT = 5000; //2 segs
    protected DatagramSocket socket;
    protected ServerManager serverManager;
    protected boolean listening;
    
    public ServerUdpListener(){
        listening = true;
        serverManager = new ServerManager();
    }
    
    @Override
    public void run(){
        serverManager.setDaemon(true);
        serverManager.start();
        try{
            int tempPort = 56321;
            socket = new DatagramSocket(tempPort); //()
            //socket.setSoTimeout(TIMEOUT);
            DatagramPacket packet = null;
            System.out.println("UDP Port:\n\t"+socket.getLocalPort()+" v3");
            
            while(listening){
                handleRequests(packet);
            } 
            
        }catch(UnknownHostException e){
             System.out.println("Destino desconhecido:\n\t"+e);
        }catch(SocketTimeoutException e){
            System.out.println("Não foi recebida qualquer resposta:\n\t"+e);
        }catch(SocketException e){
            System.out.println("Ocorreu um erro ao nível do socket UDP:\n\t"+e);
        }catch(IOException e){
            System.out.println("Ocorreu um erro no acesso ao socket:\n\t"+e);
        }catch(ClassNotFoundException e){
             System.out.println("O objecto recebido não é do tipo esperado:\n\t"+e);
        }finally{
            if(socket != null){
                socket.close();
            }
        }
    }
    
    protected void handleRequests(DatagramPacket packet) throws IOException, ClassNotFoundException
    {
        ObjectInputStream in;
        Object obj;
        packet = new DatagramPacket(new byte[MAX_SIZE], MAX_SIZE);
        socket.receive(packet);
        in = new ObjectInputStream(new ByteArrayInputStream(packet.getData(), 0, packet.getLength()));
        obj = in.readObject();
        
        if (obj instanceof ServerHeartbeat)
        {
            ServerHeartbeat heartbeat = (ServerHeartbeat)obj;
            System.out.println("Server Heatbeat data: \n\t TCPServerName: "+heartbeat.getName()
                                + "\n\t TCPServerPort: "+heartbeat.getPort());
            serverManager.processHeartbeat(heartbeat);  
        } 
        else if (obj instanceof Heartbeat)
        {
            Heartbeat heartbeat = (Heartbeat)obj;
            System.out.println("Client Heatbeat data: \n\t TCPServerName: "+heartbeat.getName()
                                + "\n\t TCPServerPort: "+heartbeat.getPort());
        } 
        else if (obj instanceof Msg)
        {
            Msg msg = (Msg)obj;
            
            if(msg.getMsg().equalsIgnoreCase(LIST)){
                sendResponse(packet, serverManager.getServerMap());
            } else /*if (msg.getMsg().equalsIgnoreCase(MSG))*/{
                System.out.println("Message: \n\t Nickname: "+msg.getName()
                                + "\n\t Text: "+msg.getMsg());
                sendResponse(packet, "resposta servidor");
            } 
        } else if (obj instanceof String){
            System.out.println((String)obj);
        } else
            System.out.println("Erro: Objecto recebido do tipo inesperado!");
    }
    
    protected <T> void sendResponse(DatagramPacket packet, T response) throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        T r = response;
        oos.writeObject(r);
        oos.flush();

        packet.setData(baos.toByteArray());
        packet.setLength(baos.size());
        socket.send(packet);
    }
    
    public int getLocalPort(){
        return socket.getLocalPort();
    }
    
    public void stopListening(){
        listening = false;
    }
}
