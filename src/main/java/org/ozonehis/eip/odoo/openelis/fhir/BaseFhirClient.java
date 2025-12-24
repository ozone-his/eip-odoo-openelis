/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.ozonehis.eip.odoo.openelis.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.DomainResource;

/**
 * Base class for classes that interact with a FHIR server.
 */
@Slf4j
public abstract class BaseFhirClient {

    private String serverName;

    protected FhirContext fhirContext;

    private IGenericClient fhirClient;

    public BaseFhirClient(String serverName) {
        this.serverName = serverName;
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
     * Creates the specified resource in the fhir server.
     *
     * @param resource the resource to create
     */
    public void create(DomainResource resource) {
        if (log.isDebugEnabled()) {
            log.debug("Creating {} in {}", resource.fhirType(), serverName);
        }

        MethodOutcome outcome;
        try {
            outcome = getFhirClient().create().resource(resource).execute();
        } catch (Exception e) {
            throw new RuntimeException(getErrorMessage(e, "create"));
        }

        if (!outcome.getCreated()) {
            throw new RuntimeException("Unexpected outcome " + outcome + " when creating invite in " + serverName);
        }

        if (log.isDebugEnabled()) {
            log.debug("Successfully created {} in {}", resource.fhirType(), serverName);
        }
    }

    protected String getErrorMessage(Exception e, String operation) {
        String msg = getServerErrorMessage(e);
        if (StringUtils.isBlank(msg)) {
            msg = "Failed to " + operation + " invite in " + serverName;
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
