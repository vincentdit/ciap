package tz.go.tirdo.ciap.domain.department.dto;

import tz.go.tirdo.ciap.domain.department.DepartmentMember;
import tz.go.tirdo.ciap.domain.user.Role;

public record DepartmentMemberResponse(
        Long userId,
        String fullName,
        String email,
        String role,
        boolean canReceiveWork,
        boolean active
) {
    public static DepartmentMemberResponse from(DepartmentMember member) {
        String role = member.getUser().getRoles().stream()
                .map(Role::getName)
                .map(Enum::name)
                .findFirst()
                .orElse("");
        return new DepartmentMemberResponse(
                member.getUser().getId(),
                member.getUser().getFullName(),
                member.getUser().getEmail(),
                role,
                member.isCanReceiveWork(),
                member.isActive());
    }
}
