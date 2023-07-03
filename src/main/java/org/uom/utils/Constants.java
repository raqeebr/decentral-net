package org.uom.utils;

import java.util.Set;

public final class Constants {
    private Constants() {
    }

    public static class Metadata {
        public static String NODE_PREFIX = "dec";

        public static Set<String> FILE_NAMES = Set.of(
            "Adventures of Tintin",
            "Jack and Jill",
            "Glee",
            "The Vampire Diaries",
            "King Arthur",
            "Windows XP",
            "Harry Potter",
            "Kung Fu Panda",
            "Lady Gaga",
            "Twilight",
            "Windows 8",
            "Mission Impossible",
            "Turn Up The Music",
            "Super Mario",
            "American Pickers",
            "Microsoft Office 2010",
            "Happy Feet",
            "Modern Family",
            "American Idol",
            "Hacking for Dummies"
        );

        public static Set<String> IGNORED_RESPONSES = Set.of(
            "LEAVEOK",
            "UNROK"
        );

        public static int FILES_COUNT = 3;
        public static int MAX_HOPS = 3;
    }

    public static class Commands {
        public static String MSG_FMT = "%d %s";
        public static String REG = "REG %s %d %s";
        public static String UNREG = "UNREG %s %d %s";
        public static String JOIN = "JOIN %s %d";
        public static String LEAVE = "LEAVE %s %d";
        public static String SEARCH = "SER %s %d %s %d";
    }

    public static class CommandResponses {
        public static final String SEROK = "SEROK %d %s %d %d %s";
        public static String JOINOK = "JOINOK %d";
    }

    public static class MessageTypes {
        public static String REGOK = "REGOK";
        public static String UNROK = "UNROK";
        public static String JOIN = "JOIN";
        public static String JOINOK = "JOINOK";
        public static String LEAVE = "LEAVE";
        public static String LEAVEOK = "LEAVEOK";
        public static String SEARCH = "SER";
        public static String SEROK = "SEROK";
    }
}
