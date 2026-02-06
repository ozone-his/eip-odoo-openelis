/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odoo.openelis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import org.junit.jupiter.api.Test;

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
