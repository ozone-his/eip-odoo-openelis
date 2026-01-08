package com.ozonehis.eip.odoo.openelis;

import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Subscription;
import org.hl7.fhir.r4.model.Subscription.SubscriptionChannelComponent;
import org.hl7.fhir.r4.model.Subscription.SubscriptionChannelType;
import org.hl7.fhir.r4.model.Subscription.SubscriptionStatus;
import com.ozonehis.eip.odoo.openelis.fhir.OpenElisFhirClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;

@Slf4j
public class StartListener {

    @Value("${" + Constants.PROP_SUB_ENDPOINT + "}")
    private String endpoint;

    private OpenElisFhirClient fhirClient;

    public StartListener(OpenElisFhirClient fhirClient) {
        this.fhirClient = fhirClient;
    }

    @EventListener(classes = {ContextRefreshedEvent.class})
    public void contextRefreshed() {
        initializeSubscriptions();
    }

    protected void initializeSubscriptions() {
        if (log.isDebugEnabled()) {
            log.debug("Checking if the Patient and ServiceRequest subscription exists in OpenELIS");
        }

        Subscription sub = fhirClient.getSubscription();
        if (sub == null) {
            SubscriptionChannelComponent channel = new SubscriptionChannelComponent();
            channel.setType(SubscriptionChannelType.RESTHOOK);
            channel.setPayload(MediaType.APPLICATION_JSON_VALUE);
            channel.setEndpoint(endpoint);
            sub = new Subscription();
            sub.setStatus(SubscriptionStatus.REQUESTED);
            sub.setCriteria(Constants.SUBSCRIPTION_CRITERIA);
            sub.setChannel(channel);
            fhirClient.create(sub);
        } else if (!endpoint.equals(sub.getChannel().getEndpoint())) {
            sub.getChannel().setEndpoint(endpoint);
            fhirClient.update(sub);
        }
    }

}
