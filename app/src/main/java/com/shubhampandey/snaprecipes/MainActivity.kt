package com.shubhampandey.snaprecipes

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.analytics.FirebaseAnalytics
import com.mongodb.stitch.android.core.Stitch
import com.mongodb.stitch.android.core.StitchAppClient
import com.mongodb.stitch.core.auth.providers.anonymous.AnonymousCredential
import com.tapadoo.alerter.Alerter
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity(), PopupMenu.OnMenuItemClickListener {

    private lateinit var mFirebaseAnalytics: FirebaseAnalytics

    private val TAG = "MainActivity"

    // Flag to check if user's account is enabled or not
    private var userStatus = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Making device rotation disabled. Only portrait will be allowed
        this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        // Obtain the FirebaseAnalytics instance.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this)

        val stitchAppClient = Stitch.getDefaultAppClient()
        signInUser(stitchAppClient)
    }

    private fun signInUser(
        stitchAppClient: StitchAppClient
    ) {
        stitchAppClient.auth.loginWithCredential(AnonymousCredential()) // Anonymous login
            .addOnSuccessListener {
                // add user id to shared preferences
                //println("Success Login")
                //getDataFromMongoDB(myCollection)

                // marking flag to true
                // means user's account is enabled and not blocked
                userStatus = true
                Log.i(TAG, "Login success for user id ${stitchAppClient.auth.user!!.id}")
            }.addOnFailureListener {
                userStatus = false
                Log.e(TAG, "Login failed")
                Toast.makeText(this, "Authentication failed", Toast.LENGTH_SHORT).show()
            }
    }

    fun showPopup(view: View) {
        PopupMenu(this, view).apply {
            // MainActivity implements OnMenuItemClickListener
            setOnMenuItemClickListener(this@MainActivity)
            inflate(R.menu.homescreenoption)
            show()
        }
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.myFavoriteMenuOption -> {
                val intent = Intent(this, FavoriteListActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.requestRecipeMenuOption -> {
                sendEmail()
                true
            }
            R.id.developerInfoMenuOption -> {
                val intent = Intent(this, DeveloperInfoActivity::class.java)
                startActivity(intent)
                true
            }
            else -> false
        }
    }

    private fun sendEmail() {
        val eMailId1 = "shubhamp922@live.com"
        val eMailIds: Array<String> = arrayOf(eMailId1)
        val subject = "Add a recipe for Snap Recipe app"
        val intent = Intent(Intent.ACTION_SENDTO)
        intent.data = Uri.parse("mailto:") // only email apps should handle this
        intent.putExtra(Intent.EXTRA_EMAIL, eMailIds)
        intent.putExtra(Intent.EXTRA_SUBJECT, subject)
        intent.putExtra(
            Intent.EXTRA_TEXT,
            "Hey Developer/Admin, \n\nPlease add a new recipe [RECIPE NAME HERE] which i don't found on search.\n\nThanks"
        )
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            Toast.makeText(this, "No email client found to send request!", Toast.LENGTH_LONG).show()
        }
    }

    private fun checkInternetConnectivity(): Boolean {
        val connectivityManager =
            this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworkInfo = connectivityManager.activeNetworkInfo
        if (activeNetworkInfo != null && activeNetworkInfo.isConnected) {
            startCameraModeLL.isClickable = true
            startSearchModeLL.isClickable = true

            return true
        } else {
            Alerter.create(this)
                .setTitle("Internet Connectivity Failed!")
                .setText("Try turning off/on WiFi/Mobile data again.")
                .setBackgroundColorRes(R.color.red)
                .setDuration(5000)
                .show()

            return false
        }
    }

    fun startCameraFindRecipe(view: View) {
        val connectvityResult = checkInternetConnectivity()
        val userStatusResult = checkUserAccountStatus()
        if (connectvityResult && userStatusResult) {
            //Toast.makeText(this, "Camera Mode", Toast.LENGTH_LONG).show()
            val intent = Intent(this, RecipeListByCameraActivity::class.java)
            startActivity(intent)
        }

    }

    private fun checkUserAccountStatus(): Boolean {
        if (userStatus) {
            return true
        }
        else {
            Alerter.create(this)
                .setTitle("Authentication failed!")
                .setBackgroundColorRes(R.color.red)
                .setDuration(3000)
                .show()

            return false
        }
    }

    fun startSearchFindRecipe(view: View) {
        val connectvityResult = checkInternetConnectivity()
        if (connectvityResult) {
            //Toast.makeText(this, "Search Mode", Toast.LENGTH_LONG).show()
            val intent = Intent(this, RecipeListBySearchActivity::class.java)
            startActivity(intent)
        }

    }
}
