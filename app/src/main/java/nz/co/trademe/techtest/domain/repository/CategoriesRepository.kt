package nz.co.trademe.techtest.domain.repository

import androidx.annotation.VisibleForTesting
import io.reactivex.Single
import nz.co.trademe.techtest.data.network.NetworkRepository
import nz.co.trademe.wrapper.models.Category

open class CategoriesRepository(
		private val networkRepository: NetworkRepository
) {

	companion object {
		const val ROOT_CATEGORY = "0"
	}

	/**
	 * The top level Category network response is a large and expensive network operation. The data is also quite stable, and doesn't
	 * appear to present a large memory footprint, so this cache should be used whenever possible to access Category data
	 */
	private var categoryCache: Category? = null

	/**
	 * Retrieve general category for a given number
	 *
	 * @param number the number associated with the category to return. Use [ROOT_CATEGORY] to request the root
	 * category
	 */
	fun getCategory(number: String): Single<Category> {
		return Single
				.defer(fun(): Single<Category> {
					// return category data cache if available, otherwise request category detail data
					val cache = categoryCache
					return if (cache != null) {
						Single.just(cache)
					} else {
						networkRepository.getCategory(number)
								.doOnSuccess { category ->
									categoryCache = category
								}
					}
				})
				// filter category data
				.map { category: Category ->
					// handle special case of the root category being requested
					if (number == ROOT_CATEGORY) return@map category

					val targetCategory = getSubcategory(number, category)

					if (targetCategory != null) {
						return@map targetCategory
					}

					// todo this case should not be possible, report it to analytics to get visibility on the issue
					// todo what are TradeMe's best practices for handling exceptional cases / errors?
					throw IllegalStateException("Failed to find Category with number: $number")
				}
	}

	/**
	 * Recursively searches the provided Category tree, returning the Category that matches the provided categoryNumber,
	 * or null if no Category with the number is found
	 */
	@VisibleForTesting
	fun getSubcategory(targetCategoryNumber: String, category: Category): Category? {
		val nextSubcategoryNumber = getNextSubcategoryNumber(targetCategoryNumber, category.id)

		// ensure there are subcategories to be processed
		val subcategories = category.subcategories ?: return null

		when (nextSubcategoryNumber) {
			// an invalid state has been reached
			null -> return null

			// the correct category depth has reached
			targetCategoryNumber -> // check all subcategories for the target category number
				subcategories.forEach { subcategory ->
					if (subcategory.id == targetCategoryNumber)
						return subcategory
				}

			// continue searching recursively down the branch
			else ->
				subcategories.forEach { subcategory ->
					// find the right branch to search down
					if (subcategory.id == nextSubcategoryNumber) {
						return getSubcategory(targetCategoryNumber, subcategory)
					}
				}
		}

		// search failed for this category branch
		return null
	}

	/**
	 * Returns the next subcategory number from [targetCategoryNumber] after the [parentCategoryNumber].
	 *
	 * Eg given an target of 1111-2222-3333- and a parent of 1111- this method will will return 1111-2222-
	 * If no subcategory Ids are found (separated by '-') or any invalid parameters are provied, then null is returned
	 *
	 * @param targetCategoryNumber the target category number from which the next subcategory number will be returned.
	 * This value cannot be empty
	 * @param parentCategoryNumber a subset of the target number, this number represents the number immediately 'before'
	 * the returned subcategory number
	 */
	@VisibleForTesting
	fun getNextSubcategoryNumber(targetCategoryNumber: String, parentCategoryNumber: String): String? {
		// an empty category number is considered an invalid query
		if (targetCategoryNumber.isEmpty()) return null

		// a query where the category number is not part of of the parentCategoryNumber is considered an invalid query
		// eg there is no next subcategory for category number 1111-2222- and parent aaaa-bbbb-cccc-
		if (!targetCategoryNumber.contains(parentCategoryNumber)) return null

		val index = targetCategoryNumber.indexOf("-", parentCategoryNumber.length)

		// if no separator was found, or the separator was the last character, then return null as no next subcategory number exists
		if (index == -1) return null

		return targetCategoryNumber.substring(0, index + 1)
	}
}