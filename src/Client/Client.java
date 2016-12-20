package Client;

import DirectoryService.ServerEntry;
import DirectoryService.ServerMonitorListener;
import common.Msg;
import common.Heartbeat;
import common.HeartbeatSender;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.RemoteException;

public class Client /*extends UnicastRemoteObject implements ServerMonitorListener*/{
    private static final String EXIT = "EXIT";
    private static final String NAME = "NAME";
   
    private static String name;
    
    private static ClientTcpHandler tcpHandler;
    private static ClientUdpHandler udpHandler;
    private static ClientUdpListener udpListener;
    private static HeartbeatSender<Heartbeat> hbSender;
    private static ClientCommands commands;
    
    public Client() throws RemoteException{}
        
    public static void main(String[] args) throws RemoteException {
        
        Client observer = new Client();
        
        
        if(args.length != 2){
            System.out.println("Sintaxe: java Client dirAdress dirUdpPort");
            return;
        }
        
        name = "guest";
        
        try 
        {
            
            InetAddress directoryServerAddr = InetAddress.getByName(args[0]);
            Integer directoryServerPort = Integer.parseInt(args[1]);
            udpHandler = new ClientUdpHandler(directoryServerAddr, directoryServerPort);
            udpListener = new ClientUdpListener(observer);
            udpListener.start();
            tcpHandler = new ClientTcpHandler();
            
            // Enviar heartbeats UDP ao serviço de directoria
            hbSender = new HeartbeatSender<Heartbeat>(
                    new Heartbeat(udpListener.getListeningPort(),name),
                    directoryServerAddr, directoryServerPort);
            hbSender.setDaemon(true);
            hbSender.start();
            
            // exemplo de uso request ao server TCP
            //tcpHandler.connectToServer(InetAddress.getByName("127.0.0.1"), 7001);
            /*System.out.println(
                    tcpHandler.sendRequest("I pedido1 I")
            );*/
            /* Exemplo para listagem de comandos
            OK-NAME luis -> altera o nome para luis
            OK-EXIT -> sai (já faz isto no while loop)
            OK-LIST -> lista os servidores ligados
            MSG -> envia uma mensagem a todos os clientes activos
            MSGTO luis -> envia uma mensagem ao luis
            ... o resto dos comandos são enviados ao tcp
            */
            
            String msg;
            commands = new ClientCommands(observer, udpHandler,tcpHandler, 
                    directoryServerAddr, directoryServerPort);
            //Adicionar este listener
            //commands.getRmiService().addObserver(observer);
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
            while(true){
                System.out.print("> ");
                msg = in.readLine();

                if(msg.equalsIgnoreCase(EXIT)) break;
                else {
                    String[] cmd = msg.split("\\s");
                    if (cmd[0].equalsIgnoreCase(NAME)){
                        if (cmd.length == 2){
                            name = cmd[1];
                            hbSender.setHeartbeat(new Heartbeat(udpListener.getListeningPort(),name));
                            System.out.println("Nome: " + name);
                            continue;
                        } else System.out.println("Erro de sintaxe: nome <nome>");
                    }
                }
                System.out.println(commands.processRequest(new Msg(name, msg)));
            }
            udpHandler.closeSocket();
            tcpHandler.closeSocket();
            
        } catch (UnknownHostException ex) {
            System.out.println("[Cliente] Destino desconhecido:\n\t"+ex);
        } catch (IOException ex) {
        }/*catch(ClassNotFoundException e){
             System.out.println("O objecto recebido não é do tipo esperado:\n\t"+e);
        }*/
    }
    
    public void printError(String e){
        System.out.println("Erro: " + e);
    }
    
    public void printContent(String s){
        System.out.println(s);
    }
    
    public String getName(){
        return name;
    }

    /*@Override
    public void printServers() throws RemoteException {
        if (commands.getLastCommand().equalsIgnoreCase("LIST")){
            StringBuilder out = new StringBuilder();
            out.append("Nao esta autenticado nos servidores: ");
            
            ArrayList<ServerEntry> serverList = commands.getRmiService().getServerList();
            Iterator sit = serverList.iterator();
            while (sit.hasNext()){
                ServerEntry se = (ServerEntry)sit.next();
                if (!se.existsClient(name))
                out.append("\n" + se.getName() + "\t" + se.getAddr().getHostAddress() + "\t" + se.getPort());
            }
            System.out.println(out.toString());
        }
    }*/
}