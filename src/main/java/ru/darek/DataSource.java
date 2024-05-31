package ru.darek;

import ru.darek.exceptions.ApplicationInitializationException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DataSource {
    private Connection connection;
    private Statement statement;

    private String url;

    public DataSource(String url) {
        this.url = url;
    }

    public Connection getConnection() { // можем легко подменить на коннект из пула клннектов
        return connection;
    }

    public Statement getStatement() {
        return statement;
    }

    public void connect() {
        try {
            connection = DriverManager.getConnection(url);
            statement = connection.createStatement();
        //    init();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new ApplicationInitializationException();
        }
    }

    public void disconnect() {
        if (statement != null) {
            try {
                statement.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

 /*   public void init() {
        try {
            statement.executeUpdate(
                    "" +
                            "create table if not exists users (" +
                            "    id          bigserial primary key," +
                            "    login       varchar(255)," +
                            "    password    varchar(255)," +
                            "    nickname    varchar(255)" +
                            ")"
            );

            statement.executeUpdate(
                    "" +
                            "create table if not exists accounts (" +
                            "    id            bigserial primary key," +
                            "    amount        bigint," +
                            "    acctype      varchar(255)," +
                            "    status        varchar(255)" +
                            ")"
            );
        } catch (SQLException e) {
            e.printStackTrace();
            throw new ApplicationInitializationException();
        }
    } */
}
