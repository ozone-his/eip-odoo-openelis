/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odoo.openelis;

import com.ozonehis.eip.odoo.openelis.fhir.OpenElisFhirClient;
import com.ozonehis.eip.odoo.openelis.task.TaskConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(TaskConfig.class)
public class Config {

    @Bean
    public StartListener startListener(OpenElisFhirClient openElisClient) {
        return new StartListener(openElisClient);
    }
}
