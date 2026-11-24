# Bank Management System

A menu-driven **Core Java console application** that manages bank customers, savings and current accounts, deposits, withdrawals, transfers, transaction history and reports. Data is stored in an **SQLite** database through **JDBC**.

> This is an educational banking simulation. It does not connect to any real bank, payment gateway or financial account.

---

## 1. What you need

| Item | Version | Notes |
|---|---|---|
| Java JDK | 11 or newer (tested on JDK 21) | `java -version` and `javac -version` must work in a terminal |
| SQLite JDBC driver | 3.50.3.0 | **Already included** in `lib/sqlite-jdbc-3.50.3.0.jar` |
| Maven (optional) | 3.6+ | Only if you prefer Maven. Not needed for the scripts below |
| IDE (optional) | NetBeans, Eclipse or IntelliJ IDEA | Any of them works |

You do **not** need to install SQLite separately. The driver jar contains SQLite itself, and the database file is created automatically.

---

## 2. Quick start (terminal, no Maven needed)

Open a terminal **inside the `BankManagementSystem` folder** (the program reads `data/` and `src/main/resources/` relative to this folder).

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

What the scripts do:
- `build` compiles every `.java` file in `src/main/java` into the `out` folder, with the driver jar on the classpath.
- `run` starts `main.Main` with `out` and `lib/*` on the classpath.

The same commands by hand:
```sh
javac --release 11 -encoding UTF-8 -d out -cp "lib/*" $(find src/main/java -name "*.java")
cp src/main/resources/schema.sql out/
java -cp "out:lib/*" main.Main          # on Windows use  "out;lib/*"
```

---

## 3. Running with Maven (optional)

The `pom.xml` declares the SQLite JDBC dependency and a runnable-jar build. With internet access:

```sh
mvn compile exec:java                 # compile and run
mvn package                           # builds target/bank-management-system-jar-with-dependencies.jar
java -jar target/bank-management-system-jar-with-dependencies.jar
```

Run these from the project folder so the program finds the `data` folder.

> Honest note: the build machine used to develop this project had no access to Maven Central, so the Maven build itself was **not** run there. The project was compiled and tested with `javac` and the same driver jar (see `TEST_CASES.md`). The `pom.xml` is standard and well-formed; if Maven reports a problem on your machine, use the scripts in section 2.

---

## 4. Opening in an IDE

**NetBeans**
1. *File > Open Project* and pick the `BankManagementSystem` folder. NetBeans recognises it as a Maven project.
2. Right-click the project > *Run*. (Main class: `main.Main`.)
3. Without internet for Maven: create a normal Java project instead, set `src/main/java` as the source folder, add `src/main/resources` as a source/resource folder, and add `lib/sqlite-jdbc-3.50.3.0.jar` under *Libraries*.

**Eclipse**
1. *File > Import > Maven > Existing Maven Projects*, choose the folder, *Finish*.
2. Right-click `Main.java` > *Run As > Java Application*.
3. Without Maven: *File > New > Java Project* (uncheck "use default location", point to this folder), then *Build Path > Add External Archives* > `lib/sqlite-jdbc-3.50.3.0.jar`.
4. Make sure the run configuration's **working directory** is the project folder (this is the default).

**IntelliJ IDEA**
1. *File > Open* the folder (or the `pom.xml`). Run `main.Main`.

---

## 5. Database configuration

| Setting | Value |
|---|---|
| Database file | `data/bank_management.db` |
| JDBC URL | `jdbc:sqlite:data/bank_management.db` |
| Driver class | `org.sqlite.JDBC` |
| Schema | `src/main/resources/schema.sql` (tables `customers`, `accounts`, `transactions`) |
| Foreign keys | Turned on for every connection (`foreign_keys=true`) |

- On first start the program creates the `data` folder and all tables automatically (`CREATE TABLE IF NOT EXISTS`). Existing data is never wiped.
- To start with an empty bank, close the program and delete `data/bank_management.db`.
- To use a different database file: `java -Dbank.db.path=data/other.db -cp "out:lib/*" main.Main`
- Money values are stored as exact text (for example `1500.00`) and handled in Java as `BigDecimal`. See `PROJECT_DOCUMENTATION.md` section 11 for why.

### Included data

The delivered `data/bank_management.db` already contains sample data: 8 customers, 9 accounts (one closed) and 13 transactions, including one transfer and one refused withdrawal. The same data is exported in `data/customers.csv`, `data/accounts.csv` and `data/transactions.csv`.

`data/sample/` holds files for testing CSV import:
- `customers_sample.csv`, `accounts_sample.csv` – clean sample data
- `customers_invalid.csv`, `accounts_invalid.csv` – files with deliberate mistakes, to show validation

---

## 6. Using the program

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

Try these with the included data:
- **8** then `SB1006` – balance inquiry with estimated interest.
- **7** then `SB1001`, `CA1005`, `500`, `y` – a transfer. Then **9 > 8** with the printed reference ID shows both sides.
- **6** then `SB1007`, `5000` – refused withdrawal (insufficient balance), saved as FAILED.
- **10 > 1** – bank summary report.
- **14 > 5** then `SB1006`, `y` – two producer and two consumer threads working on one account at the same time.

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

## 7. Running the automated tests

```sh
./run-tests.sh        # Windows: run-tests.bat
```

This compiles the program and `src/test/java/test/BankSystemTestRunner.java`, then runs 64 checks against a **separate** database (`data/test_bank.db`) so your real data is not touched. Expected last line: `=== RESULT: 64 passed, 0 failed ===`.

---

## 8. Folder structure

```
BankManagementSystem/
├── pom.xml                      Maven build file (SQLite JDBC dependency)
├── build.sh / build.bat         Compile without Maven
├── run.sh / run.bat             Run without Maven
├── run-tests.sh / .bat          Run the automated tests
├── lib/sqlite-jdbc-3.50.3.0.jar SQLite JDBC driver
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
├── src/main/resources/schema.sql
├── src/test/java/test/BankSystemTestRunner.java
├── data/                Database, CSV files, reports, backups, sample files
├── test-evidence/       Saved console output of every test run
├── README.md  PROJECT_DOCUMENTATION.md  TEST_CASES.md  PROGRESS.md  VIVA_QUESTIONS.md
```

---

## 9. Troubleshooting

| Problem | Fix |
|---|---|
| `SQLite JDBC driver not found` | The jar is not on the classpath. Use the scripts, or add `lib/sqlite-jdbc-3.50.3.0.jar` to your IDE's libraries. |
| `schema.sql not found` | Run the program from the project folder, or copy `schema.sql` into your output folder (the build scripts do this). |
| `javac: invalid flag: --release` | Your JDK is version 8. Install JDK 11 or newer. |
| `Rs.` instead of a rupee sign | Intentional: the plain text works in every Windows console. |
| `[SQLITE_BUSY] database is locked` | Another program (for example a DB browser) has the file open for writing. Close it. |
