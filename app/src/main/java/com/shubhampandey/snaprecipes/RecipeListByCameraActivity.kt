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
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.mindorks.paracamera.Camera
import com.tapadoo.alerter.Alerter
import ir.mirrajabi.searchdialog.SimpleSearchDialogCompat
import ir.mirrajabi.searchdialog.core.SearchResultListener
import kotlinx.android.synthetic.main.activity_recipe_list_by_camera.*
import okhttp3.*
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

    // Using Edamam API to get recipe details
    val edamamAppId = "88149556"
    val edamamApplicationKey = "be8c65cb0db50a3fe48df3c6e95ee6f1"

    private val TAG: String = "RecipeListByCamera"

    // global variable to store details of vegetables recognised in image
    var detectedVegetables = arrayListOf<String>()

    private var mDatabase: FirebaseDatabase? = null

    // flag variable which is used to find if atleast 1 recipe is found or not
    var count = 0

    val maxRecipePerItem = 20

    private var recipeJSONObjectArray: JSONArray = JSONArray()

    private lateinit var mFirebaseAnalytics: FirebaseAnalytics

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recipe_list_by_camera)

        // Making device rotation disabled. Only portrait will be allowed
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)

        // initialising firebase
        mDatabase = FirebaseDatabase.getInstance()

        // Obtain the FirebaseAnalytics instance.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this)


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
                    //println("Output Prediction: ${predictions.get(0).data()}")
                    //Toast.makeText(applicationContext, "Clarif AI returned results", Toast.LENGTH_LONG).show()
                    for (data in predictions[0].data()) {

                        //println("Detected items: ${data!!.name()}")

                        for (vegName in vegetableArray) {
                            if (vegName.contains(data!!.name().toString(), true)) {

                                detectedVegetables.add(vegName)

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
                    //println("Final list to search for: $detectedVegetables")
                    //Toast.makeText(applicationContext, detectedVegetables, Toast.LENGTH_LONG).show()
                    fetchRecipeDataFromFirebase(detectedVegetables, null, null)
                }
            }
        }.execute()

        // Firebase vision AI
        // To use this, change the function delcaration parameter to Bitmap
        /*
        val image = FirebaseVisionImage.fromBitmap(bitmap)
        /*
        val options = FirebaseVisionCloudImageLabelerOptions.Builder()
            .setConfidenceThreshold(0.8F)
            .build()

         */
        val labeler = FirebaseVision.getInstance().getCloudImageLabeler()

        labeler.processImage(image)
            .addOnSuccessListener { labels ->

                Toast.makeText(
                    this.applicationContext, "Vegetables found",
                    Toast.LENGTH_SHORT
                ).show()
                var sno = 0
                for (label in labels) {
                    sno++
                    val text = label.text
                    val entityId = label.entityId
                    val confidence = label.confidence
                    /*
                    println("Label Text: $text")
                    println("Entity ID: $entityId")
                    println("Confidence: $confidence")

                     */

                    val recipeDetails = RecipeDetailsDataClass() // RHS is a dataclass
                    recipeDetails.sno = sno
                    recipeDetails.recipeTitle = text
                    recipeDetails.recipeDuration = confidence.toString()

                    recipeDetailsArrayList.add(recipeDetails)
                }
                mAdapter!!.notifyDataSetChanged() // it is used to indicate that some new data add/changed
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this.applicationContext, "No vegetables found",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
         */

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

    fun fetchRecipeDataFromFirebase(
        searchQueryArr: ArrayList<String>,
        filterMaxCookTime: Int?,
        filterRecipeType: String?
    ) {
        // clearing old data from arraylist in case of filter applied
        // soo only new data will be available to user
        recipeDetailsArrayList.clear()
        recipeJSONObjectArray = JSONArray()

        var maxRecipePerDetectedItem = 0

        if (searchQueryArr.isNotEmpty()) {
            maxRecipePerDetectedItem = (maxRecipePerItem/searchQueryArr.size)
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

                                    if ((hashMap["Name"].toString().contains(searchQuery, true)
                                                || hashMap["Ingredient"].toString().contains(
                                            searchQuery,
                                            true
                                        )
                                                || hashMap["steps"].toString().contains(
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
                                    if ((hashMap["Name"].toString().contains(searchQuery, true)
                                                || hashMap["Ingredient"].toString().contains(
                                            searchQuery,
                                            true
                                        )
                                                || hashMap["steps"].toString().contains(
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
                                    if ((hashMap["Name"].toString().contains(searchQuery, true)
                                                || hashMap["Ingredient"].toString().contains(
                                            searchQuery,
                                            true
                                        )
                                                || hashMap["steps"].toString().contains(
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
                .setDuration(10000)
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
        val storedDishType = sharedPreferences.getString("dishType", "null")

        if ((storedMaxCookTime != 0 && storedDishType != "null") && (storedMaxCookTime != 0 && storedDishType != "any")) {
            val searchQuery = detectedVegetables

            if (searchQuery.isNotEmpty()) {

                // displaying filters applied text view
                filtersAppliedCameraActivity.visibility = View.VISIBLE

                // enabling Lottie animation
                lottieCookingAnimation.visibility = View.VISIBLE
                waitTitleTextView.visibility = View.VISIBLE

                if (storedDishType == "Veg") {
                    filtersAppliedCameraActivity.text =
                        "Filters applied:\nMax. cooking time: $storedMaxCookTime min.\nDish type: Vegetarian"
                }
                else {
                    filtersAppliedCameraActivity.text =
                        "Filters applied:\nMax. cooking time: $storedMaxCookTime min.\nDish type: Non-Vegetarian"
                }

                fetchRecipeDataFromFirebase(searchQuery, storedMaxCookTime, storedDishType)

                //println(filterSearchQuery)

                // after usage removing values from shared preferences
                sharedPreferences.edit().remove("maxCookingTime").apply()
                sharedPreferences.edit().remove("dishType").apply()
            }
        }
    }

    fun hideFilterAppliedText() {
        // hiding filters applied text view
        // in case of filter applied
        filtersAppliedCameraActivity.visibility = View.GONE
    }

    fun editDetectedVegetables(view: View) {
        /*
        val intent = Intent(this, ModifyDetectedVegetablesActivity::class.java)
        intent.putExtra("detectedVegetableItemsFromCamera", detectedVegetables)
        startActivity(intent)

         */

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

                    // adding new selected vegetable to recognised item string
                    detectedVegetables.add(0, item.title!!)

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
                    fetchRecipeDataFromFirebase(detectedVegetables, null, null)

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
        vegetablesItem.add(SearchModel("Banad Gobi"))
        vegetablesItem.add(SearchModel("Band Gobi"))
        vegetablesItem.add(SearchModel("Banad Gobhi"))
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

