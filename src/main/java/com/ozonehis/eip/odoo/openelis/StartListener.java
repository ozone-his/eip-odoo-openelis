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
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Subscription;
import org.hl7.fhir.r4.model.Subscription.SubscriptionChannelComponent;
import org.hl7.fhir.r4.model.Subscription.SubscriptionChannelType;
import org.hl7.fhir.r4.model.Subscription.SubscriptionStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

@Slf4j
public class StartListener {

    @Value("${" + Constants.PROP_SUB_ENDPOINT + "}")
    private String endpoint;

    @Value("${eip.odoo.fhir.username}")
    private String odooFhirUsername;

    @Value("${eip.odoo.fhir.password}")
    private char[] odooFhirPassword;

    private OpenElisFhirClient openElisClient;

    public StartListener(OpenElisFhirClient openElisClient) {
        this.openElisClient = openElisClient;
    }

    @EventListener(classes = {ContextRefreshedEvent.class})
    public void contextRefreshed() {
        initializeSubscriptions();
    }

    private void initializeSubscriptions() {
        if (log.isDebugEnabled()) {
            log.debug("Checking if the Patient and ServiceRequest subscription exists in OpenELIS");
        }

        Subscription sub = openElisClient.getSubscription();
        if (sub == null) {
            SubscriptionChannelComponent channel = new SubscriptionChannelComponent();
            configureChannel(channel);
            sub = new Subscription();
            sub.setStatus(SubscriptionStatus.REQUESTED);
            sub.setCriteria(Constants.SUBSCRIPTION_CRITERIA);
            sub.setChannel(channel);
            sub.addExtension(Constants.EXT, new BooleanType(true));
            openElisClient.create(sub);
        } else if (configureChannel(sub.getChannel())) {
            if (log.isDebugEnabled()) {
                log.debug("Updating subscription channel");
            }

            openElisClient.update(sub);
        }
    }

    private boolean configureChannel(SubscriptionChannelComponent channel) {
        boolean updated = false;

        if (!channel.hasType() || channel.getType() != SubscriptionChannelType.RESTHOOK) {
            channel.setType(SubscriptionChannelType.RESTHOOK);
            updated = true;
        }
        if (!Constants.MEDIA_TYPE.equals(channel.getPayload())) {
            channel.setPayload(Constants.MEDIA_TYPE);
            updated = true;
        }
        if (!endpoint.equals(channel.getEndpoint())) {
            channel.setEndpoint(endpoint);
            updated = true;
        }

        String authHeader = buildAuthorizationHeader();
        boolean hasCurrentAuthHeader =
                channel.getHeader().stream().map(StringType::getValue).anyMatch(authHeader::equals);
        if (!hasCurrentAuthHeader) {
            channel.getHeader().removeIf(header -> isAuthorizationHeader(header.getValue()));
            channel.addHeader(authHeader);
            updated = true;
        }

        return updated;
    }

    private String buildAuthorizationHeader() {
        String credentials = odooFhirUsername + ":" + new String(odooFhirPassword);
        return "Authorization: Basic "
                + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

    private boolean isAuthorizationHeader(String header) {
        return header != null && header.regionMatches(true, 0, "Authorization:", 0, "Authorization:".length());
    }
}
