package nz.co.trademe.techtest.ui.main.listings

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import nz.co.trademe.techtest.R
import nz.co.trademe.techtest.databinding.RowListingBinding
import nz.co.trademe.wrapper.models.SearchListing

class ListingListAdapter(private val listings: List<SearchListing>) :
		RecyclerView.Adapter<ListingListAdapter.MyViewHolder>() {

	var listener: Listener? = null

	interface Listener {
		fun onListingSelected(listingId: Long)
	}

	// Create new views (invoked by the layout manager)
	override fun onCreateViewHolder(
			parent: ViewGroup,
			viewType: Int
	): MyViewHolder {
		// create a new view
		val binding = RowListingBinding.inflate(LayoutInflater.from(parent.context), parent, false)

		return MyViewHolder(binding)
	}

	override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
		holder.updateView(listings[position])
	}

	override fun getItemCount() = listings.size

	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	 * view holder
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

	inner class MyViewHolder(private val binding: RowListingBinding) : RecyclerView.ViewHolder(binding.root) {

		private var listingId: Long = -1

		init {
			binding.touchTarget.setOnClickListener {
				listener?.onListingSelected(listingId)
			}
		}

		fun updateView(listing: SearchListing) {
			listingId = listing.listingId

			Glide.with(binding.photo.context)
					.load(listing.pictureHref)
					.centerCrop()
					.placeholder(R.drawable.ic_listing_default)
					.into(binding.photo)
			binding.heading.text = listing.title
			binding.price.text = listing.priceDisplay
		}
	}
}