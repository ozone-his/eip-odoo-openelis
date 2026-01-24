package com.ozonehis.eip.odoo.openelis;

import org.junit.jupiter.api.Test;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class DateUtilsTest {

    @Test
    void toLocalDateTime_shouldReturnNullWhenDateIsNull() {
        assertNull(DateUtils.toLocalDateTime(null));
    }

    @Test
    void toLocalDateTime_shouldReturnTheCorrectLocalDateTime() throws Exception {
        Date date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2019-06-30 18:25:43");
        LocalDateTime expected = LocalDateTime.of(2019, 6, 30, 18, 25, 43);
        assertEquals(expected, DateUtils.toLocalDateTime(date));
    }

}
