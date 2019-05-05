package nz.co.trademe.techtest.ui.main.listings

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import butterknife.ButterKnife
import com.bumptech.glide.Glide
import nz.co.trademe.techtest.R
import nz.co.trademe.wrapper.models.SearchListing

class ListingListAdapter(private val listings: List<SearchListing>) :
    RecyclerView.Adapter<ListingListAdapter.MyViewHolder>() {

    interface Listener {
        fun onCategorySelected(categoryId: Int)
        fun onListingSelected(listingId: Int)
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MyViewHolder {
        // create a new view
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.row_listing, parent, false)
        return MyViewHolder(view)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.updateView(listings[position])
    }

    override fun getItemCount() = listings.size

    /* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
     * view holder
     * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

    class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        @BindView(R.id.photo)
        lateinit var photoImageView: ImageView
        @BindView(R.id.heading)
        lateinit var headingTextView: TextView
        @BindView(R.id.price)
        lateinit var priceTextView: TextView

        init {
            ButterKnife.bind(this, view)
        }

        fun updateView(listing: SearchListing) {
            Glide.with(photoImageView.context)
                .load(listing.pictureHref)
                .placeholder(R.drawable.ic_listing_default)
                .into(photoImageView)
            headingTextView.text = listing.title
            priceTextView.text = listing.priceDisplay
        }
    }
}