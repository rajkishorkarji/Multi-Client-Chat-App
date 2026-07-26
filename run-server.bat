@echo off
chcp 65001 > nul
set "MAVEN_OPTS=-Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8"
mvn "exec:java" "-Dexec.mainClass=com.chatapp.server.ChatServer"
