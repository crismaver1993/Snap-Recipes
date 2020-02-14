package com.shubhampandey.snaprecipes

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.viewpager.widget.PagerAdapter
import de.hdodenhof.circleimageview.CircleImageView

class TabPagerAdaperForOnboarding(
    private val mContext: Context,
    private val contentItems: List<ItemsClassForOnboarding>
) :PagerAdapter() {

    @NonNull
    override fun instantiateItem(@NonNull container:ViewGroup, position:Int):Any {
        val layoutInflater = (mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater)
        @SuppressLint("InflateParams") val layoutItems = layoutInflater.inflate(R.layout.slide_tab_layout_for_onboarding, null)
        val first = layoutItems.findViewById<TextView>(R.id.TopText)
        val title = layoutItems.findViewById<TextView>(R.id.Title)
        val description = layoutItems.findViewById<TextView>(R.id.Description)
        val images = layoutItems.findViewById<CircleImageView>(R.id.Image)
        first.text = contentItems[position].first
        title.text = contentItems[position].titles
        description.text = contentItems[position].description

        images.setBackgroundResource(contentItems[position].images) // Error was here

        container.addView(layoutItems)
        return layoutItems
    }



    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view == `object`
    }

    override fun getCount(): Int {
        return contentItems.size
    }

    override fun destroyItem(@NonNull container: ViewGroup, position:Int, @NonNull `object`:Any) {
        container.removeView(`object` as View)
    }
}
