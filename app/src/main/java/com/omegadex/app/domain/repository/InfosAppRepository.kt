package com.omegadex.app.domain.repository

/** Informations générales sur l'application installée. */
interface InfosAppRepository {
    /** Version affichée de l'application, par exemple « 0.1.0 ». */
    fun version(): String
}
