package com.ozonehis.eip.odoo.openelis;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        String payload = "{\"meta\": {\"lastUpdated\": \"2019-06-30T18:25:43.511Z\"}}";
        SyncUtils.saveLastUpdated(RESOURCE_TYPE, id, DateUtils.deserialize("2019-06-30T18:25:43.512Z"));
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

}
