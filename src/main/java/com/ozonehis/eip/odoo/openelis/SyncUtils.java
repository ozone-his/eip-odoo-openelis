package com.ozonehis.eip.odoo.openelis;

import com.jayway.jsonpath.JsonPath;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Slf4j
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
        if (log.isDebugEnabled()) {
            log.debug("Saving lastUpdated timestamp for {}/{} as {}", resourceType, id, lastUpdated);
        }

        ID_LAST_UPDATED_MAP.put(resourceType + id, lastUpdated);
    }

    /**
     * Retrieves the last updated timestamp for a given resource type and id.
     *
     * @param resourceType the type of the resource
     * @param id           the identifier of the resource
     * @return the last updated timestamp of the resource, or null if no entry exists
     */
    private static LocalDateTime getLastUpdated(String resourceType, String id) {
        return ID_LAST_UPDATED_MAP.get(resourceType + id);
    }

    /**
     * Clears all resource and lastUpdated timestamp entries.
     */
    public static void clearLastUpdatedTimestamps() {
        ID_LAST_UPDATED_MAP.clear();
    }

    /**
     * Determines whether an event for a given resource should be skipped based on its last updated timestamp.
     *
     * @param resourceType the type of the resource
     * @param id           the id of the resource
     * @param payload      the JSON payload of the resource
     * @return true if the event should be skipped otherwise false.
     */
    public static boolean skip(String resourceType, String id, String payload) {
        if (payload != null) {
            LocalDateTime previousLastUpdated = getLastUpdated(resourceType, id);
            if (previousLastUpdated != null) {
                LocalDateTime lastUpdated = DateUtils.deserialize(JsonPath.read(payload, "meta.lastUpdated"));
                return lastUpdated.isBefore(previousLastUpdated) || lastUpdated.equals(previousLastUpdated);
            }
        }

        return false;
    }

}
