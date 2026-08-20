package tz.go.tirdo.ciap.domain.billing;

public enum QuotationStatus {
    DRAFT,      // created, editable
    SENT,       // issued to client
    ACCEPTED,   // client accepted -> contract generated
    REJECTED,
    EXPIRED
}
