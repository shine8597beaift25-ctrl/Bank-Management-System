package interfaces;

/**
 * Any object that can show itself on the console implements this interface.
 *
 * Used by: Customer, BankAccount (and its subclasses) and Transaction.
 * Because they all share this interface, one method such as
 * ConsoleUtil.printTable(headers, List<? extends Displayable>) can print
 * any of them. That is runtime polymorphism through an interface.
 */
public interface Displayable {

    /** Full multi-line details of the object. */
    String getDisplayText();

    /** One row of values for printing the object inside a table. */
    String[] toTableRow();

    /** Default method (Java 8+): every implementing class gets this for free. */
    default void display() {
        System.out.println(getDisplayText());
    }
}
