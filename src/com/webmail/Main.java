package com.webmail;

import com.webmail.controllers.LoginController;
import javafx.application.Application;
import javafx.stage.Stage;
import java.sql.*;
import com.webmail.*;
import static javafx.application.Application.launch;


public class Main extends Application {
    public static Main application;
    public static Conn cn;
    public static Statement st;

    @Override
    public void start(Stage stage) throws Exception {
        try{
            st = cn.getStmt();
            ResultSet rs = st.executeQuery("select * from flag");
            if(rs.next())
            {
                String us = rs.getString(3);
                String ps = rs.getString(4);
                State.username = us;
              LoginController.login1(us, ps);
                
            } 
            else 
                LoginController.show(stage);
        }
        catch(Exception e1)
        {
            e1.printStackTrace();
            
        }
        
        
        Main.application = this;
        
    }


    public static void main(String[] args) {
        cn = new Conn();
        cn.setter();
        
        launch(args);
    }
}
