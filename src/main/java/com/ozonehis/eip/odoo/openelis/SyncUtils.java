package com.ozonehis.eip.odoo.openelis;

import com.jayway.jsonpath.JsonPath;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.DomainResource;

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
     * Clears all resource and lastUpdated timestamp entries.
     */
    public static void clearLastUpdatedTimestamps() {
        ID_LAST_UPDATED_MAP.clear();
    }

    /**
     * Determines whether a resource of the specified type, id and payload should be skipped based on its last updated timestamp.
     *
     * @param resourceType the type of the resource
     * @param id           the id of the resource
     * @param payload      the JSON payload of the resource
     * @return true if the resource should be skipped otherwise false.
     */
    public static boolean skip(String resourceType, String id, String payload) {
        if (payload != null) {
            return skip(resourceType, id, DateUtils.deserialize(JsonPath.read(payload, "meta.lastUpdated")));
        }

        //This is a deleted resource
        return ID_LAST_UPDATED_MAP.containsKey(resourceType + id) && getLastUpdated(resourceType, id) == null;
    }

    /**
     * Determines whether a given resource should be skipped based on its last updated timestamp.
     *
     * @param resource the resource
     * @return true if the resource should be skipped otherwise false.
     */
    public static boolean skip(DomainResource resource) {
        return skip(resource.fhirType(), resource.getIdPart(), getLastUpdatedTimeStamp(resource));
    }

    /**
     * Retrieves the last updated timestamp of the given resource.
     *
     * @param resource the resource object whose last updated timestamp is to be retrieved
     * @return the last updated timestamp as a LocalDateTime object
     */
    public static LocalDateTime getLastUpdatedTimeStamp(DomainResource resource) {
        return DateUtils.toLocalDateTime(resource.getMeta().getLastUpdated());
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
     * Determines whether a given resource should be skipped based on the specified last updated timestamp.
     *
     * @param resourceType the type of the resource
     * @param id           the id of the resource
     * @param lastUpdated  the resource lastUpdated date
     * @return true if the resource should be skipped otherwise false.
     */
    private static boolean skip(String resourceType, String id, LocalDateTime lastUpdated) {
        LocalDateTime previousLastUpdated = getLastUpdated(resourceType, id);
        if (previousLastUpdated != null) {
            return lastUpdated.isBefore(previousLastUpdated) || lastUpdated.equals(previousLastUpdated);
        }

        return false;
    }

}
