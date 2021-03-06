package DirectoryService;

import FileServer.ServerHeartbeat;
import common.Heartbeat;
import common.Msg;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Iterator;

public class DirectoryServer {
    
    private static ServerManager serverManager;
    private static ClientManager clientManager;
    private static RMIService rmiService;
        
    public static void main(String[] args) 
    {   
        UdpRequestHandler udpRequestHandler = new UdpRequestHandler();
        udpRequestHandler.start();
        
        serverManager = new ServerManager();
        serverManager.setDaemon(true);
        serverManager.start();
        
        clientManager = new ClientManager();
        clientManager.setDaemon(true);
        clientManager.start();
        
        try {
            rmiService = new RMIService(serverManager);
            rmiService.run();
        } catch (RemoteException ex) {
            System.out.println("Erro ao iniciar o servico RMI! " + ex);
        }
    }
    
    private static class UdpRequestHandler extends Thread {
        
        private static final String LIST = "LIST";
        private static final String USERS = "USERS";
        private static final String MSG = "MSG";
        private static final String MSGTO = "MSGTO";
        
        ServerUdpListener udpListener;
        ChatService chatService;
                
        UdpRequestHandler()
        {
            udpListener = new ServerUdpListener();
            chatService = new ChatService();
        }
        
        @Override
        public void run(){
            try {
                while(udpListener.isListening()){
                    Object obj;
                    obj = udpListener.handleRequests();
                    processRequest(obj);
                }
            } catch(IOException e){
                System.out.println("Ocorreu um erro no acesso ao socket:\n\t"+e);
            }catch(ClassNotFoundException e){
                System.out.println("O objecto recebido não é do tipo esperado:\n\t"+e);
            }finally{
                if(udpListener.getSocket() != null){
                    udpListener.getSocket().close();
                }
            }
        }
        
        private void processRequest(Object obj) throws IOException
        {
            if (obj instanceof ServerHeartbeat){
                ServerHeartbeat hb = (ServerHeartbeat)obj;
                serverManager.processHeartbeat(hb, udpListener.getCurrentAddr());
                rmiService.notifyObservers();
            }
            else if (obj instanceof Heartbeat){
                Heartbeat hb = (Heartbeat)obj;
                clientManager.processHeartbeat(hb,udpListener.getCurrentAddr());
            }
            else if (obj instanceof Msg)
                processCommand((Msg)obj);
            else if (obj instanceof String)
                System.out.println((String)obj);
            else
                System.out.println("Erro: Objecto recebido do tipo inesperado!");
        }
        
        private void processCommand(Msg msg) throws IOException
        {
            if(msg.getMsg().equalsIgnoreCase(LIST))
                udpListener.sendResponse(serverManager.getServerList());
            else if (msg.getMsg().equalsIgnoreCase(USERS)){
                Iterator it = clientManager.getOnlineClients().keySet().iterator();
                StringBuilder clientsAsString = new StringBuilder();
                while (it.hasNext()){
                    String c = (String)it.next();
                    if (serverManager.isAuthenticatedClient(c)){
                        clientsAsString.append(c + "\n");
                    }
                }
                udpListener.sendResponse(clientsAsString.toString());
            }
            else {
                String[] args = msg.getMsg().split("\\s");

                if (args[0].equalsIgnoreCase(MSG))
                {
                    if (args.length >= 2){
                        Iterator it = clientManager.getOnlineClients().values().iterator();
                        while (it.hasNext()){
                            ClientEntry client = (ClientEntry) it.next();
                            String message = msg.getMsg().substring(3); //tira "msg"
                            //System.out.println("client: "+ client.getName() + " ");
                            if (serverManager.isAuthenticatedClient(client.getName())){
                                //System.out.println("is authenticated!");
                                chatService.sendMessage(
                                    msg.getName() + ": " + message,
                                    client.getAddr(), client.getPort());
                            } //else System.out.println("is NOT authenticated!");
                        }
                        udpListener.sendResponse("Mensagem de difusao enviada...");
                    }
                    
                } else if (args[0].equalsIgnoreCase(MSGTO))
                {
                    if (args.length >= 3){
                        
                        ClientEntry client = (ClientEntry) clientManager.getClient(args[1]);
                        String message = msg.getMsg().substring(3); //tira "msg"
                        System.out.println("client: "+ client.getName() + " ");
                        if (serverManager.isAuthenticatedClient(client.getName())){
                            System.out.println("is authenticated!");
                            chatService.sendMessage(
                                msg.getName() + ": " + message,
                                client.getAddr(), client.getPort());
                            udpListener.sendResponse("Mensagem enviada...");
                        } else System.out.println("is NOT authenticated!");
                    } else udpListener.sendResponse("Erro de sintaxe: MSGTO <nome> <message>");
                    
                } else udpListener.sendResponse("Unknown Command");
            }
        }
    }
}