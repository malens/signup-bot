package database;

import java.sql.*;

public class DatabaseUtil {

    private static String url;
    public static void connectToDatabase(String fileName){
        url = "jdbc:sqlite:./db/" + fileName;
        try {
            Connection connection = DriverManager.getConnection(url);
            if (connection != null){
                DatabaseMetaData meta = connection.getMetaData();
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        createTable(url);
    }

    private static void createTable(String url){
        String sql1 = "CREATE TABLE IF NOT EXISTS signups (\n" +
                "signup_id INT PRIMARY KEY NOT NULL, \n" +
                "message text\n" +
                ");";
        String sql2 = "CREATE TABLE IF NOT EXISTS roles (\n" +
                "role_id INT PRIMARY KEY, \n" +
                "name text NOT NULL, \n" +
                "FOREIGN KEY (signup_id) REFERENCES signups (signup_id)\n" +
                "ON DELETE CASCADE\n" +
                "ON UPDATE CASCADE\n" +
                ");";
        try (
                Connection conn = DriverManager.getConnection(url);
                Statement statement = conn.createStatement()
        ){
            statement.execute(sql1);
            statement.execute(sql2);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }


}
