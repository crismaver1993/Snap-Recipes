package com.shubhampandey.snaprecipes

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
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

    private lateinit var menuFavorite: Menu

    private lateinit var mFirebaseAnalytics: FirebaseAnalytics

    private val snapRecipesShortLinkPlayStore = "http://bit.ly/2OXbuVm"

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
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        updateUI()

    }

    private fun checkForFavorite() {
        val foundResult = getFavoriteDataFromDB()
        if (foundResult) {
            menuFavorite.getItem(0).icon =
                ContextCompat.getDrawable(this, R.drawable.ic_star_filled_white_24dp)
        }

    }

    private fun getFavoriteDataFromDB(): Boolean {
        // To delete database
        /*
        try {
            this.deleteDatabase("favoriterecipes")
            Toast.makeText(this, "Database deleted", Toast.LENGTH_LONG).show()

        } catch (e: Exception) {
            e.printStackTrace()
        }

         */

        try {
            // opening database
            val database = this.openOrCreateDatabase("recipes", Context.MODE_PRIVATE, null)
            database.execSQL("CREATE TABLE IF NOT EXISTS favoriterecipes (recipeTitle VARCHAR unique, recipeDuration VARCHAR, recipeCalories VARCHAR, recipeServing VARCHAR, recipeSource VARCHAR, recipeDifficulty VARCHAR, recipeIngredients VARCHAR, recipeSteps VARCHAR, recipeImageURL VARCHAR, recipeSourceURL VARCHAR)")

            val cursor = database.rawQuery("SELECT * FROM favoriterecipes", null)

            val recipeTitleIndex = cursor.getColumnIndex("recipeTitle")

            cursor.moveToFirst()

            while (cursor != null) {

                if (receivedRecipeTitle.equals(cursor.getString(recipeTitleIndex), true)) {
                    return true
                }
                cursor.moveToNext()
            }
            cursor?.close()

            return false
        } catch (e: Exception) {
            e.printStackTrace()

            return false
        }
    }


    // to show menu (vertical 3 dots)
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val menuInflater = menuInflater
        menuInflater.inflate(R.menu.recipedetailsextraoptions, menu)

        // initialising so that we can change icon for menu item option
        menuFavorite = menu!!

        // after initialising menuFavorite check if it's already in favorites
        checkForFavorite()

        return super.onCreateOptionsMenu(menu)
    }

    // when item of menu get selected
    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item!!.itemId) {
            R.id.addFavoriteMenuOption -> addToFavorite()
            R.id.shareRecipesMenuOption -> shareRecipe()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun shareRecipe() {
        Toast.makeText(this, "Sharing recipe!", Toast.LENGTH_SHORT).show()
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            // (Optional) Here we're setting the title of the content
            putExtra(Intent.EXTRA_TITLE, "Snap Recipes: Smarter way to get recipes")
            putExtra(Intent.EXTRA_TEXT, "Hey,\n\nLook at this $receivedRecipeTitle tasty recipe.\n\nRecipe source: $receivedRecipeSourceURL\n\nDownload the Snap Recipes app now: $snapRecipesShortLinkPlayStore")
            type = "text/plain"
        }

        val shareIntent = Intent.createChooser(sendIntent, null)
        startActivity(shareIntent)
    }

    private fun addToFavorite() {
        val insertResult = insertFavoriteDataInDB()
        if (insertResult) {
            // changing icon on clicking
            menuFavorite.getItem(0).icon =
                ContextCompat.getDrawable(this, R.drawable.ic_star_filled_white_24dp)

            Toast.makeText(this, "Recipe added to your favorites list!", Toast.LENGTH_LONG).show()
        } else {
            val deleteResult = deleteFavoriteDataInDB()
            if (deleteResult) {
                // changing icon on clicking
                menuFavorite.getItem(0).icon =
                    ContextCompat.getDrawable(this, R.drawable.ic_star_border_white_24dp)
                Toast.makeText(this, "Recipe removed from favorites!", Toast.LENGTH_LONG).show()
            }
            else
                Toast.makeText(this, "Unable to remove receipes from favorites!", Toast.LENGTH_LONG).show()
        }
    }

    private fun deleteFavoriteDataInDB(): Boolean {
        return try {
            // opening database
            val database = this.openOrCreateDatabase("recipes", Context.MODE_PRIVATE, null)
            database.execSQL("CREATE TABLE IF NOT EXISTS favoriterecipes (recipeTitle VARCHAR unique, recipeDuration VARCHAR, recipeCalories VARCHAR, recipeServing VARCHAR, recipeSource VARCHAR, recipeDifficulty VARCHAR, recipeIngredients VARCHAR, recipeSteps VARCHAR, recipeImageURL VARCHAR, recipeSourceURL VARCHAR)")

            // deleting values in db
            database.execSQL("DELETE FROM favoriterecipes WHERE recipeTitle='$receivedRecipeTitle'")

            true

        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun insertFavoriteDataInDB(): Boolean {
        try {
            // opening database
            val database = this.openOrCreateDatabase("recipes", Context.MODE_PRIVATE, null)
            database.execSQL("CREATE TABLE IF NOT EXISTS favoriterecipes (recipeTitle VARCHAR unique, recipeDuration VARCHAR, recipeCalories VARCHAR, recipeServing VARCHAR, recipeSource VARCHAR, recipeDifficulty VARCHAR, recipeIngredients VARCHAR, recipeSteps VARCHAR, recipeImageURL VARCHAR, recipeSourceURL VARCHAR)")

            // currently we dont have values so putting the ?
            val sqlstring =
                "INSERT INTO favoriterecipes (recipeTitle, recipeDuration, recipeCalories, recipeServing, recipeSource, recipeDifficulty, recipeIngredients, recipeSteps, recipeImageURL, recipeSourceURL) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
            val statement = database.compileStatement(sqlstring)

            // it is like prepared statement
            statement.bindString(1, receivedRecipeTitle)
            statement.bindString(2, receivedRecipeDuration)
            statement.bindString(3, receivedRecipeCalories)
            statement.bindString(4, receivedRecipeServing)
            statement.bindString(5, receivedRecipeSource)
            statement.bindString(6, receivedRecipePreparationLevel)
            statement.bindString(7, receivedRecipeIngredients)
            statement.bindString(8, receivedRecipeSteps)
            statement.bindString(9, receivedRecipeImageURL)
            statement.bindString(10, receivedRecipeSourceURL)
            statement.execute()

            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }


    // On pressing back click
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return super.onSupportNavigateUp()
    }

    private fun updateUI() {

        // Button constraints
        // buttons will be added programmatically
        val llParam = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        llParam.setMargins(0, 16, 0, 0)

        recipeDetailsTitleTextView.text = receivedRecipeTitle
        recipeDetailsDurationTextView.text = "$receivedRecipeDuration min."
        recipeDetailsDifficultyTextView.text = "$receivedRecipePreparationLevel"
        recipeDetailsServingTextView.text = "$receivedRecipeServing"

        for (i in 0 until recipeIngredientsJSONArray!!.length()) {
            val ingredientTextView = TextView(this)
            ingredientTextView.setTextColor(ContextCompat.getColor(this, R.color.black))

            // setting up bottom border of textview
            ingredientTextView.background =
                ContextCompat.getDrawable(this, R.drawable.textview_border)

            ingredientTextView.setPadding(20, 20, 8, 20)

            // set font family
            ingredientTextView.typeface =
                Typeface.create("sans-serif-condensed-medium", Typeface.NORMAL)
            ingredientTextView.text = recipeIngredientsJSONArray!![i].toString()

            // adding button view to Layout
            recipeDetailsLinearLayout.addView(ingredientTextView, llParam)
        }

        /*
        if (receivedRecipeSource!!.contains("IndianHealthyRecipes", true)) {
            val ingredientTextView = TextView(this)
            ingredientTextView.setTextColor(ContextCompat.getColor(this, R.color.black))

            // set font family
            ingredientTextView.typeface =
                Typeface.create("sans-serif-condensed-medium", Typeface.NORMAL)
            ingredientTextView.text = "View ingredients and recipe on the source!"

            // adding button view to Layout
            recipeDetailsLinearLayout.addView(ingredientTextView, llParam)

            // hiding button
            // because ingredients in IndianHealthyRecipes are somewhat wrong/or have missing information
            viewRecipeOnAppBtn.visibility = View.GONE


        } else {
            for (i in 0 until recipeIngredientsJSONArray!!.length()) {
                val ingredientTextView = TextView(this)
                ingredientTextView.setTextColor(ContextCompat.getColor(this, R.color.black))

                // setting up bottom border of textview
                ingredientTextView.background =
                    ContextCompat.getDrawable(this, R.drawable.textview_border)

                ingredientTextView.setPadding(2, 10, 2, 10)

                // set font family
                ingredientTextView.typeface =
                    Typeface.create("sans-serif-condensed-medium", Typeface.NORMAL)
                ingredientTextView.text = recipeIngredientsJSONArray!![i].toString()

                // adding button view to Layout
                recipeDetailsLinearLayout.addView(ingredientTextView, llParam)
            }
            //recipeDetailsIngredientsTextView.text = "$receivedRecipeIngredients"
            }
         */

        // downloading and showing image in view
        Picasso.get().load(receivedRecipeImageURL)
            .into(recipeDetailsImageView, object : com.squareup.picasso.Callback {
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
        val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
            this,
            titleForTransition, ViewCompat.getTransitionName(titleForTransition)!!
        )
        intent.putExtra("recipeStepsImage", receivedRecipeImageURL)
        intent.putExtra("recipetitleInString", receivedRecipeTitle)
        intent.putExtra("recipeStepsInString", receivedRecipeSteps)
        startActivity(intent, options.toBundle())
    }

}