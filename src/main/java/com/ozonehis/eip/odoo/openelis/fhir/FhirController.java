package com.ozonehis.eip.odoo.openelis.fhir;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/fhir")
@Slf4j
public class FhirController {

    private final OdooFhirClient odooFhirClient;

    @Autowired
    public FhirController(OdooFhirClient odooFhirClient) {
        this.odooFhirClient = odooFhirClient;
    }

    /**
     * Handles FHIR PUT requests and forwards the request body to Fhir Odoo service via the {@link OdooFhirClient}
     *
     * @param resourceType The resource type name
     * @param id           The id of the resource being updated.
     * @param body         The incoming FHIR resource payload.
     */
    @PutMapping("{resourceType}/{id}")
    public void onCreateOrUpdate(@PathVariable String resourceType, @PathVariable String id, @RequestBody String body) {
        try {
            odooFhirClient.update(resourceType, id, body);
        } catch (Throwable e) {
            log.warn("Failed to update resource {}/{}", resourceType, id, e);
            // Ignore failures otherwise OpenELIS will keep re-submitting it.
        }
    }

}
