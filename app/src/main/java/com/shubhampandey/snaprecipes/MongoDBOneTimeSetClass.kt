package com.shubhampandey.snaprecipes

import com.mongodb.stitch.android.core.Stitch


// Class to set MongoDB Client.
// Initialising the client requires only one time
// So extending Application class
// And adding entry into manifest>Application file about this class
class MongoDBOneTimeSetClass : android.app.Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialise Stitch
        // Required only for one time initialisation
        Stitch.initializeDefaultAppClient(
            resources.getString(R.string.stitch_app_id)
        )
    }
}