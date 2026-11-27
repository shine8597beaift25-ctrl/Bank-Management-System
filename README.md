# Bank Management System

A menu-driven **Core Java console application** that manages bank customers, savings and current accounts, deposits, withdrawals, transfers, transaction history and reports. Data is stored in a **MySQL** database through **JDBC**.

> This is an educational banking simulation. It does not connect to any real bank, payment gateway or financial account.

---

## 1. What you need

| Item | Version | Notes |
|---|---|---|
| Java JDK | 11 or newer (tested on JDK 21) | `java -version` and `javac -version` must work in a terminal |
| MySQL Community Server | **8.0.16 or newer** (tested on 8.0.46) | Older versions ignore the CHECK rules |
| MySQL Connector/J (JDBC driver) | 8.x or 9.x (pom uses 9.4.0) | Maven downloads it for you, **or** copy the jar into `lib/` (step 3) |
| Maven (optional) | 3.6+ | Only if you prefer Maven |
| IDE (optional) | NetBeans, Eclipse or IntelliJ IDEA | Any of them works |

---

## 2. Set up MySQL (one time)

1. Install **MySQL Community Server 8** from https://dev.mysql.com/downloads/mysql/ (on Windows, the *MySQL Installer* is easiest). During installation you choose a password for the `root` user – remember it.
2. Make sure the MySQL server is running (Windows: *Services > MySQL80*; Linux: `sudo service mysql start`).
3. Open **`db.properties`** in the project folder and set your own values:

```properties
db.host=localhost
db.port=3306
db.name=bank_management
db.user=root
db.password=root        <-- change to YOUR MySQL password
db.options=useSSL=false&allowPublicKeyRetrieval=true
```

That's all. You do **not** need to create the database or tables yourself: on first start the program runs `CREATE DATABASE IF NOT EXISTS bank_management` and creates the three tables from `src/main/resources/schema.sql`. Existing data is never wiped.

> Prefer not to use `root`? Create a user in MySQL and put it in `db.properties`:
> ```sql
> CREATE USER 'bankuser'@'localhost' IDENTIFIED BY 'Bank@123';
> GRANT ALL PRIVILEGES ON bank_management.* TO 'bankuser'@'localhost';
> GRANT ALL PRIVILEGES ON bank_management_test.* TO 'bankuser'@'localhost';   -- only for the automated tests
> ```
> This user can create and use only those two databases. The automated tests run with it too, but the rollback test (TC37–TC40) is **skipped**, because creating a trigger needs extra rights when MySQL binary logging is on. Run the tests as `root` to include it.

---

## 3. Get the MySQL JDBC driver

**With Maven:** nothing to do – `pom.xml` declares `com.mysql:mysql-connector-j:9.4.0` and Maven downloads it.

**Without Maven (scripts or plain IDE project):**
1. Go to https://dev.mysql.com/downloads/connector/j/ and choose **Platform Independent**.
2. Download the ZIP, unzip it, and copy `mysql-connector-j-<version>.jar` into the project's **`lib/`** folder.

---

## 4. Build and run (terminal, no Maven needed)

Open a terminal **inside the `BankManagementSystem` folder** (the program reads `db.properties` and `data/` from this folder).

**Windows**
```bat
build.bat
run.bat
```

**Linux / macOS**
```sh
chmod +x *.sh      # first time only
./build.sh
./run.sh
```

On success the first line is:
```
[INFO] Connected to MySQL database bank_management on localhost:3306 (user root)
```

The same commands by hand:
```sh
javac --release 11 -encoding UTF-8 -d out $(find src/main/java -name "*.java")
cp src/main/resources/schema.sql out/
java -cp "out:lib/*" main.Main          # on Windows use  "out;lib/*"
```

### With Maven
```sh
mvn compile exec:java                 # compile and run
mvn package                           # builds target/bank-management-system-jar-with-dependencies.jar
java -jar target/bank-management-system-jar-with-dependencies.jar
```
Run these from the project folder so the program finds `db.properties`.

> Honest note: the machine used to develop this project could not reach Maven Central, so the Maven build itself was **not** run there. The project was compiled with `javac` and tested against a real MySQL 8.0.46 server (see `TEST_CASES.md`).

---

## 5. Opening in an IDE

**NetBeans** – *File > Open Project*, pick the folder (it is a Maven project), then *Run*. Main class: `main.Main`.

**Eclipse** – *File > Import > Maven > Existing Maven Projects*, then run `Main.java` as a Java Application. Without Maven: create a Java project on this folder, set `src/main/java` (and `src/main/resources`) as source folders, and add the Connector/J jar via *Build Path > Add External Archives*.

**IntelliJ IDEA** – *File > Open* the folder or `pom.xml`, then run `main.Main`.

In every IDE the **working directory** must be the project folder (the default), so `db.properties` is found.

---

## 6. Sample data

The MySQL database starts empty. Load the sample data in one of two ways:

**Option A – from the program (shows CSV import):**
menu **12 > 4** with `data/sample/customers_sample.csv`, then **12 > 5** with `data/sample/accounts_sample.csv`.

**Option B – ready-made SQL script (8 customers, 9 accounts, 13 transactions):**
```sh
mysql -u root -p < data/sample/sample_database.sql
```
⚠️ This deletes any existing `bank_management` database first.

Other files:
- `data/customers.csv`, `data/accounts.csv`, `data/transactions.csv` – export of the sample data
- `data/sample/customers_invalid.csv`, `accounts_invalid.csv` – files with deliberate mistakes, to show validation
- `data/backup/*.sql` – example output of the backup feature (menu 12 > 6)
- `data/reports/*.txt` – example saved report (menu 10 > 7)

---

## 7. Using the program

```
====================================================
               BANK MANAGEMENT SYSTEM
====================================================

1. Customer Management
2. Create Bank Account
3. View All Bank Accounts
4. Search Bank Account
5. Deposit Money
6. Withdraw Money
7. Transfer Money
8. Check Account Balance
9. Transaction History
10. Banking Reports
11. Sort Customer Records
12. Import / Export CSV
13. Collections Demonstration
14. Multithreading Demonstration
15. Exit
====================================================
```

Try these with the sample data:
- **8** then `SB1006` – balance inquiry with estimated interest.
- **7** then `SB1001`, `CA1005`, `500`, `y` – a transfer. Then **9 > 8** with the printed reference ID shows both sides.
- **6** then `SB1007`, `5000` – refused withdrawal (insufficient balance), saved as FAILED.
- **10 > 1** – bank summary report.
- **12 > 6** – back up the whole database to an `.sql` file.
- **14 > 5** then `SB1006`, `y` – two producer and two consumer threads working on one account at the same time.

You can also look at the data directly in MySQL Workbench or the `mysql` client:
```sql
USE bank_management;
SELECT * FROM accounts;
SELECT * FROM transactions ORDER BY transaction_id DESC LIMIT 10;
```

Bank rules used by the simulation:

| Rule | Savings | Current |
|---|---|---|
| Account number prefix | `SB` | `CA` |
| Minimum opening deposit | Rs. 500 | Rs. 1,000 |
| Interest | 4% per year (estimate shown) | None |
| Largest single withdrawal / transfer | Rs. 50,000 | Rs. 2,00,000 |
| Overdraft | Not allowed | Not allowed |

Other rules: one **active** account of each type per customer; an account can be closed only when its balance is zero; a customer can be deleted only if they have no accounts; one transaction is limited to Rs. 10,00,000.

---

## 8. Running the automated tests

```sh
./run-tests.sh        # Windows: run-tests.bat
```

This compiles the program and `src/test/java/test/BankSystemTestRunner.java`, then runs 67 checks against a **separate** database, `bank_management_test`, which it deletes and re-creates each time. Your real `bank_management` data is not touched. The MySQL user needs permission to create/drop that database and to create a trigger (the `root` user can). Expected last line: `=== RESULT: 67 passed, 0 failed ===`. With a limited user the rollback test is skipped and you will see `63 passed, 0 failed, 1 skipped`.

---

## 9. Folder structure

```
BankManagementSystem/
├── pom.xml                      Maven build file (MySQL Connector/J dependency)
├── db.properties                MySQL host, port, database, user, password
├── build.sh / build.bat         Compile without Maven
├── run.sh / run.bat             Run without Maven
├── run-tests.sh / .bat          Run the automated tests
├── lib/                         Put mysql-connector-j-*.jar here (non-Maven builds)
├── src/main/java/
│   ├── main/            Main.java (menus)
│   ├── model/           Person, Customer, BankAccount, SavingsAccount, CurrentAccount, Transaction + enums
│   ├── service/         CustomerService, AccountService, TransactionService, ReportService
│   ├── repository/      DataRepository<T,K>, CustomerRepository, AccountRepository, TransactionRepository
│   ├── database/        DatabaseManager
│   ├── filehandling/    FileManager, ImportResult<T>
│   ├── collections/     CollectionsDemo, JavaConceptsDemo
│   ├── multithreading/  ReportThread, BankingRequest, Producer, Consumer, RequestQueue<T>, ProcessingStats, MultithreadingDemo
│   ├── exceptions/      5 required custom exceptions + DuplicateCustomerException
│   ├── interfaces/      Displayable, TransactionOperations
│   └── util/            ConsoleUtil, InputHelper, ValidationUtil
├── src/main/resources/schema.sql   MySQL tables, constraints and indexes
├── src/test/java/test/BankSystemTestRunner.java
├── data/                CSV files, sample SQL script, reports, backups
├── test-evidence/       Saved console output of every test run
├── README.md  PROJECT_DOCUMENTATION.md  TEST_CASES.md  PROGRESS.md  VIVA_QUESTIONS.md
```

---

## 10. Troubleshooting

| Message | Fix |
|---|---|
| `MySQL JDBC driver not found` / `No suitable driver` | The Connector/J jar is not on the classpath. Copy it into `lib/` (step 3) or use Maven. |
| `Access denied for user 'root'@'localhost'` | Wrong user name or password in `db.properties`. |
| `Communications link failure` / `Connection refused` | The MySQL server is not running, or `db.host` / `db.port` is wrong. |
| `Public Key Retrieval is not allowed` | Keep `allowPublicKeyRetrieval=true` in `db.options`. |
| `Access denied ... to database 'bank_management'` | Your MySQL user has no rights on that database. Grant them as root (step 2). |
| Negative balances not refused when typed directly in SQL | Your MySQL is older than 8.0.16 and ignores CHECK rules. Upgrade to MySQL 8. |
| `schema.sql not found` | Run the program from the project folder, or copy `schema.sql` into your output folder (the build scripts do this). |
| `javac: invalid flag: --release` | Your JDK is version 8. Install JDK 11 or newer. |
| `Rs.` instead of a rupee sign | Intentional: the plain text works in every Windows console. |
