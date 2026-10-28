# Règles R8 propres à Omégadex.
# Les bibliothèques AndroidX et Compose fournissent leurs propres règles.

# SQLCipher : classes appelées depuis le code natif (JNI), à ne pas renommer ni supprimer.
-keep class net.zetetic.database.** { *; }
-keep class net.zetetic.database.sqlcipher.** { *; }
