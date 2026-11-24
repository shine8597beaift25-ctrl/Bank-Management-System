package service;

import exceptions.CustomerNotFoundException;
import exceptions.DuplicateCustomerException;
import filehandling.ImportResult;
import model.Customer;
import repository.CustomerRepository;
import util.ValidationUtil;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Business rules for customers. Main (the menu) calls this class;
 * this class calls CustomerRepository for the SQL.
 *
 * Exception propagation: SQLException is thrown by the repository, passes
 * through this class (declared with "throws") and is finally caught in Main.
 */
public class CustomerService {

    /** Ready-made Comparators (lambda expressions and method references). */
    public static final Comparator<Customer> BY_NAME =
            Comparator.comparing(Customer::getName, String.CASE_INSENSITIVE_ORDER);
    public static final Comparator<Customer> BY_AGE =
            Comparator.comparingInt(Customer::getAge).thenComparing(Customer::getName);
    public static final Comparator<Customer> BY_EMAIL = (first, second) -> first.getEmail().compareTo(second.getEmail());

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    /** Validates and saves a new customer. Blocks duplicate IDs and duplicate emails. */
    public void registerCustomer(Customer customer) throws DuplicateCustomerException, SQLException {
        ValidationUtil.validateCustomer(customer);   // throws IllegalArgumentException for bad data
        if (customerRepository.existsById(customer.getCustomerId())) {
            throw new DuplicateCustomerException("Customer ID " + customer.getCustomerId() + " is already registered.");
        }
        if (customerRepository.findByEmail(customer.getEmail()).isPresent()) {
            throw new DuplicateCustomerException("Email " + customer.getEmail() + " is already used by another customer.");
        }
        customerRepository.save(customer);
    }

    public List<Customer> getAllCustomers() throws SQLException {
        return customerRepository.findAll();
    }

    /** Finds a customer or throws a CHECKED exception if the ID does not exist. */
    public Customer getCustomer(String customerId) throws CustomerNotFoundException, SQLException {
        String cleanedId = customerId.trim().toUpperCase();
        // orElseThrow takes a lambda that creates the exception only when needed
        return customerRepository.findById(cleanedId)
                .orElseThrow(() -> new CustomerNotFoundException(cleanedId));
    }

    public boolean customerExists(String customerId) throws SQLException {
        return customerRepository.existsById(customerId.trim().toUpperCase());
    }

    /** Saves changed details. The customer ID itself cannot be changed. */
    public void updateCustomer(Customer customer)
            throws CustomerNotFoundException, DuplicateCustomerException, SQLException {
        ValidationUtil.validateCustomer(customer);
        getCustomer(customer.getCustomerId());   // throws CustomerNotFoundException if missing

        Optional<Customer> emailOwner = customerRepository.findByEmail(customer.getEmail());
        if (emailOwner.isPresent() && !emailOwner.get().getCustomerId().equals(customer.getCustomerId())) {
            throw new DuplicateCustomerException("Email " + customer.getEmail() + " is already used by customer "
                    + emailOwner.get().getCustomerId() + ".");
        }
        customerRepository.update(customer);
    }

    /**
     * Deletes a customer who has no bank accounts.
     * Customers with accounts are kept so that account and transaction history stays complete.
     */
    public void deleteCustomer(String customerId) throws CustomerNotFoundException, SQLException {
        Customer customer = getCustomer(customerId);
        int accountCount = customerRepository.countAccounts(customer.getCustomerId());
        if (accountCount > 0) {
            throw new IllegalStateException("Customer " + customer.getCustomerId() + " has " + accountCount
                    + " account(s) on record and cannot be deleted. Accounts keep their history; close them instead.");
        }
        customerRepository.deleteById(customer.getCustomerId());
    }

    /** Suggests the next free ID, e.g. C1004 when C1003 is the highest. Uses the Stream API. */
    public String suggestNextCustomerId() throws SQLException {
        int highest = customerRepository.findAll().stream()
                .map(Customer::getCustomerId)        // "C1003"
                .map(id -> id.substring(1))          // "1003"
                .mapToInt(Integer::parseInt)         // 1003
                .max()
                .orElse(1000);
        return "C" + (highest + 1);
    }

    /** Sorting with Comparable: Collections.sort uses Customer.compareTo (by ID). */
    public List<Customer> getCustomersSortedById(boolean descending) throws SQLException {
        List<Customer> customers = new ArrayList<>(customerRepository.findAll());
        Collections.sort(customers);
        if (descending) {
            Collections.reverse(customers);
        }
        return customers;
    }

    /** Sorting with any Comparator passed in (BY_NAME, BY_AGE, BY_EMAIL ...). */
    public List<Customer> getCustomersSorted(Comparator<Customer> comparator, boolean descending) throws SQLException {
        List<Customer> customers = new ArrayList<>(customerRepository.findAll());
        Collections.sort(customers, comparator);
        if (descending) {
            Collections.reverse(customers);
        }
        return customers;
    }

    /** Saves every valid customer read from a CSV file. Problems are added to the result. */
    public void importCustomers(ImportResult<Customer> result) throws SQLException {
        for (Customer customer : result.getValidRecords()) {
            try {
                registerCustomer(customer);
                result.recordImported();
            } catch (DuplicateCustomerException | IllegalArgumentException e) {   // multi-catch
                result.addError("Customer " + customer.getCustomerId() + " skipped: " + e.getMessage());
            }
        }
    }
}
