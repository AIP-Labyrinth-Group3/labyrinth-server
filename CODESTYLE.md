# Codestyle Guidelines

</br>

Namenskonventionen:
-   Namen für Variablen, Funktionen, Klassen, usw. in englischer Sprache
-   Aussagekräftige Namen verwenden
-   In Klassennamen keine Verben verwenden
-   Klassennamen mit einem Großbuchstaben beginnen (CamelCase bei mehreren Wörtern)
-   Variablen- und Methodennamen mit einem Kleinbuchstaben beginnen (CamelCase bei mehreren Wörtern)
-   Klassenkonstanten durchgehend mit Großbuchstaben (Unterstrich bei mehreren Wörtern)
-   Domain Language verwenden

Klassen/Funktionen:
-   Imports, die nicht benötigt werden, entfernen
-   Innerhalb einer Klasse die Stepdown Rule anwenden
    -   Funktionen in der Reihenfolge lesbar, wie sie verwendet werden
    -   Befehle in einer Funktion alle vom gleichen Abstraktionslevel
-   Code, der nicht benötigt wird, löschen und nicht nur auskommentieren

Kommentare:
-   Nur einsetzen, wenn wirklich benötigt

Formatierung:
-   Sinnvolle Trennung des Codes mit Leerzeilen (siehe Seite 2:
    Beispielaufbau einer Klasse)
    -   Bei vielen Imports und Variablendeklarationen können diese thematisch gruppiert und mit Leerzeilen getrennt werden
    -   Innerhalb von Funktionen den Code auch sinnvoll mit Leerzeilen strukturieren
    -   Zwischen Annotationen und Klassen/Funktionen keine Leerzeilen
-   Jedes File mit dem Auto Formatter von IntelliJ in der Default Einstellung formatieren (Strg+Alt+Shift+L oder Rechtsklick auf File -> Reformat Code)  

Error Handling:
-   Error Handling sinnvoll einsetzen (zu viel Error Handling macht den Code auch wieder komplex und unübersichtlich)
-   Wenn Exceptions, dann spezifische Exceptions verwenden mit aussagekräftigen Fehlermeldungen

</br>

Für hier nicht definierte Sachen und für nähere Infos bitte folgenden Link beachten:
- <https://javabeginners.de/Grundlagen/Code-Konventionen.php>

</br>
</br>
</br>

Beispielaufbau einer Klasse:
``` Java
package example;

import ...

public class Example {
    private int variable1;
    private int variable2;

    public Example() {
        ...
    }

    public function1() {
        ...
    }

    public function2() {
        ...
    }
}
```
