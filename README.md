# Employee Management MCP Server

This is an AI-powered employee management system where you talk to Claude Desktop in plain English and it handles everything — finding employees, updating salaries, sending records to HR, checking audit history. No UI, no SQL, just natural language.

Built with Java 17, Spring Boot, Spring AI MCP, JPA/Hibernate, and MySQL.

---

## What makes this different from a normal CRUD app

Most projects expose a REST API and the client has to know exactly which endpoint to call. This project uses the **Model Context Protocol (MCP)** instead. The backend exposes a set of *tools* and Claude decides which ones to use based on what you ask. So when you say *"find the highest-paid IT employee and send them to HR"*, Claude figures out on its own that it needs to search first, then call the HR transfer tool — you never tell it the steps.

All the business rules (salary limits, ID validation, audit logging) live in the backend. Claude cannot bypass them.

---

## How it works

```
You type a message in Claude Desktop
        │
        ▼
Claude reads your intent and picks the right tool(s)
        │
        ▼  MCP over STDIO (JSON-RPC)
        │
        ▼
Spring Boot MCP Server receives the tool call
        │
        ├── EmployeeTools        ← tool definitions Claude can call
        ├── EmployeePolicyResource  ← policy doc Claude can read
        └── EmployeePrompt       ← prompt template for reviews
        │
        ▼
EmployeeService  (all validation and business logic lives here)
        │
        ▼
JPA / Hibernate
        │
        ▼
MySQL
        ├── employee               ← core employee records
        ├── hr_employee_records    ← every HR transfer, timestamped
        └── salary_audit_log       ← every salary change, old + new value
```

The STDIO transport means the Spring Boot jar runs as a subprocess of Claude Desktop. There's no HTTP server, no port to expose — Claude just spawns the jar and talks to it over stdin/stdout.

---

## Tools Claude can use

| Tool | What it does |
|---|---|
| `getEmployeeDetails` | Full details for an employee by ID |
| `getEmployeeName` | Just the name |
| `getEmployeeDepartment` | Just the department |
| `getEmployeeSalary` | Just the salary |
| `getEmployeeRole` | Just the role |
| `findHighestPaidEmployee` | Finds the top earner (single DB query, no in-memory sort) |
| `searchEmployees` | Filter by department, role, salary range — any combination |
| `updateEmployeeSalary` | Updates salary, writes to audit log, fully transactional |
| `sendEmployeeToHR` | Sends one employee's record to HR with a timestamp |
| `bulkSendToHR` | Sends an entire department to HR in one transaction |
| `getSalaryHistory` | Full salary change history for an employee, newest first |
| `getDepartmentSummary` | Headcount and average salary per department |

Claude also has access to:
- `employee-policy://general` — a policy document it reads when you ask policy questions
- `employee-performance-review` — a prompt template it uses when you ask for a performance review

---

## Real examples of what Claude does

**"Find the highest-paid employee in IT and send them to HR"**
1. Calls `searchEmployees(department="IT")`
2. Identifies the top salary from results
3. Calls `sendEmployeeToHR(employeeId=...)`

**"Send all Finance employees to HR"**
1. Calls `bulkSendToHR(department="Finance")`
2. Done — one tool call, one transaction, all records saved

**"Has employee 3's salary changed recently?"**
1. Calls `getSalaryHistory(employeeId=3)`
2. Returns every change with old value, new value, and when it happened

**"What does company policy say about salary approvals?"**
1. Reads the `employee-policy://general` resource
2. Answers directly — no tool call needed

**"Generate a performance review for employee 5"**
1. Invokes the `employee-performance-review` prompt template
2. Calls `getEmployeeDetails(employeeId=5)`
3. Writes a structured review combining both

---

## How to run it

### Option 1 — Local (you need MySQL on port 3306)

```bash
# create the database
mysql -u root -p -e "CREATE DATABASE employeeMcp_db;"

# build
./mvnw package -DskipTests

# run
java -jar target/Employee-0.0.1-SNAPSHOT.jar
```

### Option 2 — Docker (recommended, no local MySQL needed)

```bash
docker-compose up --build
```

MySQL starts on port 3307 on your machine. The app waits for MySQL to be healthy before it starts.

---

## Connecting to Claude Desktop

Once the jar is built, open your Claude Desktop config file:

- **Mac:** `~/Library/Application Support/Claude/claude_desktop_config.json`
- **Windows:** `%APPDATA%\Claude\claude_desktop_config.json`

Add this (replace the path with your actual jar location):

```json
{
  "mcpServers": {
    "employee-server": {
      "command": "java",
      "args": ["-jar", "/absolute/path/to/Employee-0.0.1-SNAPSHOT.jar"]
    }
  }
}
```

Restart Claude Desktop. You should see a hammer icon (🔨) in the chat input — that means Claude has loaded the tools and is ready. Just start typing.

If you're on Windows, use forward slashes or escape backslashes in the path:
```json
"args": ["-jar", "C:/Users/YourName/path/to/Employee-0.0.1-SNAPSHOT.jar"]
```

---

## Things to try once it's connected

```
"Get the complete details of employee 3."
"Who is the highest-paid employee?"
"Show me all employees in the Finance department."
"Find all engineers earning between 60,000 and 90,000."
"Update the salary of employee 2 to 95,000."
"Send employee 4's details to HR."
"Send all IT employees to HR."
"Show me the salary history for employee 3."
"Find the highest-paid IT employee and send them to HR."
"What does company policy say about salary changes?"
"Generate a performance review for employee 1."
"Give me a headcount and average salary breakdown by department."
```

---

## Database schema

```sql
employee (id, name, department, salary, role)

hr_employee_records (id, employee_id, name, department, role, salary, sent_at)

salary_audit_log (id, employee_id, employee_name, old_salary, new_salary, changed_at)
```

---

## Engineering decisions worth noting

- `updateEmployeeSalary` and `bulkSendToHR` are `@Transactional` — if the audit log write fails, the salary update rolls back too. No partial state.
- All read methods use `@Transactional(readOnly = true)` — tells the DB it can optimise for reads.
- `findHighestPaidEmployee` uses a derived JPA query (`findTopByOrderBySalaryDesc`) — one DB query, not a full table scan in memory.
- `HRRecord` and `SalaryAuditLog` have no setters — once written, they can't be changed. Audit trail is immutable.
- Salary cap of 1 crore is enforced in `EmployeeService`, not in the tool layer. Claude cannot talk the backend into skipping it.
- DB credentials are environment variables with local defaults — no secrets hardcoded for production.

---

## Running tests

```bash
./mvnw test
```

16 unit tests covering all service-layer logic. No database needed — repositories are mocked with Mockito.

---

## Project structure

```
src/main/java/com/firstMcp/Employee/
├── employee/
│   ├── Employee.java                  JPA entity
│   ├── EmployeeRepository.java        Spring Data repository
│   ├── EmployeeService.java           Business logic and validation
│   ├── EmployeeTools.java             MCP tool definitions
│   ├── EmployeePolicyResource.java    MCP resource
│   └── EmployeePrompt.java            MCP prompt template
├── audit/
│   ├── SalaryAuditLog.java
│   └── SalaryAuditLogRepository.java
├── hr/
│   ├── HRRecord.java
│   └── HRRecordRepository.java
└── exception/
    ├── EmployeeNotFoundException.java
    └── InvalidSalaryException.java
```

---

## Tech stack

| | |
|---|---|
| Java 17 | Core language |
| Spring Boot | Application framework |
| Spring AI MCP Server | MCP protocol implementation |
| Spring Data JPA / Hibernate | ORM |
| MySQL 8 | Database |
| JUnit 5 + Mockito | Unit testing |
| Docker + Docker Compose | Containerisation |
| SLF4J | Structured audit logging |
