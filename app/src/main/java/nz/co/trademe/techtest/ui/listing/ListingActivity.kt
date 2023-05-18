package nz.co.trademe.techtest.ui.listing

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.observers.DisposableSingleObserver
import io.reactivex.schedulers.Schedulers
import nz.co.trademe.techtest.R
import nz.co.trademe.techtest.core.TMApplication
import nz.co.trademe.techtest.databinding.ActivityListingBinding
import nz.co.trademe.techtest.domain.repository.ListingsRepository
import nz.co.trademe.techtest.ui.util.ErrorDialogUtil
import nz.co.trademe.wrapper.models.ListedItemDetail
import timber.log.Timber

class ListingActivity : AppCompatActivity() {

	companion object {
		private const val EXTRA_LISTING_ID = "extra_listing_id"
		private const val INVALID_LISTING_ID = -1L

		fun getIntent(activity: Activity, listingId: Long): Intent {
			val intent = Intent(activity, ListingActivity::class.java)
			intent.putExtra(EXTRA_LISTING_ID, listingId)
			return intent
		}
	}

	private val listingsRepository: ListingsRepository = TMApplication.instance.listingsRepository

	private var disposable: DisposableSingleObserver<ListedItemDetail>? = null

	private var listingId: Long = INVALID_LISTING_ID
	private var listing: ListedItemDetail? = null

	private lateinit var binding: ActivityListingBinding

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		binding = ActivityListingBinding.inflate(layoutInflater)
		setContentView(binding.root)

		// display up navigation arrow
		supportActionBar?.setDisplayHomeAsUpEnabled(true)

		listingId = intent.getLongExtra(EXTRA_LISTING_ID, INVALID_LISTING_ID)
		if (listingId == INVALID_LISTING_ID) throw IllegalArgumentException("Listing ID not provided in intent")

		// load data if it hasn't already been loaded
		if (listing == null) loadListingData()
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		if (item.itemId == android.R.id.home) {
			onBackPressedDispatcher.onBackPressed()
			return true
		}

		return super.onOptionsItemSelected(item)
	}

	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	 * update view
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

	private fun updateView() {
		with(binding){
			loadingIndicator.visibility = if (getLoadingIndicatorVisible()) View.VISIBLE else View.GONE

			val contentVisible = getContentVisible()
			photo.visibility = if (contentVisible) View.VISIBLE else View.GONE
			heading.visibility = if (contentVisible) View.VISIBLE else View.GONE
			price.visibility = if (contentVisible) View.VISIBLE else View.GONE
			listingNumber.visibility = if (contentVisible) View.VISIBLE else View.GONE
			if (contentVisible) {
				Glide.with(photo.context)
					.load(getPhotoUrl())
					.centerCrop()
					.placeholder(R.drawable.ic_listing_default)
					.into(photo)
				heading.text = getTitleText()
				price.text = getDisplayPrice()
				listingNumber.text = getListingNumberText()
			}
		}
	}

	private fun getLoadingIndicatorVisible(): Boolean {
		// show the loading indicator if the content hasn't loaded
		return !getContentVisible()
	}

	private fun getContentVisible(): Boolean {
		// only show the main content if it has loaded
		return listing != null
	}

	private fun getPhotoUrl(): String? {
		val photos = listing?.photos
		if (photos != null && photos.isEmpty()) return null
		return photos?.get(0)?.value?.large
	}

	private fun getTitleText(): String? {
		return listing?.title
	}

	private fun getDisplayPrice(): String? {
		return listing?.priceDisplay
	}

	private fun getListingNumberText(): String {
		return getString(R.string.listing_number_, listing?.listingId)
	}

	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	 * data loading
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

	private fun loadListingData() {
		// ensure we have a valid listing id
		if (listingId == INVALID_LISTING_ID) throw IllegalArgumentException("Listing ID not initialized")

		// dispose any previously ongoing request
		disposable?.dispose()

		disposable = listingsRepository.getListingDetails(listingId)
				.subscribeOn(Schedulers.io())
				.observeOn(AndroidSchedulers.mainThread())
				.doFinally {
					// clear disposable reference, indicating that this operation has completed
					disposable = null
					// update view to reflect any changes
					updateView()
				}
				.subscribeWith(object : DisposableSingleObserver<ListedItemDetail>() {
					override fun onSuccess(listingResult: ListedItemDetail) {
						listing = listingResult
					}

					override fun onError(e: Throwable) {
						Timber.e(e)
						ErrorDialogUtil.handleException(
								this@ListingActivity,
								e,
								onCancel = {
									// back out of the screen if the user cancels as the screen is unusable
									onBackPressedDispatcher.onBackPressed()
								},
								onRetry = {
									// retry the request
									loadListingData()
								})
					}
				})

		updateView()
	}
}
