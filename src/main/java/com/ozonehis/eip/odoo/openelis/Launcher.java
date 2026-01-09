package com.ozonehis.eip.odoo.openelis;

import com.ozonehis.eip.odoo.openelis.fhir.OdooFhirClient;
import com.ozonehis.eip.odoo.openelis.fhir.OpenElisFhirClient;
import com.ozonehis.eip.odoo.openelis.task.TaskConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(TaskConfig.class)
public class Launcher {

    public static void main(String[] args) {
        SpringApplication.run(Launcher.class, args);
    }

    @Bean
    public StartListener startListener(OpenElisFhirClient openElisClient, OdooFhirClient odooClient) {
        return new StartListener(openElisClient, odooClient);
    }

}
