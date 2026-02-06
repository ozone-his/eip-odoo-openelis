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
