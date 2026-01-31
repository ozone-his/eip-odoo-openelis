package com.ozonehis.eip.odoo.openelis.fhir;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.ICreate;
import ca.uhn.fhir.rest.gclient.ICreateTyped;
import ca.uhn.fhir.rest.gclient.ICriterion;
import ca.uhn.fhir.rest.gclient.ICriterionInternal;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.IUntypedQuery;
import ca.uhn.fhir.rest.gclient.IUpdate;
import ca.uhn.fhir.rest.gclient.IUpdateExecutable;
import ca.uhn.fhir.rest.gclient.IUpdateTyped;
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

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class BaseFhirClientTest {

    @Mock
    private IGenericClient mockFhirClient;

    @Mock
    private IUntypedQuery mockUnTypedQuery;

    @Mock
    private IQuery mockQuery;

    private BaseFhirClient baseClient;

    @BeforeEach
    void setUp() {
        baseClient = Mockito.spy(new OdooFhirClient());
        Mockito.lenient().doReturn(mockFhirClient).when(baseClient).getFhirClient();
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

        RuntimeException e = assertThrows(RuntimeException.class, () -> baseClient.getByIdentifier(extId, Patient.class));

        final String msg = "Found multiple resources of type Patient in Odoo with external identifier " + extId;
        Assertions.assertEquals(msg, e.getMessage());
        ICriterionInternal criterion = (ICriterionInternal) paramCaptor.getValue();
        Assertions.assertEquals("identifier", criterion.getParameterName());
        Assertions.assertEquals(extId, criterion.getParameterValue(null));
    }

    @Test
    public void create_shouldCallFhirClientWithResource() {
        Patient patient = new Patient();
        ICreate mockCreate = Mockito.mock(ICreate.class);
        ICreateTyped mockCreateTyped = Mockito.mock(ICreateTyped.class);
        MethodOutcome outcome = new MethodOutcome();
        outcome.setCreated(true);
        Mockito.when(mockFhirClient.create()).thenReturn(mockCreate);
        Mockito.when(mockCreate.resource(patient)).thenReturn(mockCreateTyped);
        Mockito.when(mockCreateTyped.execute()).thenReturn(outcome);

        baseClient.create(patient);

        Mockito.verify(mockCreateTyped).execute();
    }

    @Test
    public void create_shouldThrowRuntimeExceptionWhenExceptionOccurs() {
        Patient patient = new Patient();
        ICreate mockCreate = Mockito.mock(ICreate.class);
        ICreateTyped mockCreateTyped = Mockito.mock(ICreateTyped.class);
        Mockito.when(mockFhirClient.create()).thenReturn(mockCreate);
        Mockito.when(mockCreate.resource(patient)).thenReturn(mockCreateTyped);
        Mockito.when(mockCreateTyped.execute()).thenThrow(new RuntimeException("test"));

        RuntimeException e = assertThrows(RuntimeException.class, () -> baseClient.create(patient));

        Assertions.assertEquals("Failed to create Patient in Odoo", e.getMessage());
    }

    @Test
    public void create_shouldThrowRuntimeExceptionWhenOutcomeIsNotCreated() {
        final int status = 403;
        Patient patient = new Patient();
        ICreate mockCreate = Mockito.mock(ICreate.class);
        ICreateTyped mockCreateTyped = Mockito.mock(ICreateTyped.class);
        Mockito.when(mockFhirClient.create()).thenReturn(mockCreate);
        Mockito.when(mockCreate.resource(patient)).thenReturn(mockCreateTyped);
        MethodOutcome outcome = new MethodOutcome();
        outcome.setCreated(false);
        outcome.setResponseStatusCode(status);
        Mockito.when(mockCreateTyped.execute()).thenReturn(outcome);

        RuntimeException e = assertThrows(RuntimeException.class, () -> baseClient.create(patient));

        final String msg = "Unexpected status code " + status + " when creating Patient in Odoo";
        Assertions.assertEquals(msg, e.getMessage());
    }

    @Test
    public void update_shouldCallFhirClientWithResource() {
        Patient patient = new Patient();
        IUpdate mockUpdate = Mockito.mock(IUpdate.class);
        IUpdateTyped mockUpdateTyped = Mockito.mock(IUpdateTyped.class);
        MethodOutcome outcome = new MethodOutcome();
        outcome.setResponseStatusCode(200);
        Mockito.when(mockFhirClient.update()).thenReturn(mockUpdate);
        Mockito.when(mockUpdate.resource(patient)).thenReturn(mockUpdateTyped);
        Mockito.when(mockUpdateTyped.execute()).thenReturn(outcome);

        baseClient.update(patient);

        Mockito.verify(mockUpdateTyped).execute();
    }

    @Test
    public void update_shouldThrowRuntimeExceptionWhenExceptionOccurs() {
        Patient patient = new Patient();
        IUpdate mockUpdate = Mockito.mock(IUpdate.class);
        IUpdateTyped mockUpdateTyped = Mockito.mock(IUpdateTyped.class);
        Mockito.when(mockFhirClient.update()).thenReturn(mockUpdate);
        Mockito.when(mockUpdate.resource(patient)).thenReturn(mockUpdateTyped);
        Mockito.when(mockUpdateTyped.execute()).thenThrow(new RuntimeException("test"));

        RuntimeException e = assertThrows(RuntimeException.class, () -> baseClient.update(patient));

        Assertions.assertEquals("Failed to update Patient in Odoo", e.getMessage());
    }

    @Test
    public void update_shouldThrowRuntimeExceptionWhenOutcomeIsNotOk() {
        final int status = 403;
        Patient patient = new Patient();
        IUpdate mockUpdate = Mockito.mock(IUpdate.class);
        IUpdateTyped mockUpdateTyped = Mockito.mock(IUpdateTyped.class);
        Mockito.when(mockFhirClient.update()).thenReturn(mockUpdate);
        Mockito.when(mockUpdate.resource(patient)).thenReturn(mockUpdateTyped);
        MethodOutcome outcome = new MethodOutcome();
        outcome.setResponseStatusCode(status);
        Mockito.when(mockUpdateTyped.execute()).thenReturn(outcome);

        RuntimeException e = assertThrows(RuntimeException.class, () -> baseClient.update(patient));

        final String msg = "Unexpected status code " + status + " when updating Patient in Odoo";
        Assertions.assertEquals(msg, e.getMessage());
    }

    @Test
    public void update_shouldCallFhirClientWithResourceAndId() {
        final String id = "12345";
        final String data = "{}";
        IUpdate mockUpdate = Mockito.mock(IUpdate.class);
        IUpdateTyped mockUpdateTyped = Mockito.mock(IUpdateTyped.class);
        IUpdateExecutable mockExecutable = Mockito.mock(IUpdateExecutable.class);
        MethodOutcome outcome = new MethodOutcome();
        outcome.setResponseStatusCode(200);
        Mockito.when(mockFhirClient.update()).thenReturn(mockUpdate);
        Mockito.when(mockUpdate.resource(data)).thenReturn(mockUpdateTyped);
        Mockito.when(mockUpdateTyped.withId(id)).thenReturn(mockExecutable);
        Mockito.when(mockExecutable.execute()).thenReturn(outcome);

        baseClient.update("Patient", id, data);

        Mockito.verify(mockExecutable).execute();
    }

    @Test
    public void update_shouldThrowRuntimeExceptionWhenCalledWithResourceAndIdAndExceptionOccurs() {
        final String id = "12345";
        final String data = "{}";
        IUpdate mockUpdate = Mockito.mock(IUpdate.class);
        IUpdateTyped mockUpdateTyped = Mockito.mock(IUpdateTyped.class);
        IUpdateExecutable mockExecutable = Mockito.mock(IUpdateExecutable.class);
        Mockito.when(mockFhirClient.update()).thenReturn(mockUpdate);
        Mockito.when(mockUpdate.resource(data)).thenReturn(mockUpdateTyped);
        Mockito.when(mockUpdateTyped.withId(id)).thenReturn(mockExecutable);
        Mockito.when(mockExecutable.execute()).thenThrow(new RuntimeException("test"));

        RuntimeException e = assertThrows(RuntimeException.class, () -> baseClient.update("Patient", id, data));

        Assertions.assertEquals("Failed to update Patient in Odoo", e.getMessage());
    }

    @Test
    public void update_shouldThrowRuntimeExceptionWhenCalledWithResourceAndIdAndOutcomeIsNotOk() {
        final String id = "12345";
        final String data = "{}";
        final int status = 403;
        IUpdate mockUpdate = Mockito.mock(IUpdate.class);
        IUpdateTyped mockUpdateTyped = Mockito.mock(IUpdateTyped.class);
        IUpdateExecutable mockExecutable = Mockito.mock(IUpdateExecutable.class);
        Mockito.when(mockFhirClient.update()).thenReturn(mockUpdate);
        Mockito.when(mockUpdate.resource(data)).thenReturn(mockUpdateTyped);
        Mockito.when(mockUpdateTyped.withId(id)).thenReturn(mockExecutable);
        MethodOutcome outcome = new MethodOutcome();
        outcome.setResponseStatusCode(status);
        Mockito.when(mockExecutable.execute()).thenReturn(outcome);

        RuntimeException e = assertThrows(RuntimeException.class, () -> baseClient.update("Patient", id, data));

        final String msg = "Unexpected status code " + status + " when updating Patient/" + id + " in Odoo";
        Assertions.assertEquals(msg, e.getMessage());
    }

}
