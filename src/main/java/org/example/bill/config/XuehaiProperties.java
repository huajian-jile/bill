package org.example.bill.config;

import java.nio.file.Path;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "bill.xuehai")
public class XuehaiProperties {

    private String storageDir;

    public Path resolvedStorageDir() {
        return Path.of(storageDir);
    }
}
