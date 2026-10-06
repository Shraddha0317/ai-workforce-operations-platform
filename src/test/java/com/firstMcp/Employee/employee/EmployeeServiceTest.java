package com.firstMcp.Employee.employee;

import com.firstMcp.Employee.audit.SalaryAuditLog;
import com.firstMcp.Employee.audit.SalaryAuditLogRepository;
import com.firstMcp.Employee.exception.EmployeeNotFoundException;
import com.firstMcp.Employee.exception.InvalidSalaryException;
import com.firstMcp.Employee.hr.HRRecord;
import com.firstMcp.Employee.hr.HRRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private HRRecordRepository hrRecordRepository;

    @Mock
    private SalaryAuditLogRepository salaryAuditLogRepository;

    @InjectMocks
    private EmployeeService employeeService;

    private Employee alice;

    @BeforeEach
    void setUp() {
        alice = new Employee(1, "Alice", "IT", 75000.0, "Engineer");
    }

    // --- getEmployeeById ---

    @Test
    void getEmployeeById_returnsEmployee_whenFound() {
        when(employeeRepository.findById(1)).thenReturn(Optional.of(alice));

        Employee result = employeeService.getEmployeeById(1);

        assertThat(result.getName()).isEqualTo("Alice");
    }

    @Test
    void getEmployeeById_throwsIllegalArgument_whenIdIsZero() {
        assertThatThrownBy(() -> employeeService.getEmployeeById(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("greater than 0");
    }

    @Test
    void getEmployeeById_throwsEmployeeNotFound_whenMissing() {
        when(employeeRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.getEmployeeById(99))
                .isInstanceOf(EmployeeNotFoundException.class)
                .hasMessageContaining("99");
    }

    // --- getDepartmentSummary ---

    @Test
    void getDepartmentSummary_returnsHeadcountAndAverage() {
        Employee bob = new Employee(2, "Bob", "IT", 85000.0, "Manager");
        when(employeeRepository.findAll()).thenReturn(List.of(alice, bob));

        var summary = employeeService.getDepartmentSummary();

        assertThat(summary).containsKey("IT");
        assertThat(summary.get("IT")).contains("Headcount: 2").contains("Average Salary: 80000.00");
    }

    @Test
    void getDepartmentSummary_throwsEmployeeNotFound_whenNoEmployees() {
        when(employeeRepository.findAll()).thenReturn(List.of());

        assertThatThrownBy(() -> employeeService.getDepartmentSummary())
                .isInstanceOf(EmployeeNotFoundException.class);
    }

    // --- getHighestPaidEmployee ---

    @Test
    void getHighestPaidEmployee_returnsHighestPaid() {
        Employee bob = new Employee(2, "Bob", "HR", 90000.0, "Manager");
        when(employeeRepository.findAll()).thenReturn(List.of(alice, bob));

        Employee result = employeeService.getHighestPaidEmployee();

        assertThat(result.getName()).isEqualTo("Bob");
    }

    @Test
    void getHighestPaidEmployee_throwsEmployeeNotFound_whenNoEmployees() {
        when(employeeRepository.findAll()).thenReturn(List.of());

        assertThatThrownBy(() -> employeeService.getHighestPaidEmployee())
                .isInstanceOf(EmployeeNotFoundException.class)
                .hasMessageContaining("No employees found");
    }

    // --- searchEmployees ---

    @Test
    void searchEmployees_returnsByDepartment() {
        when(employeeRepository.findByDepartment("IT")).thenReturn(List.of(alice));

        List<Employee> result = employeeService.searchEmployees("IT", "", 0, 0);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Alice");
    }

    @Test
    void searchEmployees_returnsAll_whenNoCriteriaGiven() {
        when(employeeRepository.findAll()).thenReturn(List.of(alice));

        List<Employee> result = employeeService.searchEmployees("", "", 0, 0);

        assertThat(result).hasSize(1);
    }

    @Test
    void searchEmployees_throwsIllegalArgument_whenMinExceedsMax() {
        assertThatThrownBy(() -> employeeService.searchEmployees("IT", "", 90000, 50000))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Minimum salary cannot be greater than maximum");
    }

    @Test
    void searchEmployees_throwsIllegalArgument_whenNegativeSalary() {
        assertThatThrownBy(() -> employeeService.searchEmployees("IT", "", -1, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be negative");
    }

    // --- updateEmployeeSalary ---

    @Test
    void updateEmployeeSalary_updatesAndSavesAuditLog() {
        when(employeeRepository.findById(1)).thenReturn(Optional.of(alice));

        String result = employeeService.updateEmployeeSalary(1, 85000.0);

        assertThat(result).contains("Alice").contains("85000.0");

        ArgumentCaptor<SalaryAuditLog> captor = ArgumentCaptor.forClass(SalaryAuditLog.class);
        verify(salaryAuditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getOldSalary()).isEqualTo(75000.0);
        assertThat(captor.getValue().getNewSalary()).isEqualTo(85000.0);
    }

    @Test
    void updateEmployeeSalary_throwsInvalidSalary_whenZero() {
        assertThatThrownBy(() -> employeeService.updateEmployeeSalary(1, 0))
                .isInstanceOf(InvalidSalaryException.class);
    }

    @Test
    void updateEmployeeSalary_throwsInvalidSalary_whenExceedsLimit() {
        assertThatThrownBy(() -> employeeService.updateEmployeeSalary(1, 10_000_001))
                .isInstanceOf(InvalidSalaryException.class);
    }

    @Test
    void updateEmployeeSalary_throwsEmployeeNotFound_whenMissing() {
        when(employeeRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.updateEmployeeSalary(99, 50000))
                .isInstanceOf(EmployeeNotFoundException.class)
                .hasMessageContaining("99");
    }

    // --- sendEmployeeToHR ---

    @Test
    void sendEmployeeToHR_savesHRRecord_andReturnsSuccess() {
        when(employeeRepository.findById(1)).thenReturn(Optional.of(alice));

        String result = employeeService.sendEmployeeToHR(1);

        assertThat(result).contains("Alice");

        ArgumentCaptor<HRRecord> captor = ArgumentCaptor.forClass(HRRecord.class);
        verify(hrRecordRepository).save(captor.capture());
        assertThat(captor.getValue().getEmployeeId()).isEqualTo(1);
        assertThat(captor.getValue().getSentAt()).isNotNull();
    }

    @Test
    void sendEmployeeToHR_throwsIllegalArgument_whenIdIsZero() {
        assertThatThrownBy(() -> employeeService.sendEmployeeToHR(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("greater than 0");
    }

    @Test
    void sendEmployeeToHR_throwsEmployeeNotFound_whenMissing() {
        when(employeeRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.sendEmployeeToHR(99))
                .isInstanceOf(EmployeeNotFoundException.class)
                .hasMessageContaining("99");
    }
}
