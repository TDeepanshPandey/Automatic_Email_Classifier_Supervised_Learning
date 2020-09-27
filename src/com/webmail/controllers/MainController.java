package com.webmail.controllers;

import java.sql.*;
import javafx.event.EventHandler;
import javafx.stage.WindowEvent;
import com.webmail.LoadMoreMessage;
import com.webmail.*;
import com.webmail.State;
import java.awt.Desktop;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import java.util.Properties;
import javax.mail.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.embed.swing.SwingFXUtils;

import javafx.event.ActionEvent;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.util.Pair;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.filechooser.FileSystemView;
import org.apache.commons.lang3.StringUtils;

public class MainController implements Initializable {
	@FXML public Label lbWelcome, lbSubject, lbFrom, lbDate;
	@FXML public Button btnInbox, btnFolders, btnCompose, btnCreateFolder, btnTrash, btnPreferences, btnLogout,btnRefresh, btnSaveAttachment;
	@FXML public ListView lvMails;
	@FXML public ListView lvFolders;
	@FXML public HBox paneFolders, paneAttachments;
	@FXML public VBox paneList;
	@FXML public WebView viewMail;
        @FXML public ImageView profileImg;

	public static final int PAGE_SIZE = 25;
        public static Connection conf = Main.cn.getConf();
        public static String[] flnm1  ;
	public Folder activeFolder;
        public String actFolder = "";
	public Thread threadAddFolderContent;
	public int pageIndex = 0;
        public static Statement st1 = Main.st ;

        private List<Pair<File, String>> attachments = new ArrayList<>();
        
	public Stage getStage() {
		return (Stage) paneFolders.getScene().getWindow();
	}

        private static StringProperty profileImgURL = new SimpleStringProperty();
        public static void setProfileImgURL(String s) {
            profileImgURL.setValue(s);
        }
        
        public void setProfileImgView() {
            try {
                Image i = new Image(profileImgURL.getValue(), false);
                profileImg.setImage(i);
                
                // write image to file for next time
                File outputFile = new File(System.getProperty("user.home"), ".store/MASS/profilepic.png");
                if (!outputFile.exists()) {
                    Files.createDirectories(outputFile.getParentFile().toPath());
                    Files.createFile(outputFile.toPath());
                }
                BufferedImage bImage = SwingFXUtils.fromFXImage(i, null);
                try {
                  ImageIO.write(bImage, "png", outputFile);
                } catch (IOException e) {
                  throw new RuntimeException(e);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
	public void updateFolder(Folder folder, int page, boolean clears) {
		if (activeFolder != null) {
			// close the current folder if any
			try {
				if (folder.isOpen())
					folder.close(false);
			} catch (MessagingException e) {
				e.printStackTrace();
			}
		}

		// if we are currently adding the content of a folder to the email list, cancel the thread
		if (threadAddFolderContent != null && threadAddFolderContent.isAlive())
			threadAddFolderContent.stop();

		activeFolder = folder;
		threadAddFolderContent = new Thread(() -> {
			try {
				folder.open(Folder.READ_WRITE);

				if (clears)
					Platform.runLater(() -> lvMails.getItems().clear());

				int c = folder.getMessageCount();

				if (c == 0)
					return;

				final int cuts = PAGE_SIZE - 1;
				int[] range = new int[2];

				// this is pretty much magic
				if (page == 1) {
					range[0] = c - cuts * page;
					range[1] = c - cuts * (page - 1);
				} else {
					range[0] = c - cuts * page - (page - 1);
					range[1] = c - cuts * (page - 1) - (page - 1);
				}

				range[0] = Math.max(1, Math.min(c, range[0]));
				range[1] = Math.max(1, Math.min(c, range[1]));
				// end of magic

				System.out.println(page + ", " + range[0] + ", " + range[1]);

				Message[] msgs = folder.getMessages(range[0], range[1]);
				List<Message> list = Arrays.asList(msgs);

				Collections.reverse(list);

				Platform.runLater(() -> {
					List<Message> items = lvMails.getItems();
					if (items.size() > 0 && items.get(items.size() - 1) instanceof LoadMoreMessage) {
						items.remove(items.size() - 1);
					}

					items.addAll(list);

					if (items.size() < c) {
						// we still have messages left to load, add the "load more elements" thing
						items.add(new LoadMoreMessage());
					}
				});

				pageIndex = page;
			} catch (MessagingException e) {
				e.printStackTrace();
			}
		});
		threadAddFolderContent.start();
	}

	void setWebview(Message msg) {
		new Thread(() -> {
			try {
				String text = "";
				if (msg != null) {
					Platform.runLater(() -> {
						try {
							lbSubject.setText(msg.getSubject());
							lbFrom.setText("");
							for (Address addr : Arrays.asList(msg.getFrom())) {
								if (lbFrom.getText().equalsIgnoreCase(""))
									lbFrom.setText(addr.toString());
								else
									lbFrom.setText(lbFrom.getText() + ", " + addr.toString());
							}
							lbDate.setText(msg.getSentDate().toLocaleString());
						} catch (MessagingException e) {
							e.printStackTrace();
						}
					});

					msg.setFlag(Flags.Flag.SEEN, true);
					text = getText(msg);
				} else {
					Platform.runLater(() -> {
						lbSubject.setText("");
						lbFrom.setText("");
						lbDate.setText("");
					});
				}
				final String ftext = text;
				Platform.runLater(() -> {
					viewMail.getEngine().loadContent(ftext);
				});
			} catch (MessagingException | IOException e) {
				e.printStackTrace();
			}
		}).start();
	}

	void deleteSelection() {
		/*try {
			for (Message mail : lvMails.getSelectionModel().getSelectedItems())
				mail.setFlag(Flags.Flag.DELETED, true);

			lvMails.getItems().removeAll(lvMails.getSelectionModel().getSelectedItems());
		} catch (MessagingException e) {
			e.printStackTrace();
		}*/
	}

	void setList(ListView lv) {
		paneList.getChildren().clear();
		paneList.getChildren().add(lv);
	}
        void sn(){
            
        }
	void loadFolders() {
		try {
			
                        lvFolders.getItems().clear();
                        Mails.setFolders();
			lvFolders.setItems(Mails.folders);

			lvFolders.getSelectionModel().selectedItemProperty().addListener((obv, o, n) -> {
				//updateFolder(n, 1, true);
                                String sh = (String)n;
                                
                                System.out.println(sh+" gvjvjgjgfjg");
                                actFolder = sh;
                                
                                System.out.println(actFolder);
                                
                                lvMails.getItems().clear();
                                try{
                                    Mails.setMails(sh);
                                    
                                }
                                catch(Exception e4)
                                {
                                    e4.printStackTrace();
                                }
                                lvMails.setItems(Mails.mails);
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	void createFolder() {
		String name = InputDialogController.show(getStage());
		if (name != null) {
			try {
				Folder newfolder = State.store.getFolder(name);
				if (newfolder.exists()) {
					new Alert(Alert.AlertType.ERROR,  "A folder called " + name + " already exists!", ButtonType.OK).showAndWait();
					return;
				}
				newfolder.create(Folder.HOLDS_MESSAGES);
                                String sn = name;
                                Mails.folders.add(sn);
                                lvFolders.getItems().clear();
                                lvFolders.setItems(Mails.folders);
                                try{
                                    ResultSet rs = st1.executeQuery("SELECT MAX(no)FROM folder");
                                    if(rs.next())
                                    st1.execute("insert into folder values("+(rs.getInt(1)+1)+", '"+sn+"')");
                                    sn = sn.replace(" ", "");
                                    String s3 = "CREATE TABLE "+sn+"( sno int(10) unsigned NOT NULL, to_ varchar(200) NOT NULL, subject_ text, message_ text, date_ varchar(200) NOT NULL, PRIMARY KEY (`sno`) ) " ;
                                    st1.execute(s3);
                                }
                                catch(Exception ex)
                                {
                                    ex.printStackTrace();
                                }
			} catch (MessagingException e) {
				e.printStackTrace();
			}
		}

		loadFolders();
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		try{
                    Mails.setMails("inbox");
                }
                catch(Exception el)
                {
                    el.printStackTrace();
                }
                lvMails.setItems(Mails.mails);
		lvMails.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

		lbWelcome.setText("Welcome " + State.username);
                
                setProfileImgView();
                profileImgURL.addListener(new ChangeListener<String>(){
                    @Override
                    public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                        setProfileImgView();
                    }
                });
                
                btnSaveAttachment.disableProperty().bind(Bindings.isEmpty(paneAttachments.getChildren()));
                
                MenuItem itemMove = new MenuItem("Move");
                itemMove.setOnAction(c -> {
                    // display menu to select destination folder
                    try {
                                FXMLLoader fxmlLoader = new FXMLLoader();
                                Parent parent = fxmlLoader.load(getClass().getResource("move.fxml").openStream());
                                MoveController moveController = (MoveController) fxmlLoader.getController();
                                
                                moveController.setCurrentFolderName(actFolder);
                                String mm = (String)lvMails.getSelectionModel().getSelectedItem();
                                moveController.setMovingMessage(mm);
                        
				Stage stage = new Stage();
				stage.setTitle("Move Mail to Folder");
				stage.setResizable(false);
				stage.setScene(new Scene(parent, 600, 500));
				stage.showAndWait();
                                
                                btnRefresh.fire();
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
                
		MenuItem itemCompose = new MenuItem("Compose");
		itemCompose.setOnAction(c -> {
			try {
				Parent parent = FXMLLoader.load(getClass().getResource("compose.fxml"));
				Stage stage = new Stage();
				stage.setTitle("Compose Mail");
				stage.setResizable(false);
				stage.setScene(new Scene(parent, 1100, 650));
				stage.show();
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
                
                MenuItem itemComposefolder = new MenuItem("Compose");
		itemComposefolder.setOnAction(c -> {
			try {
				Parent parent = FXMLLoader.load(getClass().getResource("compose.fxml"));
				Stage stage = new Stage();
				stage.setTitle("Compose Mail");
				stage.setResizable(false);
				stage.setScene(new Scene(parent, 1100, 650));
				stage.show();
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
                
		MenuItem itemRefresh = new MenuItem("Refresh");
		itemRefresh.setOnAction((ActionEvent c) -> {
			//updateFolder(activeFolder, 1, true);
                        Platform.runLater(new Runnable() {
                            @Override
                            public void run() {
                                String user = State.username;
                                String pass = State.password;
                                
                                final Properties props = new Properties();
                                props.put("mail.store.protocol", "gimap");
                                props.put("mail.gimap.ssl.enable", "true");
                                props.put("mail.gimap.host", "imap.gmail.com");
                                props.put("mail.gimap.port", "993");
                                //props.put("mail.imap.connectiontimeout", "5000");
                                //props.put("mail.imap.timeout", "5000");
                                
                                try
                                {
                                    Session session = Session.getDefaultInstance(props, null);
                                    Store store = session.getStore("gimap");
                                    //System.out.println("username : "+user+" , password : "+pass);
                                    
                                    store.connect("imap.gmail.com", user, pass);
                                    String fm = actFolder;
                                    if(fm.equalsIgnoreCase("Primary"))
                                        fm = "inbox";
                                    else if(fm.equalsIgnoreCase("Social"))
                                        fm = "inbox";
                                    else if(fm.equalsIgnoreCase("Promotion"))
                                        fm = "inbox";
                                    Folder inboxFolder = store.getFolder(fm);
                                    inboxFolder.open(Folder.READ_ONLY);
                                    Message[] arr = inboxFolder.getMessages();                                    
                                    
                                    int j = arr.length;
                                    
                                    System.out.println("No of Message : "+j);
                                    String fol = actFolder;
                                    if(fol.equalsIgnoreCase("Primary"))
                                        fol = "inbox";
                                    else if(fol.equalsIgnoreCase("Social"))
                                        fol = "inbox";
                                    else if(fol.equalsIgnoreCase("Promotion"))
                                        fol = "inbox";
                                    fol = fol.toLowerCase();
                                    fol = fol.replace("[gmail]/", "");
                                    fol = fol.replace(" ", "");
                                    System.out.println(fol);
                                    
                                    String s2 = "delete from "+fol;
                                    System.out.println(s2);
                                    st1.execute(s2);
                                    
                                    String WRITE_OBJECT_SQL = "INSERT INTO "+fol+" VALUES (?, ?, ?, ?, ?)";
                                    
                                    if(fol.equals("inbox"))
                                        WRITE_OBJECT_SQL = "INSERT INTO "+fol+" VALUES (?, ?, ?, ?, ?, ?)";
                                    
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
                                        msg = getText(arr[i]);
                                        //System.out.println(msg);
                                        // set input parameters
                                        pstmt.setInt(1, (j-i));
                                        pstmt.setString(2, className);
                                        pstmt.setString(3, msg2);
                                        pstmt.setString(4, msg);
                                        pstmt.setString(5, msg3);
                                        
                                        if(fol.equals("inbox"))
                                            pstmt.setString(6, Classification.getClass(msg, null));
                                        pstmt.executeUpdate();
                                        
                                        // get the generated key for the id
                                        
                                        
                                        pstmt.close();
                                        
                                    }
                                    System.out.println(""+fol+" is stored");
                                    
                                }
                                catch(Exception me)
                                {
                                    me.printStackTrace();
                                }
                                finally
                                {
                                    
                                    try{
                                        Mails.setMails(actFolder);
                                        
                                    }
                                    catch(Exception e4)
                                    {
                                        e4.printStackTrace();
                                    }
                                    lvMails.getItems().clear();
                                    lvMails.setItems(Mails.mails);
                                }   }
                        });
		});

		MenuItem itemDelete = new MenuItem("Delete");
		itemDelete.disableProperty().bind(Bindings.size(lvMails.getSelectionModel().getSelectedItems()).isEqualTo(0));
		itemDelete.setOnAction(c -> {
                    String svn = (String)lvMails.getSelectionModel().getSelectedItem();
                    if(svn == null)
                        return;
                    int ln = svn.length();
                    //System.out.println(sn);
                    svn = svn.substring(ln-28 ,ln);
                    System.out.println(svn);
                    try{
                        String sh = actFolder;
                        if(sh.equalsIgnoreCase("Primary"))
                            sh = "inbox";
                        else if(sh.equalsIgnoreCase("Social"))
                            sh = "inbox"; 
                        else if(sh.equalsIgnoreCase("Promotion"))
                            sh = "inbox";
                        sh = sh.toLowerCase();
                        sh = sh.replace("[gmail]/", "");
                        sh = sh.replace(" ", "");
                    if(!sh.equals("trash"))
                    {
                        
                        ResultSet rs7 = st1.executeQuery("SELECT MAX(sno)FROM trash");
                        int mx = 1 ;
                        if(rs7.next())
                        { 
                            mx= rs7.getInt(1)+1;
                            System.out.println(""+mx);
                        }
                        String Query = "select * from "+sh+" where date_='"+svn+"'";
                        ResultSet rs6 = st1.executeQuery(Query);
                        if(rs6.next())
                        {
                            String WRITE_OBJECT_SQL = "INSERT INTO trash VALUES (?, ?, ?, ?, ?)";
                            PreparedStatement pstmt = conf.prepareStatement(WRITE_OBJECT_SQL);
                            pstmt.setInt(1, mx);
                            pstmt.setString(2, rs6.getString(2));
                            pstmt.setString(3, rs6.getString(3));
                            pstmt.setString(4, rs6.getString(4));
                            pstmt.setString(5, rs6.getString(5));
                            pstmt.executeUpdate();
                            pstmt.close();
                            
                            st1.execute("delete from "+sh+" where date_ = '"+svn+"'");
                            lvMails.getItems().clear();
                            Mails.setMails(actFolder);
                            lvMails.setItems(Mails.mails);
                        }
                    }
                    else
                    {
                        st1.execute("delete from "+sh+" where date_ = '"+svn+"'");
                        lvMails.getItems().clear();
                        Mails.setMails(actFolder);
                        lvMails.setItems(Mails.mails);
                    }
                    }
                    catch(Exception e7)
                    {
                        e7.printStackTrace();
                    }
                });

		MenuItem itemLogout = new MenuItem("Log out");
		itemLogout.setOnAction(c -> {
			
                        try{
                            System.out.println("  d "+flnm1+"  f "+flnm1.length);
                            int i = 0;
                            ResultSet rs4 = st1.executeQuery("select count(folname) from folder");
                            if(rs4.next())
                                flnm1 = new String[Integer.parseInt(rs4.getString(1))];
                
                
                            ResultSet rs5 = st1.executeQuery("select folname from folder");
                            while(rs5.next())
                            {
                                String sj = rs5.getString(1);
                                sj = sj.toLowerCase();
                                sj = sj.replace("[gmail]/", "");
                                sj = sj.replace(" ", "");
                                System.out.println(sj);
                                 flnm1[i] = sj;
                                i++;
                            }
                            for(int j=0;j<flnm1.length;j++)
                                 st1.execute("drop table "+flnm1[j]);
                            st1.execute("delete from flag");
                            st1.execute("delete from folder");
                        }
                        catch(Exception e)
                        {
                                                        System.out.println("  d  "+flnm1+"  f "+flnm1.length);

                            e.printStackTrace();
                        }
                        finally{
                            

                            getStage().close();
                        }
			LoginController.show(null);
			State.clear();
		});
                MenuItem itemLogoutfolder = new MenuItem("Log out");
		itemLogoutfolder.setOnAction(c -> {
			
                        try{
                            System.out.println("  d "+flnm1+"  f "+flnm1.length);
                            int i = 0;
                            ResultSet rs4 = st1.executeQuery("select count(folname) from folder");
                            if(rs4.next())
                                flnm1 = new String[Integer.parseInt(rs4.getString(1))];
                
                
                            ResultSet rs5 = st1.executeQuery("select folname from folder");
                            while(rs5.next())
                            {
                                String sj = rs5.getString(1);
                                sj = sj.toLowerCase();
                                sj = sj.replace("[gmail]/", "");
                                sj = sj.replace(" ", "");
                                System.out.println(sj);
                                 flnm1[i] = sj;
                                i++;
                            }
                            for(int j=0;j<flnm1.length;j++)
                                 st1.execute("drop table "+flnm1[j]);
                            st1.execute("delete from flag");
                            st1.execute("delete from folder");
                        }
                        catch(Exception e)
                        {
                                                        System.out.println("  d  "+flnm1+"  f "+flnm1.length);

                            e.printStackTrace();
                        }
                        finally{
                            

                            getStage().close();
                        }
			LoginController.show(null);
			State.clear();
		});


		lvMails.setContextMenu(new ContextMenu(itemCompose, itemRefresh, itemMove, itemDelete, itemLogout));
		lvFolders.setContextMenu(new ContextMenu(itemComposefolder, itemLogoutfolder));
                lvMails.getSelectionModel().selectedItemProperty().addListener((obv, o, n1) -> {
                    paneAttachments.getChildren().clear();
			/*if (n == null || n instanceof LoadMoreMessage) {
				// clear the webview
				setWebview(null);

				if (n != null) {
//					lvMails.getSelectionModel().select(o);
					updateFolder(activeFolder, pageIndex + 1, false);
				}
				return;
			}
			//setWebview(n);*/
                    //System.out.println(" sdfsdf "+obv+"  d "+o+"  h  "+n1);
                    String sn = " hi buddy";
                    sn = (String)n1;
                                        //System.out.println("hhjhvjvjhgjhv     "+sn);
                    if(sn!=null){
                    int ln = sn.length();
                    //System.out.println(sn);
                    sn = sn.substring(ln-28 ,ln);
                    String sh = actFolder;
                    if(sh.equalsIgnoreCase("Primary"))
                        sh = "inbox";
                    else if(sh.equalsIgnoreCase("Social"))
                        sh = "inbox"; 
                    else if(sh.equalsIgnoreCase("Promotion"))
                        sh = "inbox";
                    sh = sh.toLowerCase();
                    sh = sh.replace("[gmail]/", "");
                    sh = sh.replace(" ", "");
                    System.out.println(sh);
                    try{
                            ResultSet rs = st1.executeQuery("select * from "+sh+" where date_='"+sn+"'");
                            while(rs.next())
                            {
                                lbSubject.setText(rs.getString(3));
                                lbFrom.setText(rs.getString(2));
                                lbDate.setText(rs.getString(5));
                                viewMail.getEngine().loadContent(rs.getString(4));
                                
                                
                                String user = State.username;
                                String pass = State.password;
                                final Properties props = new Properties();
                                props.put("mail.store.protocol", "gimap");
                                props.put("mail.gimap.ssl.enable", "true");
                                props.put("mail.gimap.host", "imap.gmail.com");
                                props.put("mail.gimap.port", "993");
                                //props.put("mail.imap.connectiontimeout", "5000");
                                //props.put("mail.imap.timeout", "5000");
                                
                                try
                                {
                                    Session session = Session.getDefaultInstance(props, null);
                                    Store store = session.getStore("gimap");
                                    //System.out.println("username : "+user+" , password : "+pass);
                                    
                                    store.connect("imap.gmail.com", user, pass);
                                    
                                    Folder folder = store.getFolder(actFolder);
                                    folder.open(Folder.READ_ONLY);
                                    
                                    Message desiredMessage = null;
                                    for (Message m : folder.getMessages()){
                                        if (m.getSubject().startsWith(rs.getString(3))) {
                                            desiredMessage = m;
                                            break;
                                        }
                                    }
                                    if (desiredMessage.isMimeType("multipart/*")) {
                                        Multipart mp = (Multipart)desiredMessage.getContent();
                                        attachments = getAttachments(mp);
                                        
                                        Pair<File, String> attachment = attachments.get(0);
                                        final File f = attachment.getKey();
                                        Label fileLabel = new Label(attachment.getValue());

                                        ImageIcon icon = (ImageIcon) FileSystemView.getFileSystemView().getSystemIcon(f);
                                        java.awt.Image image = icon.getImage();
                                        // Create a buffered image with transparency
                                        BufferedImage bimage = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_ARGB);

                                        // Draw the image on to the buffered image
                                        Graphics2D bGr = bimage.createGraphics();
                                        bGr.drawImage(image, 0, 0, null);
                                        bGr.dispose();

                                        Image im = SwingFXUtils.toFXImage(bimage, null);

                                        fileLabel.setGraphic(
                                                new ImageView(im)
                                        );

                                        fileLabel.setOnMouseClicked(c -> {
                                            try {
                                                Desktop.getDesktop().open(f);
                                            } catch (IOException ex) {
                                                Logger.getLogger(MainController.class.getName()).log(Level.SEVERE, null, ex);
                                            }
                                        });

                                        paneAttachments.getChildren().add(fileLabel);
                                    }
                                } catch (MessagingException | SQLException | IOException e) {
                                    e.printStackTrace();
                                }
                            }
                    }
                    catch(Exception e4)
                    {
                        e4.printStackTrace();
                    }}
		});
                
                btnSaveAttachment.setOnAction(c -> {
                    Pair<File, String> attach = attachments.get(0);
                    FileChooser fileChooser = new FileChooser();
                    fileChooser.setTitle("Save Attachment");
                    fileChooser.setInitialFileName(attach.getValue());
                    
                    String extension = "";
                    int i = attach.getValue().lastIndexOf('.');
                    if (i > 0) {
                        extension = attach.getValue().substring(i+1);
                    }
                    if (extension == "") {
                        extension = "tmp";
                    }
                    fileChooser.setSelectedExtensionFilter(new ExtensionFilter("File", extension));
                    File file = fileChooser.showSaveDialog(btnSaveAttachment.getScene().getWindow());
                    if (file != null) {
                        try {
                            File attachTemp = attach.getKey();
                            Files.copy(attachTemp.toPath(), file.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
                        } catch (Exception ex) {
                            System.out.println(ex.getMessage());
                        }
                    }
                });
                
		lvMails.setOnKeyReleased(ev -> {
			/*if (ev.getCode() == KeyCode.DELETE) {
				deleteSelection();
			}*/
		});
                
		// list view folders
		setList(lvMails);

		btnInbox.setOnAction(c -> setList(lvMails));
		btnFolders.setOnAction(c -> setList(lvFolders));
		btnCompose.setOnAction(c -> itemCompose.fire());
		btnCreateFolder.setOnAction(c -> createFolder());
		btnTrash.setOnAction(c -> itemDelete.fire());
		btnPreferences.setOnAction(c -> Main.application.getHostServices().showDocument("https://mail.google.com/mail/u/0/#settings/general"));
		btnLogout.setOnAction(c -> itemLogout.fire());
                btnRefresh.setOnAction(c -> itemRefresh.fire());
		loadFolders();
		actFolder = "inbox";
	}


		private boolean textIsHtml = false;

	public String getText(Part p) throws
		MessagingException, IOException {
		if (p.isMimeType("text/*")) {
			String s = (String) p.getContent();
			textIsHtml = p.isMimeType("text/html");
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
        
        public List<Pair<File, String>> getAttachments(Multipart multipart) throws MessagingException, IOException {
            List<Pair<File, String>> attachments = new ArrayList<>();
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart bodyPart = multipart.getBodyPart(i);
                if(!Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition()) &&
                        !StringUtils.isNotBlank(bodyPart.getFileName())) {
                     continue; // dealing with attachments only
                 }
                InputStream is = bodyPart.getInputStream();

                File f = File.createTempFile(bodyPart.getFileName(), null);
                FileOutputStream fos = new FileOutputStream(f);
                byte[] buf = new byte[4096];
                int bytesRead;
                while((bytesRead = is.read(buf))!=-1) {
                    fos.write(buf, 0, bytesRead);
                }
                fos.close();
                attachments.add(new Pair(f, bodyPart.getFileName()));
            }
            return attachments;
	}

	public static void show() throws Exception {
		Parent root = FXMLLoader.load(MainController.class.getResource("main.fxml"));
		Stage stage = new Stage();
		stage.setTitle("MASS");
                stage.setResizable(false);
		stage.setScene(new Scene(root, 1330, 670));
		stage.show();
                int i = 0;
                ResultSet rs4 = st1.executeQuery("select count(folname) from folder");
                if(rs4.next())
                    flnm1 = new String[Integer.parseInt(rs4.getString(1))];
                
                
                ResultSet rs5 = st1.executeQuery("select folname from folder");
                while(rs5.next())
                {
                    String sj = rs5.getString(1);
                    sj = sj.toLowerCase();
                    sj = sj.replace("[gmail]/", "");
                    sj = sj.replace(" ", "");
                    System.out.println(sj);
                    flnm1[i] = sj;
                    i++;
                }
                
                stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
                  public void handle(WindowEvent we) {
                        System.out.println("Stage is closing");
                        
                        
                        
                    }
                  });
                try{
                    if(State.nw.equals("new"))
                        st1.execute("insert into flag values('hi', 'hello', '"+State.username+"', '"+State.password+"')");
                }
                catch(Exception e)
                {
                    e.printStackTrace();
                }
	}
}
