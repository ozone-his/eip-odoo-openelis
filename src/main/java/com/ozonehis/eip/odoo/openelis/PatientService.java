/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odoo.openelis;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import com.ozonehis.eip.odoo.openelis.fhir.OdooFhirClient;
import com.ozonehis.eip.odoo.openelis.fhir.OpenElisFhirClient;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.ServiceRequest;

@Slf4j
public class PatientService {

    private final OdooFhirClient odooFhirClient;

    private final OpenElisFhirClient openElisFhirClient;

    public PatientService(OdooFhirClient odooFhirClient, OpenElisFhirClient openElisFhirClient) {
        this.odooFhirClient = odooFhirClient;
        this.openElisFhirClient = openElisFhirClient;
    }

    public void createPatientIfMissing(String resourceType, String body) {
        if (!ServiceRequest.class.getSimpleName().equals(resourceType)) {
            log.debug("Skipping patient creation because resource type is {} not ServiceRequest", resourceType);

            return;
        }

        createPatientIfMissing(getPatientIdentifier(body));
    }

    public void createPatientIfMissing(ServiceRequest serviceRequest) {
        if (!serviceRequest.hasSubject() || !serviceRequest.getSubject().hasIdentifier()) {
            log.debug("Skipping patient creation because service request has no subject identifier");

            return;
        }

        String patientIdentifier = serviceRequest.getSubject().getIdentifier().getValue();
        createPatientIfMissing(patientIdentifier);
    }

    private void createPatientIfMissing(String patientIdentifier) {
        if (patientIdentifier == null || patientIdentifier.isBlank()) {
            log.debug("Skipping patient creation because patient identifier is blank");

            return;
        }

        if (odooFhirClient.getByIdentifier(patientIdentifier, Patient.class) != null) {
            log.debug(
                    "Skipping patient creation because patient already exists in Odoo with identifier: {}",
                    patientIdentifier);

            return;
        }

        Patient patient = openElisFhirClient.getByIdentifier(patientIdentifier, Patient.class);
        if (patient == null) {
            log.debug(
                    "Skipping patient creation because patient was not found in OpenELIS with identifier: {}",
                    patientIdentifier);

            return;
        }

        odooFhirClient.create(patient);
    }

    private String getPatientIdentifier(String body) {
        try {
            String patientIdentifier = JsonPath.read(body, "subject.identifier.value");
            log.debug("Found patient identifier in service request body: {}", patientIdentifier);

            return patientIdentifier;
        } catch (PathNotFoundException e) {
            log.debug("No patient identifier found in service request body");

            return null;
        }
    }
}
