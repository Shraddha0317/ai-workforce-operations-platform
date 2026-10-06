package com.firstMcp.Employee.employee;


import com.firstMcp.Employee.audit.SalaryAuditLog;
import com.firstMcp.Employee.audit.SalaryAuditLogRepository;
import com.firstMcp.Employee.exception.EmployeeNotFoundException;
import com.firstMcp.Employee.exception.InvalidSalaryException;
import com.firstMcp.Employee.hr.HRRecord;
import com.firstMcp.Employee.hr.HRRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class EmployeeService {

    private static final Logger log = LoggerFactory.getLogger(EmployeeService.class);

    private final EmployeeRepository employeeRepository;
    private final HRRecordRepository hrRecordRepository;
    private final SalaryAuditLogRepository salaryAuditLogRepository;

    public EmployeeService(EmployeeRepository employeeRepository,
                           HRRecordRepository hrRecordRepository,
                           SalaryAuditLogRepository salaryAuditLogRepository) {
        this.employeeRepository     = employeeRepository;
        this.hrRecordRepository     = hrRecordRepository;
        this.salaryAuditLogRepository = salaryAuditLogRepository;
    }


    @Transactional(readOnly = true)
    public Employee getEmployeeById(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException(
                    "Employee ID must be greater than 0."
            );
        }


        return employeeRepository.findById(id)
                .orElseThrow(() ->
                        new EmployeeNotFoundException(
                                "Employee with ID " + id + " not found."
                        )
                );
    }


    @Transactional(readOnly = true)
    public Employee getHighestPaidEmployee() {
        return employeeRepository.findTopByOrderBySalaryDesc()
                .orElseThrow(() -> new EmployeeNotFoundException("No employees found."));
    }




    @Transactional(readOnly = true)
    public List<Employee> searchEmployees(
            String department,
            String role,
            double minSalary,
            double maxSalary) {

        List<Employee> employees;

        if (minSalary < 0 || maxSalary < 0) {
            throw new IllegalArgumentException("Salary values cannot be negative.");
        }

        if (maxSalary > 0 && minSalary > maxSalary) {
            throw new IllegalArgumentException("Minimum salary cannot be greater than maximum salary.");
        }

        boolean hasDepartment = department != null && !department.isBlank();

        if (hasDepartment && minSalary > 0) {
            employees = employeeRepository.findByDepartmentAndSalaryGreaterThan(department, minSalary);
        } else if (hasDepartment) {
            employees = employeeRepository.findByDepartment(department);
        } else {
            employees = employeeRepository.findAll();
        }

        return employees.stream()
                .filter(e -> !hasDepartment || e.getDepartment().equalsIgnoreCase(department))
                .filter(e -> role == null || role.isBlank() || e.getRole().equalsIgnoreCase(role))
                .filter(e -> minSalary <= 0 || e.getSalary() >= minSalary)
                .filter(e -> maxSalary <= 0 || e.getSalary() <= maxSalary)
                .toList();

    }

    @Transactional(readOnly = true)
    public Map<String, String> getDepartmentSummary() {
        List<Employee> all = employeeRepository.findAll();

        if (all.isEmpty()) {
            throw new EmployeeNotFoundException("No employees found.");
        }

        Map<String, List<Employee>> byDepartment = new LinkedHashMap<>();
        for (Employee e : all) {
            byDepartment.computeIfAbsent(e.getDepartment(), k -> new java.util.ArrayList<>()).add(e);
        }

        Map<String, String> summary = new LinkedHashMap<>();
        for (Map.Entry<String, List<Employee>> entry : byDepartment.entrySet()) {
            List<Employee> dept = entry.getValue();
            double avg = dept.stream().mapToDouble(Employee::getSalary).average().orElse(0);
            summary.put(entry.getKey(),
                    "Headcount: " + dept.size() + ", Average Salary: " + String.format("%.2f", avg));
        }

        return summary;
    }

    @Transactional
    public String updateEmployeeSalary(int employeeId, double newSalary) {

        if (newSalary <= 0 || newSalary > 10_000_000) {
            throw new InvalidSalaryException("Salary must be between 1 and 1,00,00,000.");
        }

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new EmployeeNotFoundException(
                        "Employee with ID " + employeeId + " not found."));

        double oldSalary = employee.getSalary();
        employee.setSalary(newSalary);
        employeeRepository.save(employee);

        salaryAuditLogRepository.save(
                new SalaryAuditLog(employee.getId(), employee.getName(), oldSalary, newSalary));

        log.info("[SALARY UPDATE] employeeId={}, name={}, oldSalary={}, newSalary={}",
                employeeId, employee.getName(), oldSalary, newSalary);

        return "Salary of " + employee.getName() + " updated successfully to " + newSalary;
    }


    @Transactional
    public String sendEmployeeToHR(int employeeId) {

        if (employeeId <= 0) {
            throw new IllegalArgumentException("Employee ID must be greater than 0.");
        }

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new EmployeeNotFoundException(
                        "Employee with ID " + employeeId + " not found."));

        hrRecordRepository.save(new HRRecord(
                employee.getId(),
                employee.getName(),
                employee.getDepartment(),
                employee.getRole(),
                employee.getSalary()
        ));

        log.info("[HR TRANSFER] employeeId={}, name={}, department={}, role={}, salary={}",
                employee.getId(), employee.getName(),
                employee.getDepartment(), employee.getRole(), employee.getSalary());

        return "Employee " + employee.getName() + " successfully sent to HR.";
    }

    @Transactional
    public String bulkSendToHR(String department) {
        if (department == null || department.isBlank()) {
            throw new IllegalArgumentException("Department must not be empty.");
        }

        List<Employee> employees = employeeRepository.findByDepartment(department);

        if (employees.isEmpty()) {
            throw new EmployeeNotFoundException("No employees found in department: " + department);
        }

        List<HRRecord> records = employees.stream()
                .map(e -> new HRRecord(e.getId(), e.getName(), e.getDepartment(), e.getRole(), e.getSalary()))
                .toList();

        hrRecordRepository.saveAll(records);

        log.info("[BULK HR TRANSFER] department={}, count={}", department, employees.size());

        return "Successfully sent " + employees.size() + " employees from " + department + " department to HR.";
    }
}

