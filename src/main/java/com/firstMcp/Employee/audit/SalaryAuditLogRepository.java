package com.firstMcp.Employee.audit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SalaryAuditLogRepository extends JpaRepository<SalaryAuditLog, Integer> {

    List<SalaryAuditLog> findByEmployeeIdOrderByChangedAtDesc(Integer employeeId);
}
