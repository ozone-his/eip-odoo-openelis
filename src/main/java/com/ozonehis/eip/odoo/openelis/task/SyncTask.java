/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odoo.openelis.task;

import com.ozonehis.eip.odoo.openelis.Constants;
import com.ozonehis.eip.odoo.openelis.LocalDateTimeUtils;
import com.ozonehis.eip.odoo.openelis.SyncUtils;
import com.ozonehis.eip.odoo.openelis.fhir.OdooFhirClient;
import com.ozonehis.eip.odoo.openelis.fhir.OpenElisFhirClient;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static com.ozonehis.eip.odoo.openelis.Constants.PROP_DELAY;
import static com.ozonehis.eip.odoo.openelis.Constants.PROP_INITIAL_DELAY;

@Slf4j
public class SyncTask {

    private TimestampStore timestampStore;

    private OpenElisFhirClient openElisClient;

    private OdooFhirClient odooClient;

    @Value("${" + Constants.PROP_SYNC_OVERLAP + "}")
    private long overlap;

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

    public void sync(Class<? extends DomainResource> resourceType) {
        LocalDateTime timestamp = LocalDateTimeUtils.getCurrentTime();
        //TODO Should we rollback by a few seconds to close any gaps in case there were uncommitted changes
        //during the last poll?
        LocalDateTime since = timestampStore.getTimestamp(resourceType);
        if (since == null) {
            since = LocalDateTime.of(1970, 1, 1, 0, 0, 0);
        } else {
            since = since.minus(overlap, ChronoUnit.MILLIS);
        }

        List<? extends DomainResource> resources = openElisClient.getModifiedResources(resourceType, since);
        if (log.isDebugEnabled()) {
            log.debug("Found {} {} resources to sync: ", resources.size(), resourceType.getSimpleName());
        }

        resources.parallelStream().forEach(r -> {
            //To minimize duplication, skip
            if (!SyncUtils.skip(r)) {
                odooClient.update(r);
            } else if (log.isDebugEnabled()) {
                log.debug("Skipping resource {}/{} lastUpdated at {}", r.fhirType(), r.getIdPart(), SyncUtils.getLastUpdatedTimeStamp(r));
            }
        });

        timestampStore.update(timestamp, resourceType);
        SyncUtils.clearLastUpdatedTimestamps();
    }

}
