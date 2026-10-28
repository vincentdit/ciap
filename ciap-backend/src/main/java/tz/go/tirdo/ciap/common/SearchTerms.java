package tz.go.tirdo.ciap.common;

/** Turns a typed search into a safe SQL LIKE pattern. Blank input matches everything. */
public final class SearchTerms {

    private SearchTerms() {
    }

    public static String like(String q) {
        if (q == null || q.isBlank()) {
            return null;
        }
        String cleaned = q.trim().toLowerCase().replace("%", "").replace("_", "");
        return cleaned.isEmpty() ? null : "%" + cleaned + "%";
    }
}
