package com.meituan.robust.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ZipUtils {
    public static final int BUFFER_SIZE         = 4096 * 1;

    public static final int MAX_EXTRACT_ATTEMPTS = 2;

    public static boolean extract(ZipFile zipFile, ZipEntry entryFile, File extractTo) throws IOException {
        int numAttempts = 0;
        boolean isExtractionSuccessful = false;
        while (numAttempts < MAX_EXTRACT_ATTEMPTS && !isExtractionSuccessful) {
            numAttempts++;
            InputStream is = null;
            OutputStream os = null;
            System.out.println("try Extracting " + extractTo.getPath());
            try {
                is = new BufferedInputStream(zipFile.getInputStream(entryFile));
                os = new BufferedOutputStream(new FileOutputStream(extractTo));

                byte[] buffer = new byte[BUFFER_SIZE];
                int length = 0;
                while ((length = is.read(buffer)) > 0) {
                    os.write(buffer, 0, length);
                }
            } finally {
                IOHelper.closeQuietly(os);
                IOHelper.closeQuietly(is);
            }
        }

        return isExtractionSuccessful;
    }
}
