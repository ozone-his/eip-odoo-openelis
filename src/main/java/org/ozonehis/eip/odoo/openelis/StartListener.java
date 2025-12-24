package org.ozonehis.eip.odoo.openelis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

@Slf4j
public class StartListener {

    @EventListener(classes = {ContextRefreshedEvent.class})
    public void contextRefreshed() {
        if (log.isDebugEnabled()) {
            log.debug("Checking if subscription is added");
        }
    }

}
