package tz.go.tirdo.ciap.domain.department;

/** How a request was attached to a department. */
public enum RoutingStatus {
    /** Service line maps to a department that has an active Head of Department. */
    ROUTED,
    /** No active service-line route. An administrator must choose the department. */
    UNMAPPED,
    /** The department is known, but it has no active Head of Department. */
    NO_HOD
}
