package com.webmail;
import com.webmail.*;
import com.webmail.controllers.MainController;
import java.sql.*;
import javax.mail.*;
import javax.mail.internet.*;
import java.io.*;
import javafx.scene.control.ListCell;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class Mails {
    
    public static Connection conf = Main.cn.getConf();
    public static Statement stmt = Main.cn.getStmt();
    public static ObservableList mails ;
        
    public static ObservableList folders;
    
    public static void setMails(String s) throws MessagingException, Exception
    {
        try{
            mails = FXCollections.observableArrayList();
            System.out.println(s+" mail");
            s= s.toLowerCase();
            s = s.replace("[gmail]/", "");
            s = s.replace(" ", "");            

            if(s.equals("inbox"))
                s = "inbox where type_ = 'Primary'";
            else if(s.equalsIgnoreCase("Primary"))
                s = "inbox where type_ = 'Primary'";
            else if(s.equalsIgnoreCase("Social"))
                s = "inbox where type_ = 'Social'"; 
            else if(s.equalsIgnoreCase("Promotion"))
                s = "inbox where type_ = 'Promotion'";
            else if(s.equalsIgnoreCase("null"))
                return;
            
            
            
           ResultSet rs = stmt.executeQuery("select * from "+s);
           while(rs.next())
           {
               String sbj = (String)rs.getString(3);
               int len = 0;
               if(sbj != "")
                   len = sbj.length();
               if(len<=150)
               {
                   for(int l= len;l<150;l++)
                   {
                       sbj = sbj+" ";
                   }
               }
               else
               {
                   sbj = sbj.substring(0, 147);
                   sbj = sbj+"...";
               }
               
               mails.add( sbj+""+rs.getString(5));

           }
        }
        catch(Exception e){
            
        }
    }
    public static void setFolders() throws MessagingException, Exception
    {
        try{       
           folders = FXCollections.observableArrayList();
           ResultSet rs = stmt.executeQuery("select folname from folder");
           while(rs.next())
           {
               if(rs.getString(1).equalsIgnoreCase("inbox"))
               {
                   folders.add("Primary"); 
                   folders.add("Social");
                   folders.add("Promotion");
                   
               }
               else
               folders.add(rs.getString(1));             
               
           }
        }
        catch(Exception e){
            
        }
    }
    
    
}
