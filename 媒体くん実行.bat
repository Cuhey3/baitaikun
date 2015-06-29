@echo off
java -version:1.8 -jar target/Baitaikun-1.0-SNAPSHOT.jar -XX:PermSize=128m -XX:MaxPermSize=128m -Xms256m -Xmx256m -XX:NewRatio=2 -XX:SurvivorRatio=8 -server
