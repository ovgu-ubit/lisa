# LISA

Library Inventory and Statistics Application.
---------------------------------------------

Version: 1.01
-------------

Das neue Webtool erlaubt ein schnelles Auslösen und Bearbeiten von Bestandspflegearbeiten. Insbesondere die automatisch erstellte Ausleihstatistik vereinfacht den bisherigen Prozess. Nachfolgend werden wichtige Hinweise, die technische Anwendung und das bibliothekarische Vorgehen betreffend, kurz erläutert.

Technische Hinweise
-------------------

**Eingabe eines Titels per PPN, Signatur oder Barcode**

* Als Standard ist die Auto-Erkennung ausgewählt, Sie können aber auch stattdessen im Dropdown-Menü im Suchfeld rechts PPN, Signatur oder Barcode auswählen.
* Die PPN (Pica-Produktionsnummer) ist vollständig inkl. Prüfziffer und ggf. führender Null einzugeben, d.h. so, wie sie bspw. im OPAC zu finden ist.
* Die Signatur ist bitte inkl. Sonder- und Leerzeichen bis ausschließlich zum Doppelpunkt einzugeben. Beispiele:
  * Um das Exemplar 2016.02770:1 anzuzeigen, geben Sie bitte "2016.02770" ein.
  * Um das Exemplar 1953 a 102(10):4 anzuzeigen, geben Sie bitte "1953 a 102(10)" ein.
  * Um das Exemplar 1953 a 103/1(3):3 anzuzeigen, geben Sie bitte "1953 a 103/1(3)" ein.
* Barcodes können komplett eingegeben werden oder per Barcodescanner erfasst werden.

**Eingabe mehrerer Titel/Auflagen**

* Durch Klicken des grün-hinterlegten Plus‘ können mehrere Titel/Auflagen in einem Bestandspflegezettel erfasst werden.
* Durch Klicken des rot-hinterlegten Minus‘ kann eine einzelne Suche wieder entfernt werden.

**Statistik**

* Nach Klick auf den Button "Suchen" werden für alle gefundenen Titel die Titel- und Bandinformationen, zusammen mit der Statistik, angezeigt.
* Über jedem Titel wird dabei der Suchbegriff angezeigt, der zu diesem Ergebnis geführt hat. Zum Beispiel:
  * Suche: "Signatur = 1998 a 18590"
* Die Ausleihstatistik wird als Tabelle angezeigt, wobei jede Zeile für ein Exemplar und jede Spalte für ein Jahr steht. In den entsprechenden Zellen wird dann die Anzahl der Entleihungen dieses Exemplars in diesem Jahr angezeigt.

**Maximale Jahre anzeigen**

* Oberhalb des Suchfelds können Sie angeben, wie weit in die Vergangenheit die detaillierte Ausleihstatistik zurückreichen soll.
* Als Standardwert sind 10 Jahre eingestellt.
* Alle Werte, die weiter zurückliegen als die angegebenen Jahre, werden kumuliert ausgegeben (z.B. in einer Spalte "<=2012"). So werden immer alle Entleihungen in der Tabelle erfasst. Bitte beachten Sie, dass alle Entleihungen, die bis 2007 im System erfasst wurden, auf das Jahr 2007 summiert werden.

**Optionen**

* __Hilfe__: Verlinkt auf dieses Dokument.
* __Zurücksetzen__: Ihre Suchanfragen werden gelöscht und Sie können mit dem Erstellen eines neuen Bestandspflegezettels beginnen.
* __Permalink__: Ähnlich dem Zitierlink im OPAC erstellt diese Funktion einen Link, dessen direkte Eingabe im Browser zu Ihrer Suche führt, um die Ergebnisse digital zu teilen. Der Link ist automatisch in der Zwischenablage und kann per "Einfügen" oder STRG+V in andere Anwendungen eingefügt werden.
* __Herunterladen__: Erstellt den eigentlichen Bestandspflegezettel als druckbares PDF-Dokument. Wir empfehlen, aufgrund der Dokumentierbarkeit, die PDF zur weiteren Bearbeitung und Weitergabe zu nutzen.
* __Ausblenden__: Unterdrückt die Anzeige der Ausleihstatistik. Bei Bedarf kann diese wieder mittels "Einblenden" angezeigt werden.
