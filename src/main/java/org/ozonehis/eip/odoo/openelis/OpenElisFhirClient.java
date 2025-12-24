package org.ozonehis.eip.odoo.openelis;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
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

}
