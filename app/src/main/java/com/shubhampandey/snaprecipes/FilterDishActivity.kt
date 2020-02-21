package com.shubhampandey.snaprecipes

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.analytics.FirebaseAnalytics
import kotlinx.android.synthetic.main.activity_filter_dish.*

class FilterDishActivity : AppCompatActivity() {

    private val Tag = "FilterDishActivity"

    private lateinit var mFirebaseAnalytics: FirebaseAnalytics

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_filter_dish)

        // Obtain the FirebaseAnalytics instance.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this)

        val intent = intent
        val requestFromActivity = intent.getStringExtra("filterRequestFrom")

    }

    fun closeFilterActivity(view: View) {
        finish()
    }

    fun applyDishFiltersButton(view: View) {
        val maxCookingTimeSelectedID = maxDishTimeRadioGroup.checkedRadioButtonId
        val dishTypeSelectedID = dishTypeRadioGroup.checkedRadioButtonId

        val maxCookTimeValue: Int = when (maxCookingTimeSelectedID) {
            R.id.uptoTenradioButton -> {
                10
            }
            R.id.uptoTwentyradioButton -> {
                20
            }
            R.id.uptoFourtyradioButton -> {
                40
            }
            R.id.uptoEightyradioButton -> {
                80
            }
            R.id.uptoOneSixtyradioButton -> {
                160
            }
            else -> {
                0
            }
        }

        val dishTypeValye = when (dishTypeSelectedID) {
            R.id.vegradioButton -> {
                "Veg"   // these string are according to the data stored in db
            }
            R.id.nonVegRadioButton -> {
                "Non_Veg"
            }
            else -> {
                "any"
            }
        }

        if (!TextUtils.equals("0", maxCookTimeValue.toString()) || !TextUtils.equals("any", dishTypeValye)) {
            //println("Sort value $sortValue")
            //println("Dish Type value $dishTypeValye")

            val sharedPreferences = this.getSharedPreferences("com.shubhampandey.snaprecipes", android.content.Context.MODE_PRIVATE)
            if (maxCookTimeValue > 0 && !dishTypeValye.equals("any", true)) { // when both filters selected
                sharedPreferences.edit().putInt("maxCookingTime", maxCookTimeValue).apply()
                sharedPreferences.edit().putString("dishType", dishTypeValye).apply()
            }
            else if (maxCookTimeValue > 0) // when only time filter selected
                sharedPreferences.edit().putInt("maxCookingTime", maxCookTimeValue).apply()
            else if( !dishTypeValye.equals("any", true))  // when dish type filter selected
                sharedPreferences.edit().putString("dishType", dishTypeValye).apply()
            finish()
        }
        else {
            Snackbar.make(view, "Filter options must be selected", Snackbar.LENGTH_LONG).show()
            Log.w(Tag, "Atleast one filter option must be selected")
        }
    }
}
