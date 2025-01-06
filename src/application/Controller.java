	package application;
	
	import javafx.application.Application;
	import javafx.fxml.FXML;
	import javafx.fxml.FXMLLoader;
	import javafx.stage.DirectoryChooser;
	import javafx.stage.FileChooser;
	import javafx.stage.Modality;
	import javafx.stage.Stage;
	import javafx.scene.Group;
	import javafx.scene.Parent;
	import javafx.scene.Scene;
	import javafx.scene.layout.BorderPane;
	import javafx.scene.layout.VBox;
	import javafx.scene.paint.Color;
	
	import javafx.application.Platform;
	import javafx.scene.control.Alert;
	import javafx.scene.control.Alert.AlertType;
	import javafx.scene.control.ButtonType;
	import javafx.scene.control.Label;
	import javafx.scene.control.ProgressBar;
	import javafx.scene.control.TextField;
	import javafx.scene.layout.VBox;
	
	import java.io.DataInputStream;
	import java.io.DataOutputStream;
	import java.io.File;
	import java.io.FileInputStream;
	import java.io.FileOutputStream;
	import java.net.DatagramPacket;
	import java.net.DatagramSocket;
	import java.net.InetAddress;
	import java.net.ServerSocket;
	import java.net.Socket;
	import java.util.HashMap;
	import java.util.List;
	import java.util.Map;
	import java.util.Optional;
	
	
	public class Controller {
	
		@FXML
		private VBox progressContainer;
		@FXML
		private TextField rootFolderField;
		@FXML
		private TextField destinationFolderField;
		
		private Map<String, ProgressBar> progressBars = new HashMap<>(); // İlerleme çubuklarını takip etmek için
	    private Socket tcpSocket; // TCP bağlantısını temsil eden socket
	    private boolean isConnected = false; // Bağlantı durumu
	    // Yeni bir ProgressBar ekle
	    public void addProgressBar(String fileName) {
	        Platform.runLater(() -> {
	            ProgressBar progressBar = new ProgressBar(0); // 0'dan başlat
	            progressBars.put(fileName, progressBar); // Haritaya ekle
	            progressContainer.getChildren().add(progressBar); // VBox'a ekle
	            System.out.println("ProgressBar eklendi: " + fileName);
	        });
	    }
	    
	    // İlerlemeyi güncelle
	    public void updateProgress(String fileName, double progress) {
	        Platform.runLater(() -> {
	            ProgressBar progressBar = progressBars.get(fileName);
	            if (progressBar != null) {
	                progressBar.setProgress(progress); // İlerleme güncelle
	                System.out.println("ProgressBar güncellendi: " + fileName + " -> " + (progress * 100) + "%");
	            }
	        });
	    }
	    
	    // ProgressBar'ı kaldır
	    public void removeProgressBar(String fileName) {
	        Platform.runLater(() -> {
	            ProgressBar progressBar = progressBars.remove(fileName);
	            if (progressBar != null) {
	                progressContainer.getChildren().remove(progressBar); // VBox'tan kaldır
	                System.out.println("ProgressBar kaldırıldı: " + fileName);
	            }
	        });
	    }
	    private void sendFile(File file) {
	        if (tcpSocket == null || tcpSocket.isClosed()) {
	            System.out.println("TCP bağlantısı mevcut değil. Lütfen bağlanın.");
	            return;
	        }
	
	        try (DataOutputStream output = new DataOutputStream(tcpSocket.getOutputStream());
	             FileInputStream fileInput = new FileInputStream(file)) {
	
	            // Dosya adı ve boyutunu gönder
	            output.writeUTF(file.getName());
	            output.writeLong(file.length());

	
	            // Dosyayı parça parça gönder
	            byte[] buffer = new byte[256 * 1024]; // 256 KB buffer
	            int bytesRead;
	            long totalBytesSent = 0;
	            while ((bytesRead = fileInput.read(buffer)) != -1) {
	                output.write(buffer, 0, bytesRead);
	                totalBytesSent += bytesRead;
	
	                // İlerlemeyi güncelle
	                double progress = (double) totalBytesSent / file.length();
	                updateProgress(file.getName(), progress);
	            }
	
	            System.out.println("Dosya gönderimi tamamlandı: " + file.getName());
	
	            // ProgressBar'ı kaldır
	            removeProgressBar(file.getName());
	
	        } catch (Exception e) {
	            e.printStackTrace();
	            System.out.println("Dosya gönderimi sırasında hata oluştu: " + file.getName());
	        }
	    }
	    
	    
	    private void reciveFile() {
	        // 1. TCP bağlantısını kontrol et
	        if (tcpSocket == null || tcpSocket.isClosed()) {
	            System.out.println("TCP bağlantısı mevcut değil. Lütfen bağlanın.");
	            return; // Bağlantı yoksa metodu sonlandır
	        }

	        try (DataInputStream input = new DataInputStream(tcpSocket.getInputStream())) {
	            // 2. Dosya adını ve boyutunu al
	            // Sunucudan gönderilen ilk bilgi: Dosya adı
	            String fileName = input.readUTF(); // UTF-8 formatında dosya adı
	            // Sunucudan gönderilen ikinci bilgi: Dosya boyutu (byte cinsinden)
	            long fileSize = input.readLong();

	            System.out.println("Dosya Alınıyor: " + fileName + " (" + fileSize + " byte)");
	            // 3. Dosyanın kaydedileceği hedef konumu belirle
	            File destinationFile = new File(destinationFolderField.getText(), fileName);

	            // 4. İlerleme çubuğu ekle (kullanıcıya görsel durum sağlamak için)
	            addProgressBar(fileName);

	            // 5. Gelen dosyayı hedef dosyaya yazmak için bir `FileOutputStream` aç
	            try (FileOutputStream fileOutput = new FileOutputStream(destinationFile)) {
	                byte[] buffer = new byte[256 * 1024]; // 256 KB'lık bir buffer oluştur
	                int bytesRead; // Okunan veri miktarını tutacak değişken
	                long totalBytesRead = 0; // Toplam okunan byte miktarını takip etmek için

	                // 6. Dosya içeriğini okuma ve kaydetme döngüsü
	                while (totalBytesRead < fileSize && (bytesRead = input.read(buffer)) != -1) {
	                    // Buffer'dan okunan veriyi dosyaya yaz
	                    fileOutput.write(buffer, 0, bytesRead);
	                    totalBytesRead += bytesRead;

	                    // 7. İlerlemeyi güncelle
	                    double progress = (double) totalBytesRead / fileSize;
	                    updateProgress(fileName, progress);

	                    // Konsolda durum güncellemesi
	                    System.out.printf("Alınan veri: %d/%d byte (%.2f%%)%n", totalBytesRead, fileSize, progress * 100);
	                }

	                // 8. Dosya alımını tamamla
	                System.out.println("Dosya alımı tamamlandı: " + destinationFile.getAbsolutePath());
	            }

	            // 9. İlerleme çubuğunu kaldır
	            //removeProgressBar(fileName);

	        } catch (Exception e) {
	            // Hata durumunda istisnayı yakala ve bildir
	            e.printStackTrace();
	            System.out.println("Dosya alımı sırasında hata oluştu.");
	        }
	    }

	
	 
	
	
	    public void showDeveloperInfo() {
	        // Yeni bir pencere (Stage) oluştur
	        Stage developerInfoStage = new Stage();
	        developerInfoStage.setTitle("Developer Information");
	
	        // Pencerenin sahibi olarak ana pencereyi belirle
	        developerInfoStage.initModality(Modality.APPLICATION_MODAL);
	
	        // Geliştirici bilgileri
	        String developerName = "Name: Your Name Here";
	        String developerEmail = "Email: your.email@example.com";
	        String developerDetails = "Details: Experienced developer with expertise in JavaFX.";
	
	        // Bilgi alanları
	        Label nameLabel = new Label(developerName);
	        Label emailLabel = new Label(developerEmail);
	        Label detailsLabel = new Label(developerDetails);
	
	        // Dikey düzen
	        VBox layout = new VBox(10); // 10px boşluk
	        layout.getChildren().addAll(nameLabel, emailLabel, detailsLabel);
	        layout.setStyle("-fx-padding: 20; -fx-alignment: center; -fx-font-size: 14;");
	
	        // Sahne oluştur ve pencereye ekle
	        Scene scene = new Scene(layout, 400, 200); // Genişlik: 400px, Yükseklik: 200px
	        developerInfoStage.setScene(scene);
	
	        // Pencereyi göster
	        developerInfoStage.showAndWait();
	    }
	    
	    public void exit() {
	        // Çıkış onayı için bir uyarı penceresi göster
	        Alert alert = new Alert(AlertType.CONFIRMATION);
	        alert.setTitle("Exit Confirmation");
	        alert.setHeaderText("Are you sure you want to exit?");
	        alert.setContentText("All unsaved progress will be lost.");
	
	        // Kullanıcının seçimini al
	        Optional<ButtonType> result = alert.showAndWait();
	
	        if (result.isPresent() && result.get() == ButtonType.OK) {
	            // Uygulamayı kapat
	            Platform.exit();
	            System.out.println("Application closed.");
	        } else {
	            // Çıkış iptal edildi
	            System.out.println("Exit cancelled.");
	        }
	    }
	    

	
	    
	    public void connect() {
	        try (DatagramSocket udpSocket = new DatagramSocket()) {
	            udpSocket.setBroadcast(true);

	            String broadcastMessage = "Hello P2P Network!";
	            byte[] buffer = broadcastMessage.getBytes();
	            InetAddress broadcastAddress = InetAddress.getByName("255.255.255.255");

	            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, broadcastAddress, 5000);
	            udpSocket.send(packet);
	            System.out.println("UDP Broadcast gönderildi.");

	            udpSocket.setSoTimeout(5000);
	            byte[] responseBuffer = new byte[1024];
	            InetAddress responderAddress = null;

	            while (true) {
	                try {
	                    DatagramPacket responsePacket = new DatagramPacket(responseBuffer, responseBuffer.length);
	                    udpSocket.receive(responsePacket);

	                    String response = new String(responsePacket.getData(), 0, responsePacket.getLength());
	                    System.out.println("UDP Yanıt alındı: " + response + " from " + responsePacket.getAddress());

	                    if (response.equals("I am here!")) {
	                        responderAddress = responsePacket.getAddress();
	                        break;
	                    }
	                } catch (Exception e) {
	                    System.out.println("Keşif tamamlandı, daha fazla yanıt yok.");
	                    break;
	                }
	            }

	            udpSocket.close();
	            System.out.println("UDP kapatıldı.");

	            if (responderAddress != null) {
	                try {
	                    tcpSocket = new Socket(responderAddress, 6000);
	                    System.out.println("TCP bağlantısı kuruldu: " + responderAddress);
	                } catch (Exception e) {
	                    System.err.println("TCP bağlantısı kurulamadı.");
	                    e.printStackTrace();
	                }
	            } else {
	                System.out.println("Hiçbir düğüm bulunamadı.");
	            }
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	    }

	     
	    public void disconnect() {
	        if (!isConnected) {
	            System.out.println("Bağlı değil.");
	            return;
	        }
	
	        try {
	            tcpSocket.close();
	            isConnected = false;
	            System.out.println("TCP bağlantısı kapatıldı.");
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	   }
	    
		public void setShareFolder() {
		   
			// Ana pencereyi temsil eden bir Stage almanız gerekiyor
			Stage stage = new Stage();

			// FileChooser oluştur
			FileChooser fileChooser = new FileChooser();
			fileChooser.setTitle("Select File to Share");

			// Varsayılan klasör ayarı (isteğe bağlı)
			fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));

			// Tek bir dosya seçimini etkinleştir
			File selectedFile = fileChooser.showOpenDialog(stage);

			if (selectedFile != null) {
			    // Seçilen dosyayı işleme
			    String filePath = selectedFile.getAbsolutePath();
			    sendFile(selectedFile); // Dosyayı gönder

			    rootFolderField.setText(filePath);
			    System.out.println("Seçilen Dosya: " + filePath);
			} else {
			    System.out.println("Dosya seçimi iptal edildi.");
			}
		
		}
	
		public void setDestinationFolder() {
		    // Ana pencereyi temsil eden bir Stage almanız gerekiyor
		    Stage stage = new Stage();
	
		    // DirectoryChooser oluştur
		    DirectoryChooser directoryChooser = new DirectoryChooser();
		    directoryChooser.setTitle("Select Destination Folder");
	
		    // Varsayılan klasör ayarı (isteğe bağlı)
		    directoryChooser.setInitialDirectory(new File(System.getProperty("user.home")));
	
		    // Kullanıcı bir klasör seçer
		    File selectedFolder = directoryChooser.showDialog(stage);
	
		    if (selectedFolder != null) {
		    	String destinationFolderPath = selectedFolder.getAbsolutePath();
		        System.out.println("Seçilen Hedef Klasör: " + destinationFolderPath);
		        destinationFolderField.setText(destinationFolderPath);
		        reciveFile();
		    } else {
		        System.out.println("Hedef klasör seçimi iptal edildi.");
		    }
		}
		
		public void clickNewFilesOnly() {
			System.out.println("clickNewFilesOnly");
		}
		
		public void addNewFiles() {
			System.out.println("addNewFiles");
			addProgressBar("example.txt");
		}
		
		public void delNewFiles() {
			System.out.println("delNewFiles");
			removeProgressBar("example.txt");
		}
		public void addExecutedFiles() {
			System.out.println("addExecutedFiles");
		}
		public void delExecutedFiles() {
			System.out.println("delExecutedFiles");
		}
		public void search() {
			System.out.println("search");
		}
		
	}
