package nz.co.trademe.techtest.ui.main

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.TextView
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
import nz.co.trademe.techtest.ui.util.ErrorDialogUtil
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

	private lateinit var categoryId: String

	private val categories: MutableList<Category> = mutableListOf()
	private lateinit var adapter: CategoryListAdapter
	private var loadingMessageId: Int = -1

	@BindView(R.id.loading_indicator)
	lateinit var loadingIndicator: View
	@BindView(R.id.message)
	lateinit var loadingTextView: TextView
	@BindView(R.id.recycler_view)
	lateinit var recyclerView: RecyclerView

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)

		ButterKnife.bind(this)

		categoryId = intent.getStringExtra(EXTRA_CATEGORY_NUMBER) ?: CategoriesRepository.ROOT_CATEGORY

		// show the back arrow if we're not at the root category
		if (categoryId != CategoriesRepository.ROOT_CATEGORY) {
			supportActionBar?.setDisplayHomeAsUpEnabled(true)
		}

		// intialize recyclerview and adapter
		adapter = CategoryListAdapter(categories)
		adapter.listener = this
		recyclerView.layoutManager = LinearLayoutManager(this)
		recyclerView.adapter = adapter

		// initialize loading message
		if (loadingMessageId == -1) loadingMessageId = (Math.random() * 6f).toInt()

		// load data if it hasn't already been loaded
		if (categories.isEmpty()) loadCategoryData()
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		if (item.itemId == android.R.id.home) {
			onBackPressed()
			return true
		}

		return super.onOptionsItemSelected(item)
	}

	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	 * update view
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

	private fun updateView() {
		val loadingIndicatorVisible = getLoadingIndicatorVisible()
		loadingIndicator.visibility = if (loadingIndicatorVisible) View.VISIBLE else View.GONE
		if (loadingIndicatorVisible) {
			loadingTextView.text = getLoadingText()
		}

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

	private fun getLoadingText(): String {
		return when (loadingMessageId) {
			0 -> getString(R.string.loading_message_1)
			1 -> getString(R.string.loading_message_2)
			2 -> getString(R.string.loading_message_3)
			3 -> getString(R.string.loading_message_4)
			4 -> getString(R.string.loading_message_5)
			5 -> getString(R.string.loading_message_6)
			else -> getString(R.string.loading_message_7)
		}
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

	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	 * data loading
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

	private fun loadCategoryData() {
		// dispose any previously ongoing request
		disposable?.dispose()

		// load category data
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
						ErrorDialogUtil.handleException(
								this@MainActivity,
								e,
								onCancel = {
									// back out of the screen if the user cancels as the screen is unusable
									onBackPressed()
								},
								onRetry = {// retry the request
									loadCategoryData()
								})
					}
				})

		updateView()
	}
}
