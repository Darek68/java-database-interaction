create table if not exists users (id       bigserial primary key,
                                  login    varchar(255),
                                  password varchar(255),
                                  nickname varchar(255));
create table if not exists accounts (id       bigserial primary key,
                                     amount   bigint,
                                     account_type  varchar(255),
                                     status   varchar(255));
