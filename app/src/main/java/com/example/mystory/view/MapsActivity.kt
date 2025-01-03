package com.example.mystory.view

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.viewModels
import com.example.mystory.R
import com.example.mystory.api.ApiConfig
import com.example.mystory.data.session.SessionManager
import com.example.mystory.data.story.Story
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.example.mystory.databinding.ActivityMapsBinding
import com.example.mystory.model.MapsViewModel
import com.example.mystory.model.ViewModelFactory
import com.example.mystory.model.StoryRepository
import com.google.android.gms.maps.model.LatLngBounds

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var backButton: ImageView
    private var stories: List<Story> = emptyList()
    private var isMapReady = false

    private val mapsViewModel: MapsViewModel by viewModels {
        ViewModelFactory(
            StoryRepository(ApiConfig.apiService),
            SessionManager(this)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        mapsViewModel.storiesWithLocation.observe(this) { newStories ->
            stories = newStories
            if (isMapReady) {
                displayMarkers()
            }
        }

        mapsViewModel.fetchStoriesWithLocation()

        backButton = findViewById(R.id.btn_back)
        backButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        isMapReady = true
        if (stories.isNotEmpty()) {
            displayMarkers()
        }
    }

    private fun displayMarkers() {
        mMap.clear() // Clear existing markers
        if (stories.isEmpty()) return

        val boundsBuilder = LatLngBounds.Builder()
        stories.forEach { story ->
            val latLng = LatLng(story.lat ?: 0.0, story.lon ?: 0.0)
            mMap.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title(story.name)
                    .snippet(story.description)
            )
            boundsBuilder.include(latLng)
        }

        try {
            val bounds = boundsBuilder.build()
            binding.map.post {
                try {
                    mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
                } catch (e: Exception) {
                    // Fallback to using specific dimensions if post() method fails
                    val width = resources.displayMetrics.widthPixels
                    val height = resources.displayMetrics.heightPixels
                    mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, width, height, 100))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to a default location or handle error
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(-6.8957643, 107.6338462), 10f))
        }
    }
}



