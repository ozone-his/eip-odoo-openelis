package com.ozonehis.eip.odoo.openelis;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class SyncUtils {

    private static final Map<String, LocalDateTime> ID_LAST_UPDATED_MAP = Collections.synchronizedMap(new HashMap<>());

    /**
     * Saves the last updated timestamp for a given resource type and id.
     *
     * @param resourceType the type of the resource
     * @param id           the identifier of the resource
     * @param lastUpdated  the timestamp of the last update
     */
    public static void saveLastUpdated(String resourceType, String id, LocalDateTime lastUpdated) {
        ID_LAST_UPDATED_MAP.put(resourceType + id, lastUpdated);
    }

    /**
     * Retrieves the last updated timestamp for a given resource type and id.
     *
     * @param resourceType the type of the resource
     * @param id           the identifier of the resource
     * @return the last updated timestamp of the resource, or null if no entry exists
     */
    public static LocalDateTime getLastUpdated(String resourceType, String id) {
        return ID_LAST_UPDATED_MAP.get(resourceType + id);
    }

    /**
     * Clears all resource and lastUpdated timestamp entries.
     */
    public static void clearLastUpdatedCache() {
        ID_LAST_UPDATED_MAP.clear();
    }

}
