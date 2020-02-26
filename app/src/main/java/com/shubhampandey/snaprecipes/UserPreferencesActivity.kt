package com.shubhampandey.snaprecipes

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import com.google.android.material.chip.Chip
import kotlinx.android.synthetic.main.activity_user_preferences.*

class UserPreferencesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //hide status bar
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN)

        setContentView(R.layout.activity_user_preferences)

        //when this activity is about to launch we need to check its opened true or false
        if (restorePrefData())
        {
            val mainActivity = Intent(applicationContext, MainActivity::class.java)
            startActivity(mainActivity)
            finish()
        }

    }


    private fun savePrefData() {
        val dishTypeChip: Chip = findViewById(dishTypePreference.checkedChipId)
        val dietBasedChip: Chip = findViewById(dietBasedPreferece.checkedChipId)

        val sharedPreferences = this.getSharedPreferences("userPreference", android.content.Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putBoolean("Opened", true)
        editor.putString("dishTypePref", dishTypeChip.text.toString())
        editor.putString("dietBasedPref", dietBasedChip.text.toString())

        println("Diet based ${dietBasedChip.text}")
        editor.apply()
    }

    private fun restorePrefData(): Boolean {
        val sharedPreferences = this.getSharedPreferences("userPreference", android.content.Context.MODE_PRIVATE)
        val ActivityOpen: Boolean
        ActivityOpen = sharedPreferences.getBoolean("Opened", false)
        return ActivityOpen
    }

    fun saveDishPreference(view: View) {
        savePrefData()

        val mainActivity = Intent(applicationContext, MainActivity::class.java)
        startActivity(mainActivity)
        finish()
    }
}
