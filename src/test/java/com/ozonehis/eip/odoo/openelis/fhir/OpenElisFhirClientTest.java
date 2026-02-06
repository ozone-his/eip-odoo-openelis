package com.ozonehis.eip.odoo.openelis.fhir;

import static org.hl7.fhir.r4.model.Subscription.SubscriptionChannelType.RESTHOOK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.ICriterion;
import ca.uhn.fhir.rest.gclient.ICriterionInternal;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.IUntypedQuery;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import com.ozonehis.eip.odoo.openelis.Constants;
import com.ozonehis.eip.odoo.openelis.DateUtils;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.Subscription;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class OpenElisFhirClientTest {

    @Mock
    private IGenericClient mockFhirClient;

    @Mock
    private IUntypedQuery mockUnTypedQuery;

    @Mock
    private IQuery mockQuery;

    private OpenElisFhirClient client;

    @BeforeEach
    public void setUp() {
        client = Mockito.spy(new OpenElisFhirClient());
        Mockito.lenient().doReturn(mockFhirClient).when(client).getFhirClient();
    }

    @Test
    public void getSubscription_shouldReturnTheExistingSubscription() {
        Bundle bundle = new Bundle();
        Subscription sub = new Subscription();
        bundle.addEntry().setResource(sub);
        Mockito.when(mockFhirClient.search()).thenReturn(mockUnTypedQuery);
        Mockito.when(mockUnTypedQuery.forResource(Subscription.class)).thenReturn(mockQuery);
        ArgumentCaptor<ICriterion> criteriaArgCaptor = ArgumentCaptor.forClass(ICriterion.class);
        ArgumentCaptor<ICriterion> typeArgCaptor = ArgumentCaptor.forClass(ICriterion.class);
        Mockito.when(mockQuery.where(criteriaArgCaptor.capture())).thenReturn(mockQuery);
        Mockito.when(mockQuery.and(typeArgCaptor.capture()))
                .thenReturn(mockQuery)
                .thenReturn(mockQuery);
        Mockito.when(mockQuery.execute()).thenReturn(bundle);

        Subscription resource = client.getSubscription();

        assertEquals(sub, resource);
        ICriterionInternal criteriaCriterion = (ICriterionInternal) criteriaArgCaptor.getValue();
        assertEquals("criteria:exact", criteriaCriterion.getParameterName());
        assertEquals("[Patient\\,ServiceRequest]", criteriaCriterion.getParameterValue(null));
        ICriterionInternal typeCriterion =
                (ICriterionInternal) typeArgCaptor.getAllValues().get(0);
        assertEquals("type:exact", typeCriterion.getParameterName());
        assertEquals(RESTHOOK.toCode(), typeCriterion.getParameterValue(null));
        ICriterionInternal payloadCriterion =
                (ICriterionInternal) typeArgCaptor.getAllValues().get(1);
        assertEquals("payload:exact", payloadCriterion.getParameterName());
        assertEquals(Constants.MEDIA_TYPE, payloadCriterion.getParameterValue(null));
    }

    @Test
    public void getSubscription_shouldReturnNullIfNoMatchingSubscriptionExists() {
        Mockito.when(mockFhirClient.search()).thenReturn(mockUnTypedQuery);
        Mockito.when(mockUnTypedQuery.forResource(Subscription.class)).thenReturn(mockQuery);
        ArgumentCaptor<ICriterion> criteriaArgCaptor = ArgumentCaptor.forClass(ICriterion.class);
        ArgumentCaptor<ICriterion> typeArgCaptor = ArgumentCaptor.forClass(ICriterion.class);
        ArgumentCaptor<ICriterion> payloadArgCaptor = ArgumentCaptor.forClass(ICriterion.class);
        Mockito.when(mockQuery.where(criteriaArgCaptor.capture())).thenReturn(mockQuery);
        Mockito.when(mockQuery.and(typeArgCaptor.capture())).thenReturn(mockQuery);
        Mockito.when(mockQuery.and(payloadArgCaptor.capture())).thenReturn(mockQuery);
        Mockito.doThrow(ResourceNotFoundException.class).when(mockQuery).execute();
        Assertions.assertNull(client.getSubscription());
    }

    @Test
    public void getSubscription_shouldFailIfMultipleSubscriptionsAraFound() {
        Bundle bundle = new Bundle();
        bundle.addEntry().setResource(new Subscription());
        bundle.addEntry().setResource(new Subscription());
        Mockito.when(mockFhirClient.search()).thenReturn(mockUnTypedQuery);
        Mockito.when(mockUnTypedQuery.forResource(Subscription.class)).thenReturn(mockQuery);
        ArgumentCaptor<ICriterion> criteriaArgCaptor = ArgumentCaptor.forClass(ICriterion.class);
        ArgumentCaptor<ICriterion> typeArgCaptor = ArgumentCaptor.forClass(ICriterion.class);
        ArgumentCaptor<ICriterion> payloadArgCaptor = ArgumentCaptor.forClass(ICriterion.class);
        Mockito.when(mockQuery.where(criteriaArgCaptor.capture())).thenReturn(mockQuery);
        Mockito.when(mockQuery.and(typeArgCaptor.capture())).thenReturn(mockQuery);
        Mockito.when(mockQuery.and(payloadArgCaptor.capture())).thenReturn(mockQuery);
        Mockito.when(mockQuery.execute()).thenReturn(bundle);

        RuntimeException e = assertThrows(RuntimeException.class, () -> client.getSubscription());

        Assertions.assertEquals("Found multiple subscriptions of type in OpenELIS", e.getMessage());
    }

    @Test
    public void getModifiedResources_shouldReturnModifiedResources() {
        LocalDateTime since = LocalDateTime.now().minusDays(1);
        List<DomainResource> resources = new ArrayList<>();
        DomainResource resource1 = Mockito.mock(DomainResource.class);
        DomainResource resource2 = Mockito.mock(DomainResource.class);
        resources.add(resource1);
        resources.add(resource2);
        Bundle bundle = new Bundle();
        bundle.addEntry().setResource(resource1);
        bundle.addEntry().setResource(resource2);
        Mockito.when(mockFhirClient.search()).thenReturn(mockUnTypedQuery);
        Mockito.when(mockUnTypedQuery.forResource(DomainResource.class)).thenReturn(mockQuery);
        ArgumentCaptor<ICriterion> argCaptor = ArgumentCaptor.forClass(ICriterion.class);
        Mockito.when(mockQuery.where(argCaptor.capture())).thenReturn(mockQuery);
        Mockito.when(mockQuery.execute()).thenReturn(bundle);

        List<DomainResource> resultingResources = client.getModifiedResources(DomainResource.class, since);

        assertEquals(resources.size(), resultingResources.size());
        ICriterionInternal criterion = (ICriterionInternal) argCaptor.getValue();
        assertEquals("_lastUpdated", criterion.getParameterName());
        assertEquals("ge" + DateUtils.serialize(since), criterion.getParameterValue(null));
    }

    @Test
    public void getModifiedResources_shouldReturnEmptyListWhenNoResourcesAreFound() {
        LocalDateTime since = LocalDateTime.now().minusDays(1);
        Bundle bundle = new Bundle();
        Mockito.when(mockFhirClient.search()).thenReturn(mockUnTypedQuery);
        Mockito.when(mockUnTypedQuery.forResource(DomainResource.class)).thenReturn(mockQuery);
        ArgumentCaptor<ICriterion> argCaptor = ArgumentCaptor.forClass(ICriterion.class);
        Mockito.when(mockQuery.where(argCaptor.capture())).thenReturn(mockQuery);
        Mockito.when(mockQuery.execute()).thenReturn(bundle);

        List<DomainResource> resultingResources = client.getModifiedResources(DomainResource.class, since);

        assertTrue(resultingResources.isEmpty());
    }
}
