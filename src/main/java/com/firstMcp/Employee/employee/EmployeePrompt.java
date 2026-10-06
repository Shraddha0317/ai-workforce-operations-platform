package com.firstMcp.Employee.employee;




import java.util.List;

import io.modelcontextprotocol.spec.McpSchema.GetPromptResult;
import io.modelcontextprotocol.spec.McpSchema.PromptMessage;
import io.modelcontextprotocol.spec.McpSchema.Role;
import io.modelcontextprotocol.spec.McpSchema.TextContent;

import org.springframework.ai.mcp.annotation.McpArg;
import org.springframework.ai.mcp.annotation.McpPrompt;
import org.springframework.stereotype.Component;

@Component
public class EmployeePrompt {

    @McpPrompt(
            name = "employee-performance-review",
            description = "Creates a performance review prompt for an employee"
    )
    public GetPromptResult employeePerformanceReview(
            @McpArg(
                    name = "employeeId",
                    description = "Employee Id",
                    required = true
            ) String employeeId) {

        String promptText = """
                Create a professional performance review for employee ID %s.

                Include:
                - Employee details
                - Current role
                - Department
                - Current salary
                - Overall performance summary
                """.formatted(employeeId);

        return new GetPromptResult(
                "Employee Performance Review",
                List.of(
                        new PromptMessage(
                                Role.USER,
                                new TextContent(promptText)
                        )
                )
        );
    }
}