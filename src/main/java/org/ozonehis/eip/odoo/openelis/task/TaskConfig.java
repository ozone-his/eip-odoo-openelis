/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.ozonehis.eip.odoo.openelis.task;

import org.ozonehis.eip.odoo.openelis.fhir.OdooFhirClient;
import org.ozonehis.eip.odoo.openelis.fhir.OpenElisFhirClient;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
public class TaskConfig {

    @Bean
    public SyncTask SyncTask(TimestampStore timestampStore, OpenElisFhirClient openElisClient, OdooFhirClient odooClient) {
        return new SyncTask(timestampStore, openElisClient, odooClient);
    }

}
