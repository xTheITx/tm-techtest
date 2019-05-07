package nz.co.trademe.techtest.domain.repository

import io.reactivex.Single
import nz.co.trademe.techtest.data.network.NetworkRepository
import nz.co.trademe.wrapper.models.ListedItemDetail
import nz.co.trademe.wrapper.models.SearchCollection
import nz.co.trademe.wrapper.models.SearchListing

class ListingsRepository(
		private val networkRepository: NetworkRepository
) {

	/**
	 * Returns the top list of [SearchListing] for the provided Category
	 */
	fun getTopCategoryListings(categoryNumber: String): Single<List<SearchListing>> {
		return networkRepository.getTopCategoryListings(categoryNumber, 0)
				.map { collection: SearchCollection ->
					collection.list ?: ArrayList()
				}
	}

	/**
	 * Returns the details the listing
	 */
	fun getListingDetails(listingId: Long): Single<ListedItemDetail> {
		return networkRepository.getListing(listingId)
	}
}