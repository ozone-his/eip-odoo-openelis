/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.ozonehis.eip.odoo.openelis;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static java.time.ZoneId.systemDefault;

public class DateUtils {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

    /**
     * Serializes the specified date to a string including timezone component.
     *
     * @param date the date to serialize
     * @return the serialized date as a string
     */
    public static String serialize(LocalDateTime date) {
        return date.atZone(ZoneId.systemDefault()).format(DATE_FORMATTER);
    }

    /**
     * Deserializes the specified to a {@link LocalDateTime} instance based on the system timezone.
     *
     * @param date the date to deserialize
     * @return the deserialized {@link LocalDateTime} instance.
     */
    public static LocalDateTime deserialize(String date) {
        return ZonedDateTime.parse(date, DATE_FORMATTER).withZoneSameInstant(systemDefault()).toLocalDateTime();
    }

}
