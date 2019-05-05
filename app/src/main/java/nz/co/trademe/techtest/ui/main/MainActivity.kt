package nz.co.trademe.techtest.ui.main

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import butterknife.ButterKnife
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.observers.DisposableSingleObserver
import io.reactivex.schedulers.Schedulers
import nz.co.trademe.techtest.R
import nz.co.trademe.techtest.core.TMApplication
import nz.co.trademe.techtest.domain.repository.CategoriesRepository
import nz.co.trademe.techtest.ui.listing.ListingActivity
import nz.co.trademe.techtest.ui.main.categories.CategoryListAdapter
import nz.co.trademe.wrapper.models.Category
import timber.log.Timber

class MainActivity : AppCompatActivity(),
		CategoryListAdapter.Listener {

	companion object {
		private const val EXTRA_CATEGORY_NUMBER = "extra_category_number"

		fun getIntent(activity: Activity, categoryNumber: String): Intent {
			val intent = Intent(activity, MainActivity::class.java)
			intent.putExtra(EXTRA_CATEGORY_NUMBER, categoryNumber)
			return intent
		}
	}

	private val categoriesRepository: CategoriesRepository = TMApplication.instance.categoriesRepository

	private var disposable: DisposableSingleObserver<Category>? = null

	private val categories: MutableList<Category> = mutableListOf()
	private lateinit var adapter: CategoryListAdapter

	@BindView(R.id.loading_indicator)
	lateinit var loadingIndicator: ProgressBar
	@BindView(R.id.recycler_view)
	lateinit var recyclerView: RecyclerView

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)

		ButterKnife.bind(this)

		val categoryId: String = intent.getStringExtra(EXTRA_CATEGORY_NUMBER) ?: CategoriesRepository.ROOT_CATEGORY

		adapter = CategoryListAdapter(categories)
		adapter.listener = this
		recyclerView.layoutManager = LinearLayoutManager(this)
		recyclerView.adapter = adapter

		disposable = categoriesRepository.getCategory(categoryId)
				.subscribeOn(Schedulers.io())
				.observeOn(AndroidSchedulers.mainThread())
				.doFinally {
					// clear disposable reference, indicating that this operation has completed
					disposable = null
					// update view to reflect any changes
					updateView()
				}
				.subscribeWith(object : DisposableSingleObserver<Category>() {
					override fun onSuccess(category: Category) {
						category.subcategories?.let {
							categories.clear()
							categories.addAll(it)
						}
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

	private fun updateView() {
		loadingIndicator.visibility = if (getLoadingIndicatorVisible()) View.VISIBLE else View.GONE

		val contentVisible = getContentVisible()
		recyclerView.visibility = if (contentVisible) View.VISIBLE else View.GONE
		if (contentVisible) {
			adapter.notifyDataSetChanged()
		}
	}

	private fun getLoadingIndicatorVisible(): Boolean {
		// show the loading indicator if the content hasn't loaded
		return !getContentVisible()
	}

	private fun getContentVisible(): Boolean {
		// only show the main content if it has loaded
		return disposable == null
	}

	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	 * adapter callbacks
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

	override fun onCategorySelected(categoryId: String) {
		startActivity(getIntent(this, categoryId))
	}

	override fun onListingSelected(listingId: Long) {
		startActivity(ListingActivity.getIntent(this, listingId))
	}
}
