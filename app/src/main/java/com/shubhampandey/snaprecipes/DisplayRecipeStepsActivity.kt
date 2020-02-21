package com.shubhampandey.snaprecipes

import android.graphics.Typeface
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.analytics.FirebaseAnalytics
import com.mongodb.stitch.android.core.Stitch
import com.mongodb.stitch.android.services.mongodb.remote.RemoteMongoClient
import com.mongodb.stitch.android.services.mongodb.remote.RemoteMongoCollection
import com.mongodb.stitch.core.services.mongodb.remote.RemoteUpdateOptions
import com.mongodb.stitch.core.services.mongodb.remote.RemoteUpdateResult
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_display_recipe_steps.*
import org.bson.Document
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

    private lateinit var mongoClient: RemoteMongoClient
    private lateinit var myCollection: RemoteMongoCollection<org.bson.Document>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_display_recipe_steps)

        assert(
            supportActionBar != null //null check
        )
        supportActionBar!!.setDisplayHomeAsUpEnabled(true) //show back button

        // Obtain the FirebaseAnalytics instance.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this)

        initialiseStitch()

        // getting reference of Collection and Documents
        myCollection = mongoClient.getDatabase("snap_recipes")
            .getCollection("recipes")

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

    private fun initialiseStitch() {
        val stitchAppClient = Stitch.getDefaultAppClient()
        // Getting service from Stitch that we want to use
        mongoClient = stitchAppClient.getServiceClient(
            RemoteMongoClient.factory,
            "snap-recipes-mongodb-atlas"
        )
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

            recipeStepsTextView.setPadding(10, 20, 4, 20)
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

    fun iLikedRecipe(view: View) {
        feedbackProgressBar.visibility = View.VISIBLE
        updatePositiveVoteDataInMongoDB()
    }

    fun iDislikedRecipe(view: View) {
        feedbackProgressBar.visibility = View.VISIBLE
        updateNegativeVoteDataInMongoDB()
    }

    private fun updatePositiveVoteDataInMongoDB() {
        // filter, for where we have to update value
        val filterDoc = Document("Name", receivedRecipeTitle)
        // increment count by 1
        // $inc is keyword for incrementing
        val updateDoc = Document("\$inc", Document("positiveVoteCount", 1))
        // It will, Insert a single new document into the collection if update does not match
        val options = RemoteUpdateOptions().upsert(true)

        val updateTask = myCollection.updateOne(filterDoc, updateDoc, options)
        updateTask.addOnCompleteListener { p0 ->
            if ( p0.isSuccessful) {

                updateUIForFeedback()

                if ( p0.result?.upsertedId != null) {
                    val upsertedId = p0.result!!.upsertedId.toString()
                    Log.i(TAG, String.format("successfully upserted document with id: %s",
                        upsertedId))
                } else {
                    val numMatched = p0.result!!.matchedCount
                    val numModified = p0.result!!.modifiedCount
                    Log.i(TAG, String.format("successfully matched %d and modified %d documents",
                        numMatched, numModified))
                }
            } else {
                feedbackProgressBar.visibility = View.GONE
                Log.e(TAG, "failed to update document with: ", p0.exception)
                Toast.makeText(this, p0.exception!!.localizedMessage, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateNegativeVoteDataInMongoDB() {
        // filter, for where we have to update value
        val filterDoc = Document("Name", receivedRecipeTitle)
        // increment count by 1
        // $inc is keyword for incrementing
        val updateDoc = Document("\$inc", Document("negativeVoteCount", 1))
        // It will, Insert a single new document into the collection if update does not match
        val options = RemoteUpdateOptions().upsert(true)

        val updateTask = myCollection.updateOne(filterDoc, updateDoc, options)
        updateTask.addOnCompleteListener { p0 ->
            if ( p0.isSuccessful) {
                updateUIForFeedback()
                if ( p0.result?.upsertedId != null) {
                    val upsertedId = p0.result!!.upsertedId.toString()
                    Log.i(TAG, String.format("successfully upserted document with id: %s",
                        upsertedId))
                } else {
                    val numMatched = p0.result!!.matchedCount
                    val numModified = p0.result!!.modifiedCount
                    Log.i(TAG, String.format("successfully matched %d and modified %d documents",
                        numMatched, numModified))
                }
            } else {
                feedbackProgressBar.visibility = View.GONE
                Log.e(TAG, "failed to update document with: ", p0.exception)
                Toast.makeText(this, p0.exception!!.localizedMessage, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateUIForFeedback() {
        feedbackLL.visibility = View.GONE
        feedbackDoneTextView.visibility = View.VISIBLE
        feedbackProgressBar.visibility = View.GONE
    }
}
