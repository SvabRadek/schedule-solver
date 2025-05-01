package com.cocroachden.scheduler.domain;

public sealed interface Vocabulary permits CzechVocabulary, EnglishVocabulary {
    String translate(String word);
    Boolean supports(String language);
}
