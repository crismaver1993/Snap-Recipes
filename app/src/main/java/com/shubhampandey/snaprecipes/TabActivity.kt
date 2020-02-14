package com.shubhampandey.snaprecipes

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import com.google.firebase.analytics.FirebaseAnalytics


// This activity is for Onboarding

class TabActivity : AppCompatActivity() {

    lateinit var viewPager: ViewPager
    lateinit var tabPageAdapter:TabPagerAdaperForOnboarding
    lateinit var tab: TabLayout
    lateinit var start: Button
    lateinit var swipe: TextView

    private lateinit var mFirebaseAnalytics: FirebaseAnalytics

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //when this activity is about to launch we need to check its opened true or false
        if (restorePrefData())
        {
            val mainActivity = Intent(applicationContext, MainActivity::class.java)
            startActivity(mainActivity)
            finish()
        }

        // Obtain the FirebaseAnalytics instance.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this)

        //hide status bar
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN)

        setContentView(R.layout.activity_tab)

        swipe = findViewById(R.id.Swipe)
        start = findViewById(R.id.Start)
        tab = findViewById(R.id.Tab)

        // Add content here for onboarding
        val content = ArrayList<ItemsClassForOnboarding>()
        content.add(ItemsClassForOnboarding("Hi there", "Welcome on Snap Recipes", "Have a look of this recipe application \ndesigned for you easy to use and explore ", R.drawable.one))
        content.add(ItemsClassForOnboarding("", "Camera Mode", "Get decent recipes easily by just \ncapturing image of vegetable easy isn't? ", R.drawable.two))
        content.add(ItemsClassForOnboarding("", "Search Mode", "Also, get recipes by just searching for it or \nby just giving vegetable items name in search mode ", R.drawable.three))
        content.add(ItemsClassForOnboarding("", "Specially For भारत", "Get best recipes in Snap Recipes \napp specially built for Indian recipes ", R.drawable.four))

        //set viewPager
        viewPager = findViewById(R.id.viewPager)
        tabPageAdapter = TabPagerAdaperForOnboarding(this, content)
        viewPager.adapter = tabPageAdapter;
        //setup the tab layout with viewPager
        tab.setupWithViewPager(viewPager)

        tab.addOnTabSelectedListener(object : TabLayout.BaseOnTabSelectedListener<TabLayout.Tab> {
            override fun onTabSelected(tab:TabLayout.Tab) {
                //If tab layout switch to last page the button will pop with animation
                if (tab.position == content.size - 1)
                {
                    animateViewIn()
                }
                else if (tab.position == content.size - 2)
                {
                    animateViewOut()
                }
            }
            override fun onTabUnselected(tab:TabLayout.Tab) {
            }
            override fun onTabReselected(tab:TabLayout.Tab) {
            }
        })

        start.setOnClickListener {
            //it already checked the TabActivity use shared preference to know true or false
            savePrefData()
            //If yes open MainActivity
            val main = Intent(applicationContext, MainActivity::class.java)
            startActivity(main)
            finish()
        }
    }

    private fun animateViewOut() {
        swipe.visibility = View.VISIBLE
        start.visibility = View.GONE
        tab.visibility = View.VISIBLE
    }

    private fun animateViewIn() {
        //Hiding swip right text, tabs, and set Start Button Visible
        swipe.visibility = View.INVISIBLE
        start.visibility = View.VISIBLE
        tab.visibility = View.INVISIBLE
        val root = findViewById<RelativeLayout>(R.id.one)
        val count = root.childCount
        var offSet: Float = resources.getDimensionPixelSize(R.dimen.offset).toFloat()
        val interpolator = AnimationUtils.loadInterpolator(this, android.R.interpolator.linear_out_slow_in)
        //duration + interpolator
        for (i in 0 until count)
        {
            val view = root.getChildAt(i)
            view.visibility = View.VISIBLE
            view.translationX = offSet
            view.alpha = 0.85f
            view.animate()
                .translationX(0f)
                .translationY(0f)
                .alpha(1f)
                .setInterpolator(interpolator)
                .setDuration(1000L)
                .start()
            offSet *= 1.5f
        }
    }

    private fun savePrefData() {
        val preferences = applicationContext.getSharedPreferences(
            "Pre",
            Context.MODE_PRIVATE
        )
        val editor = preferences.edit()
        editor.putBoolean("Opened", true)
        editor.apply()
    }

    private fun restorePrefData(): Boolean {
        val preferences = applicationContext.getSharedPreferences(
            "Pre",
            Context.MODE_PRIVATE
        )
        val ActivityOpen: Boolean
        ActivityOpen = preferences.getBoolean("Opened", false)
        return ActivityOpen
    }
}
