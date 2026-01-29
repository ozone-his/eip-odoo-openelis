package com.ozonehis.eip.odoo.openelis;

import com.ozonehis.eip.odoo.openelis.fhir.OpenElisFhirClient;
import org.hl7.fhir.r4.model.Subscription;
import org.hl7.fhir.r4.model.Subscription.SubscriptionChannelComponent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.powermock.reflect.Whitebox;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class StartListenerTest {

    @Mock
    private OpenElisFhirClient mockClient;

    private StartListener listener;

    @BeforeEach
    public void setUp() {
        listener = new StartListener(mockClient);
    }

    @Test
    public void contextRefreshed_whenSubscriptionDoesNotExist() {
        final String endpoint = "test.test/test";
        Whitebox.setInternalState(listener, "endpoint", endpoint);

        listener.contextRefreshed();

        ArgumentCaptor<Subscription> argCaptor = ArgumentCaptor.forClass(Subscription.class);
        verify(mockClient).create(argCaptor.capture());
        Subscription subscription = argCaptor.getValue();
        Assertions.assertEquals(endpoint, subscription.getChannel().getEndpoint());
        verify(mockClient, never()).update(any(Subscription.class));
    }

    @Test
    public void contextRefreshed_shouldUpdateTheEndpointForAnExistingSubscriptionIfItIsDifferent() {
        final String endpoint = "test.test/new";
        Whitebox.setInternalState(listener, "endpoint", endpoint);
        Subscription subscription = new Subscription();
        SubscriptionChannelComponent channel = new SubscriptionChannelComponent();
        channel.setEndpoint("test.test/old");
        subscription.setChannel(channel);
        when(mockClient.getSubscription()).thenReturn(subscription);

        listener.contextRefreshed();

        verify(mockClient).getSubscription();
        verify(mockClient, never()).create(any(Subscription.class));
        verify(mockClient).update(any(Subscription.class));
    }

    @Test
    public void contextRefreshed_whenSubscriptionExistsAndEndpointIsSame() {
        final String endpoint = "test.test/test";
        Whitebox.setInternalState(listener, "endpoint", endpoint);
        Subscription subscription = new Subscription();
        SubscriptionChannelComponent channel = new SubscriptionChannelComponent();
        channel.setEndpoint(endpoint);
        subscription.setChannel(channel);
        when(mockClient.getSubscription()).thenReturn(subscription);

        listener.contextRefreshed();

        verify(mockClient).getSubscription();
        verify(mockClient, never()).create(any(Subscription.class));
        verify(mockClient, never()).update(any(Subscription.class));
    }

}
