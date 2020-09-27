
package com.webmail.controllers;

import com.webmail.Classification;
import com.webmail.Mails;
import static com.webmail.Mails.stmt;
import com.webmail.State;
import static com.webmail.controllers.MainController.conf;
import java.io.IOException;
import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javax.mail.Flags;
import javax.mail.Flags.Flag;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Store;


public class MoveController implements Initializable {
    @FXML Button btnMove, btnCancel;
    @FXML ListView folderListView;
    
    private String currentFolderName;
    private String movingMessage;
    
    public void setCurrentFolderName(final String cfn){
        this.currentFolderName = cfn;
    }
    
    public void setMovingMessage(final String mm){
        this.movingMessage = mm;
    }
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        folderListView.setItems(Mails.folders);
        
        btnMove.disableProperty().bind(folderListView.getSelectionModel().selectedItemProperty().isNull());
        
        btnMove.setOnAction((ActionEvent c) -> {
            System.out.println("moving message: " + movingMessage);
            String destFolder = (String)folderListView.getSelectionModel().getSelectedItem();
            
            if (destFolder.equals(currentFolderName)) {
                // display error: cannot move to same folder
            }
            
            // sanitize the subject of the email to be moved to match it
            String subjectOfMoving = movingMessage.substring(0, 150);
            int idx = subjectOfMoving.length()-1;
            while (subjectOfMoving.charAt(idx) == ' ') {
                --idx;
            }
            subjectOfMoving = subjectOfMoving.substring(0, idx+1);
            
            // check if shortened
            if (subjectOfMoving.endsWith("...")) {
                subjectOfMoving = subjectOfMoving.substring(0, subjectOfMoving.length() - 3);
            }
            
            // MOVE IMAP
            String user = State.username;
            String pass = State.password;

            final Properties props = new Properties();
            props.put("mail.store.protocol", "gimap");
            props.put("mail.gimap.ssl.enable", "true");
            props.put("mail.gimap.host", "imap.gmail.com");
            props.put("mail.gimap.port", "993");
            //props.put("mail.imap.connectiontimeout", "5000");
            //props.put("mail.imap.timeout", "5000");

            Message desiredIMAPMessage = null;
            
            try
            {
                Session session = Session.getDefaultInstance(props, null);
                Store store = session.getStore("gimap");

                store.connect("imap.gmail.com", user, pass);
                
                String srcFol = currentFolderName;
                if(srcFol.equalsIgnoreCase("Inbox/Primary"))
                    srcFol = "inbox";
                else if(srcFol.equalsIgnoreCase("Inbox/Social"))
                    srcFol = "inbox";
                else if(srcFol.equalsIgnoreCase("Inbox/Promotions"))
                    srcFol = "inbox";
                
                Folder sourceFolder = store.getFolder(srcFol);     
                sourceFolder.open(Folder.READ_WRITE);
                
                Message[] msgs = sourceFolder.getMessages();
                for (Message m : msgs) {
                    if (m != null && m.getSubject() != null && m.getSubject().startsWith(subjectOfMoving)) {
                        desiredIMAPMessage = m;
                        break;
                    }
                }
                if (desiredIMAPMessage == null) {
                    // we have a problem
                    throw new Exception("IMAP message not found");
                }
                
                if(destFolder.equalsIgnoreCase("Inbox/Primary"))
                    destFolder = "inbox";
                else if(destFolder.equalsIgnoreCase("Inbox/Social"))
                    destFolder = "inbox";
                else if(destFolder.equalsIgnoreCase("Inbox/Promotions"))
                    destFolder = "inbox";
                Folder destinationFolder = store.getFolder(destFolder);
                destinationFolder.open(Folder.READ_WRITE);
                
                Message[] arr = new Message[1];
                arr[0] = desiredIMAPMessage;
                sourceFolder.copyMessages(arr, destinationFolder);
                
                sourceFolder.setFlags(arr, new Flags(Flag.DELETED), true);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            // MOVE DB
            // sanitize folder name
            destFolder = sanitizeFolderNameSQL(destFolder);
            currentFolderName = sanitizeFolderNameSQL(currentFolderName);
            
            try {
                // get new sno
                ResultSet rs = stmt.executeQuery("SELECT * FROM " + destFolder+"");
                int highest = 1;
                while (rs.next()) {
                    int sno = rs.getInt(1);
                    if (sno > highest) {
                        highest = sno;
                    }
                }
                
                String CREATE_IN_NEW_TABLE_SQL = "INSERT INTO "+destFolder+" VALUES (?, ?, ?, ?, ?)";
                if(destFolder.equals("inbox"))
                    CREATE_IN_NEW_TABLE_SQL = "INSERT INTO "+destFolder+" VALUES (?, ?, ?, ?, ?, ?)";
            
                String DELETE_FROM_OLD_TABLE_SQL = "DELETE FROM " + currentFolderName + " WHERE subject_ LIKE '" + subjectOfMoving + "%'";
                
                Message msg = desiredIMAPMessage;
                int index = highest + 1;
                
                // INSERT INTO NEW TABLE
                PreparedStatement pstmt = conf.prepareStatement(CREATE_IN_NEW_TABLE_SQL);
                pstmt.setInt(1, index);
                Object to = msg.getRecipients(Message.RecipientType.TO)[0];
                pstmt.setString(2, to==null?"":to.toString());
                pstmt.setString(3, msg.getSubject());
                pstmt.setString(4, getText(msg));
                pstmt.setString(5, msg.getSentDate().toString());
                if(destFolder.equals("inbox"))
                    pstmt.setString(6, Classification.getClass(msg.getSubject(), msg.getFrom()[0].toString()));
                
                pstmt.executeUpdate();
                pstmt.close();
                
                // DELETE FROM OLD TABLE
                stmt.executeUpdate(DELETE_FROM_OLD_TABLE_SQL);
                pstmt.close();
            }
            catch (SQLException | MessagingException | IOException e) {
                e.printStackTrace();
            }
            
            btnCancel.getScene().getWindow().hide();
        });
        
        btnCancel.setOnAction(c -> {
            btnCancel.getScene().getWindow().hide();
        });
    }
    
    public String getText(Part p) throws
		MessagingException, IOException {
		if (p.isMimeType("text/*")) {
			String s = (String) p.getContent();
			return s;
		}

		if (p.isMimeType("multipart/alternative")) {
			// prefer html text over plain text
			Multipart mp = (Multipart) p.getContent();
			String text = null;
			for (int i = 0; i < mp.getCount(); i++) {
				Part bp = mp.getBodyPart(i);
				if (bp.isMimeType("text/plain")) {
					if (text == null)
						text = getText(bp);
					continue;
				} else if (bp.isMimeType("text/html")) {
					String s = getText(bp);
					if (s != null)
						return s;
				} else {
					return getText(bp);
				}
			}
			return text;
		} else if (p.isMimeType("multipart/*")) {
			Multipart mp = (Multipart) p.getContent();
			for (int i = 0; i < mp.getCount(); i++) {
				String s = getText(mp.getBodyPart(i));
				if (s != null)
					return s;
			}
		}
		return null;
	}
    
    public String sanitizeFolderNameSQL(String destFolder) {
        destFolder= destFolder.toLowerCase();
        destFolder = destFolder.replace("[gmail]/", "");
        destFolder = destFolder.replace(" ", "");
        if (destFolder.contains("inbox/")) {
            destFolder = "inbox";
        }
        
        return destFolder;
    }
}
