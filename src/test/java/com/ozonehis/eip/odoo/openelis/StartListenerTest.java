/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odoo.openelis;

import com.ozonehis.eip.odoo.openelis.fhir.OpenElisFhirClient;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Subscription;
import org.hl7.fhir.r4.model.Subscription.SubscriptionChannelComponent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.powermock.reflect.Whitebox;

public class StartListenerTest {

    private static final String USERNAME = "admin";

    private static final char[] PASSWORD = "password".toCharArray();

    private FakeOpenElisFhirClient client;

    private StartListener listener;

    @BeforeEach
    public void setUp() {
        client = new FakeOpenElisFhirClient();
        listener = new StartListener(client);
        Whitebox.setInternalState(listener, "odooFhirUsername", USERNAME);
        Whitebox.setInternalState(listener, "odooFhirPassword", PASSWORD);
    }

    @Test
    public void contextRefreshed_whenSubscriptionDoesNotExist() {
        final String endpoint = "test.test/test";
        Whitebox.setInternalState(listener, "endpoint", endpoint);

        listener.contextRefreshed();

        Subscription subscription = client.createdSubscription;
        Assertions.assertEquals(endpoint, subscription.getChannel().getEndpoint());
        Assertions.assertTrue(hasExpectedAuthorizationHeader(subscription.getChannel()));
        Assertions.assertNull(client.updatedSubscription);
    }

    @Test
    public void contextRefreshed_shouldUpdateTheEndpointForAnExistingSubscriptionIfItIsDifferent() {
        final String endpoint = "test.test/new";
        Whitebox.setInternalState(listener, "endpoint", endpoint);
        Subscription subscription = new Subscription();
        SubscriptionChannelComponent channel = new SubscriptionChannelComponent();
        channel.setEndpoint("test.test/old");
        subscription.setChannel(channel);
        client.existingSubscription = subscription;

        listener.contextRefreshed();

        Assertions.assertEquals(1, client.getSubscriptionCallCount);
        Assertions.assertNull(client.createdSubscription);
        Assertions.assertSame(subscription, client.updatedSubscription);
    }

    @Test
    public void contextRefreshed_shouldUpdateExistingSubscriptionIfAuthorizationHeaderIsMissing() {
        final String endpoint = "test.test/test";
        Whitebox.setInternalState(listener, "endpoint", endpoint);
        Subscription subscription = new Subscription();
        SubscriptionChannelComponent channel = new SubscriptionChannelComponent();
        channel.setEndpoint(endpoint);
        subscription.setChannel(channel);
        client.existingSubscription = subscription;

        listener.contextRefreshed();

        Assertions.assertEquals(1, client.getSubscriptionCallCount);
        Assertions.assertNull(client.createdSubscription);
        Assertions.assertSame(subscription, client.updatedSubscription);
        Assertions.assertTrue(hasExpectedAuthorizationHeader(subscription.getChannel()));
    }

    @Test
    public void contextRefreshed_whenSubscriptionExistsAndEndpointAndAuthorizationHeaderAreSame() {
        final String endpoint = "test.test/test";
        Whitebox.setInternalState(listener, "endpoint", endpoint);
        Subscription subscription = new Subscription();
        SubscriptionChannelComponent channel = new SubscriptionChannelComponent();
        channel.setEndpoint(endpoint);
        channel.setType(Subscription.SubscriptionChannelType.RESTHOOK);
        channel.setPayload(Constants.MEDIA_TYPE);
        channel.addHeader(expectedAuthorizationHeader());
        subscription.setChannel(channel);
        client.existingSubscription = subscription;

        listener.contextRefreshed();

        Assertions.assertEquals(1, client.getSubscriptionCallCount);
        Assertions.assertNull(client.createdSubscription);
        Assertions.assertNull(client.updatedSubscription);
    }

    private String expectedAuthorizationHeader() {
        String credentials = USERNAME + ":" + new String(PASSWORD);
        return "Authorization: Basic "
                + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

    private boolean hasExpectedAuthorizationHeader(SubscriptionChannelComponent channel) {
        return channel.getHeader().stream().map(StringType::getValue).anyMatch(expectedAuthorizationHeader()::equals);
    }

    private static class FakeOpenElisFhirClient extends OpenElisFhirClient {

        private Subscription existingSubscription;

        private Subscription createdSubscription;

        private Subscription updatedSubscription;

        private int getSubscriptionCallCount;

        @Override
        public Subscription getSubscription() {
            getSubscriptionCallCount++;
            return existingSubscription;
        }

        @Override
        public void create(DomainResource resource) {
            createdSubscription = (Subscription) resource;
        }

        @Override
        public void update(DomainResource resource) {
            updatedSubscription = (Subscription) resource;
        }
    }
}
