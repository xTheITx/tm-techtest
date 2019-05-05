package nz.co.trademe.techtest.ui.listing

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
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
				.subscribeWith(object : DisposableSingleObserver<ListedItemDetail>() {
					override fun onSuccess(listingResult: ListedItemDetail) {
						listing = listingResult
						updateView()
					}

					override fun onError(e: Throwable) {
						Timber.e(e)
						// todo error handling
					}
				})
	}

	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	 * update view
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

	fun updateView() {
		// todo loading indicator

		val listing = listing
		if (listing != null) {
			val photoUrl = listing.photos?.get(0)?.value?.large
			Glide.with(photoImageView.context)
					.load(photoUrl)
					.centerCrop()
					.placeholder(R.drawable.ic_listing_default)
					.into(photoImageView)
			headingTextView.text = listing.title
			priceTextView.text = listing.priceDisplay
			listingNumberTextView.text = getString(R.string.listing_number_, listing.listingId)
		}
	}
}
