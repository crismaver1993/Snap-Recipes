package com.shubhampandey.snaprecipes

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.analytics.FirebaseAnalytics
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Filters.*
import com.mongodb.stitch.android.core.Stitch
import com.mongodb.stitch.android.services.mongodb.remote.RemoteFindIterable
import com.mongodb.stitch.android.services.mongodb.remote.RemoteMongoClient
import com.tapadoo.alerter.Alerter
import kotlinx.android.synthetic.main.activity_recipe_list_by_search.*
import org.bson.Document
import org.json.JSONArray
import org.json.JSONObject
import java.util.*
import java.util.Arrays.asList
import java.util.regex.Pattern
import kotlin.collections.ArrayList


class RecipeListBySearchActivity : AppCompatActivity() {

    private var mRecyclerView: RecyclerView? = null
    private var mAdapter: RecyclerView.Adapter<*>? = null
    var recipeDetailsArrayList = ArrayList<RecipeDetailsDataClass>()

    private val TAG: String = "RecipeListBySearch"

    // flag variable which is used to find if atleast 1 recipe is found or not
    var count = 0

    private lateinit var mFirebaseAnalytics: FirebaseAnalytics

    private var recipeJSONObjectArray: JSONArray = JSONArray()

    private lateinit var mongoClient: RemoteMongoClient
    private var searchQuery: String? = null
    private var chip: Chip? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recipe_list_by_search)

        // getting toolbar to show the menu
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbarid)
        setSupportActionBar(toolbar)

        assert(
            supportActionBar != null //null check
        )
        supportActionBar!!.setDisplayHomeAsUpEnabled(true) //show back button

        // Obtain the FirebaseAnalytics instance.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this)

        initialiseStitch()

        // initialising chip
        chip = findViewById(searchFilterChipGroup.checkedChipId)
        // Adding listener to chip
        searchFilterChipGroup.setOnCheckedChangeListener { group, checkedId ->
            chip = group.findViewById(checkedId)
            if (checkedId == R.id.searchByDishChip)
                searchFieldeditText.hint = "Try dish name (eg. Matar Paneer)"
            else
                searchFieldeditText.hint = "Try searching Potato Tomato Onion..."

        }

        // Get the string array
        val vegetableArray = resources.getStringArray(R.array.vegetables_array)
        // Create the adapter and set it to the AutoCompleteTextView
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, vegetableArray)
        searchFieldeditText.setAdapter(adapter)

        //getting recyclerview from xml
        mRecyclerView = findViewById(R.id.searchrecipeListRecyclerView)
        //adding a layoutmanager
        val mLayoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        mRecyclerView!!.layoutManager = mLayoutManager

        mAdapter = CustomAdapterForRecylerView(recipeDetailsArrayList)
        mRecyclerView!!.adapter = mAdapter
    }

    private fun initialiseStitch() {
        val stitchAppClient = Stitch.getDefaultAppClient()
        // Getting service from Stitch that we want to use
        mongoClient = stitchAppClient.getServiceClient(
            RemoteMongoClient.factory,
            "snap-recipes-mongodb-atlas"
        )
    }

    // On pressing back click
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return super.onSupportNavigateUp()
    }

    fun findRecipeBySearch(view: View) {
        // clear count flag variable
        count = 0

        // hiding filter applied
        // incase of user searched for new query
        filtersAppliedSearhActivity.visibility = View.GONE

        // hiding not found text in case of fresh search
        notFoundSearchActivityTitleTextView.visibility = View.GONE

        // to hide keyboard after clicking button
        val inputMethodManager: InputMethodManager =
            getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(
            currentFocus?.windowToken,
            InputMethodManager.HIDE_NOT_ALWAYS
        )

        val searchField = findViewById<EditText>(R.id.searchFieldeditText)
        searchQuery = searchField.text.trim().toString()
        if (!searchQuery.isNullOrBlank()) {

            // clearing old data from arraylist which were showing in recyclerview
            // soo only new data will be available to user after applying filter
            recipeDetailsArrayList.clear()

            // enabling Lottie animation
            searchlottieCookingAnimation.visibility = View.VISIBLE
            searchwaitTitleTextView.visibility = View.VISIBLE

            // MongoDB Search
            fetchRecipeFromMongoDB(searchQuery!!, null, null)

        } else {
            searchField.error = "Search query must not be blank!"
        }
    }

    // to show menu (vertical 3 dots)
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val menuInflater = menuInflater
        menuInflater.inflate(R.menu.options, menu)

        return super.onCreateOptionsMenu(menu)
    }

    // when item of menu get selected
    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item!!.itemId) {
            R.id.filterMenuOption -> {
                val intent = Intent(this, FilterDishActivity::class.java)
                intent.putExtra("filterRequestFrom", "RecipeListBySearchActivity")
                startActivity(intent)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()

        // clear count flag variable
        count = 0

        filterResult()
    }

    private fun filterResult() {
        val sharedPreferences = this.getSharedPreferences(
            "com.shubhampandey.snaprecipes",
            android.content.Context.MODE_PRIVATE
        )
        // here retrieving the value from shared preference
        // first parameter is key and second is default value if value is not found in provided key
        val storedMaxCookTime = sharedPreferences.getInt("maxCookingTime", 0)
        val storedDishType = sharedPreferences.getString("dishType", null)

        //println("Shared $storedMaxCookTime  $storedDishType")

        if (storedMaxCookTime != 0 || storedDishType != null) {

            if (!searchQuery.isNullOrBlank()) {
                // clearing old data from arraylist in case of filter applied
                // soo only new data will be available to user
                recipeDetailsArrayList.clear()

                filtersAppliedSearhActivity.visibility = View.VISIBLE

                // enabling Lottie animation
                searchlottieCookingAnimation.visibility = View.VISIBLE
                searchwaitTitleTextView.visibility = View.VISIBLE

                when {
                    (storedMaxCookTime != 0 && storedDishType != null) -> {
                        if (storedDishType == "Veg") {
                            filtersAppliedSearhActivity.text =
                                "Filters applied:\nMax. cooking time: $storedMaxCookTime min.\nDish type: Vegetarian"
                        } else {
                            filtersAppliedSearhActivity.text =
                                "Filters applied:\nMax. cooking time: $storedMaxCookTime min.\nDish type: Non-Vegetarian"
                        }
                        fetchRecipeFromMongoDB(searchQuery!!, storedMaxCookTime, storedDishType)
                    }
                    (storedMaxCookTime > 0) -> {
                        filtersAppliedSearhActivity.text =
                            "Filters applied:\nMax. cooking time: $storedMaxCookTime min."
                        fetchRecipeFromMongoDB(searchQuery!!, storedMaxCookTime, null)
                    }
                    (storedDishType != null) -> {
                        if (storedDishType == "Veg") {
                            filtersAppliedSearhActivity.text =
                                "Filters applied:\nDish type: Vegetarian"
                        } else {
                            filtersAppliedSearhActivity.text =
                                "Filters applied:\nDish type: Non-Vegetarian"
                        }
                        fetchRecipeFromMongoDB(searchQuery!!, null, storedDishType)
                    }
                }
            } else {
                val searchField = findViewById<EditText>(R.id.searchFieldeditText)
                searchField.error = "Search query must not be blank!"
            }
            // after usage removing values from shared preferences
            sharedPreferences.edit().remove("maxCookingTime").apply()
            sharedPreferences.edit().remove("dishType").apply()
        }
    }

    private fun fetchRecipeFromMongoDB(
        searchQuery: String,
        filterMaxCookTime: Int?,
        filterRecipeType: String?
    ) {
        // clearing old data from arraylist in case of filter applied
        // soo only new data will be available to user
        recipeDetailsArrayList.clear()
        recipeJSONObjectArray = JSONArray()

        // getting reference of Collection and Documents
        val myCollection = mongoClient.getDatabase("snap_recipes")
            .getCollection("recipes")

        val result = mutableListOf<Document>()
        val query: RemoteFindIterable<Document>

        // Using MongoDB query see Documentation
        if (filterMaxCookTime != null && filterRecipeType != null) { // both filters selected
            when {
                (chip!!.id == R.id.searchByDishChip) -> { // Using chip id to find if user want to search by dish name or ingredients
                    query = myCollection
                        .find(and(regex("Name", "^$searchQuery", "i"), lte("Time", filterMaxCookTime), eq("Type", filterRecipeType)))
                        .sort(Document("totalStarRating", -1)) // sort by totalStarRating in descending order
                        .limit(20)
                }
                (chip!!.id == R.id.searchByIngredientChip) -> { // Using chip id to find if user want to search by dish name or ingredients
                    // creating a list of entered qry separated by space
                    val itemList = searchQuery.split(" ")
                    val mList = mutableListOf<Pattern>() // empty mutable list
                    for( item in itemList) {
                        mList.add(Pattern.compile(" $item", Pattern.CASE_INSENSITIVE)) // adding item in mutablelist by pattern
                    }
                    query = myCollection
                        .find(and(all("Ingredient", mList), lte("Time", filterMaxCookTime), eq("Type", filterRecipeType)))
                        .sort(Document("totalStarRating", -1)) // sort by totalStarRating in descending order
                        .limit(25)
                }
                else -> { // if non of the chip selected
                    query = myCollection
                        .find(and(regex("Name", "^$searchQuery", "i"), lte("Time", filterMaxCookTime), eq("Type", filterRecipeType)))
                        .sort(Document("totalStarRating", -1)) // sort by totalStarRating in descending order
                        .limit(20)
                }
            }

        } else if (filterMaxCookTime != null && filterRecipeType == null) { // only time filter selected
            when {
                (chip!!.id == R.id.searchByDishChip) -> { // Using chip id to find if user want to search by dish name or ingredients
                    query = myCollection
                        .find(and(regex("Name", "^$searchQuery", "i"), lte("Time", filterMaxCookTime)))
                        .sort(Document("totalStarRating", -1)) // sort by totalStarRating in descending order
                        .limit(20)
                }
                (chip!!.id == R.id.searchByIngredientChip) -> { // Using chip id to find if user want to search by dish name or ingredients
                    // creating a list of entered qry separated by space
                    val itemList = searchQuery.split(" ")
                    val mList = mutableListOf<Pattern>() // empty mutable list
                    for( item in itemList) {
                        mList.add(Pattern.compile(" $item", Pattern.CASE_INSENSITIVE)) // adding item in mutablelist by pattern
                    }
                    query = myCollection
                        .find(and(all("Ingredient", mList), lte("Time", filterMaxCookTime)))
                        .sort(Document("totalStarRating", -1)) // sort by totalStarRating in descending order
                        .limit(25)
                }
                else -> {
                    query = myCollection
                        .find(and(regex("Name", "^$searchQuery", "i"), lte("Time", filterMaxCookTime)))
                        .sort(Document("totalStarRating", -1)) // sort by totalStarRating in descending order
                        .limit(20)
                }
            }
        } else if( filterMaxCookTime == null && filterRecipeType != null){ // only dish type filter selected
            when {
                (chip!!.id == R.id.searchByDishChip) -> { // Using chip id to find if user want to search by dish name or ingredients
                    query = myCollection
                        .find(and(regex("Name", "^$searchQuery", "i"), eq("Type", filterRecipeType)))
                        .sort(Document("totalStarRating", -1)) // sort by totalStarRating in descending order
                        .limit(20)
                }
                (chip!!.id == R.id.searchByIngredientChip) -> { // Using chip id to find if user want to search by dish name or ingredients
                    // creating a list of entered qry separated by space
                    val itemList = searchQuery.split(" ")
                    val mList = mutableListOf<Pattern>() // empty mutable list
                    for( item in itemList) {
                        mList.add(Pattern.compile(" $item", Pattern.CASE_INSENSITIVE)) // adding item in mutablelist by pattern
                    }

                    query = myCollection
                        .find(and(all("Ingredient", mList), eq("Type", filterRecipeType)))
                        .sort(Document("totalStarRating", -1)) // sort by totalStarRating in descending order
                        .limit(25)
                }
                else -> {
                    query = myCollection
                        .find(and(regex("Name", searchQuery, "i"), eq("Type", filterRecipeType)))
                        .sort(Document("totalStarRating", -1)) // sort by totalStarRating in descending order
                        .limit(20)
                }
            }
        }
        else { // no filters selected
            when {
                (chip!!.id == R.id.searchByDishChip) -> { // Using chip id to find if user want to search by dish name or ingredients
                    // general query without filters
                    query = myCollection
                        .find(regex("Name", "^$searchQuery", "i"))
                        .sort(Document("totalStarRating", -1)) // sort by totalStarRating in descending order
                        .limit(20)
                }
                (chip!!.id == R.id.searchByIngredientChip) -> { // Using chip id to find if user want to search by dish name or ingredients
                    // separating search query separated by space
                    val itemList = searchQuery.split(" ")
                    val mList = mutableListOf<Pattern>() // empty mutable list
                    for( item in itemList) {
                        mList.add(Pattern.compile(" $item", Pattern.CASE_INSENSITIVE)) // adding item in mutablelist by appending with pattern
                    }
                    // general query without filters
                    query = myCollection
                        .find(all("Ingredient", mList))
                        .sort(Document("totalStarRating", -1)) // sort by totalStarRating in descending order
                        .limit(25)
                }
                else -> {
                    query = myCollection
                        .find(regex("Name", "^$searchQuery", "i"))
                        .sort(Document("totalStarRating", -1)) // sort by totalStarRating in descending order
                        .limit(20)
                }
            }
        }
        // storing result in variable result which is Mutable list of Document
        query.into(result).addOnSuccessListener {
            //println("Result success")
            var count = 0
            result.forEach {
                recipeJSONObjectArray.put(JSONObject(it.toJson())) // adding recipes in Json array
                count += 1
            }

            updateUIFromMongoDB(recipeJSONObjectArray, count)

        }.addOnFailureListener {
            Log.e(TAG, "Can not get result from MongoDB")
            Alerter.create(this@RecipeListBySearchActivity)
                .setTitle("Something went wrong!")
                .setText("Try searching again.")
                .setBackgroundColorRes(R.color.orange)
                .setDuration(5000)
                .show()

            // disabling Lottie animation
            searchlottieCookingAnimation.visibility = View.GONE
            searchwaitTitleTextView.visibility = View.GONE
        }
    }

    private fun updateUIFromMongoDB(body: JSONArray, count: Int) {

        notFoundSearchActivityTitleTextView.visibility = View.GONE

        //val JSONObjectResult = JSONObject(body)

        // for testing purposes
        //println("Output in JSON: " + body + " Count: " + count)

        if (count >= 1) {
            for (i in 0 until body.length()) {
                val JSONObjectResult = body.getJSONObject(i)
                // accessing each node by name from the JSON Object
                val recipeDetails = RecipeDetailsDataClass() // RHS is a dataclass
                recipeDetails.recipeTitle =
                    JSONObjectResult.getString("Name").toString()
                // checking if key exist or not in JSON
                if (JSONObjectResult.has("calories"))
                    recipeDetails.recipeCalories =
                        JSONObjectResult.getString("calories").toString()
                else
                    recipeDetails.recipeCalories =
                        "--"
                recipeDetails.recipeDuration =
                    JSONObjectResult.getString("Time").toString()
                recipeDetails.recipeSource =
                    JSONObjectResult.getString("source").toString()
                recipeDetails.recipeImageURL =
                    JSONObjectResult.getString("Image").toString()
                recipeDetails.recipeServing =
                    JSONObjectResult.getString("yield").toString()
                recipeDetails.recipeIngredients =
                    JSONObjectResult.getString("Ingredient").toString()
                recipeDetails.recipeSourceURL =
                    JSONObjectResult.getString("recipeURL").toString()
                // checking if key exist or not in JSON
                if (JSONObjectResult.has("Level"))
                    recipeDetails.cookingDifficulty =
                        JSONObjectResult.getString("Level").toString()
                else
                    recipeDetails.cookingDifficulty =
                        "N/A."
                recipeDetails.recipeShortDescription =
                    JSONObjectResult.getString("Description").toString()
                recipeDetails.recipeType =
                    JSONObjectResult.getString("Type").toString()
                recipeDetails.recipeCookingSteps =
                    JSONObjectResult.getString("steps").toString()

                recipeDetailsArrayList.add(recipeDetails)
            }
        } else {
            Alerter.create(this@RecipeListBySearchActivity)
                .setTitle("No recipes found!")
                .setText("Try searching again. (eg. Potato or Aloo)")
                .setBackgroundColorRes(R.color.orange)
                .setDuration(5000)
                .show()

            // disabling Lottie animation
            searchlottieCookingAnimation.visibility = View.GONE
            searchwaitTitleTextView.visibility = View.GONE

            notFoundSearchActivityTitleTextView.visibility = View.VISIBLE
        }

        // Using runOnUiThread because we are currently on another thread (OkHttp creates new thread)
        // So to access/change Ui elements we have to use this
        // Otherwise we will get error
        // If you try to touch view of UI thread from another thread, you will get Android CalledFromWrongThreadException.
        this@RecipeListBySearchActivity.runOnUiThread(java.lang.Runnable {
            // disabling Lottie animation
            searchlottieCookingAnimation.visibility = View.GONE
            searchwaitTitleTextView.visibility = View.GONE
            mAdapter!!.notifyDataSetChanged() // it is used to indicate that some new data add/changed
        })
    }
}



