package ru.darek;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.darek.exceptions.ApplicationInitializationException;

import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PreparedStatementsMap<T> {
    public static final Logger logger = LogManager.getLogger(PreparedStatementsMap.class.getName());
    private DataSource dataSource;
    private Class<T> cls;
    private Map<String, PreparedStatement> preparedStatements = new HashMap<>();
    private List<Field> cachedFields;
    private List<Field> allFields;
    private StringBuilder query;
    private PreparedStatement ps;
    public PreparedStatementsMap(DataSource dataSource, Class cls) {
        this.dataSource = dataSource;
        this.cls = cls;
        allFields = Arrays.stream(cls.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(RepositoryField.class))
                .collect(Collectors.toList());
        prepareCreate();
        prepareFindById();
        prepareFindAll();
        prepareUpdate();
        prepareDeleteById();
        prepareDeleteAll();
    }
    public Map<String, PreparedStatement> getPreparedStatements() {
        return preparedStatements;
    }

    private void prepareDeleteAll() { // deleteAll()  TRUNCATE TABLE ?
        query = new StringBuilder("TRUNCATE TABLE ");
        String tableName = cls.getAnnotation(RepositoryTable.class).title();
        query.append(tableName);
        logger.debug("prepareDeleteAll-query " + query);
        try {
            ps = dataSource.getConnection().prepareStatement(query.toString());
            preparedStatements.put("psDeleteAll",ps);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new ApplicationInitializationException();
        }
    }

    private void prepareDeleteById() { // deleteById(id)  DELETE FROM Users WHERE id = ?
        query = new StringBuilder("DELETE FROM ");
        String tableName = cls.getAnnotation(RepositoryTable.class).title();
        query.append(tableName).append(" WHERE id = ?");
        logger.debug("prepareDeleteById-query " + query);
        try {
            ps = dataSource.getConnection().prepareStatement(query.toString());
            preparedStatements.put("psDeleteById",ps);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new ApplicationInitializationException();
        }
    }

    private void prepareUpdate() { // UPDATE users SET login = ?, password= ?, nickname = ? WHERE id = ?;
        query = new StringBuilder("UPDATE ");
        String tableName = cls.getAnnotation(RepositoryTable.class).title();
        query.append(tableName).append(" SET ");  // 'UPDATE users SET '
        for (Field f : cachedFields) {
            if (f.isAnnotationPresent(RepositoryFieldName.class)) {
                query.append(f.getAnnotation(RepositoryFieldName.class).title());
            } else {
                query.append(f.getName());
            }
            query.append(" = ?, ");
        } // 'UPDATE users SET login = ?, password= ?, nickname = ?, '
        query.setLength(query.length() - 2);
        query.append(" ");
        query.append("WHERE id = ?;"); // UPDATE users SET login = ?, password= ?, nickname = ? WHERE id = ? ;
        logger.debug("prepareUpdate-query " + query);
        try {
            ps = dataSource.getConnection().prepareStatement(query.toString());
            preparedStatements.put("psUpdate",ps);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new ApplicationInitializationException();
        }
    }

    private void prepareFindAll() {
        query = new StringBuilder("select ");
        for (Field f : allFields) {
            if (f.isAnnotationPresent(RepositoryFieldName.class)) {
                query.append(f.getAnnotation(RepositoryFieldName.class).title());
            } else {
                query.append(f.getName());
            }
            query.append(", ");
        }
        query.setLength(query.length() - 2);
        query.append(" from ");
        String tableName = cls.getAnnotation(RepositoryTable.class).title();
        query.append(tableName);
        try {
            ps = dataSource.getConnection().prepareStatement(query.toString());
            preparedStatements.put("psFindAll",ps);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new ApplicationInitializationException();
        }
    }

    private void prepareFindById() {
        query = new StringBuilder("select ");
        for (Field f : allFields) {
            if (f.isAnnotationPresent(RepositoryFieldName.class)) {
                query.append(f.getAnnotation(RepositoryFieldName.class).title());
            } else {
                query.append(f.getName());
            }
            query.append(", ");
        }
        query.setLength(query.length() - 2);
        query.append(" from ");
        String tableName = cls.getAnnotation(RepositoryTable.class).title();
        query.append(tableName).append(" where id = ?");
        logger.debug("prepareFindById-query " + query);
        try {
            ps = dataSource.getConnection().prepareStatement(query.toString());
            preparedStatements.put("psFindById",ps);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new ApplicationInitializationException();
        }
    }

    private void prepareCreate() {  // 'insert into users (login, password, nickname) values (?, ?, ?');
        query = new StringBuilder("insert into ");
        String tableName = cls.getAnnotation(RepositoryTable.class).title();
        query.append(tableName).append(" (");
        // 'insert into users ('
        cachedFields = Arrays.stream(cls.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(RepositoryField.class))
                .filter(f -> !f.isAnnotationPresent(RepositoryIdField.class))
                .collect(Collectors.toList());
        for (Field f : cachedFields) { // TODO Сделать использование геттеров
            f.setAccessible(true);
        }
        for (Field f : cachedFields) {
            if (f.isAnnotationPresent(RepositoryFieldName.class)) {
                query.append(f.getAnnotation(RepositoryFieldName.class).title());
            } else {
                query.append(f.getName());
            }
            query.append(", ");
        }
        // 'insert into users (login, password, nickname, '
        query.setLength(query.length() - 2);
        // 'insert into users (login, password, nickname'
        query.append(") values (");
        for (Field f : cachedFields) {
            query.append("?, ");
        }
        // 'insert into users (login, password, nickname) values (?, ?, ?, '
        query.setLength(query.length() - 2);
        // 'insert into users (login, password, nickname) values (?, ?, ?'
        query.append(");");
        try {
            ps = dataSource.getConnection().prepareStatement(query.toString());
            preparedStatements.put("psCreate",ps);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new ApplicationInitializationException();
        }
    }
}
