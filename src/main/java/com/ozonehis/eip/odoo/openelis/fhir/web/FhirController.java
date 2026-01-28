package com.ozonehis.eip.odoo.openelis.fhir.web;

import com.jayway.jsonpath.JsonPath;
import com.ozonehis.eip.odoo.openelis.DateUtils;
import com.ozonehis.eip.odoo.openelis.SyncUtils;
import com.ozonehis.eip.odoo.openelis.fhir.OdooFhirClient;
import com.ozonehis.eip.odoo.openelis.task.SyncTask;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Controller
@RequestMapping("/fhir")
@Slf4j
public class FhirController {

    private final OdooFhirClient odooFhirClient;

    private SyncTask syncTask;

    @Autowired
    public FhirController(OdooFhirClient odooFhirClient, SyncTask syncTask) {
        this.odooFhirClient = odooFhirClient;
        this.syncTask = syncTask;
    }

    /**
     * Handles FHIR requests to create or update a resource by forwarding it to the FHIR Odoo service via the {@link OdooFhirClient}
     *
     * @param resourceType The resource type name
     * @param id           The id of the resource being updated.
     * @param body         The incoming FHIR resource payload.
     * @return ResponseEntity object
     */
    @PutMapping("{resourceType}/{id}")
    public ResponseEntity createOrUpdate(@PathVariable("resourceType") String resourceType, @PathVariable("id") String id, @RequestBody String body) {
        int status = 200;
        try {
            waitForSyncTask();
            status = odooFhirClient.update(resourceType, id, body);
            LocalDateTime lastUpdated = DateUtils.deserialize(JsonPath.read(body, "meta.lastUpdated"));
            SyncUtils.saveLastUpdated(resourceType, id, lastUpdated);
        } catch (Throwable e) {
            log.error("Failed to update resource {}/{}", resourceType, id, e);
            // Ignore failures otherwise OpenELIS will keep re-submitting it.
        }

        return ResponseEntity.status(status).build();
    }

    /**
     * Handles FHIR requests to delete a resource by forwarding it to the FHIR Odoo service via the {@link OdooFhirClient}
     *
     * @param resourceType The resource type name
     * @param id           The id of the resource being deleted.
     * @return ResponseEntity object
     */
    @DeleteMapping("{resourceType}/{id}")
    public ResponseEntity delete(@PathVariable String resourceType, @PathVariable String id) {
        try {
            waitForSyncTask();
            odooFhirClient.delete(resourceType, id);
            SyncUtils.saveLastUpdated(resourceType, id, null);
        } catch (Throwable e) {
            log.error("Failed to update resource {}/{}", resourceType, id, e);
            // Ignore failures otherwise OpenELIS will keep re-submitting it.
        }

        return ResponseEntity.noContent().build();
    }

    private void waitForSyncTask() throws InterruptedException {
        boolean waitForTask = syncTask.isExecuting();
        if (waitForTask) {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            while (syncTask.isExecuting()) {
                long wait = stopWatch.getTime(TimeUnit.MILLISECONDS);
                if (wait > 60000) {
                    if (log.isDebugEnabled()) {
                        log.debug("Wait time for sync task has expired after {} seconds.", wait / 1000);
                    }

                    break;
                }

                if (log.isTraceEnabled()) {
                    log.trace("Waiting for sync task to complete...");
                }

                Thread.sleep(1000);
            }

            stopWatch.stop();
            if (log.isDebugEnabled()) {
                log.debug("Done waiting for sync task after {} seconds.", stopWatch.getTime(TimeUnit.SECONDS));
            }
        }
    }

}
