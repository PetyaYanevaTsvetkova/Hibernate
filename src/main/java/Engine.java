import entities.Address;
import entities.Employee;
import entities.Town;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class Engine implements Runnable {
    private final EntityManager entityManager;
    private BufferedReader bufferedReader;

    public Engine(EntityManager entityManager) {
        this.entityManager = entityManager;
        this.bufferedReader = new BufferedReader(new InputStreamReader(System.in));
    }

    @Override
    public void run() {
        System.out.println("Select ex number:");
        try {
            int exNum = Integer.parseInt(bufferedReader.readLine());

            switch (exNum) {
                case 2:
                    townsToUpperCase();
                case 3:
                    containsEmployee();
                case 4:
                    employeesWithSalaryOver50000();
                case 5:
                    employeesFromDepartment();
                case 6:
                    addingNewAddressUpdatingEmployee();
                case 7:
                    addressesWithEmployeeCount();
                case 10:
                    increaseSalaries();
                case 12:
                    employeesMaximumSalaries();
                case 13:
                    removeTowns();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            entityManager.close();
        }
    }

    private void removeTowns() throws IOException {
        System.out.println("Enter town name:");
        String townName = bufferedReader.readLine();

        Town town = entityManager.createQuery("SELECT t FROM Town t WHERE t.name = :t_name", Town.class)
                .setParameter("t_name", townName)
                .getSingleResult();

        int affectedRows = removeAddressesByTownId(town.getId());

        entityManager.getTransaction().begin();
        entityManager.remove(town);
        entityManager.getTransaction().commit();

        System.out.printf("%d address in %s is deleted%n", affectedRows, townName);
    }


    private int removeAddressesByTownId(Integer id) {
        List<Address> addresses = entityManager.createQuery
                        ("SELECT a FROM Address a WHERE a.town.id = :p_id", Address.class)
                .setParameter("p_id", id)
                .getResultList();

        entityManager.getTransaction().begin();
        addresses.forEach(entityManager::remove);
        entityManager.getTransaction().commit();

        return addresses.size();
    }

    @SuppressWarnings("unchecked")
    private void employeesMaximumSalaries() {
        List<Objects[]> rows = entityManager
                .createNativeQuery("SELECT  d.name, MAX(e.salary) AS 'm_salary'\n" +
                        "FROM departments d\n" +
                        "join employees e on d.department_id = e.department_id\n" +
                        "GROUP BY d.name\n" +
                        "HAVING m_salary NOT BETWEEN 30000 AND 70000;")
                .getResultList();
    }

    private void increaseSalaries() {
        entityManager.getTransaction().begin();
        int effectedRows = entityManager.createQuery
                        ("UPDATE Employee e SET e.salary = e.salary * 1.2 WHERE e.department.id IN :ids")
                .setParameter("ids", Set.of(1, 2, 4, 11))
                .executeUpdate();
        entityManager.getTransaction().commit();
        System.out.println(effectedRows);
    }

    private void addressesWithEmployeeCount() {
        List<Address> addresses = entityManager.createQuery
                        ("SELECT a FROM Address a ORDER BY a.employees.size DESC", Address.class)
                .setMaxResults(10)
                .getResultList();

        addresses
                .forEach(address -> {
                    System.out.printf("%s , %s - %d employees%n",
                            address.getText(),
                            address.getTown() == null ? "Unknown" : address.getTown().getName(),
                            address.getEmployees().size());
                });
    }

    private void addingNewAddressUpdatingEmployee() throws IOException {
        System.out.printf("Enter employee last name:");
        String lastName = bufferedReader.readLine();

        Employee employee = entityManager.createQuery
                        ("SELECT e FROM Employee e WHERE e.lastName = :l_name", Employee.class)
                .setParameter("l_name", lastName)
                .getSingleResult();

        Address address = createAddress("Vitoshka 15");

        entityManager.getTransaction().begin();
        employee.setAddress(address);
        entityManager.getTransaction().commit();
    }

    private Address createAddress(String s) {

        Address address = new Address();
        address.setText(s);

        entityManager.getTransaction().begin();
        entityManager.persist(address);
        entityManager.getTransaction().commit();
        return address;
    }

    private void employeesFromDepartment() {
        String department = "Research and Development";
        entityManager.createQuery
                        ("SELECT e FROM Employee e WHERE e.department.name = :department_name " +
                                "ORDER BY e.salary, e.id", Employee.class)
                .setParameter("department_name", department)
                .getResultList()
                .forEach(e -> {
                    System.out.printf("%s %s from %s - $%.2f%n",
                            e.getFirstName(),
                            e.getLastName(),
                            e.getDepartment().getName(),
                            e.getSalary());
                });
    }

    private void employeesWithSalaryOver50000() {
        entityManager.createQuery
                        ("SELECT e FROM Employee e WHERE e.salary > :min_salary", Employee.class)
                .setParameter("min_salary", BigDecimal.valueOf(50000L))
                .getResultStream()
                .map(Employee::getFirstName)
                .forEach(System.out::println);
    }

    private void containsEmployee() throws IOException {
        System.out.println("Enter employee full name:");
        String[] fullName = bufferedReader.readLine().split("\\s+");
        String firstName = fullName[0];
        String lastName = fullName[1];

        Long singleResult = entityManager.createQuery
                        ("SELECT count(e) FROM Employee e " +
                                "WHERE e.firstName=:f_name AND e.lastName= :l_name", Long.class)
                .setParameter("f_name", firstName)
                .setParameter("l_name", lastName)
                .getSingleResult();

        System.out.println(singleResult == 0 ? "No" : "Yes");
    }

    private void townsToUpperCase() {
        entityManager.getTransaction().begin();

        Query query = entityManager.createQuery
                ("UPDATE Town t SET t.name = upper(t.name) WHERE length(t.name) <= 5 ");
        System.out.println(query.executeUpdate());

        entityManager.getTransaction().commit();
    }

}
