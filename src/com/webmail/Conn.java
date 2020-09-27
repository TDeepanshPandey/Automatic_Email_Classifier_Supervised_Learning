package com.webmail;
import java.sql.*;

public class Conn {
    
    public static Connection conf;
    public static Statement stmt;
    
    public static void setter()  
    {
        try
        {
            Class.forName("com.mysql.jdbc.Driver");
            String url = "jdbc:mysql://localhost:3306/mass2?user=root&password=";
            conf = DriverManager.getConnection(url);
            stmt =  conf.createStatement();
        }
        catch(Exception en)
        {
            en.printStackTrace();
        }        
    }
    
    public static void closer()  
    {
        try
        {
            conf.close();
            stmt.close();
        }
        catch(Exception en)
        {
            en.printStackTrace();
        }        
    }
    
    public static Connection getConf()  
    {
        return conf;       
    }
    
    public static Statement getStmt()  
    {
        return stmt;      
    }
}
