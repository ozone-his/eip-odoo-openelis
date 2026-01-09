package com.ozonehis.eip.odoo.openelis;

import com.ozonehis.eip.odoo.openelis.fhir.OdooFhirClient;
import com.ozonehis.eip.odoo.openelis.fhir.OpenElisFhirClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Subscription;
import org.hl7.fhir.r4.model.Subscription.SubscriptionChannelComponent;
import org.hl7.fhir.r4.model.Subscription.SubscriptionChannelType;
import org.hl7.fhir.r4.model.Subscription.SubscriptionStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

import java.util.Base64;

import static java.nio.charset.StandardCharsets.UTF_8;

@Slf4j
public class StartListener {

    @Value("${" + Constants.PROP_SUB_ENDPOINT + "}")
    private String endpoint;

    private OpenElisFhirClient openElisClient;

    private OdooFhirClient odooClient;

    public StartListener(OpenElisFhirClient openElisClient, OdooFhirClient odooClient) {
        this.openElisClient = openElisClient;
        this.odooClient = odooClient;
    }

    @EventListener(classes = {ContextRefreshedEvent.class})
    public void contextRefreshed() {
        initializeSubscriptions();
    }

    protected void initializeSubscriptions() {
        if (log.isDebugEnabled()) {
            log.debug("Checking if the Patient and ServiceRequest subscription exists in OpenELIS");
        }

        Subscription sub = openElisClient.getSubscription();
        final String auth = getAuthHeader();
        if (sub == null) {
            SubscriptionChannelComponent channel = new SubscriptionChannelComponent();
            channel.setType(SubscriptionChannelType.RESTHOOK);
            channel.setPayload(Constants.MEDIA_TYPE);
            channel.setEndpoint(endpoint);
            if (StringUtils.isNotBlank(auth)) {
                if (log.isDebugEnabled()) {
                    log.debug("Adding authentication header");
                }

                channel.addHeader(auth);
            }

            sub = new Subscription();
            sub.setStatus(SubscriptionStatus.REQUESTED);
            sub.setCriteria(Constants.SUBSCRIPTION_CRITERIA);
            sub.setChannel(channel);
            openElisClient.create(sub);
        } else {
            boolean endpointChanged = !endpoint.equals(sub.getChannel().getEndpoint());
            if (endpointChanged) {
                sub.getChannel().setEndpoint(endpoint);
            }

            String existingAuth = null;
            if (sub.getChannel().hasHeader()) {
                existingAuth = sub.getChannel().getHeader().get(0).getValue();
            }

            boolean authChanged;
            if ((auth == null && existingAuth != null) || (auth != null && existingAuth == null)) {
                authChanged = true;
                if (auth == null && existingAuth != null) {
                    if (log.isDebugEnabled()) {
                        log.debug("Removing authentication header");
                    }

                    sub.getChannel().getHeader().clear();
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("Adding authentication header to existing subscription");
                    }

                    sub.getChannel().addHeader(auth);
                }
            } else {
                authChanged = !auth.equals(existingAuth);
                if (authChanged) {
                    if (log.isDebugEnabled()) {
                        log.debug("Updating authentication header");
                    }

                    sub.getChannel().getHeader().clear();
                    sub.getChannel().addHeader(auth);
                }
            }

            if (endpointChanged | authChanged) {
                openElisClient.update(sub);
            }
        }
    }

    private String getAuthHeader() {
        final String user = odooClient.getUsername();
        if (StringUtils.isNotBlank(user)) {
            final byte[] authBytes = (user + ":" + new String(odooClient.getPassword())).getBytes(UTF_8);
            return "Authorization: Basic " + Base64.getEncoder().encodeToString(authBytes);
        }

        return null;
    }

}
