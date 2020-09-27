package com.webmail;
import com.webmail.*;
import com.webmail.controllers.*;
import java.sql.*;
import javax.mail.*;
import javax.mail.internet.*;
import java.io.*;

public class SaveData {
    
    public static Connection conf = Main.cn.getConf();
    public static Statement stmt = Main.cn.getStmt();
    public static Message[] arr;
    public static Folder[] fn;
    public static String[] flnm ;
    
   public static String getText(Part p) throws
		MessagingException, IOException {
		if (p.isMimeType("text/*")) {
			String s = (String) p.getContent();

			return s;
		}

		if (p.isMimeType("multipart/alternative")) {

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
    
    public static void sett() throws MessagingException, Exception
    {
        fn = State.store.getDefaultFolder().list("*");
        flnm = new String[fn.length];
        for(int l=0; l<fn.length;l++)
        {
            try
            {
                if(!(fn[l].toString().equalsIgnoreCase("[gmail]")))
                {
                        
                    fn[l].open(Folder.READ_ONLY);

                    arr = fn[l].getMessages();	

                    int j = arr.length;
                    System.out.println("No of Message : "+j);
                    String fol = fn[l].toString();
                    fol = fol.toLowerCase();
                    fol = fol.replace("[gmail]/", "");
                    fol = fol.replace(" ", "");
                    System.out.println(fol);
                    
                    
                    if(!State.nw.equals("new"))
                    {
                        String s2 = "delete from "+fol;
                        System.out.println(s2);
                        stmt.execute(s2);
                    }
                    else
                    {
                        String s3 = "CREATE TABLE "+fol+"( sno int(10) unsigned NOT NULL, to_ varchar(200) NOT NULL, subject_ longtext, message_ longtext, date_ varchar(200) NOT NULL, PRIMARY KEY (`sno`) ) " ;
                        if(fol.equals("inbox"))
                            s3 = "CREATE TABLE inbox( sno int(10) unsigned NOT NULL, to_ varchar(200) NOT NULL, subject_ longtext, message_ longtext, date_ varchar(200) NOT NULL, type_ varchar(200) Not Null, PRIMARY KEY (`sno`) ) ";
          
                        stmt.execute(s3);
                        
                        stmt.execute("insert into folder values("+l+", '"+fn[l]+"')");
                    }
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

    
    					String className = ""+from[0];
    					
    					
    					PreparedStatement pstmt = conf.prepareStatement(WRITE_OBJECT_SQL);
						msg = getText(arr[i]);
					
					    pstmt.setInt(1, (j-i));
					    pstmt.setString(2, className);
					    pstmt.setString(3, msg2);
					    pstmt.setString(4, msg);
					    pstmt.setString(5, msg3);
                                            if(fol.equals("inbox"))
                                                pstmt.setString(6, Classification.getClass(msg, null));
					    pstmt.executeUpdate();
					
					    
					
					    pstmt.close();
					    
					}
                                System.out.println(""+fol+" is stored");
                                            
                }
                                       
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
        }
        LoginController.st2 = "done";
        
    }
    
    
}
