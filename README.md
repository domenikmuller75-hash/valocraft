# ValorantCraft (Prototyp, Minecraft 26.3, Fabric)

## Voraussetzungen
- Java 25 (JDK), Gradle 9.6.0 (nur einmal fuer den Wrapper)
- Fabric Loader 0.19.5+, Fabric API 0.161.0+26.3

## Bauen
    gradle wrapper --gradle-version 9.6.0
    ./gradlew build          (Windows: gradlew.bat build)
Die Mod liegt danach in build/libs/valorantcraft-0.1.0.jar -> in den mods-Ordner kopieren
(zusammen mit der Fabric API).

## Testen (Kreativmodus)
    /give @s valorantcraft:vandal
    /give @s valorantcraft:phantom
    /give @s valorantcraft:sheriff
    /give @s valorantcraft:classic
Rechtsklick = schiessen, Shift + Rechtsklick = nachladen.

## Ohne Installation bauen (GitHub)
1. Neues Repository auf github.com anlegen, alle Dateien hochladen (Ordner .github mit).
2. Reiter "Actions" -> Workflow "Build" laeuft automatisch (oder "Run workflow").
3. Fertigen Lauf oeffnen -> unten bei "Artifacts" valorantcraft-jar herunterladen, ZIP entpacken.

## In die Modrinth App
1. Profil erstellen: Minecraft 26.3, Loader Fabric.
2. Im Profil "Inhalt hinzufuegen" -> Fabric API suchen und installieren.
3. valorantcraft-0.1.0.jar per Drag & Drop ins Profilfenster ziehen (oder "Inhalt hinzufuegen" -> "Aus Datei").
