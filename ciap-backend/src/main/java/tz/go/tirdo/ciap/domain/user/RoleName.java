package tz.go.tirdo.ciap.domain.user;

/**
 * System roles per CIAP blueprint Module 1 (Authentication & Access Control).
 */
public enum RoleName {
    ADMIN,
    MANAGER,
    /** Head of one department. This role does not see other departments. */
    HOD,
    EXPERT,
    FINANCE,
    CLIENT
}
