package ru.darek;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

            AbstractRepository<User> repository = new AbstractRepository<>(dataSource, User.class);
            User user = new User("bob", "123", "bob");
            User user1 = new User("zob", "321", "zorro");
            repository.create(user);
            repository.create(user1);
            List<User> users = repository.findAll();
            logger.info("Все пользователи: " + (! users.isEmpty()?users.toString():" нет пользователей!"));
            logger.info("Поиск пользователя 2.. " );
            User user2 = repository.findById(2L);
            logger.info("Пользователь 2: " + (user2!=null?user2.toString():" не найден!"));
            logger.info("Апдейт пользователя 2.. " );
            if (user2!=null) {
                user2.setNickname("Вася");
                user2.setPassword("777");
                repository.update(user2);
                user2 = repository.findById(2L);
                logger.info("Пользователь 2: " + (user2 != null ? user2.toString() : " не найден!"));
            }
            logger.info("Удалили пользователя 2 ?: " + repository.deleteById(2L));
            users = repository.findAll();
            logger.info("Все пользователи: " + (! users.isEmpty()?users.toString():" нет пользователей!"));
            logger.info("Удаляем всех пользователей.. " );
            repository.deleteAll();
            users = repository.findAll();
            logger.info("Оставшиеся пользователи: " + (! users.isEmpty()?(users.toString() + "\n"):" нет пользователей!\n"));

            AbstractRepository<Account> accountAbstractRepository = new AbstractRepository<>(dataSource, Account.class);
            Account account = new Account(100L, "credit", "blocked");
            accountAbstractRepository.create(account);
            Account account2 = new Account(300L, "card", "actived");
            accountAbstractRepository.create(account2);
            List<Account> accounts = accountAbstractRepository.findAll();
            logger.info("Все счета: " + (! accounts.isEmpty()?accounts.toString():" нет счетов!"));
            logger.info("Поиск счета №2... " );
            account2 = accountAbstractRepository.findById(2L);
            logger.info("Счет 2: " +  ((account2!=null) ? account2.toString() : " нет такого!"));
//            accounts = accountAbstractRepository.findAll();
//            logger.info("Все счета: " + (accounts.isEmpty()?accounts.toString():" нет счетов!"));
            logger.info("Удаляем счет №1... " );
         //   accountAbstractRepository.deleteById(2L);
            logger.info("Удалили счет 1 ?: " + accountAbstractRepository.deleteById(1L));
            accounts = accountAbstractRepository.findAll();
            logger.info("Все счета: " + (! accounts.isEmpty()?accounts.toString():" нет счетов!"));

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            dataSource.disconnect();
        }
    }
}
