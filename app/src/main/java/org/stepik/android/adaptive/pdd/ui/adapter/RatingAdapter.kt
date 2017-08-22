package org.stepik.android.adaptive.pdd.ui.adapter

import android.graphics.drawable.Drawable
import android.support.v4.content.ContextCompat
import android.support.v4.graphics.drawable.DrawableCompat
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.stepik.android.adaptive.pdd.R
import org.stepik.android.adaptive.pdd.data.SharedPreferenceMgr
import org.stepik.android.adaptive.pdd.data.model.RatingItem
import org.stepik.android.adaptive.pdd.databinding.ItemRatingBinding

class RatingAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val profileId = SharedPreferenceMgr.getInstance().profileId

    private lateinit var leaderIconDrawable: Drawable
    private lateinit var leaderIconDrawableSelected: Drawable

    companion object {
        private val RATING_ITEM_VIEW_TYPE = 1
        private val SEPARATOR_VIEW_TYPE = 2

        private val SEPARATOR = -1L

        @JvmStatic
        private fun isRatingGap(current: RatingItem, next: RatingItem) =
                current.rank + 1 != next.rank

        @JvmStatic
        private fun isNotSeparatorStub(item: RatingItem) =
                item.user != SEPARATOR
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        val context = recyclerView.context

        leaderIconDrawable = DrawableCompat.wrap(ContextCompat.getDrawable(context, R.drawable.ic_crown))
        DrawableCompat.setTint(leaderIconDrawable, ContextCompat.getColor(context, R.color.colorYellow))

        leaderIconDrawableSelected = DrawableCompat.wrap(ContextCompat.getDrawable(context, R.drawable.ic_crown))
        DrawableCompat.setTint(leaderIconDrawableSelected, ContextCompat.getColor(context, android.R.color.white))
    }

    private val items = ArrayList<RatingItem>()

    override fun getItemViewType(position: Int) =
        if (items[position].user != SEPARATOR) {
            RATING_ITEM_VIEW_TYPE
        } else {
            SEPARATOR_VIEW_TYPE
        }

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int) =
            when(viewType) {
                RATING_ITEM_VIEW_TYPE -> RatingViewHolder(ItemRatingBinding.inflate(LayoutInflater.from(parent?.context), parent, false))
                SEPARATOR_VIEW_TYPE -> SeparatorViewHolder(LayoutInflater.from(parent?.context).inflate(R.layout.ranks_separator, parent, false))
                else -> throw IllegalStateException("Unknown view type $viewType")
            }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            RATING_ITEM_VIEW_TYPE -> {
                (holder as RatingViewHolder).binding.let {
                    it.rank.text = items[position].rank.toString()
                    it.exp.text = items[position].exp.toString()
                    it.name.text = items[position].name

                    it.root.isSelected = profileId == items[position].user

                    if (items[position].rank == 1) {
                        it.icon.setImageDrawable(if (profileId == items[position].user) {
                            leaderIconDrawableSelected
                        } else {
                            leaderIconDrawable
                        })

                        it.icon.visibility = View.VISIBLE
                        it.rank.visibility = View.GONE
                    } else {
                        it.icon.visibility = View.GONE
                        it.rank.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    fun set(data: Collection<RatingItem>) {
        items.clear()
        items.addAll(data)
        addSeparator()
        notifyDataSetChanged()
    }

    private fun addSeparator() {
        (items.size - 2 downTo 0)
                .filter { isRatingGap(items[it], items[it + 1]) && isNotSeparatorStub(items[it]) && isNotSeparatorStub(items[it + 1]) }
                .forEach { items.add(it + 1, RatingItem(0, "", 0, SEPARATOR)) }
    }


    class RatingViewHolder(val binding: ItemRatingBinding) : RecyclerView.ViewHolder(binding.root)
    class SeparatorViewHolder(view: View) : RecyclerView.ViewHolder(view)
}