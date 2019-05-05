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
     * General search
     *
     *
     * Allows you to search for listings on Trade Me by category, by keywords or a combination of
     * these two. The result set can be filtered by a variety of properties, including availability of
     * Buy Now, Pay Now, item condition or seller.Parameters listed below apply to all categories.
     * Additional parameters are available for certain categories (for example, category 3405, digital
     * cameras). Use the return_metadata parameter in combination with the category parameter to
     * retrieve information about the available parameters.
     */
    fun generalSearch(filters: Map<String, String>): Single<SearchCollection> {
        // todo refactor Map into concrete SearchQuery type with well defined enums/constants
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