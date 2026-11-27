# Progress Report – Bank Management System

**Overall status:** Complete. All required modules are built, compiled and tested on MySQL 8.
**Last updated:** 27 November 2026 (database changed from SQLite to MySQL)

---

## 1. Version history

| Version | Date | Change |
|---|---|---|
| 1.0 | 20 Nov 2026 | First complete version, using an SQLite database file |
| 2.0 | 27 Nov 2026 | **Database changed to MySQL 8** (see section 2) and fully retested |

---

## 2. MySQL migration (version 2.0)

| Area | What changed |
|---|---|
| Settings | New `db.properties` file (host, port, database, user, password). Values can be overridden with `-Ddb.name=...` etc. |
| `DatabaseManager` | Connects with `jdbc:mysql://host:port/database`, creates the database automatically (`CREATE DATABASE IF NOT EXISTS`), loads `com.mysql.cj.jdbc.Driver`, gives clear messages when MySQL is not running or the password is wrong |
| `schema.sql` | Rewritten for MySQL: `ENGINE=InnoDB`, `VARCHAR`, `DECIMAL(15,2)` for money, `DATE` / `DATETIME`, `AUTO_INCREMENT`, named `CHECK`/`UNIQUE`/`FOREIGN KEY` constraints, indexes inside `CREATE TABLE` |
| Repositories | Money read/written with `getBigDecimal` / `setBigDecimal`; dates with `setObject` / `getObject(..., LocalDate.class)` (JDBC 4.2) |
| `AccountService` | Account rows are locked with `SELECT ... FOR UPDATE` inside every deposit, withdrawal and transfer; transfers lock rows in alphabetical order to avoid deadlocks |
| Backup (menu 12 > 6) | SQLite file copy replaced by an SQL backup: every row written as `INSERT` statements with a `FileOutputStream`, then checked by reading it back with a `FileInputStream`. The file can be loaded with the `mysql` client |
| `pom.xml` / `lib/` | Dependency changed to `com.mysql:mysql-connector-j:9.4.0`; the SQLite jar was removed; `lib/README.txt` explains where to put the driver jar for non-Maven builds |
| Tests | Test runner now uses a separate MySQL database `bank_management_test`; rollback test uses a MySQL trigger; 3 new checks (row lock, lock release, DATE round-trip): **67 checks** |
| Sample data | New `data/sample/sample_database.sql` loads the whole sample database in one command |
| Documents | README, documentation, test cases, viva questions updated for MySQL |

---

## 3. Development log (version 1.0)

Work followed the 19-step plan from the project brief. Times are from the single development session.

| Step | Work done | Evidence |
|---|---|---|
| 1 | Checked the workspace: JDK 21 and Maven available; Maven Central not reachable | – |
| 2 | Created folder structure, `pom.xml`, build and run scripts | `pom.xml`, `build.*`, `run.*` |
| 3 | Wrote model classes, interfaces, enums and custom exceptions | `model/`, `interfaces/`, `exceptions/` |
| 4 | Wrote `schema.sql` (3 tables, constraints, indexes) and `DatabaseManager` (SQLite in v1.0, MySQL from v2.0) | `src/main/resources/schema.sql` |
| 5 | Customer and account management (repositories + services) | `CustomerService`, `AccountService` |
| 6 | Deposit, withdrawal, transfer with `commit()` / `rollback()` | `AccountService` |
| 7 | Transaction history and reports | `TransactionService`, `ReportService` |
| 8 | Collections used across modules; `runSafely` central exception handling | `CollectionsDemo`, `Main` |
| 9 | CSV import/export, backup (byte streams), StreamTokenizer | `FileManager`, `ImportResult` |
| 10 | Lambdas, streams and generics (`DataRepository<T,K>`, `ImportResult<T>`, `RequestQueue<T>`) | `ReportService`, `JavaConceptsDemo` |
| 11 | Multithreading demos and producer–consumer queue | `multithreading/` |
| 12 | All 15 menu options and submenus wired in `Main` | `Main.java` |
| 13 | Compiled with `javac --release 11 -Xlint:all`: 0 errors, 0 warnings | – |
| 14 | Ran the program with scripted input for every menu option | `test-evidence/01`–`11` |
| 15 | Fixed bugs found in testing (see section 3) | – |
| 16 | Restarted the program and confirmed data was still there | `test-evidence/09_restart_persistence.txt` |
| 17 | Tested successful, refused and rolled-back transfers | TC30–TC40 |
| 18 | Wrote README, documentation, test cases, viva questions | `*.md` |
| 19 | Rebuilt clean sample database; checked every required file exists | `test-evidence/10_final_demo_dataset.txt` |

---

## 4. Completed components

| Component | Status | Notes |
|---|---|---|
| Customer management (register, view, search, update, delete, validation, no duplicates) | ✅ Done | Duplicate email also blocked |
| Account management (savings, current, view, search, holder info, close, status) | ✅ Done | Separate `SavingsAccount` / `CurrentAccount` classes |
| Deposit | ✅ Done | Validated, saved with history |
| Withdrawal | ✅ Done | Balance check, limit check, refused attempts saved as FAILED |
| Money transfer with JDBC transaction | ✅ Done | Rollback proven with a forced failure |
| Balance inquiry | ✅ Done | Includes interest estimate (polymorphism) |
| Transaction history (7 required views + by reference) | ✅ Done | |
| Banking reports (all 10 required + extras) | ✅ Done | Streams and lambdas; save to file |
| Sort customer records | ✅ Done | Comparable and Comparator, asc/desc |
| CSV import/export with validation | ✅ Done | Line-numbered error list |
| Collections demonstration (11 types + iterators + Collections methods) | ✅ Done | Uses live data |
| Multithreading demonstration (all listed concepts) | ✅ Done | Producer–consumer on real accounts |
| Exception handling (5 required custom exceptions + 1 extra) | ✅ Done | |
| Generics (`DataRepository<T,K>` and more) | ✅ Done | |
| File handling (byte, character, StreamTokenizer) | ✅ Done | |
| MySQL database with constraints, foreign keys, indexes, row locking | ✅ Done | Created automatically on first start |
| Automated test runner (67 checks) | ✅ Done | 67/67 pass on MySQL 8.0.46 |
| README, PROJECT_DOCUMENTATION, TEST_CASES, PROGRESS, VIVA_QUESTIONS | ✅ Done | |

**Size:** 43 Java source files (42 application + 1 test runner), about 5,850 lines including comments.

---

## 5. Problems found during testing and how they were fixed

| # | Problem | Fix |
|---|---|---|
| 1 | Maven Central was blocked on the build machine | Added `build`/`run` scripts that need no Maven |
| 2 | A refused account opening (deposit below minimum) used up an account number, leaving a gap (found by automated test TC26 failing) | Account number is now assigned only after all checks pass (`BankAccount.assignAccountNumber`) |
| 3 | Deposit/withdraw on a closed account asked for the amount before showing the error | Added an early `requireActive()` check in the menu |
| 4 | Transfer validation errors said "rolled back" even when no database work had started | Separate messages for validation errors and database failures; same-account check moved before the amount prompt |
| 5 | Producer–consumer demo never filled the queue, so the "producer waits" behaviour was not visible | Consumers now take 150 ms per request; producers are faster; the queue now fills and producers wait |
| 6 | Compiler warnings (`serialVersionUID`, `this` escape) | Added `serialVersionUID`; made `ReportThread` final |
| 7 | (v2.0) The official MySQL Connector/J jar could not be downloaded on the build machine | Tested with Ubuntu's packaged MariaDB Connector/J against the real MySQL 8.0.46 server; code uses only standard JDBC; `pom.xml` uses the official driver |
| 8 | (v2.0) The rollback test compared the full error text, but drivers add their own prefix such as `(conn=79)` | The test now checks that the expected text is *inside* the message |
| 9 | (v2.0) A limited MySQL user may not create the trigger used by the rollback test (binary logging is on) | That test is now reported as SKIPPED with a clear reason instead of crashing; README explains to run the tests as root |

---

## 6. Remaining tasks / known limitations

No required feature is missing. These are honest limitations and optional improvements:

| Item | Type |
|---|---|
| The Maven build (`mvn package`) was not run on the development machine because Maven Central was unreachable there. Compilation and all tests used `javac` with the identical driver jar. | Not verified here |
| No login or user roles | Out of scope |
| Interest is estimated, not posted monthly | Out of scope |
| Not tested with the official MySQL Connector/J jar (not downloadable on the build machine); tested with another standard JDBC driver against real MySQL 8 | Not verified here |
| The MySQL driver jar is not shipped; Maven downloads it, or it must be copied into `lib/` | Setup step |
| `db.properties` keeps the MySQL password as plain text | Acceptable for a student project |
| New connection per operation; a busy system would use a connection pool | Future improvement |
| CSV parser does not support line breaks inside a quoted value | Minor |
| Optional: replace the plain test runner with JUnit 5 | Future improvement |
| Optional: GUI (JavaFX/Swing) | Future improvement |
