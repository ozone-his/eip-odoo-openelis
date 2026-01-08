/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.ozonehis.eip.odoo.openelis.task;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.ozonehis.eip.odoo.openelis.fhir.OdooFhirClient;
import org.ozonehis.eip.odoo.openelis.fhir.OpenElisFhirClient;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDateTime;
import java.util.List;

import static org.ozonehis.eip.odoo.openelis.Constants.PROP_DELAY;
import static org.ozonehis.eip.odoo.openelis.Constants.PROP_INITIAL_DELAY;

public class SyncTask {

    private TimestampStore timestampStore;

    private OpenElisFhirClient openElisClient;

    private OdooFhirClient odooClient;

    public SyncTask(TimestampStore timestampStore, OpenElisFhirClient openElisClient, OdooFhirClient odooClient) {
        this.timestampStore = timestampStore;
        this.openElisClient = openElisClient;
        this.odooClient = odooClient;
    }

    @Scheduled(initialDelayString = "${" + PROP_INITIAL_DELAY + "}", fixedDelayString = "${" + PROP_DELAY + "}")
    public void execute() {
        sync(Patient.class);
        sync(ServiceRequest.class);
    }

    public void sync(Class<? extends IBaseResource> resourceType) {
        LocalDateTime since = timestampStore.getTimestamp(resourceType);
        if (since == null) {
            since = LocalDateTime.of(1970, 1, 1, 0, 0, 0);
        }

        List<? extends IBaseResource> resources = openElisClient.getModifiedResources(resourceType, since);
        System.out.println("Resource count: " + resources.size());
    }

}
