#!/usr/bin/env bash
echo "set global binlog_format = 'ROW'" | docker exec -i db mysql -h localhost -uroot -proot
curl -s http://docs.killbill.io/0.16/ddl.sql | docker exec -i db mysql -h localhost -uroot -proot -D killbill
echo "create database kaui;" | docker exec -i db mysql -h localhost -uroot -proot
curl -s https://raw.githubusercontent.com/killbill/killbill-admin-ui/master/db/ddl.sql | mysql -h 127.0.0.1 -uroot -proot -D kaui
echo "insert into kaui_allowed_users (kb_username, description, created_at, updated_at) values ('admin', 'super admin', NOW(), NOW());" | docker exec -i db mysql -h localhost -uroot -proot -D kaui
cat  src/main/resources/ddl.sql | docker exec -i db mysql -h localhost -uroot -proot -D killbill