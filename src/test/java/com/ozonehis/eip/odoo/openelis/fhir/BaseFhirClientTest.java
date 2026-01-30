package com.ozonehis.eip.odoo.openelis.fhir;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.ICriterion;
import ca.uhn.fhir.rest.gclient.ICriterionInternal;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.IUntypedQuery;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class BaseFhirClientTest {

    @Mock
    private IGenericClient mockFhirClient;

    @Mock
    private IUntypedQuery mockUnTypedQuery;

    @Mock
    private IQuery mockQuery;

    @Mock
    private ICriterion mockCriterion;

    private BaseFhirClient baseClient;

    @BeforeEach
    void setUp() {
        baseClient = Mockito.spy(new OdooFhirClient());
        Mockito.doReturn(mockFhirClient).when(baseClient).getFhirClient();
    }

    @Test
    public void getByIdentifier_shouldReturnThePatientWithTheMatchingExternalId() {
        String extId = "12345";
        Bundle bundle = new Bundle();
        Patient patient = new Patient();
        bundle.addEntry().setResource(patient);
        Mockito.when(mockFhirClient.search()).thenReturn(mockUnTypedQuery);
        Mockito.when(mockUnTypedQuery.forResource(Patient.class)).thenReturn(mockQuery);
        ArgumentCaptor<ICriterion> paramCaptor = ArgumentCaptor.forClass(ICriterion.class);
        Mockito.when(mockQuery.where(paramCaptor.capture())).thenReturn(mockQuery);
        Mockito.when(mockQuery.execute()).thenReturn(bundle);

        Patient resource = baseClient.getByIdentifier(extId, Patient.class);

        Assertions.assertEquals(patient, resource);
        ICriterionInternal criterion = (ICriterionInternal) paramCaptor.getValue();
        Assertions.assertEquals("identifier", criterion.getParameterName());
        Assertions.assertEquals(extId, criterion.getParameterValue(null));
    }

    @Test
    public void getByIdentifier_shouldReturnNullIfNoResourceMatchesTheExternalId() {
        String extId = "12345";
        Mockito.when(mockFhirClient.search()).thenReturn(mockUnTypedQuery);
        Mockito.when(mockUnTypedQuery.forResource(Patient.class)).thenReturn(mockQuery);
        ArgumentCaptor<ICriterion> paramCaptor = ArgumentCaptor.forClass(ICriterion.class);
        Mockito.when(mockQuery.where(paramCaptor.capture())).thenReturn(mockQuery);
        Mockito.doThrow(ResourceNotFoundException.class).when(mockQuery).execute();

        Patient resource = baseClient.getByIdentifier(extId, Patient.class);

        Assertions.assertNull(resource);
        ICriterionInternal criterion = (ICriterionInternal) paramCaptor.getValue();
        Assertions.assertEquals("identifier", criterion.getParameterName());
        Assertions.assertEquals(extId, criterion.getParameterValue(null));
    }

    @Test
    public void getByIdentifier_shouldFailIfMultipleResourcesMatchTheExternalId() {
        String extId = "12345";
        Bundle bundle = new Bundle();
        bundle.addEntry().setResource(new Patient());
        bundle.addEntry().setResource(new Patient());
        Mockito.when(mockFhirClient.search()).thenReturn(mockUnTypedQuery);
        Mockito.when(mockUnTypedQuery.forResource(Patient.class)).thenReturn(mockQuery);
        ArgumentCaptor<ICriterion> paramCaptor = ArgumentCaptor.forClass(ICriterion.class);
        Mockito.when(mockQuery.where(paramCaptor.capture())).thenReturn(mockQuery);
        Mockito.when(mockQuery.execute()).thenReturn(bundle);

        RuntimeException e = Assertions.assertThrows(RuntimeException.class, () -> baseClient.getByIdentifier(extId, Patient.class));

        final String msg = "Found multiple resources of type Patient in Odoo with external identifier " + extId;
        Assertions.assertEquals(msg, e.getMessage());
        ICriterionInternal criterion = (ICriterionInternal) paramCaptor.getValue();
        Assertions.assertEquals("identifier", criterion.getParameterName());
        Assertions.assertEquals(extId, criterion.getParameterValue(null));
    }

}
