package com.shubhampandey.snaprecipes

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import com.google.firebase.analytics.FirebaseAnalytics
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_recipe_details.*
import org.json.JSONArray
import org.json.JSONException


class RecipeDetailsActivity : AppCompatActivity() {

    private val TAG: String = "RecipeDetailsActivity"

    var receivedRecipeTitle: String? = null
    var receivedRecipeDuration: String? = null
    var receivedRecipeCalories: String? = null
    var receivedRecipeServing: String? = null
    var receivedRecipeIngredients: String? = null
    // variable to store JSONArray created from string
    var recipeIngredientsJSONArray: JSONArray? = null
    var receivedRecipeImageURL: String? = null
    var receivedRecipeSource: String? = null
    var receivedRecipeSourceURL: String? = null
    var receivedRecipePreparationLevel: String? = null
    var receivedRecipeSteps: String? = null
    var receivedRecipeType: String? = null

    private lateinit var mFirebaseAnalytics: FirebaseAnalytics

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recipe_details)

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
        receivedRecipeTitle = intent.getStringExtra("recipeLabel")
        receivedRecipeDuration = intent.getStringExtra("recipeDuration")
        receivedRecipeCalories = intent.getStringExtra("recipeCalories")
        if (receivedRecipeDuration == "0.0") {
            receivedRecipeDuration = "N/A"
        }
        receivedRecipeServing = intent.getStringExtra("recipeServing") // in no. of persons
        receivedRecipeIngredients = intent.getStringExtra("recipeIngredients")
        receivedRecipeImageURL = intent.getStringExtra("recipeImageURL")
        receivedRecipeSource = intent.getStringExtra("recipeSource")
        receivedRecipeSourceURL = intent.getStringExtra("recipeSourceURL")
        receivedRecipePreparationLevel = intent.getStringExtra("recipePreparationDifficulty")
        receivedRecipeSteps = intent.getStringExtra("recipeSteps")
        receivedRecipeType = intent.getStringExtra("recipeType")

        // creating JSONArray from string
        try {
            recipeIngredientsJSONArray = JSONArray(receivedRecipeIngredients)
            //println("JSONArray of Ingredients $recipeIngredientsJSONArray")
        }
        catch (e: JSONException) {
            e.printStackTrace()
        }

        updateUI()

    }

    // On pressing back click
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return super.onSupportNavigateUp()
    }

    private fun updateUI() {

        // Button constraints
        // buttons will be added programmatically
        val llParam = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        llParam.setMargins(0, 16, 0, 0)

        recipeDetailsTitleTextView.text = receivedRecipeTitle
        recipeDetailsDurationTextView.text = "$receivedRecipeDuration min."
        recipeDetailsDifficultyTextView.text = "$receivedRecipePreparationLevel"
        recipeDetailsServingTextView.text = "$receivedRecipeServing"
        if (receivedRecipeSource!!.contains("IndianHealthyRecipes", true)) {
            val ingredientTextView = TextView(this)
            ingredientTextView.setTextColor(ContextCompat.getColor(this, R.color.black))

            // set font family
            ingredientTextView.typeface = Typeface.create("sans-serif-condensed-medium", Typeface.NORMAL)
            ingredientTextView.text = "View ingredients and recipe on the source!"

            // adding button view to Layout
            recipeDetailsLinearLayout.addView(ingredientTextView, llParam)

            // hiding button
            // because ingredients in IndianHealthyRecipes are somewhat wrong/or have missing information
            viewRecipeOnAppBtn.visibility = View.GONE


        }
        else {
            for (i in 0 until recipeIngredientsJSONArray!!.length()) {
                val ingredientTextView = TextView(this)
                ingredientTextView.setTextColor(ContextCompat.getColor(this, R.color.black))

                // setting up bottom border of textview
                ingredientTextView.background = ContextCompat.getDrawable(this, R.drawable.textview_border)

                ingredientTextView.setPadding(2, 10, 2, 10)

                // set font family
                ingredientTextView.typeface = Typeface.create("sans-serif-condensed-medium", Typeface.NORMAL)
                ingredientTextView.text = recipeIngredientsJSONArray!![i].toString()

                // adding button view to Layout
                recipeDetailsLinearLayout.addView(ingredientTextView, llParam)
            }
            //recipeDetailsIngredientsTextView.text = "$receivedRecipeIngredients"
        }

        // downloading and showing image in view
        Picasso.get().load(receivedRecipeImageURL).into(recipeDetailsImageView, object : com.squareup.picasso.Callback {
            override fun onSuccess() {
                // hiding progress bar
                recipeDetailsProgressBar.visibility = View.GONE

            }

            override fun onError(e: Exception?) {
                Log.e(TAG, "${e!!.localizedMessage}")
                // hiding progress bar
                recipeDetailsProgressBar.visibility = View.GONE

            }

        })
    }

    // going in another activity to display recipe source
    fun openRecipeSource(view: View) {
        val intent = Intent(applicationContext, DisplayRecipeSourceActivity::class.java)
        intent.putExtra("recipeSourceRecipeURL", receivedRecipeSourceURL)
        startActivity(intent)
    }

    fun openRecipeStepsInApp(view: View) {

        // shared animation between activity
        val titleForTransition = findViewById<TextView>(R.id.recipeDetailsTitleTextView)
        val intent = Intent(this, DisplayRecipeStepsActivity::class.java)
        val options = ActivityOptionsCompat.makeSceneTransitionAnimation(this,
            titleForTransition, ViewCompat.getTransitionName(titleForTransition)!!
        )
        intent.putExtra("recipeStepsImage", receivedRecipeImageURL)
        intent.putExtra("recipetitleInString", receivedRecipeTitle)
        intent.putExtra("recipeStepsInString", receivedRecipeSteps)
        startActivity(intent, options.toBundle())
    }

}