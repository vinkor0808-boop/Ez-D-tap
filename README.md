# Snow DTap

Fabric-Mod fuer Minecraft 1.21.11.

Ablauf: Gegner hitten, Obsidian platzieren -> Mod wechselt auf Crystal -> ein Linksklick -> Double Pop.

## Bauen (GitHub Actions)
1. Neues Repo auf GitHub anlegen und den INHALT dieser ZIP hochladen (der Ordner .github muss mit drin sein).
2. Reiter "Actions" -> "Build" laeuft automatisch.
3. Wenn er gruen ist: Build anklicken -> unten bei "Artifacts" snow-dtap herunterladen.
4. Die .jar (ohne "-sources") in den mods-Ordner legen. Fabric API wird gebraucht.

Triple Pop: in DTapClient.java `POPS = 2` auf `3` aendern.
