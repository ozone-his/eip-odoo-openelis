package com.ozonehis.eip.odoo.openelis.task;

import com.ozonehis.eip.odoo.openelis.EipFileUtils;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.powermock.reflect.Whitebox;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Properties;

@ExtendWith(MockitoExtension.class)
public class TimestampStoreTest {

    private static MockedStatic<EipFileUtils> mockEipFileUtils;

    private TimestampStore store;

    @BeforeEach
    public void setUp() {
        mockEipFileUtils = Mockito.mockStatic(EipFileUtils.class);
        store = new TimestampStore();
    }

    @AfterEach
    public void tearDown() {
        mockEipFileUtils.close();
    }

    @Test
    public void getTimestamp_shouldReturnNullWhenGetTimestampIsCalledAndNoTimestampFound() {
        Whitebox.setInternalState(store, "props", new Properties());

        LocalDateTime result = store.getTimestamp(Patient.class);

        Assertions.assertNull(result);
    }

    @Test
    public void getTimestamp_shouldReturnTimestampWhenGetTimestampIsCalledAndTimestampFound() {
        final String ts = "2021-01-01T00:00:00.000-06:00";
        LocalDateTime expected = ZonedDateTime.parse(ts).toLocalDateTime();
        Properties props = new Properties();
        props.put(Patient.class.getSimpleName(), ts);
        Whitebox.setInternalState(store, "props", props);

        LocalDateTime result = store.getTimestamp(Patient.class);

        Assertions.assertEquals(expected, result);
    }

}
