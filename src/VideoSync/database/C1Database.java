/*
 * ****************************************************************
 * File: 			C1Database.java
 * Date Created:  	August 22, 2016
 * Programmer:		Danny Hale
 *
 * Purpose:			To process C1 data going into and out of an SQL
 * 					database.
 *
 * ****************************************************************
 */

package VideoSync.database;

import VideoSync.views.tabbed_panels.DataWindow;

import javax.swing.*;
import java.awt.*;
import java.sql.*;

@SuppressWarnings({"rawtypes", "unchecked"})
public class C1Database
{
    Connection connection;
    ResultSet results;
    String database_ip;
    String table_name;

    public C1Database(DataWindow g)
    {
        database_ip = "10.160.32.147";
        JTextField ip = new JTextField(database_ip, 10);
        JComboBox tables = new JComboBox();
        boolean valid;

        JPanel main = new JPanel(new BorderLayout());
        JPanel left = new JPanel(new BorderLayout());
        JPanel right = new JPanel(new BorderLayout());
        left.add(new JLabel("IP: "), BorderLayout.NORTH);
        left.add(new JLabel("Tables: "), BorderLayout.SOUTH);
        right.add(ip, BorderLayout.NORTH);
        right.add(tables, BorderLayout.SOUTH);
        main.add(left, BorderLayout.WEST);
        main.add(right, BorderLayout.CENTER);
        tables.setPreferredSize(new Dimension(280, 21));

        do
        {
            valid = true;
            int choice = JOptionPane.showConfirmDialog(g, main, "Select Database", JOptionPane.OK_CANCEL_OPTION);
            if(choice == JOptionPane.OK_OPTION)
            {
                String cmd = "jdbc:mariadb://" + ip.getText() + ":3306/BCMS?user=root&password=caltrans";

                try
                {
                    connection = DriverManager.getConnection(cmd);
                    PreparedStatement stmt = connection.prepareStatement("show tables");

                    stmt.execute();
                    ResultSet rs = stmt.getResultSet();
                    stmt.close();

                    while(rs.next())
                    {
                        tables.addItem(rs.getString(1));
                    }
                    rs.close();

                    JOptionPane.showConfirmDialog(g, main, "Select Table", JOptionPane.OK_CANCEL_OPTION);

                    database_ip = ip.getText();
                    table_name = tables.getSelectedItem().toString();

                    results = query();
                    connection.close();
                }
                catch(SQLException e)
                {
                    e.printStackTrace();
                    valid = false;
                }
            }
        }
        while(!valid);
    }

    public String getDatabaseIP()
    {
        return database_ip;
    }

    public String getTableName()
    {
        return table_name;
    }

    public ResultSet getResultSet()
    {
        return results;
    }

    private ResultSet query()
    {
        ResultSet rs = null;
        try
        {
            PreparedStatement stmt = connection.prepareStatement("select C1_Pin, C1_Reader_State, C1_Reader_Millis from " + table_name);
            stmt.execute();
            rs = stmt.getResultSet();
            stmt.close();
        }
        catch(SQLException e)
        {
            e.printStackTrace();
        }

        return rs;
    }
}
