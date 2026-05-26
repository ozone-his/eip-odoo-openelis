/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odoo.openelis;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ozonehis.eip.odoo.openelis.fhir.OdooFhirClient;
import com.ozonehis.eip.odoo.openelis.fhir.OpenElisFhirClient;
import java.util.Map;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ServiceRequestPatientServiceTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Mock
    private OdooFhirClient mockOdooClient;

    @Mock
    private OpenElisFhirClient mockOpenElisClient;

    private ServiceRequestPatientService service;

    @BeforeEach
    public void setUp() {
        service = new ServiceRequestPatientService(mockOdooClient, mockOpenElisClient);
    }

    @Test
    public void createSubjectPatientFromServiceRequestIfMissing_shouldCreateMissingPatient() {
        String patientIdentifier = "patient-identifier";
        ServiceRequest serviceRequest = new ServiceRequest();
        serviceRequest.setSubject(new Reference().setIdentifier(new Identifier().setValue(patientIdentifier)));
        Patient patient = new Patient();
        when(mockOdooClient.getByIdentifier(patientIdentifier, Patient.class)).thenReturn(null);
        when(mockOpenElisClient.getByIdentifier(patientIdentifier, Patient.class))
                .thenReturn(patient);

        service.createSubjectPatientFromServiceRequestIfMissing(serviceRequest);

        InOrder inOrder = Mockito.inOrder(mockOdooClient);
        inOrder.verify(mockOdooClient).getByIdentifier(patientIdentifier, Patient.class);
        inOrder.verify(mockOdooClient).create(patient);
        verify(mockOpenElisClient).getByIdentifier(patientIdentifier, Patient.class);
    }

    @Test
    public void
            createSubjectPatientFromServiceRequestIfMissing_shouldNotCreatePatientWhenIdentifierAlreadyExistsInOdoo() {
        String patientIdentifier = "patient-identifier";
        ServiceRequest serviceRequest = new ServiceRequest();
        serviceRequest.setSubject(new Reference().setIdentifier(new Identifier().setValue(patientIdentifier)));
        when(mockOdooClient.getByIdentifier(patientIdentifier, Patient.class)).thenReturn(new Patient());

        service.createSubjectPatientFromServiceRequestIfMissing(serviceRequest);

        verify(mockOpenElisClient, never()).getByIdentifier(patientIdentifier, Patient.class);
        verify(mockOdooClient, never()).create(Mockito.any(Patient.class));
    }

    @Test
    public void createSubjectPatientFromPayloadIfMissing_shouldCreateMissingPatientFromServiceRequestPayload()
            throws Exception {
        String patientIdentifier = "patient-identifier";
        String body =
                MAPPER.writeValueAsString(Map.of("subject", Map.of("identifier", Map.of("value", patientIdentifier))));
        Patient patient = new Patient();
        when(mockOdooClient.getByIdentifier(patientIdentifier, Patient.class)).thenReturn(null);
        when(mockOpenElisClient.getByIdentifier(patientIdentifier, Patient.class))
                .thenReturn(patient);

        service.createSubjectPatientFromPayloadIfMissing(ServiceRequest.class.getSimpleName(), body);

        verify(mockOdooClient).getByIdentifier(patientIdentifier, Patient.class);
        verify(mockOpenElisClient).getByIdentifier(patientIdentifier, Patient.class);
        verify(mockOdooClient).create(patient);
    }

    @Test
    public void createSubjectPatientFromPayloadIfMissing_shouldIgnoreNonServiceRequestPayload() throws Exception {
        String body = MAPPER.writeValueAsString(
                Map.of("subject", Map.of("identifier", Map.of("value", "patient-identifier"))));

        service.createSubjectPatientFromPayloadIfMissing(Patient.class.getSimpleName(), body);

        verify(mockOdooClient, never()).getByIdentifier(Mockito.anyString(), Mockito.eq(Patient.class));
        verify(mockOpenElisClient, never()).getByIdentifier(Mockito.anyString(), Mockito.eq(Patient.class));
        verify(mockOdooClient, never()).create(Mockito.any(Patient.class));
    }
}
