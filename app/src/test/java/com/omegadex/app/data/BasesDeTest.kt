package com.omegadex.app.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.omegadex.app.data.jeu.JeuDatabase
import com.omegadex.app.data.progression.ProgressionDatabase

/** Bases en mémoire pour les tests Robolectric. */
object BasesDeTest {
    fun jeu(): JeuDatabase =
        Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), JeuDatabase::class.java)
            .allowMainThreadQueries()
            .build()

    fun progression(): ProgressionDatabase =
        Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), ProgressionDatabase::class.java)
            .allowMainThreadQueries()
            .build()

    /** Quelques données de Rubis Oméga : 2 étapes, la lignée d'Arcko, une attaque, des lieux et rencontres. */
    fun remplirJeu(base: JeuDatabase) {
        val sql = base.openHelper.writableDatabase
        listOf(
            "INSERT INTO etape VALUES (0, 'Départ', 'Rival', NULL, 5, 'Bourg-en-Vol')",
            "INSERT INTO etape VALUES (1, 'Roxanne', 'Roxanne', 'Roche', 14, 'Mérouville')",
            "INSERT INTO lieu VALUES (1, 'Route 101', 'ROUTE', 0)",
            "INSERT INTO lieu VALUES (2, 'Route 104', 'ROUTE', 1)",
            "INSERT INTO quete VALUES (1, 0, 1, 'PRINCIPALE', 1, 'Choisir un starter', NULL)",
            "INSERT INTO quete VALUES (2, 0, 1, 'ANNEXE', 1, 'Capturer un Zigzaton', NULL)",
            "INSERT INTO quete VALUES (3, 1, 2, 'PRINCIPALE', 1, 'Battre Roxanne', 'Badge Roche')",
            "INSERT INTO espece VALUES " +
                "(252, 1, 'Arcko', 'Plante', NULL, 'Bois Gecko', 0.5, 5.0, 40, " +
                "45, 35, 65, 55, 70, '1 Vit', 45, 0.875, 'Monstre,Dragon', 0)",
            "INSERT INTO espece VALUES " +
                "(253, 2, 'Massko', 'Plante', NULL, 'Bois Gecko', 0.9, 21.6, 50, " +
                "65, 45, 85, 65, 95, '2 Vit', 45, 0.875, 'Monstre,Dragon', 0)",
            "INSERT INTO espece VALUES " +
                "(254, 3, 'Jungko', 'Plante', NULL, 'Forêt', 1.7, 52.2, 70, 85, " +
                "65, 105, 85, 120, '3 Vit', 45, 0.875, 'Monstre,Dragon', 1)",
            "INSERT INTO espece VALUES " +
                "(1, NULL, 'Bulbizarre', 'Plante', 'Poison', 'Graine', 0.7, 6.9, " +
                "45, 49, 49, 65, 65, 45, '1 Atq Spé', 45, 0.875, 'Monstre,Végétal', 0)",
            "INSERT INTO talent VALUES (1, 'Engrais', 'Plante plus forte en danger.')",
            "INSERT INTO talent VALUES (2, 'Délestage', 'Vitesse si objet perdu.')",
            "INSERT INTO espece_talent VALUES (252, 1, 0)",
            "INSERT INTO espece_talent VALUES (252, 2, 1)",
            "INSERT INTO evolution VALUES (1, 252, 253, 'NIVEAU', '16')",
            "INSERT INTO evolution VALUES (2, 253, 254, 'NIVEAU', '36')",
            "INSERT INTO attaque VALUES (1, 'Écras''Face', 'Normal', 'PHYSIQUE', 40, 100, 35, 'Frappe.')",
            "INSERT INTO attaque_apprise VALUES (252, 1, 'NIVEAU', 1, NULL)",
            "INSERT INTO rencontre VALUES (1, 1, 252, 'CADEAU', 5, 5)",
            "INSERT INTO rencontre VALUES (2, 2, 253, 'HERBE', 12, 14)",
        ).forEach { sql.execSQL(it) }
    }
}
