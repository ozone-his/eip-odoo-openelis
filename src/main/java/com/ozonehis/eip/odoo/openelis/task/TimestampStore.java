package com.ozonehis.eip.odoo.openelis.task;

import com.ozonehis.eip.odoo.openelis.Constants;
import com.ozonehis.eip.odoo.openelis.DateUtils;
import com.ozonehis.eip.odoo.openelis.EipFileUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.DomainResource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Properties;

@Slf4j
@Component
public class TimestampStore {

    @Value("${" + Constants.PROP_TASK_LAST_RUN_TS_FILE + "}")
    private String filename;

    private File file;

    private Properties props;

    public LocalDateTime getTimestamp(Class<? extends DomainResource> resourceType) {
        final String ts = getProps().getProperty(resourceType.getSimpleName());
        if (StringUtils.isBlank(ts)) {
            if (log.isDebugEnabled()) {
                log.debug("No saved timestamp found for {} resource", resourceType.getSimpleName());
            }

            return null;
        }

        return DateUtils.deserialize(ts);
    }

    public synchronized void update(LocalDateTime timestamp, Class<? extends DomainResource> resourceType) {
        final String resource = resourceType.getSimpleName();
        Properties propsTemp = new Properties();
        propsTemp.putAll(getProps());
        final String newTimestamp = DateUtils.serialize(timestamp);
        propsTemp.put(resourceType.getSimpleName(), newTimestamp);
        if (log.isDebugEnabled()) {
            log.debug("Updating timestamps for {} resource to {}", resource, newTimestamp);
        }

        try {
            propsTemp.store(EipFileUtils.openOutputStream(getFile()), null);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save timestamps for " + resource + " resource ", e);
        }

        getProps().put(resourceType.getSimpleName(), DateUtils.serialize(timestamp));
        if (log.isDebugEnabled()) {
            log.debug("Successfully saved timestamps for {} resource", resource);
        }
    }

    private Properties getProps() {
        if (props == null) {
            synchronized (this) {
                if (props == null) {
                    props = new Properties();

                    try {
                        File file = getFile();
                        log.info("Loading timestamps from {}", file);
                        props.load(EipFileUtils.openInputStream(file));
                        log.info("Successfully loaded timestamps");
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to load timestamps", e);
                    }
                }
            }
        }

        return props;
    }

    protected File getFile() {
        if (file == null) {
            synchronized (this) {
                if (file == null) {
                    File fileTemp = new File(filename);
                    if (!fileTemp.exists()) {
                        if (!fileTemp.getParentFile().exists()) {
                            log.info("Creating timestamp directory");
                            if (fileTemp.getParentFile().mkdirs()) {
                                log.info("Successfully created timestamp directory");
                            } else {
                                throw new RuntimeException("Failed to create timestamp directory");
                            }
                        }

                        log.info("Creating timestamp file");

                        try {
                            if (fileTemp.createNewFile()) {
                                log.info("Successfully created timestamp file");
                            } else {
                                log.info("Failed to create timestamp file because it already exists");
                            }
                        } catch (IOException e) {
                            throw new RuntimeException("Failed to create timestamp file", e);
                        }
                    }

                    file = fileTemp;
                }
            }
        }

        return file;
    }

}
