package tz.go.tirdo.ciap.domain.department.dto;

import tz.go.tirdo.ciap.domain.department.Department;

public record DepartmentResponse(
        Long id,
        String name,
        String code,
        boolean active,
        Long hodUserId,
        String hodName,
        String hodEmail
) {
    public static DepartmentResponse from(Department department) {
        var hod = department.getHodUser();
        return new DepartmentResponse(
                department.getId(),
                department.getName(),
                department.getCode(),
                department.isActive(),
                hod == null ? null : hod.getId(),
                hod == null ? null : hod.getFullName(),
                hod == null ? null : hod.getEmail());
    }
}
