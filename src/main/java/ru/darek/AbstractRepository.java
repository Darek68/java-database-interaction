package ru.darek;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.darek.exceptions.ApplicationInitializationException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import java.util.stream.Collectors;

public class AbstractRepository<T> {
    public static final Logger logger = LogManager.getLogger(AbstractRepository.class.getName());
    private DataSource dataSource;
    private Class<T> cls;
    private Constructor<T> constructor;
    private PreparedStatementsMap preparedStatementsMap;
    private PreparedStatement psm;
    private Method method;
    private List<Field> cachedFields;
    private List<Field> allFields;
    private Map<String, EntityField<T>> entityFields = new HashMap<>();

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
        cachedFields = Arrays.stream(cls.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(RepositoryField.class))
                .filter(f -> !f.isAnnotationPresent(RepositoryIdField.class))
                .collect(Collectors.toList());
        logger.debug(" allFields: " +
                allFields.stream().map(n -> n.getName()).collect(Collectors.joining(",", "{", "}")));
        preparedStatementsMap = new PreparedStatementsMap(dataSource, cls);
        initEntityFields();
    }
    private void initEntityFields() {
        String fieldName;
        try {
            for (Field f : this.allFields) { // id login password nickname
                if (f.isAnnotationPresent(RepositoryFieldName.class)) {
                    fieldName = f.getAnnotation(RepositoryFieldName.class).title();
                } else {
                    fieldName = f.getName();
                }
                entityFields.put(f.getName(),new EntityField<T>(
                        f,
                        fieldName,
                        this.cls.getDeclaredMethod(getMethodName("get", f.getName())),
                        this.cls.getDeclaredMethod(getMethodName("set", f.getName()), f.getType()),
                        f.getType())
                );
            }
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
    public void create(T entity) {
        psm = (PreparedStatement) preparedStatementsMap.getPreparedStatements().get(RepoPreparedStatement.CREATE);
        try {
            for (int i = 0; i < cachedFields.size(); i++) {
                psm = entityFields.get(cachedFields.get(i).getName()).setValuePS(psm,i + 1,entity);
            }
            psm.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
            throw new ApplicationInitializationException("Не смогли записать в БД: " + e.getMessage());
        }
    }

    public T findById(Long id) {
        psm = (PreparedStatement) preparedStatementsMap.getPreparedStatements().get(RepoPreparedStatement.FINDBYID);
        T entity = null;
        try {
            psm.setLong(1, id);
            ResultSet rs = psm.executeQuery();
            if (rs.wasNull()) return entity;
            int nr = 0;
            entity = (T) constructor.newInstance();
            while (rs.next()) {
                logger.debug("rs: " + rs.toString());
                logger.debug("allFields: " + allFields.stream().map(n -> n.getName()).collect(Collectors.joining(",", "{", "}")));
                for (Field f : allFields) { // id login password nickname
                    nr += 1;
                    entity = entityFields.get(f.getName()).setValueEntity(rs,nr,entity);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new ApplicationInitializationException("Поиск в БД по идентификатору " + id + " вызвал ошибку: " + e.getMessage());
        }
        return entity;
    }

    public List<T> findAll() {
        psm = (PreparedStatement) preparedStatementsMap.getPreparedStatements().get(RepoPreparedStatement.FINDALL);
        List<T> entitys = new ArrayList<T>();
        T entity;
        int nr;
        try {
            ResultSet rs = psm.executeQuery();
            if (rs.wasNull()) return entitys;
            while (rs.next()) {
                nr = 0;
                entity = (T) constructor.newInstance();
                for (Field f : allFields) { // id login password nickname
                    nr += 1;
                    entity = entityFields.get(f.getName()).setValueEntity(rs,nr,entity);
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
        psm = (PreparedStatement) preparedStatementsMap.getPreparedStatements().get(RepoPreparedStatement.UPDATE);
        int i;
        try {
            for (i = 0; i < cachedFields.size(); i++) {
                psm = entityFields.get(cachedFields.get(i).getName()).setValuePS(psm,i + 1,entity);
            }
            method = entity.getClass().getDeclaredMethod("getId"); // В allFields он первый, а в запросе последний
            psm.setObject(i + 1, method.invoke(entity));
            psm.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
            throw new ApplicationInitializationException("Попытка изменения записи " + entity.toString() + " вызвала ошибку БД: " + e.getMessage());
        }
    }

    public boolean deleteById(Long id) {
        psm = (PreparedStatement) preparedStatementsMap.getPreparedStatements().get(RepoPreparedStatement.DELETEBYID);
        try {
            psm.setLong(1, id);
            return psm.executeUpdate() == 1;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ApplicationInitializationException("Не удалось удалить запись: " + e.getMessage());
        }
    }

    public void deleteAll() {
        psm = (PreparedStatement) preparedStatementsMap.getPreparedStatements().get(RepoPreparedStatement.DELETEALL);
        try {
            psm.execute();
        } catch (Exception e) {
            e.printStackTrace();
            throw new ApplicationInitializationException("Попытка удаления всех записей вызвала ошибку БД: " + e.getMessage());
        }
    }

    private String getMethodName(String pref, String name) {
        return pref + name.substring(0, 1).toUpperCase() + name.substring(1);
    }
}
