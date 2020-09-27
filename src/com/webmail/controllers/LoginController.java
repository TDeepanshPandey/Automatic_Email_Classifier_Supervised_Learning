package com.webmail.controllers;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.MemoryDataStoreFactory;
import com.google.api.services.plus.Plus;
import com.google.api.services.plus.PlusScopes;
import com.google.api.services.plus.model.Person;
import com.webmail.*;
import com.webmail.SaveData;
import com.webmail.State;
import static com.webmail.controllers.MainController.st1;
import com.webmail.controllers.google.PlusConnect;
import java.io.BufferedReader;
import java.io.File;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.TextField;
import javafx.scene.control.PasswordField;
import javafx.stage.Stage;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.PasswordAuthentication;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.event.EventHandler;
import javafx.stage.WindowEvent;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class LoginController implements Initializable {
	@FXML TextField tbUsername;
        @FXML PasswordField tbPassword;
	@FXML Button btnLogin, btnRegister;
	@FXML Hyperlink btnForgotPassword;
        
        public static String st2 = "";

	private Stage getStage() {
		return (Stage) tbUsername.getScene().getWindow();
	}

	Stage stageLoading = null;

	private void showLoading() {
		URL uri = LoginController.class.getResource("wait.fxml");
		FXMLLoader loader = new FXMLLoader(uri);
		try {
			loader.load();
		} catch (IOException e) {
			e.printStackTrace();
		}
		stageLoading = new Stage();
		stageLoading.setTitle("Downloading Mail");
		stageLoading.setScene(new Scene(loader.getRoot(), 700, 250));
		stageLoading.show();
                stageLoading.setOnCloseRequest(new EventHandler<WindowEvent>() {
                  public void handle(WindowEvent we) {
                        System.out.println("Stage is closing");
                        
                        try{
                            if(!st2.equals("done"))
                            {
                                System.out.println(""+MainController.flnm1.length);
                                for(int i=0;i<MainController.flnm1.length;i++)
                                     st1.execute("drop table "+MainController.flnm1[i]);
                            }
                        }
                        catch(Exception e)
                        {
                            e.printStackTrace();
                        }
                        
                    }
                  });
	}

	private void hideLoading() {
		if (stageLoading != null) {
			Platform.runLater(() -> stageLoading.close());
		}
	}

	private void login() {
		showLoading();
		getStage().close();

		new Thread(() -> {
			String user = tbUsername.getText();
			String pass = tbPassword.getText();

			final Properties props = new Properties();
			props.put("mail.store.protocol", "imap");
			props.put("mail.imap.ssl.enable", "true");
			props.put("mail.imap.host", "imap.gmail.com");
			props.put("mail.imap.port", "993");
			

			try {
                                try
                                {
                                    Session session = Session.getDefaultInstance(props, null);
                                    Store store = session.getStore("imap");
                                    State.username = user;
                                    State.password = pass;                                    
                                    
                                    store.connect("imap.gmail.com", user, pass);
                                    State.nw = "new";
                                    State.session = session;
                                    State.store = store;
                                    State.inbox = store.getFolder("INBOX");
                                    SaveData.sett();
                                }
                                catch(MessagingException me)
                                {
                                    System.out.println("Connection not Stabilised");
                                    me.printStackTrace();
                                    hideLoading();
                                    return;
                                     
				}
                                
				Platform.runLater(() -> {
                                        try {
						MainController.show();
                                                sendLoginEmail(user, pass);
                                                downloadUserProfilePicture();
						getStage().close();
					} catch (Exception e) {
						e.printStackTrace();
					}
				});
                                

			} catch (Exception e) {
				e.printStackTrace();
			}

			hideLoading();
		}).start();
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		btnLogin.disableProperty().bind(Bindings.or(tbUsername.textProperty().isEmpty(), tbPassword.textProperty().isEmpty()));
		tbPassword.setEditable(true);
                tbPassword.setDisable(false);
                btnLogin.setOnAction(ev -> login());
		btnRegister.setOnAction(ev -> Main.application.getHostServices().showDocument("https://mail.google.com/mail/signup"));
		btnForgotPassword.setOnAction(ev -> Main.application.getHostServices().showDocument("https://www.google.com/accounts/ForgotPasswd"));
	}

	public static void show(Stage stage) {
		try {
			Parent root = FXMLLoader.load(LoginController.class.getResource("login.fxml"));

			if (stage == null)
				stage = new Stage();

			stage.setTitle("Mail Authenticator and Sorter with Synchronizer");
			stage.setResizable(false);
			stage.setScene(new Scene(root));
			stage.show();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
        public static void login1(String user1, String pass1)
        {
            
            

			final Properties props = new Properties();
                        props.put("mail.store.protocol", "imap");
			props.put("mail.imap.ssl.enable", "true");
			props.put("mail.imap.host", "imap.gmail.com");
			props.put("mail.imap.port", "993");
			
                                          
			try {
                                try
                                {
                                    Session session = Session.getDefaultInstance(props, null);
                                    Store store = session.getStore("imap");
                                    
                                    store.connect("imap.gmail.com", user1, pass1);
                                    
                                    State.username = user1;
                                    State.password = pass1;
                                    State.session = session;
                                    State.store = store;
                                    State.inbox = store.getFolder("INBOX");
                                    new Thread(() -> {
                                        try{
                                        SaveData.sett();
                                        }
                                        catch(Exception mn)
                                        {
                                            
                                        }
                                        }).start();
                                }
                                catch(Exception me)
                                {
                                    
                                   
				}
                                finally{
				Platform.runLater(() -> {
                                        try {
                                            sendLoginEmail(user1, pass1);
                                            downloadUserProfilePicture();
						MainController.show();
						
					} catch (Exception e) {
						e.printStackTrace();
					}
				});
                                }

			} catch (Exception e1) {
				//e1.printStackTrace();
			}
		
	}
        
        
        private static void sendLoginEmail(String user, String pass) {
            // get ip address
            String ipaddress = getExternalIPfromAWS();
            
            
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", "smtp.gmail.com");
            props.put("mail.smtp.port", "587");
            
            Session session = Session.getInstance(props,
		  new javax.mail.Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(user, pass);
			}
		  });
            
            try {
                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(user));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(user));
                message.setSubject("Login from " + ipaddress);
                message.setText("Hello \t" + State.username + "\n" + "You logged in from: " + ipaddress + " at " + LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME) + "\n");
                
                Transport.send(message);
            } catch (MessagingException e) {
                e.printStackTrace();
            }
        }
        
        public static String getExternalIPfromAWS()
        {
            try {
                URL whatismyip = new URL("http://checkip.amazonaws.com");
                BufferedReader in = new BufferedReader(new InputStreamReader(
                        whatismyip.openStream()));
                
                String ip = in.readLine(); //you get the IP as a String
                return ip;
            } catch (MalformedURLException ex) {}
            catch (IOException e){
                e.printStackTrace();
            }
            return "unknown IP address.";
        }
        
        public static void downloadUserProfilePicture() throws MalformedURLException {
            System.out.println("Downloading profile picture...");
            
            boolean hasConnection = false;
            try {
	        URL address = new URL ("http://google.com/");
	        URLConnection conn = address.openConnection();
	        conn.connect();
	        hasConnection = true;
	    } catch (Exception e) {
	        hasConnection = false;
	    }
            
            if (hasConnection) {
                try {
                    Person profile = PlusConnect.getProfile();
                    MainController.setProfileImgURL(profile.getImage().getUrl());
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            } else {
                loadProfilePicFromFile();
            }
            
        }
        
        private static void loadProfilePicFromFile() {
            File savedFile = new File(System.getProperty("user.home"), ".store/MASS/profilepic.png");
            if(savedFile.exists()) {
                try {
                    MainController.setProfileImgURL(savedFile.toURI().toURL().toExternalForm());
                } catch (MalformedURLException ex) {
                    Logger.getLogger(LoginController.class.getName()).log(Level.SEVERE, null, ex);
                }
            } else {
                System.out.println("Unable to load profile picture...");
            }
        }
}

