/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.webedeh.bank;
import java.sql.*;

/**
 *
 * @author FtN
 */
public class SQLConn {
    Connection conn = null;
    Statement stmt = null;
    public Connection getConnect() {
        String JDBC_DRIVER = "com.mysql.jdbc.Driver";
        String JDBC_URL = "jdbc:mysql://localhost:3306/bank?useSSL=false&useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC";
        String USER = "root";
        String PASS = "";
        try {
            Class.forName(JDBC_DRIVER);
            conn = DriverManager.getConnection(JDBC_URL, USER, PASS);
        } catch (Exception e) {
            System.out.println("Connection Failed : " + e.toString());
        }
        return conn;
    }
    public ResultSet query(String query){
        try{
            conn = this.getConnect();
            stmt = conn.createStatement();
            return stmt.executeQuery(query);
        }
        catch (Exception e) {
            System.out.println("Connection Failed : " + e.toString());
            return null;
        }
    }
    public Boolean update(String query){
        try{
            conn = this.getConnect();
            stmt = conn.createStatement();
            stmt.executeUpdate(query);
            return true;
        }
        catch (Exception e) {
            System.out.println("Connection Failed : " + e.toString());
            return false;
        }
    }
}
