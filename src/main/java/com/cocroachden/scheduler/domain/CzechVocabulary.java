package com.cocroachden.scheduler.domain;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public final class CzechVocabulary implements Vocabulary {

    private final static Map<String, String> VOCABULARY = new HashMap<>();

    static {
        VOCABULARY.put("Schedule", "Rozvrh");
        VOCABULARY.put("Configuration", "Nastaveni");
        VOCABULARY.put("Start", "Start");
        VOCABULARY.put("End", "Konec");
        VOCABULARY.put("Day", "Den");
        VOCABULARY.put("Night", "Noc");
        VOCABULARY.put("Off", "Volno");
        VOCABULARY.put("Negative", "Zapor");
        VOCABULARY.put("Desirable", "Chteny");
        VOCABULARY.put("Result", "Vysledek");
        VOCABULARY.put("Assignment", "Zadani");
        VOCABULARY.put("Please enter start date in format D.M.YY", "Zadejte pocatecni datum ve formatu D.M.YY");
        VOCABULARY.put("Please enter end date in format D.M.YY", "Zadejte konecne datum ve formatu D.M.YY");
        VOCABULARY.put("Please enter number of employees", "Zadejte pocet zamestnancu v rozvrhu");
        VOCABULARY.put("File %s already exists. Do you want to overwrite it? Yes/No", "Soubor %s uz existuje. Chcete ho prepsat? Ano/Ne");
        VOCABULARY.put("Done!", "Hotovo!");
        VOCABULARY.put("Please enter yes or no", "Prosim zadej 'ano' nebo 'ne'");
        VOCABULARY.put("Undesirable", "Nechteny");
        VOCABULARY.put("Name", "Jmeno");
        VOCABULARY.put("Ideal shift count", "Idealni pocet smen");
        VOCABULARY.put("Minimum shift count", "Minimalni pocet smen");
        VOCABULARY.put("D", "D");
        VOCABULARY.put("N", "N");
        VOCABULARY.put("O", "V");
        VOCABULARY.put("Yes", "Ano");
        VOCABULARY.put("yes", "ano");
        VOCABULARY.put("No", "Ne");
        VOCABULARY.put("ne", "ne");
        VOCABULARY.put("Employee", "Zamestnanec");
        VOCABULARY.put("Employee1", "Zamestnanec1");
        VOCABULARY.put("Employee2", "Zamestnanec2");
        VOCABULARY.put("Required employee count on day shift", "Pocet lidi na denni");
        VOCABULARY.put("Required employee count on night shift", "Pocet lidi na nocni");
        VOCABULARY.put("Max shift count in a row", "Maximum smen v rade");
        VOCABULARY.put("Max shift count in a week", "Maximum smen v tydnu");
        VOCABULARY.put(
                "Schedule Instructions",
                """
                        Pouzij symboly nize k vytvoreni pozadavku v rozvrhu:
                        
                        # Symboly smen:
                        D = Denni
                        N = Nocni
                        V - Volno
                        
                        # Modifikatory:
                        ! = Negativ
                        + = Chtena
                        - = Nechtena
                        (Modifikatory se pouzivaji pred symboly smen, napriklad '!N'. Modifikatory nefunguji v kombinaci se symbolem pro volno 'V' a budou ignorovany.)
                        
                        # Priklady:
                        'D' = 'Vyzadovana denni smena'.
                        '!D' = 'Nedostupny pro denni smenu'.
                        '+D' = 'Preferuje denni smenu'.
                        '-D' = 'Radeji bez nocni smeny'.
                        """
        );
        VOCABULARY.put("Monday", "Pondeli");
        VOCABULARY.put("Tuesday", "Utery");
        VOCABULARY.put("Wednesday", "Streda");
        VOCABULARY.put("Thursday", "Ctvrtek");
        VOCABULARY.put("Friday", "Patek");
        VOCABULARY.put("Saturday", "Sobota");
        VOCABULARY.put("Sunday", "Nedele");
        VOCABULARY.put("Count of D", "Pocet D");
        VOCABULARY.put("Count of N", "Pocet N");
        VOCABULARY.put("Count of V", "Pocet V");
        VOCABULARY.put("Total", "Celkem");
        VOCABULARY.put("Summary", "Shrnuti");
        VOCABULARY.put("Terminating generation.", "Generovani preruseno.");
    }

    @Override
    public String translateFromEn(String word) {
        return VOCABULARY.getOrDefault(word, "--translation not found--");
    }

    @Override
    public String translateToEn(final String word) {
        return VOCABULARY.entrySet().stream()
                         .filter(e -> e.getValue().equalsIgnoreCase(word))
                         .map(Map.Entry::getKey)
                         .findAny()
                         .orElse("--translation not found--");
    }

    @Override
    public Boolean supports(final String language) {
        return language.equalsIgnoreCase("cz");
    }
}
