package com.firstMcp.Employee.hr;



import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "hr_employee_records")
public class HRRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Integer employeeId;
    private String name;
    private String department;
    private String role;
    private double salary;
    private LocalDateTime sentAt;

    public HRRecord() {
    }

    public HRRecord(Integer employeeId, String name, String department,
                    String role, double salary) {
        this.employeeId = employeeId;
        this.name = name;
        this.department = department;
        this.role = role;
        this.salary = salary;
        this.sentAt = LocalDateTime.now();
    }

    public Integer getId()        { return id; }
    public Integer getEmployeeId() { return employeeId; }
    public String getName()        { return name; }
    public String getDepartment()  { return department; }
    public String getRole()        { return role; }
    public double getSalary()      { return salary; }
    public LocalDateTime getSentAt() { return sentAt; }
}