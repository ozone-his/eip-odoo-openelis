package org.ozonehis.eip.odoo.openelis.task;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Resource;
import org.ozonehis.eip.odoo.openelis.Constants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Properties;

import static java.time.ZoneId.systemDefault;
import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;

@Slf4j
@Component
public class TimeStampStore {

    @Value("${" + Constants.PROP_TASK_LAST_RUN_TS_FILE + "}")
    private String filename;

    private LocalDateTime timestamp;

    private Properties props;

    public LocalDateTime getTimestamp(Class<Resource> resourceType) {
        final String ts = getProps().getProperty(resourceType.getSimpleName());
        if (StringUtils.isBlank(ts)) {
            if (log.isDebugEnabled()) {
                log.debug("No saved timestamp found for {} resource", resourceType.getSimpleName());
            }

            return null;
        }

        return ZonedDateTime.parse(ts, ISO_OFFSET_DATE_TIME).withZoneSameInstant(systemDefault()).toLocalDateTime();
    }

    public void update(LocalDateTime timestamp, Class<Resource> resourceType) {
        final String resource = resourceType.getSimpleName();
        Properties propsTemp = new Properties(getProps());
        final String newTimestamp = timestamp.format(ISO_OFFSET_DATE_TIME);
        propsTemp.put(resourceType.getSimpleName(), newTimestamp);
        if (log.isDebugEnabled()) {
            log.debug("Updating timestamps for {} resource to {}", resource, newTimestamp);
        }

        try {
            propsTemp.store(FileUtils.openOutputStream(getFile()), null);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save timestamps for " + resource + " resource ", e);
        }

        getProps().put(resourceType.getSimpleName(), timestamp.format(ISO_OFFSET_DATE_TIME));
        if (log.isDebugEnabled()) {
            log.debug("Successfully saved timestamps for {} resource", resource);
        }
    }

    private Properties getProps() {
        if (props == null) {
            props = new Properties();

            try {
                File file = getFile();
                log.info("Loading timestamps from {}", file);
                props.load(FileUtils.openInputStream(file));
                log.info("Successfully loaded timestamps");
            } catch (IOException e) {
                throw new RuntimeException("Failed to load timestamps", e);
            }
        }

        return props;
    }

    protected File getFile() {
        File file = new File(filename);
        if (!file.exists()) {
            if (file.getParentFile().exists()) {
                log.info("Creating timestamp directory");
                if (file.getParentFile().mkdirs()) {
                    log.info("Successfully created timestamp directory");
                } else {
                    throw new RuntimeException("Failed to create timestamp directory");
                }
            }

            log.info("Creating timestamp file");

            try {
                if (file.createNewFile()) {
                    log.info("Successfully created timestamp file");
                } else {
                    log.info("Failed to create timestamp file because it already exists");
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to create timestamp file", e);
            }
        }

        return file;
    }

}
