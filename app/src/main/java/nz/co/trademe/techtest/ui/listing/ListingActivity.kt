package nz.co.trademe.techtest.ui.listing

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import butterknife.BindView
import butterknife.ButterKnife
import com.bumptech.glide.Glide
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.observers.DisposableSingleObserver
import io.reactivex.schedulers.Schedulers
import nz.co.trademe.techtest.R
import nz.co.trademe.techtest.core.TMApplication
import nz.co.trademe.techtest.domain.repository.ListingsRepository
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

	private var listing: ListedItemDetail? = null

	@BindView(R.id.loading_indicator)
	lateinit var loadingIndicator: ProgressBar
	@BindView(R.id.photo)
	lateinit var photoImageView: ImageView
	@BindView(R.id.heading)
	lateinit var headingTextView: TextView
	@BindView(R.id.price)
	lateinit var priceTextView: TextView
	@BindView(R.id.listing_number)
	lateinit var listingNumberTextView: TextView


	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_listing)

		ButterKnife.bind(this)

		val listingId: Long = intent.getLongExtra(EXTRA_LISTING_ID, INVALID_LISTING_ID)
		if (listingId == INVALID_LISTING_ID) throw IllegalArgumentException("Listing ID not provided in intent")

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
						// todo error handling
					}
				})

		updateView()
	}

	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	 * update view
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

	private fun updateView() {
		loadingIndicator.visibility = if (getLoadingIndicatorVisible()) View.VISIBLE else View.GONE

		val contentVisible = getContentVisible()
		photoImageView.visibility = if (contentVisible) View.VISIBLE else View.GONE
		headingTextView.visibility = if (contentVisible) View.VISIBLE else View.GONE
		priceTextView.visibility = if (contentVisible) View.VISIBLE else View.GONE
		listingNumberTextView.visibility = if (contentVisible) View.VISIBLE else View.GONE
		if (contentVisible) {
			Glide.with(photoImageView.context)
					.load(getPhotoUrl())
					.centerCrop()
					.placeholder(R.drawable.ic_listing_default)
					.into(photoImageView)
			headingTextView.text = getTitleText()
			priceTextView.text = getDisplayPrice()
			listingNumberTextView.text = getListingNumberText()
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
		return listing?.photos?.get(0)?.value?.large
	}

	private fun getTitleText(): String? {
		return listing?.title
	}

	private fun getDisplayPrice(): String? {
		return listing?.priceDisplay
	}

	private fun getListingNumberText(): String? {
		return getString(R.string.listing_number_, listing?.listingId)
	}
}
