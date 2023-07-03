package org.uom.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileUtils {
    public static boolean hasMatchingFileName(String fileName, String query) {
        Pattern pattern = Pattern.compile(".*\\b" + query + "\\b.*", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(fileName);
        return matcher.find();
    }
}
