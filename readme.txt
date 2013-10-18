Create project in eclipse (E.g: C:\eclipse\workspace\PROJ_DIR)
Clone the github repository to folder src in C:\eclipse\workspace\PROJ_DIR

In command terminal:
cd C:\eclipse\workspace\PROJ_DIR\src; 
javac -cp .;jsoup-1.7.2.jar webimagecrawler/*.java; 
java -cp .;jsoup-1.7.2.jar webimagecrawler/ThreadedCrawler;