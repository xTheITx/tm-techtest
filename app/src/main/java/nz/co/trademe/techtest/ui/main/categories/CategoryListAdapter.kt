package nz.co.trademe.techtest.ui.main.categories

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.observers.DisposableSingleObserver
import io.reactivex.schedulers.Schedulers
import nz.co.trademe.techtest.R
import nz.co.trademe.techtest.core.TMApplication
import nz.co.trademe.techtest.domain.repository.ListingsRepository
import nz.co.trademe.techtest.ui.main.listings.ListingListAdapter
import nz.co.trademe.wrapper.models.Category
import nz.co.trademe.wrapper.models.SearchListing
import timber.log.Timber

class CategoryListAdapter(private val categories: List<Category>) :
		RecyclerView.Adapter<CategoryListAdapter.MyViewHolder>(),
		ListingListAdapter.Listener {

	interface Listener {
		fun onCategorySelected(categoryId: String)
		fun onListingSelected(listingId: Long)
	}

	var listener: Listener? = null

	// Create new views (invoked by the layout manager)
	override fun onCreateViewHolder(
			parent: ViewGroup,
			viewType: Int
	): MyViewHolder {
		// create a new view
		val view = LayoutInflater.from(parent.context)
				.inflate(R.layout.row_category_listings, parent, false)
		return MyViewHolder(view)
	}

	override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
		holder.bind(categories[position])
	}

	override fun getItemCount() = categories.size

	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	 * listings adapter
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

	override fun onListingSelected(listingId: Long) {
		listener?.onListingSelected(listingId)
	}

	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	 * view holder
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

	inner class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {

		private val listingsRepository: ListingsRepository = TMApplication.instance.listingsRepository

		private lateinit var category: Category

		private val listings: MutableList<SearchListing> = mutableListOf()
		private val adapter: ListingListAdapter

		private var disposable: DisposableSingleObserver<List<SearchListing>>? = null

		@BindView(R.id.loading_indicator)
		lateinit var loadingIndicator: ProgressBar
		@BindView(R.id.heading)
		lateinit var headingTextView: TextView
		@BindView(R.id.listings_recycler_view)
		lateinit var listingRecyclerView: RecyclerView

		init {
			ButterKnife.bind(this, view)

			adapter = ListingListAdapter(listings)
			adapter.listener = this@CategoryListAdapter
			listingRecyclerView.layoutManager = LinearLayoutManager(view.context, LinearLayoutManager.HORIZONTAL, false)
			listingRecyclerView.adapter = adapter
		}

		fun bind(category: Category) {
			this.category = category

			// dispose any previously ongoing request
			disposable?.dispose()

			disposable = listingsRepository.getTopCategoryListings(category.id)
					.subscribeOn(Schedulers.io())
					.observeOn(AndroidSchedulers.mainThread())
					.doFinally {
						// clear disposable reference, indicating that this operation has completed
						disposable = null
						// update view to reflect any changes
						updateView()
					}
					.subscribeWith(object : DisposableSingleObserver<List<SearchListing>>() {
						override fun onSuccess(topListings: List<SearchListing>) {
							listings.clear()
							listings.addAll(topListings)
							adapter.notifyDataSetChanged()
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
			headingTextView.text = getCategoryName()

			loadingIndicator.visibility = if (getLoadingIndicatorVisible()) View.VISIBLE else View.GONE
			listingRecyclerView.visibility = if (getListingsVisible()) View.VISIBLE else View.GONE
		}

		private fun getCategoryName(): String {
			return category.name
		}

		private fun getLoadingIndicatorVisible(): Boolean {
			// show the loading indicator if the content hasn't loaded
			return !getListingsVisible()
		}

		private fun getListingsVisible(): Boolean {
			// only show the listings if they have loaded
			return disposable == null
		}

		/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
		 * ui listeners
		 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

		@OnClick(R.id.heading) fun onCategorySelected() {
			listener?.onCategorySelected(category.id)
		}
	}
}