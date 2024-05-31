package ru.darek;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.darek.exceptions.ApplicationInitializationException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Path;
import java.sql.SQLException;
import java.sql.Statement;

public class DbMigrator {
    public static final Logger logger = LogManager.getLogger(DbMigrator.class.getName());
    private DataSource dataSource;

    public DbMigrator(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void migrate() {
        {
         //   try (FileReader reader = new FileReader("init.sql");
            try (FileReader reader = new FileReader(this.getClass().getResource("/init.sql").getPath());
                BufferedReader bufferedReader = new BufferedReader(reader);){
                StringBuilder builder = new StringBuilder();
                String line;
                int lineNumber = 0;
                int count = 0;
                while ((line = bufferedReader.readLine()) != null) {
                    lineNumber += 1;
                    line = line.trim();
                    if (line.isEmpty()) continue;
                    builder.append(line);
                    if (line.endsWith(";"))
                        try {
                            dataSource.getStatement().execute(builder.toString());
                          //  System.out.println(++count + " executed: " + builder.toString());
                            logger.debug(++count + " executed: " + builder.toString());
                            builder.setLength(0);
                        }
                        catch (SQLException e) {
                          //  System.err.println("Error at line " + lineNumber + " : " + e.getMessage() + "\n");
                            logger.fatal("Error in init.sql at line " + lineNumber + " : " + e.getMessage() + "\n");
                            e.printStackTrace();
                            throw new ApplicationInitializationException();
                        }
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

    }
}
