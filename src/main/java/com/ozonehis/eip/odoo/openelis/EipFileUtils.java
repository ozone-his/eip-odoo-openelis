/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odoo.openelis;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Paths;
import org.apache.commons.io.FileUtils;

public class EipFileUtils {

    /**
     * @throws IOException
     * @see FileUtils#openInputStream(File)
     */
    public static FileInputStream openInputStream(File file) throws IOException {
        return FileUtils.openInputStream(file);
    }

    /**
     * @throws IOException
     * @see FileUtils#openOutputStream(File)
     */
    public static FileOutputStream openOutputStream(File file) throws IOException {
        return FileUtils.openOutputStream(file);
    }

    /**
     * Creates a new file instance for the specified file path.
     *
     * @param fileName the name of the file to be created
     * @return a File object representing the file
     * @throws IOException
     */
    public static File createFile(String fileName) {
        return new File(fileName);
    }

    /**
     * @see Paths#get(String, String...)
     */
    public static File get(String first, String... more) {
        return Paths.get(first, more).toFile();
    }
}
