package com.cocroachden.scheduler.domain;

import java.util.HashMap;
import java.util.Map;

//@Service
public final class EnglishVocabulary implements Vocabulary {

    private final static Map<String, String> VOCABULARY = new HashMap<>();

    static {
        VOCABULARY.put(
                "Schedule Instructions",
                """
                        Assign employee availabilities by using below symbols:
                        
                        # Shift symbols:
                        D = Day shift
                        N = Night Shift
                        O - Off
                        
                        # Modifiers:
                        ! = Not
                        + = Desirable
                        - = Undesirable
                        (Modifiers are to be used before shift symbols, e.g. !N. Modifiers do not work in combination with 'O'(OFF) shift symbol and will be ignored.)
                        
                        # Examples:
                        'D' = 'Requires day shift'.
                        '!D' = 'Unavailable for day shift'.
                        '+D' = 'Prefers day shift'.
                        '-D' = 'Undesired day shift'.
                        """
        );
    }

    @Override
    public String translate(String word) {
        return VOCABULARY.getOrDefault(word, word);
    }

    @Override
    public Boolean supports(final String language) {
        return language.equalsIgnoreCase("en");
    }
}
