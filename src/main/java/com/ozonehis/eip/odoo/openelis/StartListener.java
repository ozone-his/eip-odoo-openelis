/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odoo.openelis;

import com.ozonehis.eip.odoo.openelis.fhir.OpenElisFhirClient;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.BooleanType;
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
            channel.setType(SubscriptionChannelType.RESTHOOK);
            channel.setPayload(Constants.MEDIA_TYPE);
            channel.setEndpoint(endpoint);
            sub = new Subscription();
            sub.setStatus(SubscriptionStatus.REQUESTED);
            sub.setCriteria(Constants.SUBSCRIPTION_CRITERIA);
            sub.setChannel(channel);
            sub.addExtension(Constants.EXT, new BooleanType(true));
            openElisClient.create(sub);
        } else if (!endpoint.equals(sub.getChannel().getEndpoint())) {
            if (log.isDebugEnabled()) {
                log.debug("Updating subscription endpoint");
            }

            sub.getChannel().setEndpoint(endpoint);
            openElisClient.update(sub);
        }
    }
}
