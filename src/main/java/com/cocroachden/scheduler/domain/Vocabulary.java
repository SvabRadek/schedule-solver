package com.cocroachden.scheduler.domain;

public sealed interface Vocabulary permits CzechVocabulary, EnglishVocabulary {
    String translateFromEn(String word);
    String translateToEn(String word);
    Boolean supports(String language);
}
