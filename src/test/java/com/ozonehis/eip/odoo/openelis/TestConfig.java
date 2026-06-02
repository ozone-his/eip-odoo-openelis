/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odoo.openelis;

import com.ozonehis.eip.odoo.openelis.fhir.OdooFhirClient;
import com.ozonehis.eip.odoo.openelis.fhir.OpenElisFhirClient;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;

public class TestConfig {

    @Bean
    public OdooFhirClient odooFhirClient() {
        return Mockito.mock(OdooFhirClient.class);
    }

    @Bean
    public OpenElisFhirClient openElisFhirClient() {
        return Mockito.mock(OpenElisFhirClient.class);
    }

    @Bean
    public PatientService serviceRequestPatientService(
            OdooFhirClient odooFhirClient, OpenElisFhirClient openElisFhirClient) {
        return Mockito.mock(PatientService.class);
    }
}
