package tz.go.tirdo.ciap.domain.document;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Where document files are stored on disk. Configure via ciap.storage.root
 * (defaults to a "ciap-documents" directory under the working directory).
 */
@ConfigurationProperties(prefix = "ciap.storage")
public record StorageProperties(String root) {
    public StorageProperties {
        if (root == null || root.isBlank()) {
            root = "ciap-documents";
        }
    }
}
