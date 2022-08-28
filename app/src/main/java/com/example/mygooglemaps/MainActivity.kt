package com.example.mygooglemaps

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.mygooglemaps.Utils.Companion.TAG_MAP_FRAGMENT
import com.example.mygooglemaps.Utils.Companion.TAG_MARKERS_LIST_FRAGMENT

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initMapFragment()
    }

    private fun initMapFragment() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.main_container, MapsFragment.newInstance(), TAG_MAP_FRAGMENT)
            .commit()
    }

    override fun onBackPressed() {
        supportFragmentManager.findFragmentByTag(TAG_MARKERS_LIST_FRAGMENT)?.let {
            supportFragmentManager.beginTransaction().replace(R.id.main_container, MapsFragment.newInstance(), TAG_MAP_FRAGMENT).commit()
        } ?: super.onBackPressed()
    }

}