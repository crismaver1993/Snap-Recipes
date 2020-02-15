package com.shubhampandey.snaprecipes

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.analytics.FirebaseAnalytics
import kotlinx.android.synthetic.main.activity_favorite_list.*

class FavoriteListActivity : AppCompatActivity() {

    private var mRecyclerView: RecyclerView? = null
    private var mAdapter: RecyclerView.Adapter<*>? = null
    var recipeDetailsArrayList = ArrayList<RecipeDetailsDataClass>()

    private lateinit var mFirebaseAnalytics: FirebaseAnalytics

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorite_list)


        assert(

            supportActionBar != null //null check
        )
        supportActionBar!!.setDisplayHomeAsUpEnabled(true) //show back button

        // Obtain the FirebaseAnalytics instance.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this)

        getFavoriteRecipeList()

        //getting recyclerview from xml
        mRecyclerView = findViewById(R.id.favoriteListRecyclerView)
        //adding a layoutmanager
        val mLayoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        mRecyclerView!!.layoutManager = mLayoutManager

        mAdapter = CustomAdapterForRecylerView(recipeDetailsArrayList)
        mRecyclerView!!.adapter = mAdapter
    }

    override fun onResume() {
        super.onResume()

        // after each iteration of loop the list view will get refereshed so that new added items shown up
        // refreshing the view
        mAdapter!!.notifyDataSetChanged() // it is used to indicate that some new data add/changed
    }

    override fun onStart() {
        super.onStart()

        // after each iteration of loop the list view will get refereshed so that new added items shown up
        // refreshing the view
        mAdapter!!.notifyDataSetChanged() // it is used to indicate that some new data add/changed
    }

    override fun onPause() {
        super.onPause()

        // after each iteration of loop the list view will get refereshed so that new added items shown up
        // refreshing the view
        mAdapter!!.notifyDataSetChanged() // it is used to indicate that some new data add/changed
    }

    private fun getFavoriteRecipeList() {
        val result = getFavoriteDataFromDB()
        favoriteListProgressBar.visibility = View.GONE
        if (result > 0) {
            noFavoriteRecipeTextView.visibility = View.GONE
            lottieFavoriteAnimation.visibility = View.GONE
            //println("True")
        }
        else {
            noFavoriteRecipeTextView.visibility = View.VISIBLE
            lottieFavoriteAnimation.visibility = View.VISIBLE
        }
    }

    private fun getFavoriteDataFromDB(): Int {
        // To delete database
        /*
        try {
            this.deleteDatabase("favoriterecipes")
            Toast.makeText(this, "Database deleted", Toast.LENGTH_LONG).show()

        } catch (e: Exception) {
            e.printStackTrace()
        }

         */

        var count = 0

        // opening database
        val database = this.openOrCreateDatabase("recipes", Context.MODE_PRIVATE, null)
        database.execSQL("CREATE TABLE IF NOT EXISTS favoriterecipes (recipeTitle VARCHAR unique, recipeDuration VARCHAR, recipeCalories VARCHAR, recipeServing VARCHAR, recipeSource VARCHAR, recipeDifficulty VARCHAR, recipeIngredients VARCHAR, recipeSteps VARCHAR, recipeImageURL VARCHAR, recipeSourceURL VARCHAR)")

        val cursor = database.rawQuery("SELECT * FROM favoriterecipes", null)

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    val recipeTitleIndex = cursor.getColumnIndex("recipeTitle")
                    val recipeDurationIndex = cursor.getColumnIndex("recipeDuration")
                    val recipeCaloriesIndex = cursor.getColumnIndex("recipeCalories")
                    val recipeServingIndex = cursor.getColumnIndex("recipeServing")
                    val recipeSourceIndex = cursor.getColumnIndex("recipeSource")
                    val recipeDifficultyIndex = cursor.getColumnIndex("recipeDifficulty")
                    val recipeIngredientsIndex = cursor.getColumnIndex("recipeIngredients")
                    val recipeStepsIndex = cursor.getColumnIndex("recipeSteps")
                    val recipeImageURLIndex = cursor.getColumnIndex("recipeImageURL")
                    val recipeSourceURLIndex = cursor.getColumnIndex("recipeSourceURL")

                    // accessing each node by name from the JSON Object
                    val recipeDetails = RecipeDetailsDataClass() // RHS is a dataclass
                    recipeDetails.recipeTitle =
                        cursor.getString(recipeTitleIndex) // adding database values into variable
                    recipeDetails.recipeCalories =
                        cursor.getString(recipeCaloriesIndex)
                    recipeDetails.recipeDuration =
                        cursor.getString(recipeDurationIndex)
                    recipeDetails.recipeSource =
                        cursor.getString(recipeSourceIndex)
                    recipeDetails.recipeImageURL =
                        cursor.getString(recipeImageURLIndex)
                    recipeDetails.recipeServing =
                        cursor.getString(recipeServingIndex)
                    recipeDetails.recipeIngredients =
                        cursor.getString(recipeIngredientsIndex)
                    recipeDetails.recipeSourceURL =
                        cursor.getString(recipeSourceURLIndex)
                    recipeDetails.cookingDifficulty =
                        cursor.getString(recipeDifficultyIndex)
                    recipeDetails.recipeCookingSteps =
                        cursor.getString(recipeStepsIndex)

                    recipeDetailsArrayList.add(recipeDetails)

                    count++

                    if (count > 1) {
                        // after each iteration of loop the list view will get refereshed so that new added items shown up
                        // refreshing the view
                        mAdapter?.notifyDataSetChanged() // it is used to indicate that some new data add/changed
                    }
                }
                while (cursor.moveToNext())
            }
        }
        cursor.close()
        database.close()

        return count
    }

    // On pressing back click
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return super.onSupportNavigateUp()
    }
}
