package com.ozonehis.eip.odoo.openelis.fhir;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.StringClientParam;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import com.ozonehis.eip.odoo.openelis.Constants;
import com.ozonehis.eip.odoo.openelis.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.Subscription;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static org.hl7.fhir.instance.model.api.IAnyResource.SP_RES_LAST_UPDATED;
import static org.hl7.fhir.r4.model.Subscription.CRITERIA;
import static org.hl7.fhir.r4.model.Subscription.SP_PAYLOAD;
import static org.hl7.fhir.r4.model.Subscription.SP_TYPE;
import static org.hl7.fhir.r4.model.Subscription.SubscriptionChannelType.RESTHOOK;

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
                    .and(new StringClientParam(SP_TYPE).matchesExactly().value(RESTHOOK.toCode()))
                    .and(new StringClientParam(SP_PAYLOAD).matchesExactly().value(Constants.MEDIA_TYPE)).execute();
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

    /**
     * Fetches all resources from a fhir server modified since the specified time.
     *
     * @param resourceType the resource type to match
     * @param since        the date instance to compare against
     * @return list of modified resources
     */
    public <T extends DomainResource> List<T> getModifiedResources(Class<T> resourceType, LocalDateTime since) {
        final String resource = resourceType.getSimpleName();
        if (log.isDebugEnabled()) {
            log.debug("Getting all resource of type {} modified since {}", resource, since);
        }

        final String sinceStr = DateUtils.serialize(since);
        Bundle bundle = (Bundle) getFhirClient().search().forResource(resourceType)
                .where(new StringClientParam(SP_RES_LAST_UPDATED).matches().value("ge" + sinceStr)).execute();
        return (List) bundle.getEntry().stream().parallel().map(e -> e.getResource()).collect(Collectors.toList());
    }

}
