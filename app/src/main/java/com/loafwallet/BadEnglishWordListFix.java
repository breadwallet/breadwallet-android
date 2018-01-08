package com.loafwallet;

public final class BadEnglishWordListFix {

    /**
     * When the phrase contains the bad words from before, we replace the correct words with the bad words again.
     */
    public static void restoreBadWordsToDictionaryIffPresentInPhrase(String phrase, final String[] cleanWordList) {
        if (phrase.contains("mTitle")) {
            replaceWord(cleanWordList, "title", "mTitle");
        }
        if (phrase.contains("mMessage")) {
            replaceWord(cleanWordList, "message", "mMessage");
        }
    }

    /**
     * Phrases are lower cased, but these bad words must have their case restored.
     */
    public static String restoreCaseOnAnyBadWords(String phrase) {
        return phrase.replace("mtitle", "mTitle").replace("mmessage", "mMessage");
    }

    /**
     * To allow backward support for two bad English words that were in the word list.
     *
     * @param word
     * @return True iff the word is one of the bad words issued in the past
     */
    public static boolean isBadEnglishWord(final String word) {
        return "mtitle".equals(word) || "mmessage".equals(word);
    }

    private static void replaceWord(final String[] cleanWordList, final String find, final String replace) {
        for (int i = 0; i < cleanWordList.length; i++) {
            if (find.equals(cleanWordList[i])) {
                cleanWordList[i] = replace;
                return;
            }
        }
    }
}