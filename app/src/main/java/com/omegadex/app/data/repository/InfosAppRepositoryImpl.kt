package com.omegadex.app.data.repository

import android.content.Context
import com.omegadex.app.domain.repository.InfosAppRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class InfosAppRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : InfosAppRepository {
    override fun version(): String =
        context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
}
