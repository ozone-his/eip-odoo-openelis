/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.ozonehis.eip.odoo.openelis;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.BasicAuthInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OdooFhirClient extends BaseFhirClient {

    @Value("${eip.odoo.fhir.url}")
    private String serverUrl;

    @Value("${eip.odoo.fhir.username}")
    private String username;

    @Value("${eip.odoo.fhir.password}")
    private char[] password;

    public OdooFhirClient() {
        super("Odoo");
    }

    @Override
    protected IGenericClient createFhirClient() {
        IGenericClient fhirClient = fhirContext.newRestfulGenericClient(serverUrl + "/odoo/fhir/R4");
        fhirClient.registerInterceptor(new BasicAuthInterceptor(username, new String(password)));
        return fhirClient;
    }

}
