package com.shubhampandey.snaprecipes

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso

class CustomAdapterForRecylerView (val recipeList: ArrayList<RecipeDetailsDataClass>) : RecyclerView.Adapter<CustomAdapterForRecylerView.ViewHolder>() {

    // for animation of recylerview
    private var lastPosition = -1

    private var context: Context? = null

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.recipe_list_layout, parent, false)
        context = view.context
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return recipeList.count()
    }

    override fun onBindViewHolder(holder: CustomAdapterForRecylerView.ViewHolder, position: Int) {
        holder.bindItems(recipeList[position])

        // calling method to add animation
        setAnimation(holder.itemView, position)

        holder.itemView.setOnClickListener {
            val intent = Intent(context, RecipeDetailsActivity::class.java)
            intent.putExtra("recipeLabel", recipeList[position].recipeTitle)
            intent.putExtra("recipeCalories", recipeList[position].recipeCalories)
            intent.putExtra("recipeDuration", recipeList[position].recipeDuration)
            intent.putExtra("recipeServing", recipeList[position].recipeServing)
            intent.putExtra("recipeImageURL", recipeList[position].recipeImageURL)
            intent.putExtra("recipeIngredients", recipeList[position].recipeIngredients)
            intent.putExtra("recipeSource", recipeList[position].recipeSource)
            intent.putExtra("recipeSourceURL", recipeList[position].recipeSourceURL)
            intent.putExtra("recipePreparationDifficulty", recipeList[position].cookingDifficulty)
            intent.putExtra("recipeSteps", recipeList[position].recipeCookingSteps)
            intent.putExtra("recipeType", recipeList[position].recipeType)

            context!!.startActivity(intent)
        }
    }

    // adding animation to recycler view
    fun setAnimation(viewToAnimate: View, position: Int) {
        if (position > lastPosition) {
            val animation = ScaleAnimation(0.0f, 1.0f, 0.0f, 1.0f,
                Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 1.0f)
            animation.duration = 700 // in ms

            viewToAnimate.startAnimation(animation)

            lastPosition = position

        }
    }

    //the class is holing the list view
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        // binding data to view
        fun bindItems(recipeList: RecipeDetailsDataClass) {
            val recipeTitleTextView = itemView.findViewById(R.id.recipeTitleNewTextView) as TextView
            val recipeCaloriesTextView = itemView.findViewById(R.id.recipeCaloriesTextView) as TextView
            val recipeSourceTextView = itemView.findViewById(R.id.recipeSourceTextView) as TextView
            val recipeDurationTextView  = itemView.findViewById(R.id.recipeTotalTimeTextView) as TextView
            val recipeImageView  = itemView.findViewById(R.id.recipeImage) as ImageView

            recipeTitleTextView.text = recipeList.recipeTitle + "..."
            recipeCaloriesTextView.text = "Calories: ${recipeList.recipeCalories}"
            recipeSourceTextView.text = "Src: ${recipeList.recipeSource}"
            if (recipeList.recipeDuration == "0.0") {
                recipeDurationTextView.text = "N/A"
            }
            else {
                recipeDurationTextView.text = "${recipeList.recipeDuration} min."
            }

            // getting image URL of recipe
            val recipeImageURL = recipeList.recipeImageURL
            // downloading and showing image in view
            Picasso.get().load(recipeImageURL).into(recipeImageView)
        }
    }

}