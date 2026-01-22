package com.ozonehis.eip.odoo.openelis.fhir.web;


import com.jayway.jsonpath.JsonPath;
import com.ozonehis.eip.odoo.openelis.DateUtils;
import com.ozonehis.eip.odoo.openelis.fhir.OdooFhirClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/fhir")
@Slf4j
public class FhirController {

    private static final Map<String, LocalDateTime> ID_LAST_UPDATED_MAP = Collections.synchronizedMap(new HashMap<>());

    private final OdooFhirClient odooFhirClient;

    @Autowired
    public FhirController(OdooFhirClient odooFhirClient) {
        this.odooFhirClient = odooFhirClient;
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
            status = odooFhirClient.update(resourceType, id, body);
            LocalDateTime lastUpdated = DateUtils.deserialize(JsonPath.read(body, "meta.lastUpdated"));
            ID_LAST_UPDATED_MAP.put(resourceType + id, lastUpdated);
            if (log.isDebugEnabled()) {
                log.debug("Storing id={} with lastUpdated={}", id, lastUpdated);
            }
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
            odooFhirClient.delete(resourceType, id);
            ID_LAST_UPDATED_MAP.put(resourceType + id, null);
        } catch (Throwable e) {
            log.error("Failed to update resource {}/{}", resourceType, id, e);
            // Ignore failures otherwise OpenELIS will keep re-submitting it.
        }

        return ResponseEntity.noContent().build();
    }

}
