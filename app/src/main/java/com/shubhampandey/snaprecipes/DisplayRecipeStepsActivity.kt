package com.shubhampandey.snaprecipes

import android.graphics.Typeface
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.firebase.analytics.FirebaseAnalytics
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_display_recipe_steps.*
import org.json.JSONArray
import org.json.JSONException

class DisplayRecipeStepsActivity : AppCompatActivity() {

    var receivedRecipeImageURLInStepsActivity: String? = null
    var receivedRecipeTitle: String? = null
    var receivedRecipeSteps: String? = null
    // variable to store JSONArray created from string
    var recipeStepsJSONArray: JSONArray? = null

    private val TAG = "DisplayRecipeSteps"

    private lateinit var mFirebaseAnalytics: FirebaseAnalytics

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_display_recipe_steps)

        assert(
            supportActionBar != null //null check
        )
        supportActionBar!!.setDisplayHomeAsUpEnabled(true) //show back button

        // Obtain the FirebaseAnalytics instance.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this)

        // receiving transmitted data
        val intent = intent
        // assigning transmitted info to a variable
        // getStringExtra("key which was used during transmitting info")
        receivedRecipeImageURLInStepsActivity = intent.getStringExtra("recipeStepsImage")
        receivedRecipeTitle = intent.getStringExtra("recipetitleInString")
        receivedRecipeSteps = intent.getStringExtra("recipeStepsInString")

        // creating JSONArray from string
        try {
            recipeStepsJSONArray = JSONArray(receivedRecipeSteps)
            //println("JSONArray of Ingredients $recipeIngredientsJSONArray")
        }
        catch (e: JSONException) {
            e.printStackTrace()
        }

        updateUI()
    }

    private fun updateUI() {

        // downloading and showing image in view
        Picasso.get().load(receivedRecipeImageURLInStepsActivity).into(recipeStepDetailsImageView, object : com.squareup.picasso.Callback {
            override fun onSuccess() {
                // hiding progress bar
                recipeStepDetailsProgressBar.visibility = View.GONE

            }

            override fun onError(e: Exception?) {
                Log.e(TAG, "${e!!.localizedMessage}")
                // hiding progress bar
                recipeStepDetailsProgressBar.visibility = View.GONE

            }

        })

        recipestepDetailsTitleTextView.text = receivedRecipeTitle
        // Button constraints
        // buttons will be added programmatically
        val llParam = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        llParam.setMargins(0, 16, 0, 0)

        for (i in 0 until recipeStepsJSONArray!!.length()) {
            val recipeStepsTextView = TextView(this)
            recipeStepsTextView.setTextColor(ContextCompat.getColor(this, R.color.black))

            recipeStepsTextView.setPadding(2, 10, 2, 10)
            recipeStepsTextView.background = ContextCompat.getDrawable(this, R.drawable.textview_border)

            // set font family
            recipeStepsTextView.typeface = Typeface.create("sans-serif-condensed-medium", Typeface.NORMAL)
            recipeStepsTextView.text = recipeStepsJSONArray!![i].toString()

            // adding button view to Layout
            recipeStepsLinearLayout.addView(recipeStepsTextView, llParam)
        }
    }

    // On pressing back click
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return super.onSupportNavigateUp()
    }
}
