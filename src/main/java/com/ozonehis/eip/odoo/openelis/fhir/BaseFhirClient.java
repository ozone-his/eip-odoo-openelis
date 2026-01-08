/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odoo.openelis.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DomainResource;

/**
 * Base class for classes that interact with a FHIR server.
 */
@Slf4j
public abstract class BaseFhirClient {

    @Getter
    private String sourceName;

    protected FhirContext fhirContext;

    private IGenericClient fhirClient;

    public BaseFhirClient(String sourceName) {
        this.sourceName = sourceName;
    }

    /**
     * Gets the {@link IGenericClient} instance
     *
     * @return IGenericClient
     */
    protected IGenericClient getFhirClient() {
        if (fhirClient == null) {
            synchronized (this) {
                if (fhirClient == null) {
                    fhirContext = FhirContext.forR4();
                    fhirContext.getRestfulClientFactory().setConnectTimeout(30000);
                    fhirContext.getRestfulClientFactory().setConnectionRequestTimeout(120000);
                    fhirContext.getRestfulClientFactory().setSocketTimeout(120000);
                    fhirClient = createFhirClient();
                }
            }
        }

        return fhirClient;
    }

    /**
     * Fetches a resource from a fhir server with an identifier matching the specified external identifier.
     *
     * @param externalId the externalId to match
     * @return a fhir resource if a match is found otherwise null
     */
    public <T extends IBaseResource> T getByIdentifier(String externalId, Class<T> resourceType) {
        final String resource = resourceType.getSimpleName();
        if (log.isDebugEnabled()) {
            log.debug("Getting {} from {} with identifier: {}", resource, sourceName, externalId);
        }

        try {
            Bundle bundle = (Bundle) getFhirClient().search().forResource(resourceType)
                    .where(new TokenClientParam("identifier").exactly().identifier(externalId)).execute();
            if (bundle.getEntry().size() == 1) {
                if (log.isDebugEnabled()) {
                    log.debug("Found {} in {} with identifier: {}", resource, sourceName, externalId);
                }

                return (T) bundle.getEntry().get(0).getResource();
            } else if (bundle.getEntry().size() > 1) {
                throw new RuntimeException("Found multiple resources of type " + resource + " in " + sourceName
                        + " with external identifier " + externalId);
            }
        } catch (ResourceNotFoundException e) {
            //Ignore
        }

        if (log.isDebugEnabled()) {
            log.debug("No {} found in hcw@home with identifier: {}", resource, externalId);
        }

        return null;
    }

    /**
     * Creates the specified resource in the fhir server.
     *
     * @param resource the resource to create
     */
    public void create(DomainResource resource) {
        if (log.isDebugEnabled()) {
            log.debug("Creating {} in {}", resource.fhirType(), sourceName);
        }

        MethodOutcome outcome;
        try {
            outcome = getFhirClient().create().resource(resource).execute();
        } catch (Exception e) {
            throw new RuntimeException(getErrorMessage(e, resource.fhirType(), "create"));
        }

        if (!outcome.getCreated()) {
            throw new RuntimeException("Unexpected outcome " + outcome + " when creating " + resource.fhirType() + " in " + sourceName);
        }

        if (log.isDebugEnabled()) {
            log.debug("Successfully created {} in {}", resource.fhirType(), sourceName);
        }
    }

    /**
     * Updates the specified resource in the fhir server.
     *
     * @param resource the resource to update
     */
    public void update(DomainResource resource) {
        if (log.isDebugEnabled()) {
            log.debug("Updating {} in {}", resource.fhirType(), sourceName);
        }

        MethodOutcome outcome;
        try {
            outcome = getFhirClient().update().resource(resource).execute();
        } catch (Exception e) {
            throw new RuntimeException(getErrorMessage(e, resource.fhirType(), "update"));
        }

        if (outcome.getResponseStatusCode() != 200) {
            throw new RuntimeException("Unexpected outcome " + outcome + " when updating " + resource.fhirType() + " in " + sourceName);
        }

        if (log.isDebugEnabled()) {
            log.debug("Successfully updated {} in {}", resource.fhirType(), sourceName);
        }
    }

    protected String getErrorMessage(Exception e, String resourceName, String operation) {
        String msg = getServerErrorMessage(e);
        if (StringUtils.isBlank(msg)) {
            msg = "Failed to " + operation + " " + resourceName + " in " + sourceName;
        }

        return msg;
    }

    private String getServerErrorMessage(Exception e) {
        if (e instanceof BaseServerResponseException) {
            return ((BaseServerResponseException) e).getResponseBody();
        }

        return null;
    }

    /**
     * Gets the {@link IGenericClient} instance
     *
     * @return IGenericClient
     */
    protected abstract IGenericClient createFhirClient();

}
