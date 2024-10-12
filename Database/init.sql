SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS dur;
DROP TABLE IF EXISTS medicines;
DROP TABLE IF EXISTS medication_log;
DROP TABLE IF EXISTS notifications;
DROP TABLE IF EXISTS reports;
DROP TABLE IF EXISTS images;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS clinic_location;
SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE users (
                       id BIGINT NOT NULL AUTO_INCREMENT,
                       role ENUM('user', 'admin') NOT NULL,
                       user_id VARCHAR(20) NULL UNIQUE,
                       user_password VARCHAR(30) NULL,
                       name VARCHAR(5) NOT NULL,
                       age TINYINT NULL,
                       nick_name VARCHAR(10) NULL UNIQUE,
                       notification_value ENUM('bottle', 'app', 'mix', 'none') NOT NULL,
                       bottle_id CHAR(5) NULL UNIQUE,
                       zipcode CHAR(5) NULL,
                       morning_time TIME NOT NULL DEFAULT '08:00',
                       afternoon_time TIME NOT NULL DEFAULT '13:00',
                       evening_time TIME NOT NULL DEFAULT '18:00',
                       PRIMARY KEY (id)
);

CREATE TABLE medicines (
                           id BIGINT NOT NULL AUTO_INCREMENT,
                           name VARCHAR(110) NOT NULL,
                           product_code CHAR(9) NULL UNIQUE,
                           category VARCHAR(100) NULL,
                           manufacturer VARCHAR(100) NULL,
                           PRIMARY KEY (id),
                           INDEX (product_code)
);

CREATE TABLE dur (
                     id BIGINT NOT NULL AUTO_INCREMENT,
                     a_product_code CHAR(9) NOT NULL,
                     b_product_code CHAR(9) NOT NULL,
                     reason VARCHAR(170) NOT NULL,
                     etc VARCHAR(150) NULL,
                     PRIMARY KEY (id),
                     FOREIGN KEY (a_product_code) REFERENCES medicines(product_code),
                     FOREIGN KEY (b_product_code) REFERENCES medicines(product_code)
);

CREATE TABLE notifications (
                               id BIGINT NOT NULL AUTO_INCREMENT,
                               medicine_id BIGINT NOT NULL,
                               user_absolute_id BIGINT NOT NULL,
                               created_at DATE NOT NULL,
                               total SMALLINT NULL,
                               remaining_dose SMALLINT NULL,
                               renewal_date DATE NULL,
                               daily_dose ENUM('one', 'two', 'three') NOT NULL,
                               morning BOOLEAN NULL,
                               afternoon BOOLEAN NULL,
                               evening BOOLEAN NULL,
                               PRIMARY KEY (id),
                               FOREIGN KEY (medicine_id) REFERENCES medicines(id),
                               FOREIGN KEY (user_absolute_id) REFERENCES users(id)
);

CREATE TABLE medication_log (
                                id BIGINT NOT NULL AUTO_INCREMENT,
                                notification_id BIGINT NOT NULL,
                                user_absolute_id BIGINT NOT NULL,
                                time TIMESTAMP NOT NULL,
                                status BOOLEAN NOT NULL,
                                PRIMARY KEY (id),
                                FOREIGN KEY (notification_id) REFERENCES notifications(id),
                                FOREIGN KEY (user_absolute_id) REFERENCES users(id)
);

CREATE TABLE clinic_location (
                                 id BIGINT NOT NULL AUTO_INCREMENT,
                                 district_name VARCHAR(3) NOT NULL,
                                 type VARCHAR(9) NOT NULL,
                                 name VARCHAR(20) NOT NULL,
                                 tel VARCHAR(13) NOT NULL,
                                 address VARCHAR(50) NOT NULL,
                                 inpatient_room SMALLINT NOT NULL,
                                 hospital_bed SMALLINT NOT NULL,
                                 longitude DOUBLE PRECISION NOT NULL,
                                 latitude DOUBLE PRECISION NOT NULL,
                                 PRIMARY KEY (id)
);

CREATE TABLE images (
                        id BIGINT NOT NULL AUTO_INCREMENT,
                        path VARCHAR(255) NOT NULL,
                        name VARCHAR(255) NOT NULL,
                        type VARCHAR(30) NOT NULL,
                        PRIMARY KEY (id)
);

CREATE TABLE reports (
                         id BIGINT NOT NULL AUTO_INCREMENT,
                         user_absolute_id BIGINT NOT NULL,
                         image_id BIGINT NULL,
                         title VARCHAR(255) NOT NULL,
                         content TEXT NOT NULL,
                         create_at TIMESTAMP,
                         PRIMARY KEY (id),
                         FOREIGN KEY (user_absolute_id) REFERENCES users(id),
                         FOREIGN KEY (image_id) REFERENCES images(id)
);