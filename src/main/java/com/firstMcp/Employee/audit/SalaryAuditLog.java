package com.firstMcp.Employee.audit;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "salary_audit_log")
public class SalaryAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Integer employeeId;
    private String employeeName;
    private double oldSalary;
    private double newSalary;
    private LocalDateTime changedAt;

    public SalaryAuditLog() {}

    public SalaryAuditLog(Integer employeeId, String employeeName,
                          double oldSalary, double newSalary) {
        this.employeeId   = employeeId;
        this.employeeName = employeeName;
        this.oldSalary    = oldSalary;
        this.newSalary    = newSalary;
        this.changedAt    = LocalDateTime.now();
    }

    public Integer getId()              { return id; }
    public Integer getEmployeeId()      { return employeeId; }
    public String getEmployeeName()     { return employeeName; }
    public double getOldSalary()        { return oldSalary; }
    public double getNewSalary()        { return newSalary; }
    public LocalDateTime getChangedAt() { return changedAt; }
}
