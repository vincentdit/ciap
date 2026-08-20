package tz.go.tirdo.ciap.domain.document;

/**
 * Classifies a stored document by its role in the consultancy lifecycle.
 */
public enum DocumentCategory {
    REQUIREMENT,    // client-supplied scope / requirements
    QUOTATION,      // generated or signed quotation
    CONTRACT,       // signed contract
    REPORT,         // deliverable / consultancy report
    INVOICE,        // billing document
    CORRESPONDENCE, // letters, emails, memos
    OTHER
}
