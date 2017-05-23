# --- !Ups

create table USER (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_name TEXT NOT NULL,
  about_me TEXT NOT NULL,
  is_admin BOOLEAN NOT NULL
);

create table BLOG (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  title TEXT NOT NULL,
  description TEXT NOT NULL
);

alter table BLOG add constraint B_USER_FK foreign key(user_id) references USER(id) on update NO ACTION on delete NO ACTION;

create table BLOG_POST (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  blog_id BIGINT NOT NULL,
  title TEXT NOT NULL,
  content TEXT NOT NULL,
  created TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  edited TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

alter table BLOG_POST add constraint BP_USER_FK foreign key(user_id) references USER(id) on update NO ACTION on delete NO ACTION;
alter table BLOG_POST add constraint BP_BLOG_FK foreign key(blog_id) references BLOG(id) on update NO ACTION on delete NO ACTION;

# --- !Downs
DROP TABLE 'USER';
DROP TABLE 'BLOG';
DROP TABLE 'BLOG_POST';