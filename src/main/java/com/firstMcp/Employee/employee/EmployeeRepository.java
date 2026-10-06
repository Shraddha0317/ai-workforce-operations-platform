package com.firstMcp.Employee.employee;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;


public interface EmployeeRepository extends JpaRepository<Employee, Integer> {

    List<Employee> findByDepartment(String department);

    List<Employee> findByDepartmentAndSalaryGreaterThan(
            String department, double salary);

    Optional<Employee> findTopByOrderBySalaryDesc();
}