package com.shubhampandey.snaprecipes

import ir.mirrajabi.searchdialog.core.Searchable

// Model class for search dialog library
class SearchModel(private var mTitle: String) : Searchable {

    fun setTitle(mTitle: String) {
        this.mTitle = mTitle
    }

    override fun getTitle(): String? {
        return mTitle
    }
}