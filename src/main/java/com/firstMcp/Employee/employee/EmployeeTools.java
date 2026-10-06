package com.firstMcp.Employee.employee;


import com.firstMcp.Employee.audit.SalaryAuditLog;
import com.firstMcp.Employee.audit.SalaryAuditLogRepository;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class EmployeeTools {

    private final EmployeeService employeeService;
    private final SalaryAuditLogRepository salaryAuditLogRepository;

    public EmployeeTools(EmployeeService employeeService,
                         SalaryAuditLogRepository salaryAuditLogRepository) {
        this.employeeService          = employeeService;
        this.salaryAuditLogRepository = salaryAuditLogRepository;
    }

    @McpTool(description = "Get the name of an employee by their ID")
    public String GetEmployeName(@McpToolParam(description = "Employee ID, must be greater than 0") int Empid) {
        return employeeService.getEmployeeById(Empid).getName();
    }

    @McpTool(description = "Get the department of an employee by their ID")
    public String getEmployeeDepartment(@McpToolParam(description = "Employee ID, must be greater than 0") int empId) {
        return employeeService.getEmployeeById(empId).getDepartment();
    }

    @McpTool(description = "Get the salary of an employee by their ID")
    public String getEmployeeSalary(@McpToolParam(description = "Employee ID, must be greater than 0") int empId) {
        return String.valueOf(employeeService.getEmployeeById(empId).getSalary());
    }

    @McpTool(description = "Get the job role of an employee by their ID")
    public String getEmployeeRole(@McpToolParam(description = "Employee ID, must be greater than 0") int empId) {
        return employeeService.getEmployeeById(empId).getRole();
    }

    @McpTool(description = "Get complete details of an employee including name, department, salary, and role")
    public String getEmployeeDetails(@McpToolParam(description = "Employee ID, must be greater than 0") int empId) {
        Employee e = employeeService.getEmployeeById(empId);
        return "Name: " + e.getName()
                + ", Department: " + e.getDepartment()
                + ", Salary: " + e.getSalary()
                + ", Role: " + e.getRole();
    }

    @McpTool(description = "find the employee with the highest salary and return their complete details")
    public String findHighestPaidEmployee() {

        Employee employee = employeeService.getHighestPaidEmployee();

        return "Name: " + employee.getName()
                + ", Department: " + employee.getDepartment()
                + ", Salary: " + employee.getSalary()
                + ", Role: " + employee.getRole();
    }



    @McpTool(description = "search employees by department, role, minimum salary and maximum salary")
    public String searchEmployees(
            @McpToolParam(description = "Department to filter by, or empty for any department") String department,
            @McpToolParam(description = "Role to filter by, or empty for any role") String role,
            @McpToolParam(description = "Minimum salary, or 0 for no minimum") double minSalary,
            @McpToolParam(description = "Maximum salary, or 0 for no maximum") double maxSalary) {

        List<Employee> employees = employeeService.searchEmployees(
                department, role, minSalary, maxSalary);

        if (employees.isEmpty()) {
            return "No employees found";
        }

        StringBuilder result = new StringBuilder();

        for (Employee employee : employees) {
            result.append("ID: ").append(employee.getId())
                    .append(", Name: ").append(employee.getName())
                    .append(", Department: ").append(employee.getDepartment())
                    .append(", Role: ").append(employee.getRole())
                    .append(", Salary: ").append(employee.getSalary())
                    .append("\n");
        }

        return result.toString();
    }

    @McpTool(description = "Get a summary of all departments showing headcount and average salary")
    public String getDepartmentSummary() {
        Map<String, String> summary = employeeService.getDepartmentSummary();
        StringBuilder result = new StringBuilder();
        for (Map.Entry<String, String> entry : summary.entrySet()) {
            result.append("Department: ").append(entry.getKey())
                    .append(" — ").append(entry.getValue())
                    .append("\n");
        }
        return result.toString();
    }

    @McpTool(description = "Get the salary change history for an employee, most recent first")
    public String getSalaryHistory(
            @McpToolParam(description = "Employee ID, must be greater than 0") int employeeId) {

        List<SalaryAuditLog> logs =
                salaryAuditLogRepository.findByEmployeeIdOrderByChangedAtDesc(employeeId);

        if (logs.isEmpty()) {
            return "No salary history found for employee ID " + employeeId;
        }

        StringBuilder result = new StringBuilder();
        for (SalaryAuditLog entry : logs) {
            result.append("Changed at: ").append(entry.getChangedAt())
                    .append(", Old salary: ").append(entry.getOldSalary())
                    .append(", New salary: ").append(entry.getNewSalary())
                    .append("\n");
        }
        return result.toString();
    }
    @McpTool(description = "Update an employee's salary. Salary must be greater than 0 and cannot exceed 1 crore.")
    public String updateEmployeeSalary(
            @McpToolParam(description = "Employee ID, must be greater than 0") int employeeId,
            @McpToolParam(description = "New salary amount") double newSalary) {

        return employeeService.updateEmployeeSalary(employeeId, newSalary);
    }

    @McpTool(
            description = "Send an employee's details to the HR system. Employee ID must be greater than 0."
    )
    public String sendEmployeeToHR(
            @McpToolParam(description = "Employee Id") int employeeId) {

        return employeeService.sendEmployeeToHR(employeeId);
    }

    @McpTool(description = "Send all employees from a specific department to the HR system in a single operation")
    public String bulkSendToHR(
            @McpToolParam(description = "Department name to send all employees to HR") String department) {
        return employeeService.bulkSendToHR(department);
    }
}
