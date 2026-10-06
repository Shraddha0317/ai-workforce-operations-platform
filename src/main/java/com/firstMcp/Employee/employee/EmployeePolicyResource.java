package com.firstMcp.Employee.employee;



import org.springframework.ai.mcp.annotation.McpResource;
import org.springframework.stereotype.Component;

@Component
public class EmployeePolicyResource {

    @McpResource(
            uri = "employee-policy://general",
            name = "Employee Policy",
            description = "Provides the company's general employee policy"
    )
    public String getEmployeePolicy() {
        return """
                Employee Policy

                1. Employees must maintain professional conduct.
                2. Working hours are 9 AM to 6 PM.
                3. Employees are entitled to paid leave according to company policy.
                4. Salary changes require HR approval.
                """;
    }
}