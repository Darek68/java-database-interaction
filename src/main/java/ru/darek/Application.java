package ru.darek;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.darek.for_remove.UsersDao;
import ru.darek.for_remove.UsersDaoImpl;

import java.lang.reflect.Method;
import java.util.List;

public class Application {
    // Домашнее задание:
    // - Реализовать класс DbMigrator - он должен при старте создавать все необходимые таблицы из файла init.sql
    // Доработать AbstractRepository
    // - Доделать findById(id), findAll(), update(), deleteById(id), deleteAll()
    // - Сделать возможность указывать имя столбца таблицы для конкретного поля (например, поле accountType маппить на столбец с именем account_type)
    // - Добавить проверки, если по какой-то причине невозможно проинициализировать репозиторий, необходимо бросать исключение, чтобы
    // программа завершила свою работу (в исключении надо объяснить что сломалось)
    // - Работу с полями объектов выполнять только через геттеры/сеттеры
    public static final Logger logger = LogManager.getLogger(Application.class.getName());
    public static void main(String[] args) {
        DataSource dataSource = null;
        try {
            dataSource = new DataSource("jdbc:h2:file:./db;MODE=PostgreSQL");
            dataSource.connect();

            DbMigrator dbMigrator = new DbMigrator(dataSource);
            dbMigrator.migrate();

//            UsersDao usersDao = new UsersDaoImpl(dataSource);
//            logger.info(usersDao.findAll());

            AbstractRepository<User> repository = new AbstractRepository<>(dataSource, User.class);
            User user = new User("bob", "123", "bob");
            User user1 = new User("zob", "321", "zorro");
            repository.create(user);
            repository.create(user1);
//            logger.info(usersDao.findAll());
            List<User> users = repository.findAll();
            logger.info("Все пользователи: " + (users!=null?users.toString():" нет пользователи!"));
            User user2 = repository.findById(2L);
            logger.info("Пользователь 2: " + (user2!=null?user2.toString():" не найден!"));
            if (user2!=null) {
                user2.setNickname("Вася");
                user2.setPassword("777");
                repository.update(user2);
                user2 = repository.findById(2L);
                logger.info("Пользователь 2: " + (user2 != null ? user2.toString() : " не найден!"));
            }
            logger.info("Удалили пользователя 1 ?: " + repository.deleteById(1L));
            users = repository.findAll();
            logger.info("Оставшиеся пользователи: " + (users!=null?users.toString():" нет пользователи!"));
            repository.deleteAll();
            users = repository.findAll();
            logger.info("Оставшиеся пользователи: " + (users!=null?users.toString():" нет пользователи!\n"));


//            List<User> users = repository.findAll();
//            logger.info("Все пользователи: " + (users!=null?users.toString():" нет пользователи!"));

            AbstractRepository<Account> accountAbstractRepository = new AbstractRepository<>(dataSource, Account.class);
            Account account = new Account(100L, "credit", "blocked");
            accountAbstractRepository.create(account);
            Account account2 = new Account(300L, "card", "actived");
            accountAbstractRepository.create(account2);
            account2 = accountAbstractRepository.findById(2L);
            logger.info("Счет 2: " +  ((account2!=null) ? account2.toString() : " нет такого!"));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            dataSource.disconnect();
        }
    }
}
