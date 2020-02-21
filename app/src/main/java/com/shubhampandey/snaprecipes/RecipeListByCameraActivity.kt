package com.shubhampandey.snaprecipes

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Typeface
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import clarifai2.api.ClarifaiBuilder
import clarifai2.api.ClarifaiClient
import clarifai2.api.ClarifaiResponse
import clarifai2.dto.input.ClarifaiInput
import clarifai2.dto.model.Model
import clarifai2.dto.model.output.ClarifaiOutput
import clarifai2.dto.prediction.Concept
import com.google.firebase.analytics.FirebaseAnalytics
import com.mindorks.paracamera.Camera
import com.mongodb.client.model.Filters.*
import com.mongodb.stitch.android.core.Stitch
import com.mongodb.stitch.android.services.mongodb.remote.RemoteFindIterable
import com.mongodb.stitch.android.services.mongodb.remote.RemoteMongoClient
import com.tapadoo.alerter.Alerter
import ir.mirrajabi.searchdialog.SimpleSearchDialogCompat
import ir.mirrajabi.searchdialog.core.SearchResultListener
import kotlinx.android.synthetic.main.activity_recipe_list_by_camera.*
import okhttp3.*
import org.bson.Document
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.*
import kotlin.collections.ArrayList


class RecipeListByCameraActivity : AppCompatActivity() {

    private lateinit var camera: Camera
    private val PERMISSION_REQUEST_CODE = 1
    private lateinit var client: ClarifaiClient

    private var mRecyclerView: RecyclerView? = null
    private var mAdapter: RecyclerView.Adapter<*>? = null
    var recipeDetailsArrayList = ArrayList<RecipeDetailsDataClass>()

    private val TAG: String = "RecipeListByCamera"

    // global variable to store details of vegetables recognised in image
    var detectedVegetables = arrayListOf<String>()

    // flag variable which is used to find if atleast 1 recipe is found or not
    var count = 0

    private var recipeJSONObjectArray: JSONArray = JSONArray()

    private lateinit var mFirebaseAnalytics: FirebaseAnalytics

    private lateinit var mongoClient: RemoteMongoClient
    private var searchQuery: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recipe_list_by_camera)

        // Making device rotation disabled. Only portrait will be allowed
        this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        // Obtain the FirebaseAnalytics instance.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this)

        initialiseStitch()

        camera = Camera.Builder()
            .resetToCorrectOrientation(true)//1 Rotates the camera bitmap to the correct orientation from meta data.
            .setTakePhotoRequestCode(Camera.REQUEST_TAKE_PHOTO)//2 Sets the request code for your onActivityResult() method.
            .setDirectory("pics")//3 Sets the directory in which your pictures will be saved.
            .setName("delicious_${System.currentTimeMillis()}")//4 Sets the name of each picture taken according to the system time.
            .setImageFormat(Camera.IMAGE_JPEG)//5 Sets the image format to JPEG.
            .setCompression(50)//6 Sets a compression rate of 75% to use less system resources.
            .build(this)

        // Initialising Clarifai client
        client = ClarifaiBuilder(getString(R.string.clarifai_api_key))
            .client(OkHttpClient())
            .buildSync()

        takePicture()

        //getting recyclerview from xml
        mRecyclerView = findViewById(R.id.recipeListRecyclerView)
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

    override fun onResume() {
        super.onResume()

        // clear count flag variable
        count = 0

        filterResult()
    }

    fun takePicture() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
            || ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions()
        } else {
            // else all permissions granted, go ahead and take a picture using camera
            try {
                camera.takePicture()
            } catch (e: Exception) {
                // Show a toast for exception
                Toast.makeText(
                    this.applicationContext, "Error: " + e.localizedMessage,
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA
            ), PERMISSION_REQUEST_CODE
        )

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty()
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED
                ) {
                    try {
                        camera.takePicture()
                    } catch (e: Exception) {
                        Toast.makeText(
                            this.applicationContext, "Something went wrong!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(this, "Restart app to give permission", Toast.LENGTH_LONG).show()
                }
                return
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {

            if (requestCode == Camera.REQUEST_TAKE_PHOTO) {
                val bitmap = camera.cameraBitmap // bitmap image
                if (bitmap != null) {

                    // converting bitmap into byte array
                    val outputStream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 50, outputStream)
                    val byteArrayOfBitmap: ByteArray = outputStream.toByteArray()
                    //println("Bytes array $byteArray")

                    // To use Firebase Vision pass bitmap directly to the function
                    onImageCaptured(byteArrayOfBitmap) // passing bytearray of bitmap to function

                } else {
                    Toast.makeText(
                        this.applicationContext, "Try image capturing again",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            }
        }
        // when user did not taken photo or clicked cancel
        else {
            finish()
        }
    }

    private fun onImageCaptured(byteArrayOfBitmap: ByteArray) { // Now we will upload our image to the Clarifai API

        // clear count flag variable
        count = 0

        // Get the string array
        // It is used to compare result of prediction with our vegetables list
        val vegetableArray = resources.getStringArray(R.array.vegetables_array)

        // Button constraints
        // buttons will be added programmatically
        val llParam = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        llParam.setMargins(16, 0, 0, 0)

        // showing modify vegetables detected item options
        addVegetableItemsImageView.visibility = View.VISIBLE

        object : AsyncTask<Void?, Void?, ClarifaiResponse<List<ClarifaiOutput<Concept?>>?>>() {

            override fun doInBackground(vararg params: Void?): ClarifaiResponse<List<ClarifaiOutput<Concept?>>?> {
                val model: Model<Concept> = client.defaultModels.foodModel()

                // To get concept names from the given model ID
                /*
                val testRes = client.getModelByID("bd367be194cf45149e75f01d59f77ba7").executeSync()
                //println("Test Response:" + testRes.get())
                val listConcept = testRes.get().outputInfo()
                println(listConcept)

                 */

                return model.predict()
                    .withInputs(ClarifaiInput.forImage(byteArrayOfBitmap))
                    .withMinValue(0.7) // minimum value/prediction value
                    .withMaxConcepts(8) // maximum recognised concept we should get
                    .executeSync()
            }

            override fun onPostExecute(response: ClarifaiResponse<List<ClarifaiOutput<Concept?>>?>) {
                if (!response.isSuccessful) {
                    Toast.makeText(
                        applicationContext,
                        "Error while contacting to server",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                    return
                }
                val predictions = response.get()
                if (predictions.isEmpty()) {
                    //println("Clarif AI did not return any results")
                    Toast.makeText(
                        applicationContext,
                        "No result found. Retry capturing image",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                    return
                } else {
                    var detectedCount = 0
                    //println("Output Prediction: ${predictions.get(0).data()}")
                    //Toast.makeText(applicationContext, "Clarif AI returned results", Toast.LENGTH_LONG).show()
                    for (data in predictions[0].data()) {

                        //println("Detected items: ${data!!.name()}")

                        for (vegName in vegetableArray) {
                            if (vegName.contains(data!!.name().toString(), true)) {

                                // it used just to show horizontal detected Vegetables
                                detectedVegetables.add(vegName)

                                // if detected item > 1 then add space to the right
                                searchQuery += if (detectedCount > 0)
                                    " $vegName"
                                else
                                    "$vegName"

                                detectedCount++

                                // creating buttons to show recognised vegetables
                                val btn = Button(applicationContext)

                                // set button design
                                btn.background = ContextCompat.getDrawable(
                                    this@RecipeListByCameraActivity,
                                    R.drawable.custom_white_botton
                                )
                                // set text color
                                btn.setTextColor(
                                    ContextCompat.getColor(
                                        this@RecipeListByCameraActivity,
                                        R.color.black
                                    )
                                )
                                // set font family
                                btn.typeface =
                                    Typeface.create("sans-serif-condensed-light", Typeface.NORMAL)

                                // adding text in button
                                btn.text = data!!.name()

                                // adding button view to Layout
                                detectedItemLinearLayout.addView(
                                    btn,
                                    llParam
                                ) // detectedItemLinearLayout is id of Linear Layout

                                // stop loop if items matches as early as possible
                                break
                            }
                        }
                    }
                    if (detectedCount > 0)
                        fetchRecipeFromMongoDB(searchQuery!!, null, null)
                    else {
                        Alerter.create(this@RecipeListByCameraActivity)
                            .setTitle("No vegetable detected!")
                            .setText("Try re-capturing vegetable image again.")
                            .setBackgroundColorRes(R.color.orange)
                            .setDuration(5000)
                            .show()

                        // disabling Lottie animation
                        lottieCookingAnimation.visibility = View.GONE
                        waitTitleTextView.visibility = View.GONE

                        notFoundCameraActivityTitleTextView.visibility = View.VISIBLE
                    }


                }
            }
        }.execute()

        // enabling Lottie animation
        lottieCookingAnimation.visibility = View.VISIBLE
        waitTitleTextView.visibility = View.VISIBLE
    }

    override fun onDestroy() {
        super.onDestroy()
        // delete the image after closing of activity
        camera.deleteImage()
    }

    // finish the current activity and go to parent activity
    fun goToParentActivity(view: View) {
        finish()
    }

    fun openFilterActivity(view: View) {
        val intent = Intent(this, FilterDishActivity::class.java)
        intent.putExtra("filterRequestFrom", "RecipeListByCameraActivity")
        startActivity(intent)
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

            if (searchQuery != "") {
                // clearing old data from arraylist in case of filter applied
                // soo only new data will be available to user
                recipeDetailsArrayList.clear()

                filtersAppliedCameraActivity.visibility = View.VISIBLE

                // enabling Lottie animation
                lottieCookingAnimation.visibility = View.VISIBLE
                waitTitleTextView.visibility = View.VISIBLE

                when {
                    (storedMaxCookTime != 0 && storedDishType != null) -> {
                        if (storedDishType == "Veg") {
                            filtersAppliedCameraActivity.text =
                                "Filters applied:\nMax. cooking time: $storedMaxCookTime min.\nDish type: Vegetarian"
                        } else {
                            filtersAppliedCameraActivity.text =
                                "Filters applied:\nMax. cooking time: $storedMaxCookTime min.\nDish type: Non-Vegetarian"
                        }
                        fetchRecipeFromMongoDB(searchQuery!!, storedMaxCookTime, storedDishType)
                    }
                    (storedMaxCookTime > 0) -> {
                        filtersAppliedCameraActivity.text =
                            "Filters applied:\nMax. cooking time: $storedMaxCookTime min."
                        fetchRecipeFromMongoDB(searchQuery!!, storedMaxCookTime, null)
                    }
                    (storedDishType != null) -> {
                        if (storedDishType == "Veg") {
                            filtersAppliedCameraActivity.text =
                                "Filters applied:\nDish type: Vegetarian"
                        } else {
                            filtersAppliedCameraActivity.text =
                                "Filters applied:\nDish type: Non-Vegetarian"
                        }
                        fetchRecipeFromMongoDB(searchQuery!!, null, storedDishType)
                    }
                }
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

        //println("Search query $searchQuery")

        // getting reference of Collection and Documents
        val myCollection = mongoClient.getDatabase("snap_recipes")
            .getCollection("recipes")

        val result = mutableListOf<Document>()
        val query: RemoteFindIterable<Document>

        // Using MongoDB query see Documentation
        if (filterMaxCookTime != null && filterRecipeType != null) { // both filters selected
            val regexQry = searchQuery.replace(" ", " | ", true)
            //println("Regex query $regexQry")
            query = myCollection
                .find(
                    and(
                        regex("Ingredient", regexQry, "i"),
                        lte("Time", filterMaxCookTime),
                        eq("Type", filterRecipeType)
                    )
                )
                .sort(Document("positiveVoteCount", -1)) // sort by positiveVoteCount in descending order
                .limit(25)

        } else if (filterMaxCookTime != null && filterRecipeType == null) { // only time filter selected
            val regexQry = searchQuery.replace(" ", " | ", true)
            //println("Regex query $regexQry")
            query = myCollection
                .find(
                    and(
                        regex("Ingredient", regexQry, "i"),
                        lte("Time", filterMaxCookTime)
                    )
                )
                .sort(Document("positiveVoteCount", -1)) // sort by positiveVoteCount in descending order
                .limit(25)
        } else if (filterMaxCookTime == null && filterRecipeType != null) { // only dish type filter selected
            val regexQry = searchQuery.replace(" ", " | ", true)
            //println("Regex query $regexQry")
            query = myCollection
                .find(
                    and(
                        regex("Ingredient", regexQry, "i"),
                        eq("Type", filterRecipeType)
                    )
                )
                .sort(Document("positiveVoteCount", -1)) // sort by positiveVoteCount in descending order
                .limit(25)
        } else { // no filters selected
            val regexQry = searchQuery.replace(" ", " | ", true)
            //println("Regex query $regexQry")
            query = myCollection
                .find(regex("Ingredient", regexQry, "i"))
                .sort(Document("positiveVoteCount", -1)) // sort by positiveVoteCount in descending order
                .limit(25)
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
            Alerter.create(this@RecipeListByCameraActivity)
                .setTitle("Something went wrong!")
                .setText("Try searching again.")
                .setBackgroundColorRes(R.color.orange)
                .setDuration(5000)
                .show()

            // disabling Lottie animation
            lottieCookingAnimation.visibility = View.GONE
            waitTitleTextView.visibility = View.GONE
        }
    }

    private fun updateUIFromMongoDB(body: JSONArray, count: Int) {

        notFoundCameraActivityTitleTextView.visibility = View.GONE

        //val JSONObjectResult = JSONObject(body)

        // for testing purposes
        //println("Output in JSON: " + body + " Count: " + count)

        // hide filter applied text after getting recipe data
        if (filtersAppliedCameraActivity.isVisible)
            hideFilterAppliedText()

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
            Alerter.create(this@RecipeListByCameraActivity)
                .setTitle("No recipes found!")
                .setText("Try re-capturing vegetable image again.")
                .setBackgroundColorRes(R.color.orange)
                .setDuration(5000)
                .show()

            // disabling Lottie animation
            lottieCookingAnimation.visibility = View.GONE
            waitTitleTextView.visibility = View.GONE

            notFoundCameraActivityTitleTextView.visibility = View.VISIBLE
        }

        // Using runOnUiThread because we are currently on another thread (OkHttp creates new thread)
        // So to access/change Ui elements we have to use this
        // Otherwise we will get error
        // If you try to touch view of UI thread from another thread, you will get Android CalledFromWrongThreadException.
        this@RecipeListByCameraActivity.runOnUiThread(java.lang.Runnable {
            // disabling Lottie animation
            lottieCookingAnimation.visibility = View.GONE
            waitTitleTextView.visibility = View.GONE
            mAdapter!!.notifyDataSetChanged() // it is used to indicate that some new data add/changed
        })
    }

    fun hideFilterAppliedText() {
        // hiding filters applied text view
        // in case of filter applied
        filtersAppliedCameraActivity.visibility = View.GONE
    }

    fun editDetectedVegetables(view: View) {
        // Button constraints
        // buttons will be added programmatically
        val llParam = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        llParam.setMargins(16, 0, 0, 0)

        val simpleSearchDialogCompat =
            SimpleSearchDialogCompat(this, "Add Vegetables", "Try Potato, Tomato, Cauliflower...",
                null, createVegetableListData(),
                SearchResultListener<SearchModel> { dialog, item, position ->


                    detectedVegetables.add(0, item.title!!)

                    // adding new selected vegetable to recognised item string
                    searchQuery += " ${item.title}"

                    // enabling Lottie animation
                    lottieCookingAnimation.visibility = View.VISIBLE
                    waitTitleTextView.visibility = View.VISIBLE

                    // creating buttons to show add vegetables
                    val btn = Button(applicationContext)

                    // set button design
                    btn.background = ContextCompat.getDrawable(
                        this@RecipeListByCameraActivity,
                        R.drawable.custom_white_botton
                    )
                    // set text color
                    btn.setTextColor(
                        ContextCompat.getColor(
                            this@RecipeListByCameraActivity,
                            R.color.black
                        )
                    )
                    // set font family
                    btn.typeface =
                        Typeface.create("sans-serif-condensed-light", Typeface.NORMAL)

                    // adding text in button
                    btn.text = item!!.title

                    // adding button view to Layout
                    detectedItemLinearLayout.addView(
                        btn,
                        llParam
                    ) // detectedItemLinearLayout is id of Linear Layout

                    // call function to search again with filters
                    fetchRecipeFromMongoDB(searchQuery!!, null, null)

                    dialog!!.dismiss()
                }
            )

        simpleSearchDialogCompat.show()
    }


    // To create list of vegetable to manually add in camera mode
    private fun createVegetableListData(): ArrayList<SearchModel>? {
        val vegetablesItem = ArrayList<SearchModel>()
        // Indian Vegetable in English
        vegetablesItem.add(SearchModel("Potato"))
        vegetablesItem.add(SearchModel("Tomato"))
        vegetablesItem.add(SearchModel("Onion"))
        vegetablesItem.add(SearchModel("Lady Finger"))
        vegetablesItem.add(SearchModel("Broccoli"))
        vegetablesItem.add(SearchModel("Cabbage"))
        vegetablesItem.add(SearchModel("Cauliflower"))
        vegetablesItem.add(SearchModel("Pumpkin"))
        vegetablesItem.add(SearchModel("Beans"))
        vegetablesItem.add(SearchModel("Chickpea"))
        vegetablesItem.add(SearchModel("Pea"))
        vegetablesItem.add(SearchModel("Carrot"))
        vegetablesItem.add(SearchModel("Radish"))
        vegetablesItem.add(SearchModel("Cucumber"))
        vegetablesItem.add(SearchModel("Brinjal"))

        // Indian Vegetable in Hindi
        vegetablesItem.add(SearchModel("Aloo"))
        vegetablesItem.add(SearchModel("Aaloo"))
        vegetablesItem.add(SearchModel("Tamatar"))
        vegetablesItem.add(SearchModel("Pyaz"))
        vegetablesItem.add(SearchModel("Bhindi"))
        vegetablesItem.add(SearchModel("Gobi"))
        vegetablesItem.add(SearchModel("Gobhi"))
        vegetablesItem.add(SearchModel("Band Gobi"))
        vegetablesItem.add(SearchModel("Band Gobhi"))
        vegetablesItem.add(SearchModel("Muli"))
        vegetablesItem.add(SearchModel("Kaddu"))
        vegetablesItem.add(SearchModel("Sem"))
        vegetablesItem.add(SearchModel("Matar"))
        vegetablesItem.add(SearchModel("Mutter"))
        vegetablesItem.add(SearchModel("Gajar"))
        vegetablesItem.add(SearchModel("Kheera"))
        vegetablesItem.add(SearchModel("Baigan"))
        return vegetablesItem
    }
}

