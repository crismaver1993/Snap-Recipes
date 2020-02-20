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
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
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


class RecipeListBySearchActivity : AppCompatActivity() {

    private var mRecyclerView: RecyclerView? = null
    private var mAdapter: RecyclerView.Adapter<*>? = null
    var recipeDetailsArrayList = ArrayList<RecipeDetailsDataClass>()

    // Using Edamam API to get recipe details
    /*
    val edamamAppId = "88149556"
    val edamamApplicationKey = "be8c65cb0db50a3fe48df3c6e95ee6f1"

     */

    private val TAG: String = "RecipeListBySearch"

    //private lateinit var mDatabase: FirebaseDatabase

    // flag variable which is used to find if atleast 1 recipe is found or not
    var count = 0

    private lateinit var mFirebaseAnalytics: FirebaseAnalytics

    private var recipeJSONObjectArray: JSONArray = JSONArray()

    private lateinit var mongoClient: RemoteMongoClient

    private var searchQuery: String? = null

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

        // initialising firebase
        //mDatabase = FirebaseDatabase.getInstance()

        // Obtain the FirebaseAnalytics instance.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this)

        initialiseStitch()

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
        searchQuery = searchFieldeditText?.text.toString()
        if (!searchQuery.isNullOrBlank()) {

            // adding complete string
            //searchQueryArrList.add(searchQuery)

            /*
            // adding splitted string based on space
            val splittedQuery = searchQuery.split(" ")
            if (splittedQuery.size > 1) {
                for (item in splittedQuery)
                    searchQueryArrList.add(item)
            }

             */

            //println("Query on button pressed $splittedQuery")

            // clearing old data from arraylist which were showing in recyclerview
            // soo only new data will be available to user after applying filter
            recipeDetailsArrayList.clear()

            // enabling Lottie animation
            searchlottieCookingAnimation.visibility = View.VISIBLE
            searchwaitTitleTextView.visibility = View.VISIBLE
            //fetchRecipeDataFromFirebase(searchQueryArrList, null, null) // call function to get recipe data

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
            query = myCollection
                .find(and(regex("Name", "^$searchQuery", "i"), lte("Time", filterMaxCookTime), eq("Type", filterRecipeType)))
                .limit(15)

        } else if (filterMaxCookTime != null && filterRecipeType == null) { // only time filter selected
            query = myCollection
                .find(and(regex("Name", "^$searchQuery", "i"), lte("Time", filterMaxCookTime)))
                .limit(15)

        } else if( filterMaxCookTime == null && filterRecipeType != null){ // only dish type filter selected
            query = myCollection
                .find(and(regex("Name", "^$searchQuery", "i"), eq("Type", filterRecipeType)))
                .limit(15)
        }
        else {  // general query without filters
            query = myCollection
                .find(regex("Name", "^$searchQuery", "i"))
                .limit(15)
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


/*
// For Firebase based search
fun fetchRecipeDataFromFirebase(
    searchQueryArr: ArrayList<String>,
    filterMaxCookTime: Int?,
    filterRecipeType: String?
) {
    // clearing old data from arraylist in case of filter applied
    // soo only new data will be available to user
    recipeDetailsArrayList.clear()
    recipeJSONObjectArray = JSONArray()

    //println("Search Query $searchQueryArr")

    var maxRecipePerDetectedItem = 0

    if (searchQueryArr.isNotEmpty()) {
        maxRecipePerDetectedItem = (maxRecipePerItem/(searchQueryArr.size)-1)
    }

    val firebaseReference = mDatabase!!.getReference("recipes")
    firebaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
        override fun onCancelled(p0: DatabaseError) {
            Log.e(TAG, "Firebase Data Fetch Cancelled/Error")
        }

        override fun onDataChange(p0: DataSnapshot) {
            for (searchQuery in searchQueryArr) {
                //println("Vegetable: $searchQuery")
                count = 0
                for (data in p0.children) {

                    // teminating condition for loop
                    if (count==maxRecipePerDetectedItem) {
                        //println("Terminated on $count")
                        break
                    }

                    if (count < maxRecipePerDetectedItem) {
                        val hashMap: HashMap<String, Any> = data.value as HashMap<String, Any>
                        if (hashMap.size > 0) {
                            //println(hashMap)

                            if (filterMaxCookTime != null && filterRecipeType != null) {

                                // to convert string to int safely
                                var firebaseRecipeTime = 0
                                try {
                                    firebaseRecipeTime = hashMap["Time"].toString().toInt()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                                val firebaseRecipeType: String = hashMap["Type"].toString()

                                if ((hashMap["Name"].toString().startsWith(searchQuery, true)
                                            || hashMap["Name"].toString().endsWith(
                                        searchQuery,
                                        true
                                    ))
                                    && (firebaseRecipeTime <= filterMaxCookTime)
                                    && (firebaseRecipeType.equals(filterRecipeType, true))
                                ) {
                                    count += 1
                                    //println("Count Inside: $count")

                                    recipeJSONObjectArray.put(JSONObject(hashMap))
                                    //println("Time and Type Count : $count")
                                }

                            }
                            else if (filterMaxCookTime != null && filterRecipeType == null) {
                                var firebaseRecipeTime = 0
                                try {
                                    firebaseRecipeTime = hashMap["Time"].toString().toInt()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }


                                // Will show recipe upto count 20
                                if ((hashMap["Name"].toString().startsWith(searchQuery, true)
                                            || hashMap["Name"].toString().endsWith(
                                        searchQuery,
                                        true
                                    ))
                                    && (firebaseRecipeTime <= filterMaxCookTime)
                                ) {
                                    count += 1
                                    //println("Count Inside: $count")
                                    recipeJSONObjectArray.put(JSONObject(hashMap))
                                    //println("Time Count : $count")
                                }
                            }
                            else    {
                                // Will show recipe upto count 20
                                if ((hashMap["Name"].toString().startsWith(searchQuery, true)
                                            || hashMap["Name"].toString().endsWith(
                                        searchQuery,
                                        true
                                    ))
                                ) {
                                    count += 1
                                    //println("Count Inside: $count")

                                    recipeJSONObjectArray.put(JSONObject(hashMap))
                                    //println("Any Count : $count")
                                }
                            }
                        }
                    }
                    else
                        break
                }
            }
            updateUIFromFirebase(recipeJSONObjectArray, count)
        }
    })
}

private fun updateUIFromFirebase(body: JSONArray, count: Int) {

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

 */
}



