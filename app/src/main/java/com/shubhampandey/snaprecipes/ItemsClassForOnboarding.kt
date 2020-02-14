package com.shubhampandey.snaprecipes

class ItemsClassForOnboarding(first:String, titles:String, description:String, images:Int) {
    val titles:String
    val description:String
    val first:String
    var images:Int = 0

    init{
        this.first = first
        this.titles = titles
        this.description = description
        this.images = images
    }
}