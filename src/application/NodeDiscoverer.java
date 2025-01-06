package application;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;

public class NodeDiscoverer {

    private static final int UDP_PORT = 5000;
    private static final int TCP_PORT = 6000;
    private static final String BROADCAST_MESSAGE = "Hello P2P Network!";
    private static final int TIMEOUT = 5000; // Yanıt bekleme süresi

    public void discoverAndSendFile(String filePath) {
        try (DatagramSocket udpSocket = new DatagramSocket()) {
            udpSocket.setBroadcast(true);

            // UDP Broadcast gönder
            byte[] buffer = BROADCAST_MESSAGE.getBytes();
            InetAddress broadcastAddress = InetAddress.getByName("255.255.255.255");
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, broadcastAddress, UDP_PORT);
            udpSocket.send(packet);
            System.out.println("UDP broadcast gönderildi: " + BROADCAST_MESSAGE);

            // Yanıtları dinle
            udpSocket.setSoTimeout(TIMEOUT);
            InetAddress responderAddress = null;

            while (true) {
                try {
                    byte[] responseBuffer = new byte[1024];
                    DatagramPacket responsePacket = new DatagramPacket(responseBuffer, responseBuffer.length);
                    udpSocket.receive(responsePacket);

                    String response = new String(responsePacket.getData(), 0, responsePacket.getLength());
                    System.out.println("UDP Yanıt alındı: " + response + " from " + responsePacket.getAddress());

                    if (response.equals("I am here!")) {
                        responderAddress = responsePacket.getAddress();
                        break;
                    }
                } catch (Exception e) {
                    System.out.println("Keşif tamamlandı. Daha fazla yanıt yok.");
                    break;
                }
            }

            // UDP soketini kapat
            udpSocket.close();
            System.out.println("UDP kapatıldı.");

            // TCP bağlantısı kur
            if (responderAddress != null) {
                try (Socket tcpSocket = new Socket(responderAddress, TCP_PORT);
                     DataOutputStream output = new DataOutputStream(tcpSocket.getOutputStream());
                     FileInputStream fileInput = new FileInputStream(new File(filePath))) {

                    System.out.println("TCP bağlantı kuruldu: " + responderAddress);

                    // Dosya boyutunu gönder
                    long fileSize = new File(filePath).length();
                    output.writeLong(fileSize);
                    System.out.println("Dosya boyutu gönderildi: " + fileSize + " byte");

                    // Dosyayı parça parça gönder
                    byte[] fileBuffer = new byte[256 * 1024]; // 256 KB buffer
                    int bytesRead;
                    while ((bytesRead = fileInput.read(fileBuffer)) != -1) {
                        output.write(fileBuffer, 0, bytesRead);
                        System.out.println("Parça gönderildi: " + bytesRead + " byte");
                    }

                    System.out.println("Dosya gönderimi tamamlandı.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        NodeDiscoverer discoverer = new NodeDiscoverer();
        discoverer.discoverAndSendFile("C:\\Users\\samli\\eclipse-workspace\\P2P\\src\\application\\example.txt"); // Gönderilecek dosya
    }
}


/*import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;

public class NodeDiscoverer {

    private static final int UDP_PORT = 5000;
    private static final int TCP_PORT = 6000;
    private static final String BROADCAST_MESSAGE = "Hello P2P Network!";
    private static final int TIMEOUT = 5000; // Yanıt bekleme süresi

    public void discoverAndConnect() {
        try (DatagramSocket udpSocket = new DatagramSocket()) {
            udpSocket.setBroadcast(true);

            // UDP Broadcast gönder
            byte[] buffer = BROADCAST_MESSAGE.getBytes();
            InetAddress broadcastAddress = InetAddress.getByName("255.255.255.255");
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, broadcastAddress, UDP_PORT);
            udpSocket.send(packet);
            System.out.println("UDP broadcast gönderildi: " + BROADCAST_MESSAGE);

            // Yanıtları dinle
            udpSocket.setSoTimeout(TIMEOUT);
            InetAddress responderAddress = null;

            while (true) {
                try {
                    byte[] responseBuffer = new byte[1024];
                    DatagramPacket responsePacket = new DatagramPacket(responseBuffer, responseBuffer.length);
                    udpSocket.receive(responsePacket);

                    String response = new String(responsePacket.getData(), 0, responsePacket.getLength());
                    System.out.println("UDP Yanıt alındı: " + response + " from " + responsePacket.getAddress());

                    if (response.equals("I am here!")) {
                        responderAddress = responsePacket.getAddress();
                        break;
                    }
                } catch (Exception e) {
                    System.out.println("Keşif tamamlandı. Daha fazla yanıt yok.");
                    break;
                }
            }

            // UDP soketini kapat
            udpSocket.close();
            System.out.println("UDP kapatıldı.");

            // TCP bağlantısı kur
            if (responderAddress != null) {
                try (Socket tcpSocket = new Socket(responderAddress, TCP_PORT)) {
                    System.out.println("TCP bağlantı kuruldu: " + responderAddress);

                    // TCP üzerinden veri alışverişi
                    try (DataOutputStream output = new DataOutputStream(tcpSocket.getOutputStream());
                         DataInputStream input = new DataInputStream(tcpSocket.getInputStream())) {

                        String message = "Merhaba!";
                        output.writeUTF(message);
                        System.out.println("TCP Mesaj gönderildi: " + message);

                        String response = input.readUTF();
                        System.out.println("TCP Yanıt alındı: " + response);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        NodeDiscoverer discoverer = new NodeDiscoverer();
        discoverer.discoverAndConnect();
    }
}*/



/*import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class NodeDiscoverer {

    private static final int PORT = 5000;
    private static final String BROADCAST_MESSAGE = "Hello P2P Network!";
    private static final int TIMEOUT = 5000; // Yanıt bekleme süresi

    public void discoverNodes() {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setBroadcast(true);

            byte[] buffer = BROADCAST_MESSAGE.getBytes();
            InetAddress broadcastAddress = InetAddress.getByName("255.255.255.255");

            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, broadcastAddress, PORT);
            socket.send(packet);
            System.out.println("UDP flood mesajı gönderildi.");

            // Yanıtları dinle
            socket.setSoTimeout(TIMEOUT);
            while (true) {
                try {
                    byte[] responseBuffer = new byte[1024];
                    DatagramPacket responsePacket = new DatagramPacket(responseBuffer, responseBuffer.length);
                    socket.receive(responsePacket);

                    String response = new String(responsePacket.getData(), 0, responsePacket.getLength());
                    System.out.println("Yanıt alındı: " + response + " from " + responsePacket.getAddress());
                } catch (Exception e) {
                    System.out.println("Keşif tamamlandı. Daha fazla yanıt beklenmiyor.");
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        NodeDiscoverer discoverer = new NodeDiscoverer();
        discoverer.discoverNodes();
    }
}*/
