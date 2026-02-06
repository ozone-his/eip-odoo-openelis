package com.ozonehis.eip.odoo.openelis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import java.time.LocalDateTime;
import org.hl7.fhir.r4.model.DomainResource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the SyncUtils class, specifically for ensuring proper handling of the
 * save, skip, and clear operations related to resource timestamps.
 */
public class SyncUtilsTest {

    private static final String RESOURCE_TYPE = "Patient";

    @AfterEach
    void tearDown() {
        SyncUtils.clearLastUpdatedTimestamps();
    }

    @Test
    public void skip_shouldReturnFalseNullWhenPayloadIsNull() {
        assertFalse(SyncUtils.skip(RESOURCE_TYPE, "5", null));
    }

    @Test
    public void skip_shouldReturnFalseWhenPreviousLastUpdatedTimestampIsNull() {
        String payload = "{\"meta\": {\"lastUpdated\": \"2019-06-30T18:25:43.511Z\"}}";
        assertFalse(SyncUtils.skip(RESOURCE_TYPE, "5", payload));
    }

    @Test
    public void skip_shouldReturnFalseIfLastUpdatedIsAfterPreviousTimestamp() {
        final String id = "5";
        String payload = "{\"meta\": {\"lastUpdated\": \"2019-06-30T18:25:43.511Z\"}}";
        SyncUtils.saveLastUpdated(RESOURCE_TYPE, id, DateUtils.deserialize("2019-06-30T18:25:43.510Z"));
        assertFalse(SyncUtils.skip(RESOURCE_TYPE, id, payload));
    }

    @Test
    public void skip_shouldReturnTrueIfLastUpdatedTimestampIsBeforePreviousOne() {
        final String id = "5";
        SyncUtils.saveLastUpdated(RESOURCE_TYPE, id, DateUtils.deserialize("2019-06-30T18:25:43.512Z"));
        String payload = "{\"meta\": {\"lastUpdated\": \"2019-06-30T18:25:43.511Z\"}}";
        assertTrue(SyncUtils.skip(RESOURCE_TYPE, id, payload));
    }

    @Test
    public void skip_shouldReturnTrueIfLastUpdatedTimestampIsTheSameAsPreviousOne() {
        final String id = "5";
        final String lastUpdated = "2019-06-30T18:25:43.511Z";
        String payload = "{\"meta\": {\"lastUpdated\": \"" + lastUpdated + "\"}}";
        SyncUtils.saveLastUpdated(RESOURCE_TYPE, id, DateUtils.deserialize(lastUpdated));
        assertTrue(SyncUtils.skip(RESOURCE_TYPE, id, payload));
    }

    @Test
    public void skip_shouldReturnTrueForAProcessedDeletedEvent() {
        final String id = "5";
        SyncUtils.saveLastUpdated(RESOURCE_TYPE, id, null);
        assertTrue(SyncUtils.skip(RESOURCE_TYPE, id, null));
    }

    @Test
    public void clearLastUpdatedTimestamps_shouldClearAllSavedTimestamps() {
        String payload1 = "{\"meta\": {\"lastUpdated\": \"2022-01-01T10:00:00.000Z\"}}";
        SyncUtils.saveLastUpdated(RESOURCE_TYPE, "1", DateUtils.deserialize("2022-01-01T10:00:00.001Z"));
        SyncUtils.saveLastUpdated(RESOURCE_TYPE, "2", null);
        assertTrue(SyncUtils.skip(RESOURCE_TYPE, "1", payload1));
        assertTrue(SyncUtils.skip(RESOURCE_TYPE, "2", null));

        SyncUtils.clearLastUpdatedTimestamps();

        assertFalse(SyncUtils.skip(RESOURCE_TYPE, "1", payload1));
        assertFalse(SyncUtils.skip(RESOURCE_TYPE, "2", null));
    }

    @Test
    public void getLastUpdatedTimestamp_shouldReturnLastUpdatedValue() {
        final String lastUpdatedStr = "2022-01-01T10:00:00.000";
        LocalDateTime expected = LocalDateTime.parse(lastUpdatedStr);
        String jsonResource = "{\"resourceType\":\"Patient\", \"id\":\"test\", \"meta\":{\"lastUpdated\":\""
                + lastUpdatedStr + "\"}}";
        DomainResource resource =
                (DomainResource) FhirContext.forR4().newJsonParser().parseResource(jsonResource);
        assertEquals(expected, SyncUtils.getLastUpdatedTimeStamp(resource));
    }
}
