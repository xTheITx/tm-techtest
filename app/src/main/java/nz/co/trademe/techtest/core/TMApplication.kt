package nz.co.trademe.techtest.core

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import nz.co.trademe.techtest.BuildConfig
import nz.co.trademe.techtest.data.network.NetworkRepository
import nz.co.trademe.techtest.domain.repository.CategoriesRepository
import nz.co.trademe.techtest.domain.repository.ListingsRepository
import timber.log.Timber

class TMApplication : Application() {

	companion object {
		@JvmStatic
		@SuppressLint("StaticFieldLeak")
		lateinit var instance: TMApplication
	}

	lateinit var networkRepository: NetworkRepository
	lateinit var categoriesRepository: CategoriesRepository
	lateinit var listingsRepository: ListingsRepository

	override fun attachBaseContext(base: Context?) {
		super.attachBaseContext(base)
		instance = this
	}

	override fun onCreate() {
		super.onCreate()

		// enable console logging for debug builds
		// todo improve reporting of exceptions
		if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())

		initDependencies()
	}

	private fun initDependencies() {
		networkRepository = NetworkRepository()

		categoriesRepository = CategoriesRepository(networkRepository)
		listingsRepository = ListingsRepository(networkRepository)
	}


}