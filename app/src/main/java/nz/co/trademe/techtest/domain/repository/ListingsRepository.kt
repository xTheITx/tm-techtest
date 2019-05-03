package nz.co.trademe.techtest.domain.repository

import io.reactivex.Single
import nz.co.trademe.techtest.data.network.NetworkRepository
import nz.co.trademe.wrapper.models.ListedItemDetail

class ListingsRepository(
    private val networkRepository: NetworkRepository
) {

    /**
     * Retrieve the details of a single listing
     *
     * @param listingId the listingId associated with the listing to return
     */
    private fun getListings(listingId: Long): Single<ListedItemDetail> {
        return networkRepository.getListing(listingId)
    }
}