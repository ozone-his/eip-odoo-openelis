package org.ozonehis.eip.odoo.openelis.task;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.ozonehis.eip.odoo.openelis.Constants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;

import static java.time.ZoneId.systemDefault;
import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;

@Slf4j
@Component
public class TimeStampStore {

    @Value("${" + Constants.PROP_TASK_LAST_RUN_TS_FILE + "}")
    private String filename;

    private File file;

    private LocalDateTime timestamp;

    public LocalDateTime getLastRunDatetime() {
        if (file == null) {
            file = getFile();
        }

        try {
            log.info("Loading saved timestamp");
            String datetime = FileUtils.readFileToString(file, StandardCharsets.UTF_8);

            if (StringUtils.isBlank(datetime)) {
                log.info("No saved timestamp found");
            } else {
                timestamp = ZonedDateTime.parse(datetime, ISO_OFFSET_DATE_TIME).withZoneSameInstant(systemDefault()).toLocalDateTime();
                log.info("Done loading saved timestamp");
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read timestamp file", e);
        }

        return timestamp;
    }

    public void save(LocalDateTime timestamp) {
        File f = getFile();
        if (log.isDebugEnabled()) {
            log.debug("Saving timestamp");
        }

        try {
            FileUtils.writeStringToFile(f, timestamp.toString(), StandardCharsets.UTF_8);
            if (log.isDebugEnabled()) {
                log.debug("Successfully saved timestamp");
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to save timestamp", e);
        }

        this.timestamp = timestamp;
    }

    private File getFile() {
        if (file == null) {
            file = new File(filename);
        }

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
