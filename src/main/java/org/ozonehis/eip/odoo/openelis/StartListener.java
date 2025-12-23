/*
 * Copyright (C) Amiyul LLC - All Rights Reserved
 *
 * This source code is protected under international copyright law. All rights
 * reserved and protected by the copyright holder.
 *
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holder. If you encounter this file and do not have
 * permission, please contact the copyright holder and delete this file.
 */
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
