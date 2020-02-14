package com.shubhampandey.snaprecipes

// data class for showing data in recycler view
data class RecipeDetailsDataClass(
    var recipeTitle: String? = null,
    var recipeSource: String? = null,
    var recipeCalories: String? = null,
    var recipeDuration: String? = null,
    var recipeImageURL: String? = null,
    var recipeServing: String? = null, // No. of person can eat
    var recipeIngredients: String? = null,
    var recipeCookingSteps: String? = null,
    var recipeSourceURL: String? = null,
    var cookingDifficulty: String? = null,
    var recipeShortDescription: String? = null,
    var recipeType: String? = null // veg or non veg

)