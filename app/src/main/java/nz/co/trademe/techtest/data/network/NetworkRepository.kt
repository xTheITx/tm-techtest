package nz.co.trademe.techtest.data.network

import io.reactivex.Single
import nz.co.trademe.wrapper.TradeMeApi
import nz.co.trademe.wrapper.TradeMeApiService
import nz.co.trademe.wrapper.models.Category
import nz.co.trademe.wrapper.models.ListedItemDetail
import nz.co.trademe.wrapper.models.SearchCollection
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory

class NetworkRepository {

    private val service: TradeMeApiService = TradeMeApi(RxJava2CallAdapterFactory.create()).get()

    /**
     * Retrieve the top listings [SearchCollection] for the provided [categoryNumber]
     *
     * This is a paged request, starting at page 0
     */
    fun getTopCategoryListings(categoryNumber: String, page: Int): Single<SearchCollection> {
        val filters = hashMapOf(
            "category" to categoryNumber,
            "page" to page.toString(),
            "rows" to "20",
            "sort_order" to "Default"
        )

        return service.generalSearch(filters)
    }

    /**
     * Retrieve general categories
     *
     * Retrieves all or part of the Trade Me category tree.
     */
    fun getCategory(number: String): Single<Category> {
        return service.getCategory(number)
    }

    /**
     * Retrieve the details of a single listing
     */
    fun getListing(listingId: Long): Single<ListedItemDetail> {
        return service.getListing(listingId)
    }

}