package nz.co.trademe.techtest.ui.main.categories

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
		holder.updateView(categories[position])
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

		private lateinit var categoryNumber: String

		private val listings: MutableList<SearchListing> = mutableListOf()
		private val adapter: ListingListAdapter

		private var disposable: DisposableSingleObserver<List<SearchListing>>? = null

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

		fun updateView(category: Category) {
			categoryNumber = category.id

			headingTextView.text = category.name

			disposable = listingsRepository.getTopCategoryListings(category.id)
					.subscribeOn(Schedulers.io())
					.observeOn(AndroidSchedulers.mainThread())
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

			// todo show listings loading indicator
		}

		@OnClick(R.id.heading) fun onCategorySelected() {
			listener?.onCategorySelected(categoryNumber)
		}
	}
}