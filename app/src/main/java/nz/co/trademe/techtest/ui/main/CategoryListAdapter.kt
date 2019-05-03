package nz.co.trademe.techtest.ui.main

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import butterknife.ButterKnife
import nz.co.trademe.techtest.R
import nz.co.trademe.wrapper.models.Category

class CategoryListAdapter(private val categories: List<Category>) :
    RecyclerView.Adapter<CategoryListAdapter.MyViewHolder>() {

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
            .inflate(R.layout.row_category_listings, parent, false)
        return MyViewHolder(view)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.updateView(categories[position])
    }

    override fun getItemCount() = categories.size

    /* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
     * view holder
     * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

    class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        @BindView(R.id.heading)
        lateinit var headingTextView: TextView

        init {
            ButterKnife.bind(this, view)
        }

        fun updateView(category: Category) {
            headingTextView.text = category.name
        }
    }
}