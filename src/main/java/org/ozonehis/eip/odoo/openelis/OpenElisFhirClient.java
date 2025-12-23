/*
 * Copyright (C) Amiyul LLC - All Rights Reserved
 *
 * This source code is protected under international copyright law. All rights
 * reserved and protected by the copyright holder.
 *
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holder. If you encounter this file and do not have
 * permission, please contact the copyright holder and delete this file.
 */
package org.ozonehis.eip.odoo.openelis;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import org.springframework.beans.factory.annotation.Value;

public class OpenElisFhirClient extends BaseFhirClient {

    @Value("${eip.openelis.url}")
    private String baseUrl;

    @Value("${eip.openelis.username}")
    private String email;

    @Value("${eip.openelis.password}")
    private char[] password;

    private FhirContext fhirContext;

    private IGenericClient fhirClient;

    public OpenElisFhirClient() {
        super("OpenELIS");
    }

    @Override
    protected IGenericClient getFhirClient() {
        if (fhirClient == null) {
            synchronized (this) {
                if (fhirClient == null) {
                    fhirContext = FhirContext.forR4();
                    fhirContext.getRestfulClientFactory().setConnectTimeout(30000);
                    fhirContext.getRestfulClientFactory().setConnectionRequestTimeout(120000);
                    fhirContext.getRestfulClientFactory().setSocketTimeout(120000);
                    fhirContext.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
                    fhirClient = fhirContext.newRestfulGenericClient(baseUrl + "/fhir");
                    //TODO Register authenticator
                }
            }
        }

        return fhirClient;
    }

}
