package ru.darek;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.darek.exceptions.ApplicationInitializationException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AbstractRepository<T> {
    public static final Logger logger = LogManager.getLogger(AbstractRepository.class.getName());
    private DataSource dataSource;
    private Class<T> cls;
    private Constructor<T> constructor;
    private Map<String, Method> methods;
    private PreparedStatementsMap preparedStatementsMap;
    private PreparedStatement psm;
    private Map<String,PreparedStatement> ps;

    private PreparedStatement psCreate;
    private PreparedStatement psFindById;
    private PreparedStatement psFindAll;
    private PreparedStatement psUpdate;
    private PreparedStatement psDeleteById;
    private PreparedStatement psDeleteAll;

    private List<Field> cachedFields;
    private List<Field> allFields;
    private StringBuilder query;

    public AbstractRepository(DataSource dataSource, Class<T> cls) {
        this.dataSource = dataSource;
        this.cls = cls;
        try {
            constructor = cls.getConstructor();
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        logger.debug(" cls " + cls.getName());
        logger.debug(" constructor " + constructor.getName());
        allFields = Arrays.stream(cls.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(RepositoryField.class))
                .collect(Collectors.toList());
        logger.debug(" allFields: " +
                allFields.stream().map(n -> n.getName()).collect(Collectors.joining(",", "{", "}")));
        prepareCreate(cls);
        prepareFindById(cls);
        prepareFindAll(cls);
        prepareUpdate(cls);
        prepareDeleteById(cls);
        prepareDeleteAll(cls);
        preparedStatementsMap = new PreparedStatementsMap(dataSource,cls);
    }

    public void create(T entity) {
        psm = (PreparedStatement) preparedStatementsMap.getPreparedStatements().get("psCreate");
        try {
            for (int i = 0; i < cachedFields.size(); i++) {
                psCreate.setObject(i + 1, cachedFields.get(i).get(entity));
                psm.setObject(i + 1, cachedFields.get(i).get(entity));
            }
            psCreate.executeUpdate();
        } catch (Exception e) {
            throw new ApplicationInitializationException("Не смогли записать в БД: " + e.getMessage());
        }
    }

    public T findById(Long id) {
        T entity = null;
        try {
            psFindById.setLong(1, id);
            ResultSet rs = psFindById.executeQuery();
            if (rs.wasNull()) return entity;
            int nr = 0;
            while (rs.next()) {
                if (entity == null) entity = (T) constructor.newInstance();
                logger.debug("rs: " + rs.toString());
                logger.debug("allFields: " + allFields.stream().map(n -> n.getName()).collect(Collectors.joining(",", "{", "}")));
                for (Field f : allFields) { // id login password nickname
                    logger.debug("f.getClass: " + f.getClass() + "    f.getName: " + f.getName());
                    logger.debug("getter: " + getSetterName(f.getName()));
                    logger.debug("Тип поля: " + f.getType().getTypeName());
                    Method method = entity.getClass().getDeclaredMethod(getSetterName(f.getName()), f.getType());
                    logger.debug("method: " + method.getName());
                    nr += 1;
                    if (f.getType() == Long.class) method.invoke(entity, rs.getLong(nr));
                    if (f.getType() == String.class) method.invoke(entity, rs.getObject(nr));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new ApplicationInitializationException("Поиск в БД по идентификатору " + id + " вызвал ошибку: " + e.getMessage());
        }
        return entity;
    }

    public List<T> findAll() {
        List<T> entitys = new ArrayList<T>();
        T entity;
        int nr;
        try {
            ResultSet rs = psFindAll.executeQuery();
            if (rs.wasNull()) return entitys;
            while (rs.next()) {
                nr = 0;
                entity = (T) constructor.newInstance();
                for (Field f : allFields) { // id login password nickname
                    Method method = entity.getClass().getDeclaredMethod(getSetterName(f.getName()), f.getType());
                    nr += 1;
                    if (f.getType() == Long.class) method.invoke(entity, rs.getLong(nr));
                    if (f.getType() == String.class) method.invoke(entity, rs.getObject(nr));
                }
                entitys.add(entity);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new ApplicationInitializationException("Запрос всех записей в БД вызвал ошибку: " + e.getMessage());
        }
        return entitys;
    }

    public void update(T entity) {
        int i;
        try {
            for (i = 0; i < cachedFields.size(); i++) {
                psUpdate.setObject(i + 1, cachedFields.get(i).get(entity));
            }
            Method method = entity.getClass().getDeclaredMethod("getId");
            psUpdate.setObject(i + 1, method.invoke(entity));
            psUpdate.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
            throw new ApplicationInitializationException("Попытка изменения записи " + entity.toString() + " вызвала ошибку БД: " + e.getMessage());
        }
    }

    public boolean deleteById(Long id) {
        try {
            psDeleteById.setLong(1, id);
            //  return psDeleteById.execute();
            return psDeleteById.executeUpdate() == 1;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ApplicationInitializationException("Не удалось удалить запись: " + e.getMessage());
        }
    }

    public void deleteAll() {
        try {
            psDeleteAll.execute();
        } catch (Exception e) {
            e.printStackTrace();
            throw new ApplicationInitializationException("Попытка удаления всех записей вызвала ошибку БД: " + e.getMessage());
        }
    }

    private void prepareDeleteAll(Class<T> cls) { // deleteAll()  TRUNCATE TABLE ?
        query = new StringBuilder("TRUNCATE TABLE ");
        String tableName = cls.getAnnotation(RepositoryTable.class).title();
        query.append(tableName);
        logger.debug("prepareDeleteAll-query " + query);
        try {
            psDeleteAll = dataSource.getConnection().prepareStatement(query.toString());
        } catch (SQLException e) {
            e.printStackTrace();
            throw new ApplicationInitializationException();
        }
    }

    private void prepareDeleteById(Class<T> cls) { // deleteById(id)  DELETE FROM Users WHERE id = ?
        query = new StringBuilder("DELETE FROM ");
        String tableName = cls.getAnnotation(RepositoryTable.class).title();
        query.append(tableName).append(" WHERE id = ?");
        logger.debug("prepareDeleteById-query " + query);
        try {
            psDeleteById = dataSource.getConnection().prepareStatement(query.toString());
        } catch (SQLException e) {
            e.printStackTrace();
            throw new ApplicationInitializationException();
        }
    }

    private void prepareUpdate(Class<T> cls) { // UPDATE users SET login = ?, password= ?, nickname = ? WHERE id = ?;
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
         //   query.append(f.getName()).append(" = ?, ");
        } // 'UPDATE users SET login = ?, password= ?, nickname = ?, '
        query.setLength(query.length() - 2);
        query.append(" ");
        query.append("WHERE id = ?;"); // UPDATE users SET login = ?, password= ?, nickname = ? WHERE id = ? ;
        logger.debug("prepareUpdate-query " + query);
        try {
            psUpdate = dataSource.getConnection().prepareStatement(query.toString());
        } catch (SQLException e) {
            e.printStackTrace();
            throw new ApplicationInitializationException();
        }
    }

    private void prepareFindAll(Class<T> cls) {
        query = new StringBuilder("select ");
        for (Field f : allFields) {
            if (f.isAnnotationPresent(RepositoryFieldName.class)) {
                query.append(f.getAnnotation(RepositoryFieldName.class).title());
            } else {
                query.append(f.getName());
            }
            query.append(", ");
//            query.append(f.getName());
//            query.append(", ");
        }
        query.setLength(query.length() - 2);
        query.append(" from ");
        String tableName = cls.getAnnotation(RepositoryTable.class).title();
        query.append(tableName);
        try {
            psFindAll = dataSource.getConnection().prepareStatement(query.toString());
        } catch (SQLException e) {
            e.printStackTrace();
            throw new ApplicationInitializationException();
        }
    }

    private void prepareFindById(Class<T> cls) {
        query = new StringBuilder("select ");
        for (Field f : allFields) {
            if (f.isAnnotationPresent(RepositoryFieldName.class)) {
                query.append(f.getAnnotation(RepositoryFieldName.class).title());
            } else {
                query.append(f.getName());
            }
            query.append(", ");
//            query.append(f.getName());
//            query.append(", ");
        }
        query.setLength(query.length() - 2);
        query.append(" from ");
        String tableName = cls.getAnnotation(RepositoryTable.class).title();
        query.append(tableName).append(" where id = ?");
        logger.debug("prepareFindById-query " + query);
        try {
            psFindById = dataSource.getConnection().prepareStatement(query.toString());
        } catch (SQLException e) {
            e.printStackTrace();
            throw new ApplicationInitializationException();
        }
    }

    private void prepareCreate(Class<T> cls) {  // 'insert into users (login, password, nickname) values (?, ?, ?');
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
            psCreate = dataSource.getConnection().prepareStatement(query.toString());
        } catch (SQLException e) {
            e.printStackTrace();
            throw new ApplicationInitializationException();
        }
    }

    private String getSetterName(String name) {
        return "set" + name.substring(0, 1).toUpperCase() + name.substring(1);
    }
}
