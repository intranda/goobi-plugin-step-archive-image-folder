---
title: Archivierung von Bildordnern
identifier: intranda_step_archiveimagefolder
published: true
description: Goobi Step Plugin für das Kopieren von Bildordnern auf einen externen Speicher
---
## Einführung
Dieses Step Plugin für Goobi workflow kopiert Bildordner auf einen externen Speicher, der mittels sftp (ssh) angebunden ist und legt eine Datei an, die Goobi workflow dazu veranlasst eine Warnung im Vorgang anzuzeigen. Die Warnung wird in den Vorgangsdetails sowie im Metadateneditor angezeigt.


## Installation
Das Plugin besteht aus der folgenden Datei:

```bash
goobi_plugin_step_archiveimagefolder-base.jar
```

Diese Datei muss in dem richtigen Verzeichnis installiert werden, so dass diese nach der Installation an folgendem Pfad vorliegt:

```bash
/opt/digiverso/goobi/plugins/step/plugin_intranda_step_archiveimagefolder-base.jar
```

Daneben gibt es eine Konfigurationsdatei, die an folgender Stelle liegen muss:

```bash
/opt/digiverso/goobi/config/plugin_intranda_step_archiveimagefolder.xml
```


## Überblick und Funktionsweise
Zur Inbetriebnahme des Plugins muss dieses für einen oder mehrere gewünschte automatische Aufgaben im Workflow aktiviert werden. Dies erfolgt wie im folgenden Screenshot aufgezeigt durch Auswahl des Plugins `intranda_step_archiveimagefolder` aus der Liste der installierten Plugins.

![Integration des Plugins in den Workflow](screen1_de.png)

Das Plugin kopiert die Dateien aus dem konfigurierten Ordner auf den SSH-Server, schreibt eine XML-Datei mit den Infos wo die Dateien liegen in den Vorgangsordner und löscht (wenn so konfiguriert) danach die Bilder auf dem Goobi-Speicher und schließt den Schritt.

Die Bilder können danach mit dem Plugin `goobi-plugin-administration-restorearchivedimagefolder` wieder hergestellt werden.


## Konfiguration
Die Konfiguration des Plugins erfolgt über die Konfigurationsdatei `plugin_intranda_step_archiveimagefolder.xml` und kann im laufenden Betrieb angepasst werden. Im folgenden ist eine beispielhafte Konfigurationsdatei aufgeführt:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<config_plugin>
        <!--
        order of configuration is:
          1.) project name and step name matches
          2.) step name matches and project is *
          3.) project name matches and step name is *
          4.) project name and step name are *
	-->

    <config>
        <!-- which projects to use for (can be more then one, otherwise use *) -->
        <project>*</project>
        <step>*</step>

        <!-- the folder to archive -->
        <folder>master</folder>
        <!-- delete local files after copying and close step -->
        <deleteAndCloseAfterCopy>false</deleteAndCloseAfterCopy>
        <method>
            <name>SSH</name>
            <host>bandspeicher.intranda.com</host>
            <user>intranda</user>
        </method>
    </config>

</config_plugin>
```

Für die Authentifizierung am ssh-Server wird an den üblichen Stellen (`$USER_HOME/.ssh`) nach public keys gesucht. Andere Authentifizierungsmethoden wie username/password sind bisher nicht vorgesehen.  
Die Einstellung `<deleteAndCloseAfterCopy>false</deleteAndCloseAfterCopy>` ist für den Fall gedacht, dass die Dateien auf dem SSH-Server erst einmal in einem Buffer gespeichert und danach noch auf ein Band geschrieben werden. In diesem Fall kann der Arbeitsschritt offen bleiben und durch einen Callback des Bandspeicher-Systems geschlossen werden. Der Callback ist bisher für keinen Bandspeicher implementiert, lässt sich jedoch durch die Standard Goobi workflow REST API abbilden.
