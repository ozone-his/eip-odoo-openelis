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
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.ServiceRequest;

public class ServiceRequestPatientService {

    private final OdooFhirClient odooFhirClient;

    private final OpenElisFhirClient openElisFhirClient;

    public ServiceRequestPatientService(OdooFhirClient odooFhirClient, OpenElisFhirClient openElisFhirClient) {
        this.odooFhirClient = odooFhirClient;
        this.openElisFhirClient = openElisFhirClient;
    }

    public void createSubjectPatientFromPayloadIfMissing(String resourceType, String body) {
        if (!ServiceRequest.class.getSimpleName().equals(resourceType)) {
            return;
        }

        createPatientIfMissing(getSubjectPatientIdentifier(body));
    }

    public void createSubjectPatientFromServiceRequestIfMissing(ServiceRequest serviceRequest) {
        if (!serviceRequest.hasSubject() || !serviceRequest.getSubject().hasIdentifier()) {
            return;
        }

        String patientIdentifier = serviceRequest.getSubject().getIdentifier().getValue();
        createPatientIfMissing(patientIdentifier);
    }

    private void createPatientIfMissing(String patientIdentifier) {
        if (patientIdentifier == null || patientIdentifier.isBlank()) {
            return;
        }

        if (odooFhirClient.getByIdentifier(patientIdentifier, Patient.class) != null) {
            return;
        }

        Patient patient = openElisFhirClient.getByIdentifier(patientIdentifier, Patient.class);
        if (patient == null) {
            return;
        }

        odooFhirClient.create(patient);
    }

    private String getSubjectPatientIdentifier(String body) {
        try {
            return JsonPath.read(body, "subject.identifier.value");
        } catch (PathNotFoundException e) {
            return null;
        }
    }
}
