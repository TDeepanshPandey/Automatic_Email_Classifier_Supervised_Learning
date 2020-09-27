package com.webmail.controllers;

import com.webmail.Classification;
import com.webmail.Conn;
import com.webmail.SaveData;
import com.webmail.State;
import static com.webmail.controllers.MainController.conf;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.web.HTMLEditor;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.*;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.regex.Pattern;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;

public class ComposeController implements Initializable {
	@FXML public TextField tbDest, tbCc, tbBcc, tbSubject, tbFileName;
	@FXML public HTMLEditor htmlEditor;
	@FXML public Button btnAttach, btnSend;
        public static Statement st1 = Conn.getStmt() ;
	File file = null;
        Stage stageLoading = null;

	Stage getStage() {
		return (Stage) tbDest.getScene().getWindow();
	}

	public static final Pattern EmailEx = Pattern.compile("^[\\\\\\\\w!#$%&’*+/=?`{|}~^-]+(?:\\\\\\\\.[\\\\\\\\w!#$%&’*+/=?`{|}~^-]+)*@(?:[a-zA-Z0-9-]+\\\\\\\\.)+[a-zA-Z]{2,6}$", Pattern.CASE_INSENSITIVE);

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		btnSend.setOnAction(event -> {
			try {
				Properties props = new Properties();
				props.put("mail.smtp.starttls.enable", "true");
				props.put("mail.smtp.host", "smtp.gmail.com");
				props.put("mail.smtp.auth", "true");
				props.put("mail.smtp.port", "587");


				Session sesh = Session.getInstance(props, new Authenticator() {
					@Override
					protected PasswordAuthentication getPasswordAuthentication() {
						return new PasswordAuthentication(State.username, State.password);
					}
				});

				MimeMessage msg = new MimeMessage(sesh);
				msg.setFrom(new InternetAddress(State.username));

				String to = tbDest.getText();
				List<String> ccs = Arrays.asList(tbCc.getText().split(" "));
				List<String> bccs = Arrays.asList(tbBcc.getText().split(" "));

//				if (EmailEx.matcher(to).find())
					msg.addRecipient(Message.RecipientType.TO, new InternetAddress(to));

				for (String cc : ccs) {
					if (EmailEx.matcher(cc).find())
						msg.addRecipient(Message.RecipientType.CC, new InternetAddress(cc));
				}

				for (String bcc : bccs) {
					if (EmailEx.matcher(bcc).find())
						msg.addRecipient(Message.RecipientType.BCC, new InternetAddress(bcc));
				}

				msg.setSubject(tbSubject.getText());

				Multipart multipart = new MimeMultipart();

				BodyPart msgBody = new MimeBodyPart();
				msgBody.setContent(htmlEditor.getHtmlText(), "text/html; charset=utf-8");
				multipart.addBodyPart(msgBody);

				if (file != null && file.exists()) {
					BodyPart msgAttach = new MimeBodyPart();
					DataSource src = new FileDataSource(file);
					msgAttach.setDataHandler(new DataHandler(src));
					msgAttach.setFileName(file.getName());
					multipart.addBodyPart(msgAttach);
				}

				msg.setContent(multipart);

                                showLoading();
				Transport.send(msg);
                                
                                fs();
                                hideLoading();

				getStage().close();
			} catch (MessagingException e) {
				e.printStackTrace();
			}
		});

		btnSend.disableProperty().bind(Bindings.or(tbDest.textProperty().isEmpty(), tbSubject.textProperty().isEmpty()));
		btnAttach.setOnAction(c -> {
			FileChooser chooser = new FileChooser();
			File file = chooser.showOpenDialog(getStage());

			if (file != null) {
				this.file = file;
				tbFileName.setText(file.getName());
			}
		});
	}
        
        private void showLoading() {
		URL uri = ComposeController.class.getResource("sent.fxml");
		FXMLLoader loader = new FXMLLoader(uri);
		try {
			loader.load();
		} catch (IOException e) {
                    e.printStackTrace();
		}
                
		stageLoading = new Stage();
		stageLoading.setTitle("Sending Mail...");
		stageLoading.setResizable(false);
                stageLoading.setScene(new Scene(loader.getRoot(), 650, 250));
		stageLoading.show();
	}

	private void hideLoading() {
		if (stageLoading != null) {
			Platform.runLater(() -> stageLoading.close());
		}
	}
        
        private void fs(){
            Platform.runLater(() -> {
			String user = State.username;
			String pass = State.password;

			final Properties props = new Properties();
			props.put("mail.store.protocol", "imap");
			props.put("mail.imap.ssl.enable", "true");
			props.put("mail.imap.host", "imap.gmail.com");
			props.put("mail.imap.port", "993");
			//props.put("mail.imap.connectiontimeout", "5000");
			//props.put("mail.imap.timeout", "5000");

			try
                        {
                            Session session = Session.getDefaultInstance(props, null);
                            Store store = session.getStore("imap");
                            //System.out.println("username : "+user+" , password : "+pass);                           
                                    
                            store.connect("imap.gmail.com", user, pass);
                            
                            Folder inboxFolder = store.getFolder("[Gmail]/Sent Mail");
                            inboxFolder.open(Folder.READ_ONLY);
                            Message[] arr = inboxFolder.getMessages();	
					
                            int j = arr.length;
	
                                                        
                            String s2 = "delete from sentmail";
                            System.out.println(s2);
                            st1.execute(s2);
                            
                            String WRITE_OBJECT_SQL = "INSERT INTO sentmail VALUES (?, ?, ?, ?, ?)";

                   // static final String READ_OBJECT_SQL = "SELECT * FROM spam WHERE sno = ?";
                                for(int i=0; i<j;i++)
					{
						//System.out.println("\n--------------------------Message"+(i+1)+"--------------------------");
						//arr[i].writeTo(System.out);
						Address[] from = arr[i].getFrom();
						Object op = arr[i].getContent();
						String msg="";
						String msg2=(String)arr[i].getSubject();
						if(msg2!=null)
                                                    msg2 = msg2.replace("'",",,");
						String msg3=""+arr[i].getSentDate();
						
						//System.out.println("From : " + from[0]);
						//System.out.println("Subject : " + arr[i].getSubject());
						//System.out.println("Date : " + msg3);
						//System.out.println("Message : " + op);
				
    
    					String className = ""+from[0];
    					
    					
    					PreparedStatement pstmt = conf.prepareStatement(WRITE_OBJECT_SQL);
						msg = SaveData.getText(arr[i]);
						//System.out.println(msg);
					    // set input parameters
					    pstmt.setInt(1, (j-i));
					    pstmt.setString(2, className);
					    pstmt.setString(3, msg2);
					    pstmt.setString(4, msg);
					    pstmt.setString(5, msg3);
                                            pstmt.executeUpdate();
					
					    // get the generated key for the id
					    
					
					    pstmt.close();
					    
					}
                                System.out.println("sentmail is stored");
					
                        }
                        catch(Exception me)
                        {
                            me.printStackTrace();
                        }
            });
        }
}
