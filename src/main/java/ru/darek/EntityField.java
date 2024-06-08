package ru.darek;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class EntityField <T> {
    public static final Logger logger = LogManager.getLogger(EntityField.class.getName());
    private Field field;
    private String actualFieldName; // если дадите возможность задавать столбцам кастомные имена
    private Method getter;
    private Method setter;
    private Class cls;

    public EntityField(Field field, String actualFieldName, Method getter, Method setter, Class cls) {
        this.field = field;
        this.actualFieldName = actualFieldName;
        this.getter = getter;
        this.setter = setter;
        this.cls  = cls;
        logger.debug("EntityField: " + this.field.getName() + " " + this.actualFieldName + " " + this.getter.getName() + " " + this.setter.getName() + " " + this.cls.getName());
    }
    public PreparedStatement setValuePS(PreparedStatement ps, int i,T entity) throws Exception {
        if (cls == Long.class)   ps.setLong(  i, (Long)   this.getter.invoke(entity));
        if (cls == String.class) ps.setString(i, (String) this.getter.invoke(entity));
        // ToDo можно расширить список для всех типов..
        return ps;
    }
    public T setValueEntity(ResultSet rs, int i, T entity) throws Exception {
        if (cls == Long.class)   this.setter.invoke(entity, rs.getLong(i));
        if (cls == String.class) this.setter.invoke(entity, rs.getObject(i));
        // ToDo можно расширить список для всех типов..
        return entity;
    }
}
