package com.firstMcp.Employee.employee;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Employee {

    @Id
    private int id;
    private String name;
    private String department;
    private double salary;
    private String role;

    public Employee() {
    }
    public Employee(int id, String name, String department, double salary, String role) {
        this.id = id;
        this.name = name;
        this.department = department;
        this.salary = salary;
        this.role = role;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDepartment() {
        return department;
    }

    public double getSalary() {
        return salary;
    }

    public void setSalary(double salary) {
        this.salary = salary;
    }

    public String getRole() {
        return role;
    }
}