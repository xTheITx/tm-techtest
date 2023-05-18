package nz.co.trademe.techtest.ui.main.categories

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.BiFunction
import io.reactivex.observers.DisposableSingleObserver
import io.reactivex.schedulers.Schedulers
import nz.co.trademe.techtest.core.TMApplication
import nz.co.trademe.techtest.databinding.RowCategoryListingsBinding
import nz.co.trademe.techtest.domain.repository.ListingsRepository
import nz.co.trademe.techtest.ui.main.listings.ListingListAdapter
import nz.co.trademe.wrapper.models.Category
import nz.co.trademe.wrapper.models.SearchListing
import timber.log.Timber
import java.util.concurrent.TimeUnit

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
		val binding = RowCategoryListingsBinding.inflate(
			LayoutInflater.from(parent.context), parent, false)
		return MyViewHolder(binding)
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

	inner class MyViewHolder(private val binding: RowCategoryListingsBinding) : RecyclerView.ViewHolder(binding.root) {


		private val listingsRepository: ListingsRepository = TMApplication.instance.listingsRepository

		private val retryLimit = 3

		private lateinit var category: Category

		private val listings: MutableList<SearchListing> = mutableListOf()
		private val adapter: ListingListAdapter = ListingListAdapter(listings)

		private var disposable: DisposableSingleObserver<List<SearchListing>>? = null

		init {
			adapter.listener = this@CategoryListAdapter
			binding.listingsRecyclerView.layoutManager = LinearLayoutManager(binding.root.context, LinearLayoutManager.HORIZONTAL, false)
			binding.listingsRecyclerView.adapter = adapter

			binding.heading.setOnClickListener{
				listener?.onCategorySelected(category.id)
			}
		}

		fun bind(category: Category) {
			this.category = category

			// dispose any previously ongoing request
			disposable?.dispose()

			disposable = listingsRepository.getTopCategoryListings(category.id)
					.subscribeOn(Schedulers.io())
					.observeOn(AndroidSchedulers.mainThread())
					// implement basic exponential backoff retrying
					// https://pamartinezandres.com/rxjava-2-exponential-backoff-retry-only-when-internet-is-available-5a46188ab175
					.retryWhen { errors: Flowable<Throwable> ->
						errors.zipWith(
								Flowable.range(1, retryLimit + 1),
								BiFunction<Throwable, Int, Int> { error: Throwable, retryCount: Int ->
									if (retryCount > retryLimit) {
										throw error
									} else {
										retryCount
									}
								}
						).flatMap { retryCount: Int ->
							Flowable.timer(
									Math.pow(2.toDouble(), retryCount.toDouble()).toLong(), TimeUnit.SECONDS
							)
						}
					}
					// clean up once the operation is complete
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
							// only report the error to get visibility on the issue
							Timber.e(e)

							// We intentionally don't prompt the user here as the screen is still usable, and there isn't much else we
							// can do at this stage as we have already retried the request
						}
					})

			updateView()
		}

		/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
		 * update view
		 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

		private fun updateView() {
			// disable touch events for leaf categories to prevent navigation
			binding.heading.isEnabled = getHasSubcategories()

			binding.heading.text = getCategoryName()

			binding.loadingIndicator.visibility = if (getLoadingIndicatorVisible()) View.VISIBLE else View.GONE
			binding.listingsRecyclerView.visibility = if (getListingsVisible()) View.VISIBLE else View.GONE
		}

		private fun getHasSubcategories(): Boolean {
			return !category.isLeaf
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
	}
}