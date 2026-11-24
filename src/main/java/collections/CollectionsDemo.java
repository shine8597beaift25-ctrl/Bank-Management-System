package collections;

import model.AccountStatus;
import model.AccountType;
import model.BankAccount;
import model.Customer;
import model.Transaction;
import util.ConsoleUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Demonstrates the Java Collections Framework using the bank's own data.
 *
 * The lists passed in come from the database. If the database is still empty,
 * a few clearly-labelled sample objects (NOT saved anywhere) are used instead,
 * so every demonstration always has something to show.
 *
 * Collections used elsewhere in the real project:
 *   ArrayList      - every repository result list
 *   LinkedList     - ImportResult error list, RequestQueue storage
 *   HashSet        - duplicate detection in CSV import
 *   LinkedHashMap  - report summary, CSV rows with line numbers
 *   TreeMap        - reports grouped by account/transaction type
 *   Queue          - producer-consumer RequestQueue
 */
public class CollectionsDemo {

    private final List<Customer> customers;
    private final List<BankAccount> accounts;
    private final List<Transaction> transactions;
    private final boolean usingSampleData;

    public CollectionsDemo(List<Customer> customers, List<BankAccount> accounts, List<Transaction> transactions) {
        this.usingSampleData = customers.isEmpty() || accounts.isEmpty();
        this.customers = usingSampleData ? sampleCustomers() : new ArrayList<>(customers);
        this.accounts = usingSampleData ? sampleAccounts() : new ArrayList<>(accounts);
        this.transactions = new ArrayList<>(transactions);
    }

    /** The customers used by the demo (live data, or the sample list if the database is empty). */
    public List<Customer> getCustomers() {
        return customers;
    }

    public List<BankAccount> getAccounts() {
        return accounts;
    }

    public void printDataSourceNote() {
        if (usingSampleData) {
            ConsoleUtil.printInfo("Database has no customers/accounts yet - using SAMPLE data (not saved).");
        } else {
            ConsoleUtil.printInfo("Using live data: " + customers.size() + " customers, " + accounts.size()
                    + " accounts, " + transactions.size() + " transactions.");
        }
    }

    // =====================================================================
    // 1. Lists
    // =====================================================================

    public void demonstrateLists() {
        ConsoleUtil.printSubHeader("ArrayList and LinkedList");

        // ArrayList: fast access by index - good for "show customer number 1"
        List<Customer> customerList = new ArrayList<>(customers);
        System.out.println("ArrayList size         : " + customerList.size());
        System.out.println("ArrayList get(0)       : " + customerList.get(0));
        System.out.println("ArrayList contains it? : " + customerList.contains(customerList.get(0)));

        // LinkedList: fast add/remove at both ends - good for a "recently viewed accounts" list
        LinkedList<String> recentlyViewed = new LinkedList<>();
        for (BankAccount account : accounts) {
            recentlyViewed.addFirst(account.getAccountNumber());   // newest at the front
            if (recentlyViewed.size() > 3) {
                recentlyViewed.removeLast();                       // keep only the 3 most recent
            }
        }
        System.out.println("LinkedList (3 most recently viewed accounts, newest first): " + recentlyViewed);
        System.out.println("LinkedList getFirst() = " + recentlyViewed.getFirst()
                + ", getLast() = " + recentlyViewed.getLast());
    }

    // =====================================================================
    // 2. Sets
    // =====================================================================

    public void demonstrateSets() {
        ConsoleUtil.printSubHeader("HashSet, LinkedHashSet and TreeSet");

        // HashSet: no duplicates, no guaranteed order
        Set<String> customerIdsWithAccounts = new HashSet<>();
        for (BankAccount account : accounts) {
            customerIdsWithAccounts.add(account.getCustomerId());   // a duplicate ID is simply ignored
        }
        System.out.println("HashSet - customers who own at least one account (" + customerIdsWithAccounts.size()
                + " unique out of " + accounts.size() + " accounts): " + customerIdsWithAccounts);

        // LinkedHashSet: no duplicates, keeps insertion order
        Set<String> accountsInActivityOrder = new LinkedHashSet<>();
        for (Transaction transaction : transactions) {
            accountsInActivityOrder.add(transaction.getAccountNumber());
        }
        if (accountsInActivityOrder.isEmpty()) {
            for (BankAccount account : accounts) {
                accountsInActivityOrder.add(account.getAccountNumber());
            }
        }
        System.out.println("LinkedHashSet - accounts in order of first activity: " + accountsInActivityOrder);

        // TreeSet: no duplicates, always sorted
        Set<String> sortedNames = new TreeSet<>();
        for (Customer customer : customers) {
            sortedNames.add(customer.getName());
        }
        System.out.println("TreeSet - customer names sorted A-Z: " + sortedNames);
    }

    // =====================================================================
    // 3. Maps
    // =====================================================================

    public void demonstrateMaps() {
        ConsoleUtil.printSubHeader("HashMap, LinkedHashMap and TreeMap");

        // HashMap: key -> value, fast lookup. Count accounts per customer.
        Map<String, Integer> accountsPerCustomer = new HashMap<>();
        for (BankAccount account : accounts) {
            // getOrDefault + put; Integer values are auto-boxed/unboxed
            accountsPerCustomer.put(account.getCustomerId(),
                    accountsPerCustomer.getOrDefault(account.getCustomerId(), 0) + 1);
        }
        System.out.println("HashMap - number of accounts per customer: " + accountsPerCustomer);

        // LinkedHashMap: keeps insertion order. Account -> balance in the order listed.
        Map<String, String> balances = new LinkedHashMap<>();
        for (BankAccount account : accounts) {
            balances.put(account.getAccountNumber(), ConsoleUtil.formatMoney(account.getBalance()));
        }
        System.out.println("LinkedHashMap - balance by account (insertion order): " + balances);

        // TreeMap: keys kept sorted. Total balance per account type.
        Map<AccountType, BigDecimal> totalByType = new TreeMap<>();
        for (BankAccount account : accounts) {
            totalByType.merge(account.getAccountType(), account.getBalance(), BigDecimal::add);
        }
        System.out.println("TreeMap - total balance per account type (sorted keys): " + totalByType);
    }

    // =====================================================================
    // 4. Queue, Deque and PriorityQueue
    // =====================================================================

    public void demonstrateQueues() {
        ConsoleUtil.printSubHeader("Queue, Deque and PriorityQueue");

        // Queue (FIFO): customers served at the counter in arrival order
        Queue<String> counterQueue = new LinkedList<>();
        for (Customer customer : customers) {
            counterQueue.offer(customer.getName());
        }
        System.out.println("Queue (first in, first out) - customers waiting: " + counterQueue);
        System.out.println("   poll() serves: " + counterQueue.poll() + " | next in line (peek): " + counterQueue.peek());

        // Deque used as a STACK (last in, first out): undo history of recent operations
        Deque<String> operationHistory = new ArrayDeque<>();
        operationHistory.push("Deposit Rs. 500 to " + accounts.get(0).getAccountNumber());
        operationHistory.push("Withdraw Rs. 200 from " + accounts.get(0).getAccountNumber());
        operationHistory.push("View balance of " + accounts.get(accounts.size() - 1).getAccountNumber());
        System.out.println("Deque as a stack - latest operation first: " + operationHistory);
        System.out.println("   pop() undoes: " + operationHistory.pop());
        operationHistory.offerLast("Oldest note added at the back");
        System.out.println("   Deque works at both ends: first = " + operationHistory.peekFirst()
                + " | last = " + operationHistory.peekLast());

        // PriorityQueue: always gives the highest balance first (VIP service order)
        PriorityQueue<BankAccount> vipQueue =
                new PriorityQueue<>(Comparator.comparing(BankAccount::getBalance).reversed());
        vipQueue.addAll(accounts);
        System.out.println("PriorityQueue - serve highest balance first:");
        int position = 1;
        while (!vipQueue.isEmpty() && position <= 3) {
            System.out.println("   " + position++ + ". " + vipQueue.poll());
        }
    }

    // =====================================================================
    // 5. Iterator and ListIterator
    // =====================================================================

    public void demonstrateIterators() {
        ConsoleUtil.printSubHeader("Iterator and ListIterator");

        // Iterator: the SAFE way to remove items while looping
        List<BankAccount> workingList = new ArrayList<>(accounts);
        Iterator<BankAccount> iterator = workingList.iterator();
        int removed = 0;
        while (iterator.hasNext()) {
            BankAccount account = iterator.next();
            if (!account.isActive()) {
                iterator.remove();   // list.remove() inside a for-each loop would throw ConcurrentModificationException
                removed++;
            }
        }
        System.out.println("Iterator removed " + removed + " closed account(s). Active accounts left: " + workingList.size());

        // ListIterator: can go forwards AND backwards, and can replace items with set()
        List<String> names = new ArrayList<>();
        for (Customer customer : customers) {
            names.add(customer.getName());
        }
        ListIterator<String> listIterator = names.listIterator();
        while (listIterator.hasNext()) {
            String name = listIterator.next();
            listIterator.set(name.toUpperCase());   // replace the current item
        }
        System.out.print("ListIterator backwards (after set() to UPPER CASE): ");
        while (listIterator.hasPrevious()) {
            System.out.print(listIterator.previous() + (listIterator.hasPrevious() ? ", " : ""));
        }
        System.out.println();
    }

    // =====================================================================
    // 6. Comparable, Comparator and the Collections utility class
    // =====================================================================

    public void demonstrateSortingUtilities() {
        ConsoleUtil.printSubHeader("Comparable, Comparator and Collections methods");
        List<Customer> list = new ArrayList<>(customers);

        Collections.sort(list);   // uses Customer.compareTo (Comparable) - by customer ID
        System.out.println("Collections.sort (Comparable, by ID)      : " + list);

        Collections.sort(list, Comparator.comparing(Customer::getName));   // Comparator - by name
        System.out.println("Collections.sort (Comparator, by name)    : " + list);

        Collections.reverse(list);
        System.out.println("Collections.reverse                        : " + list);

        Comparator<Customer> byAge = Comparator.comparingInt(Customer::getAge);
        System.out.println("Collections.max (oldest, by age Comparator): " + Collections.max(list, byAge)
                + " - age " + Collections.max(list, byAge).getAge());
        System.out.println("Collections.min (youngest)                 : " + Collections.min(list, byAge)
                + " - age " + Collections.min(list, byAge).getAge());

        List<BankAccount> accountList = new ArrayList<>(accounts);
        System.out.println("Collections.max (Comparable, highest account number): " + Collections.max(accountList));
        System.out.println("Collections.min (Comparator, lowest balance)         : "
                + Collections.min(accountList, Comparator.comparing(BankAccount::getBalance)));
    }

    // =====================================================================
    // Sample data (used only when the database is empty)
    // =====================================================================

    private static List<Customer> sampleCustomers() {
        List<Customer> sample = new ArrayList<>();
        sample.add(new Customer("C9003", "Meera Iyer", 41, "meera@example.com", "9876500003", "Chennai"));
        sample.add(new Customer("C9001", "Arjun Rao", 29, "arjun@example.com", "9876500001", "Bengaluru"));
        sample.add(new Customer("C9002", "Zoya Khan", 35, "zoya@example.com", "9876500002", "Mumbai"));
        return sample;
    }

    private static List<BankAccount> sampleAccounts() {
        List<BankAccount> sample = new ArrayList<>();
        LocalDate today = LocalDate.now();
        sample.add(BankAccount.createAccount(AccountType.SAVINGS, "SB9001", "C9001", "Arjun Rao",
                new BigDecimal("15000.00"), today, AccountStatus.ACTIVE));
        sample.add(BankAccount.createAccount(AccountType.CURRENT, "CA9002", "C9002", "Zoya Khan",
                new BigDecimal("82000.50"), today, AccountStatus.ACTIVE));
        sample.add(BankAccount.createAccount(AccountType.SAVINGS, "SB9003", "C9003", "Meera Iyer",
                new BigDecimal("0.00"), today, AccountStatus.CLOSED));
        sample.add(BankAccount.createAccount(AccountType.SAVINGS, "SB9004", "C9002", "Zoya Khan",
                new BigDecimal("4300.00"), today, AccountStatus.ACTIVE));
        return sample;
    }
}
