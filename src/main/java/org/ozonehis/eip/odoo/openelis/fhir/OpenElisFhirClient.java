package org.ozonehis.eip.odoo.openelis.fhir;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.StringClientParam;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Subscription;
import org.ozonehis.eip.odoo.openelis.Constants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static org.hl7.fhir.r4.model.Subscription.CRITERIA;
import static org.hl7.fhir.r4.model.Subscription.SP_PAYLOAD;
import static org.hl7.fhir.r4.model.Subscription.SP_TYPE;
import static org.hl7.fhir.r4.model.Subscription.SubscriptionChannelType.RESTHOOK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Component
@Slf4j
public class OpenElisFhirClient extends BaseFhirClient {

    @Value("${eip.openelis.url}")
    private String baseUrl;

    public OpenElisFhirClient() {
        super("OpenELIS");
    }

    @Override
    protected IGenericClient createFhirClient() {
        return fhirContext.newRestfulGenericClient(baseUrl + "/fhir");
    }

    public Subscription getSubscription() {
        if (log.isDebugEnabled()) {
            log.debug("Getting subscription from {}", getSourceName());
        }

        try {
            Bundle bundle = (Bundle) getFhirClient().search().forResource(Subscription.class)
                    .where(CRITERIA.matchesExactly().value(Constants.SUBSCRIPTION_CRITERIA))
                    .and(new StringClientParam(SP_TYPE).matchesExactly().value(RESTHOOK.name()))
                    .and(new StringClientParam(SP_PAYLOAD).matchesExactly().value(APPLICATION_JSON_VALUE));
            if (bundle.getEntry().size() == 1) {
                if (log.isDebugEnabled()) {
                    log.debug("Found subscription in {}", getSourceName());
                }

                return (Subscription) bundle.getEntry().get(0).getResource();
            } else if (bundle.getEntry().size() > 1) {
                throw new RuntimeException("Found multiple subscriptions of type in " + getSourceName());
            }
        } catch (ResourceNotFoundException e) {
            //Ignore
        }

        if (log.isDebugEnabled()) {
            log.debug("Subscription not found in {}", getSourceName());
        }

        return null;
    }

}
