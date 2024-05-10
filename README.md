# Database-Engine
Database Engine that supports some features like:
- Creating a table
- Inserting into a table
- Updating a table
- Deleting from a table
- Selecting from a table
- Creating an Index -using B+ Tree-
- Writing SQL queries
- Supported data types: **int** (java.lang.Integer), **double** (java.lang.Double) and **varchar** (java.lang.String)   
</br>

<div style="display: flex;">
  <img src="https://img.shields.io/badge/Apache-maven-%23C71A36?style=for-the-badge&logo=ApacheMaven" alt="Apache Maven badge">
  <img src="https://img.shields.io/badge/Junit5-tests-%2325A162?style=for-the-badge&logo=Junit5" alt="Junit 5 badge">
  <img src="https://img.shields.io/badge/Git--%23F05032?style=for-the-badge&logo=Git" alt="Git badge">
  <img src="https://img.shields.io/badge/-Github-%23181717?style=for-the-badge&logo=Github" alt="Github badge">
  <img src="https://img.shields.io/badge/-Java-%23ED8B00?style=for-the-badge&logo=java&logoColor=white" alt ="Java badge">
  <img src = "https://img.shields.io/badge/-ANTLR-%237231d6?style=for-the-badge&logo=ANTLR&logoColor=white" alt = "ANTLR badge">
</div>

---
## Tools
- Language used is [Java](https://www.java.com/en/)
- Project is built using [maven](https://maven.apache.org/)  
- Tests are written in [Junit5](https://junit.org/junit5/)
- GUI is built with [JavaFx](https://openjfx.io/)
- SQL is parsed using [ANTLR](https://www.antlr.org/)

---

## Code examples

### Normal Selection
```java
SQLTerm[] sqlTerms = new SQLTerm[2];
sqlTerms[0] = new SQLTerm("Students", "gpa", "=", 4.0);
sqlTerms[1] = new SQLTerm("Students", "id", ">", 100);
String[] strArrOperator = new String[] { "AND" };
engine.selectFromTable(sqlTerms, strArrOperator);
```
### Selection Using SQL
```java
StringBuffer command = new StringBuffer("SELECT * FROM Students WHERE gpa = 4.0 AND id > 100");
engine.parseSQL(command);
```
---

### Normal Insertion
```java
Hashtable<String, Object> htblColNameValue = new Hashtable<>();
htblColNameValue.put("id", 1);
htblColNameValue.put("name", "student1");
htblColNameValue.put("gpa", 3.3);
engine.insertIntoTable("Students", htblColNameValue);
```
### Insertion Using SQL
```java
StringBuffer command = new StringBuffer("INSERT INTO Students(id, gpa, name) VALUES(1, 3.3, 'student1')");
		engine.parseSQL(command);
```
---

### Normal Deletion
```java
Hashtable<String, Object> htblColNameValue = new Hashtable<>();
htblColNameValue.put("gpa", 3.3);
engine.DeleteFromTable("Students", htblColNameValue);
```
### Deletion Using SQL
```java
StringBuffer command = new StringBuffer("DELETE FROM  Students WHERE gpa = 3.3");
		engine.parseSQL(command);
```
---

## Run Locally

1- Clone the project

```bash
  git clone https://github.com/AhmedHawater2003/Database-Engine.git
```

2- Go to the project directory


3- Build the project using Maven
```bash
  mvn clean
```

4- Run Javafx project using Maven
```bash
  mvn javafx:run
```

##  License
[![MIT License](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)



## Authors
- [Ahd Mostafa](https://github.com/AhdMostafa0)
- [Ahmed Hawater](https://github.com/AhmedHawater2003)
- [Ali Mahmoud Shokry](https://github.com/AliShokryy)
- [Abdullah Mahmoud](https://github.com/dodzii)
- [Abdelrahman M.Talaat](https://github.com/Talaat-jr)
- [Youssef Elshenawy](https://github.com/youssefelshenawy)
