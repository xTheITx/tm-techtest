package nz.co.trademe.techtest.domain.repository

import io.reactivex.Single
import nz.co.trademe.techtest.data.network.NetworkRepository
import nz.co.trademe.wrapper.models.Category

class CategoriesRepository(
    private val networkRepository: NetworkRepository
) {

    companion object {
        const val ROOT_CATEGORY = "0"
    }

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

                @Suppress("FoldInitializerAndIfToElvis")
                if (targetCategory == null) {
                    // todo this case should not be possible, report it to analytics to get visibility on the issue
                    // todo what are TradeMe's best practices for handling exceptional cases / errors?
                    throw IllegalStateException("Failed to find Category with number: $number")
                }

                targetCategory
            }
    }

    /**
     * Recursively searches the provided Category tree, returning the Category that matches the provided categoryNumber,
     * or null if no Category with the number is found
     */
    private fun getSubcategory(targetCategoryNumber: String, category: Category): Category? {
        val subcategories = category.subcategories ?: return null
        subcategories.forEach { subcategory ->
            val nextSubcategoryNumber = getNextSubcategoryNumber(targetCategoryNumber, category.id)

            // check if we have reached the correct category depth
            if (nextSubcategoryNumber == null) {
                // check all subcategories for the target category number
                if (subcategory.id == targetCategoryNumber)
                    return subcategory

            } else { // keep searching down the tree

                // check if this subcategory is the correct branch
                // if it is, then continue searching recursively down the branch
                if (subcategory.id == nextSubcategoryNumber)
                    return getSubcategory(targetCategoryNumber, subcategory)
            }

        }

        // search failed for this category branch
        return null
    }

    /**
     * Returns the first subcategory id from a category number.
     *
     * Eg given an categoryNumber of 1111-2222-3333 will return 1111-2222
     * If no subcategory Ids are found (separated by '-') then null is returned
     * // todo update documentation?
     */
    private fun getNextSubcategoryNumber(categoryNumber: String, parentCategoryNumber: String): String? {
        val index = categoryNumber.indexOf("-", parentCategoryNumber.length)
        if (index == -1) return null

        return categoryNumber.substring(index)
    }
}