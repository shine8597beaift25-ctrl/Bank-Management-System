package collections;

import model.BankAccount;
import model.Customer;
import util.ConsoleUtil;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Demonstrates language features that do not fit naturally into one banking
 * menu option: String pool, == vs equals(), StringBuffer, arrays, wrapper classes,
 * lazy streams, functional interfaces and generics with wildcards.
 * (These same concepts are also USED in the real modules - see the comments.)
 */
public class JavaConceptsDemo {

    /** A custom functional interface: exactly one abstract method, so a lambda can implement it. */
    @FunctionalInterface
    public interface InterestCalculator {
        BigDecimal calculate(BigDecimal principal, BigDecimal ratePercent);
    }

    /** A simple GENERIC class: holds two related values of any types. */
    public static class Pair<A, B> {
        private final A first;
        private final B second;

        public Pair(A first, B second) {
            this.first = first;
            this.second = second;
        }

        public A getFirst() {
            return first;
        }

        public B getSecond() {
            return second;
        }

        @Override
        public String toString() {
            return "(" + first + ", " + second + ")";
        }
    }

    private final List<Customer> customers;
    private final List<BankAccount> accounts;

    public JavaConceptsDemo(List<Customer> customers, List<BankAccount> accounts) {
        this.customers = customers;
        this.accounts = accounts;
    }

    // =====================================================================
    // Strings, arrays, wrapper classes
    // =====================================================================

    public void demonstrateStringsArraysWrappers() {
        ConsoleUtil.printSubHeader("Strings, Arrays and Wrapper Classes");

        // ---- String pool and == vs equals() ----
        String literalOne = "SAVINGS";               // stored in the String pool
        String literalTwo = "SAVINGS";               // reuses the SAME pooled object
        String created = new String("SAVINGS");      // a NEW object on the heap
        System.out.println("literalOne == literalTwo      : " + (literalOne == literalTwo) + "  (same pooled object)");
        System.out.println("literalOne == new String(...) : " + (literalOne == created) + " (different objects)");
        System.out.println("literalOne.equals(created)    : " + literalOne.equals(created) + "  (same text - always use equals)");
        System.out.println("created.intern() == literalOne: " + (created.intern() == literalOne) + "  (intern() returns the pooled copy)");

        // ---- Immutability ----
        String name = "ravi kumar";
        String upper = name.toUpperCase();
        System.out.println("Immutability: after toUpperCase(), name is still '" + name + "', new String is '" + upper + "'");

        // ---- Comparison, searching, manipulation, formatting ----
        String email = "Ravi.Kumar@Example.com";
        System.out.println("equalsIgnoreCase       : " + email.equalsIgnoreCase("ravi.kumar@example.com"));
        System.out.println("compareTo(\"Anil\")      : " + "Ravi".compareTo("Anil") + " (positive = comes after)");
        System.out.println("indexOf('@')           : " + email.indexOf('@'));
        System.out.println("substring (domain)     : " + email.substring(email.indexOf('@') + 1));
        System.out.println("contains(\"Example\")    : " + email.contains("Example"));
        System.out.println("replace + toLowerCase  : " + email.replace("Example", "bank").toLowerCase());
        System.out.println("String.format          : " + String.format("%-10s|%8s|%,12.2f", "SB1001", "SAVINGS", new BigDecimal("125000.5")));
        System.out.println("Masked phone           : " + maskPhone("9876543210"));

        // ---- StringBuilder vs StringBuffer ----
        StringBuilder builder = new StringBuilder("SB");   // fast, NOT thread-safe (used in reports)
        builder.append(1001).append('-').append("ACTIVE").insert(0, '[').append(']');
        StringBuffer buffer = new StringBuffer("Log:"); // synchronized, thread-safe (used in ReportThread)
        buffer.append(" deposit").append(" ok").reverse().reverse();
        System.out.println("StringBuilder result   : " + builder + " | StringBuffer result: " + buffer);

        // ---- Multidimensional array: monthly deposits (rows = account, columns = month) ----
        int[][] monthlyDeposits = {
                {5000, 2000, 3500},
                {12000, 0, 8000},
                {700, 700, 700}
        };
        String[] months = {"Jan", "Feb", "Mar"};
        String[][] tableRows = new String[monthlyDeposits.length][];
        for (int row = 0; row < monthlyDeposits.length; row++) {
            int rowTotal = 0;
            String[] cells = new String[months.length + 2];
            cells[0] = "Account " + (row + 1);
            for (int column = 0; column < months.length; column++) {
                cells[column + 1] = String.valueOf(monthlyDeposits[row][column]);
                rowTotal += monthlyDeposits[row][column];
            }
            cells[months.length + 1] = String.valueOf(rowTotal);
            tableRows[row] = cells;
        }
        System.out.println("2-D array int[3][3] of monthly deposits:");
        ConsoleUtil.printTable(new String[] {"Account", "Jan", "Feb", "Mar", "Total"}, tableRows);

        // ---- Searching an array ----
        String[] accountNumbers = {"SB1004", "CA1001", "SB1002", "CA1003"};
        Arrays.sort(accountNumbers);
        System.out.println("Arrays.sort            : " + Arrays.toString(accountNumbers));
        System.out.println("Arrays.binarySearch SB1002 -> index " + Arrays.binarySearch(accountNumbers, "SB1002"));

        // ---- Wrapper classes, autoboxing, unboxing, parsing ----
        int age = Integer.parseInt("34");                 // String -> int
        Integer boxedAge = age;                           // autoboxing int -> Integer
        int unboxed = boxedAge;                           // unboxing Integer -> int
        double rate = Double.parseDouble("4.0");          // fine for a rate shown on screen
        BigDecimal money = new BigDecimal("1500.75");     // money must be BigDecimal
        List<Integer> ages = new ArrayList<>();
        ages.add(25);                                     // autoboxing when adding to a collection
        ages.add(boxedAge);
        int sum = 0;
        for (int value : ages) {                          // unboxing in the loop
            sum += value;
        }
        System.out.println("Integer.parseInt(\"34\") = " + age + ", boxed = " + boxedAge + ", unboxed = " + unboxed
                + ", Double.parseDouble = " + rate + ", sum of List<Integer> = " + sum);
        System.out.println("Integer.valueOf(127) == Integer.valueOf(127): " + (Integer.valueOf(127) == Integer.valueOf(127))
                + " (cached) but always compare wrapper values with equals()");
        System.out.println("Why BigDecimal? 0.1 + 0.2 as double = " + (0.1 + 0.2)
                + " but as BigDecimal = " + new BigDecimal("0.1").add(new BigDecimal("0.2")) + ". Money: " + money);
    }

    /** "9876543210" -> "98******10" using a char array (String manipulation). */
    private static String maskPhone(String phone) {
        char[] characters = phone.toCharArray();
        for (int i = 2; i < characters.length - 2; i++) {
            characters[i] = '*';
        }
        return new String(characters);
    }

    // =====================================================================
    // Lambdas, streams (lazy evaluation) and generics
    // =====================================================================

    public void demonstrateLambdaStreamsGenerics() {
        ConsoleUtil.printSubHeader("Lambda, Functional Interfaces, Lazy Streams and Generics");

        // ---- Built-in functional interfaces ----
        Predicate<BankAccount> richAccount = account -> account.getBalance().compareTo(new BigDecimal("10000")) >= 0;
        Function<Customer, String> toInitials = customer -> Arrays.stream(customer.getName().split(" "))
                .map(part -> part.substring(0, 1))
                .collect(Collectors.joining());
        Supplier<String> bankName = () -> BankAccount.BANK_NAME;
        Consumer<String> printer = System.out::println;            // method reference
        BiFunction<BigDecimal, BigDecimal, BigDecimal> addMoney = BigDecimal::add;

        printer.accept("Supplier gives the bank name : " + bankName.get());
        printer.accept("BiFunction adds 100 + 250.50  : " + addMoney.apply(new BigDecimal("100"), new BigDecimal("250.50")));
        printer.accept("Function -> customer initials : " + customers.stream().map(toInitials).collect(Collectors.toList()));
        printer.accept("Predicate -> accounts >= Rs. 10,000: " + accounts.stream().filter(richAccount)
                .map(BankAccount::getAccountNumber).collect(Collectors.toList()));

        // ---- Custom functional interface ----
        InterestCalculator simpleInterest = (principal, rate) -> principal.multiply(rate).divide(new BigDecimal("100"));
        printer.accept("Custom @FunctionalInterface: 4% of Rs. 25,000 = Rs. "
                + simpleInterest.calculate(new BigDecimal("25000"), new BigDecimal("4")));

        // ---- Lazy evaluation: intermediate steps run only when a terminal step asks for data ----
        System.out.println("\nLazy evaluation - building a pipeline (nothing printed yet):");
        Stream<String> pipeline = accounts.stream()
                .filter(account -> {
                    System.out.println("   filter() checking " + account.getAccountNumber());
                    return account.isActive();
                })
                .map(account -> {
                    System.out.println("   map() converting " + account.getAccountNumber());
                    return account.getAccountNumber();
                });
        System.out.println("Pipeline built. Now calling the terminal operation limit(2).collect(...):");
        List<String> firstTwoActive = pipeline.limit(2).collect(Collectors.toList());
        System.out.println("Result: " + firstTwoActive + " - processing stopped as soon as 2 items were found.");

        // ---- sorted, distinct, skip, limit ----
        List<String> distinctTypes = accounts.stream().map(account -> account.getAccountType().getLabel())
                .distinct().sorted().collect(Collectors.toList());
        List<String> skipFirstName = customers.stream().map(Customer::getName).sorted().skip(1)
                .collect(Collectors.toList());
        System.out.println("distinct() account types      : " + distinctTypes);
        System.out.println("sorted().skip(1) customer names: " + skipFirstName);

        // ---- Generics: generic class, generic method, bounded type, wildcard, diamond operator ----
        Pair<String, BigDecimal> accountBalance = new Pair<>(accounts.get(0).getAccountNumber(), accounts.get(0).getBalance());
        System.out.println("Generic class Pair<String, BigDecimal> : " + accountBalance);
        System.out.println("Generic method firstOrDefault (String) : " + firstOrDefault(List.of("SB1001", "CA1002"), "none"));
        System.out.println("Generic method firstOrDefault (empty)  : " + firstOrDefault(new ArrayList<Integer>(), -1));
        System.out.println("Bounded <T extends Number> average of [10, 20, 45] = " + averageOf(List.of(10, 20, 45)));
        System.out.println("Wildcard List<? extends BankAccount> count = " + countActive(accounts));
        // Type safety: the next line would NOT compile, because the list only accepts Integers:
        //   List<Integer> numbers = new ArrayList<>(); numbers.add("ten");
    }

    /** Generic method: works for a list of ANY type T. */
    public static <T> T firstOrDefault(List<T> items, T defaultValue) {
        return items.isEmpty() ? defaultValue : items.get(0);
    }

    /** Bounded type: T must be a Number (Integer, Double, BigDecimal ...). */
    public static <T extends Number> double averageOf(List<T> numbers) {
        double total = 0;
        for (T number : numbers) {
            total += number.doubleValue();
        }
        return numbers.isEmpty() ? 0 : total / numbers.size();
    }

    /** Wildcard: accepts List<BankAccount>, List<SavingsAccount> or List<CurrentAccount>. */
    public static long countActive(List<? extends BankAccount> accountList) {
        return accountList.stream().filter(BankAccount::isActive).count();
    }
}
