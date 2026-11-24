# Progress Report – Bank Management System

**Overall status:** Complete. All required modules are built, compiled and tested.
**Last updated:** 20 November 2026

---

## 1. Development log

Work followed the 19-step plan from the project brief. Times are from the single development session.

| Step | Work done | Evidence |
|---|---|---|
| 1 | Checked the workspace: JDK 21 and Maven available; Maven Central not reachable, so the SQLite JDBC jar was taken from the official GitHub release and placed in `lib/` | `lib/sqlite-jdbc-3.50.3.0.jar` |
| 2 | Created folder structure, `pom.xml`, build and run scripts | `pom.xml`, `build.*`, `run.*` |
| 3 | Wrote model classes, interfaces, enums and custom exceptions | `model/`, `interfaces/`, `exceptions/` |
| 4 | Wrote `schema.sql` (3 tables, constraints, indexes) and `DatabaseManager` | `src/main/resources/schema.sql` |
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

## 2. Completed components

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
| SQLite database with constraints, foreign keys, indexes | ✅ Done | |
| Automated test runner (64 checks) | ✅ Done | 64/64 pass |
| README, PROJECT_DOCUMENTATION, TEST_CASES, PROGRESS, VIVA_QUESTIONS | ✅ Done | |

**Size:** 43 Java source files (42 application + 1 test runner), about 5,600 lines including comments.

---

## 3. Problems found during testing and how they were fixed

| # | Problem | Fix |
|---|---|---|
| 1 | Maven Central was blocked on the build machine | Downloaded the same driver version from the official GitHub release into `lib/`; added `build`/`run` scripts that need no Maven |
| 2 | A refused account opening (deposit below minimum) used up an account number, leaving a gap (found by automated test TC26 failing) | Account number is now assigned only after all checks pass (`BankAccount.assignAccountNumber`) |
| 3 | Deposit/withdraw on a closed account asked for the amount before showing the error | Added an early `requireActive()` check in the menu |
| 4 | Transfer validation errors said "rolled back" even when no database work had started | Separate messages for validation errors and database failures; same-account check moved before the amount prompt |
| 5 | Producer–consumer demo never filled the queue, so the "producer waits" behaviour was not visible | Consumers now take 150 ms per request; producers are faster; the queue now fills and producers wait |
| 6 | Compiler warnings (`serialVersionUID`, `this` escape) | Added `serialVersionUID`; made `ReportThread` final |

---

## 4. Remaining tasks / known limitations

No required feature is missing. These are honest limitations and optional improvements:

| Item | Type |
|---|---|
| The Maven build (`mvn package`) was not run on the development machine because Maven Central was unreachable there. Compilation and all tests used `javac` with the identical driver jar. | Not verified here |
| No login or user roles | Out of scope |
| Interest is estimated, not posted monthly | Out of scope |
| SQLite is for one computer, not a multi-branch network | Design limit |
| CSV parser does not support line breaks inside a quoted value | Minor |
| Optional: replace the plain test runner with JUnit 5 | Future improvement |
| Optional: GUI (JavaFX/Swing) | Future improvement |
