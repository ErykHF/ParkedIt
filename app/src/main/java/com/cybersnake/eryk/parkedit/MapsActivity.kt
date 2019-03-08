package com.cybersnake.eryk.parkedit

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.android.synthetic.main.activity_maps.*


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var locationRequest: LocationRequest = LocationRequest()
    private var locationCallback: LocationCallback = LocationCallback()
    private val MY_PERMISSION_ACCESS_COURSE_LOCATION = 10
    private val DEFAULT_ZOOM = 16f
    private lateinit var mMap: GoogleMap


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        permissions()

    }

    override fun onDestroy() {

        super.onDestroy()

            fusedLocationClient.removeLocationUpdates(locationCallback)

    }


    override fun onPause() {
        super.onPause()

        fusedLocationClient.removeLocationUpdates(locationCallback)

    }

    fun isLocationOn() {

        var service: LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        var gpsEnabled: Boolean = service.isProviderEnabled(LocationManager.GPS_PROVIDER)

        if (!gpsEnabled) {


            val enableGPS = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(enableGPS)
        }

    }

    private fun permissions(){
        if (Build.VERSION.SDK_INT>=23){
            if (ActivityCompat.
                            checkSelfPermission(this,
                                    android.Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){
                requestPermissions(arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), MY_PERMISSION_ACCESS_COURSE_LOCATION)
                return
            }

        }
        isLocationOn()
        setMarker()
    }




    @SuppressLint("MissingPermission")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode)  {
            MY_PERMISSION_ACCESS_COURSE_LOCATION -> {

                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permissions Granted", Toast.LENGTH_SHORT).show()
                    this.isLocationOn()
                    this.setMarker()
                    return


                } else if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    val dialog = AlertDialog.Builder(this)
                    dialog.setMessage("This Permission Needs To Be Granted In Order To Use The App").setTitle("Location Permission Required")

                    dialog.setPositiveButton("OK") { dialog, which ->
                        permissions()
                    }

                    dialog.setNegativeButton("No") { dialog, which -> Toast.makeText(this@MapsActivity, "Location Permission Required", Toast.LENGTH_LONG).show() }

                    dialog.show()

                }
            }
        }
    }


    @SuppressLint("MissingPermission")
    fun setMarker() {

        val lastLocation = fusedLocationClient.lastLocation!!

        lastLocation?.addOnCompleteListener {
            if (it.isSuccessful) {
                Log.d("Last Location", "Location Found $lastLocation")
                val currentLocation = lastLocation?.result
                currentLocation
                mMap.isMyLocationEnabled = true
                mMap.uiSettings.isMyLocationButtonEnabled = true
                Toast.makeText(this, "Location Found", Toast.LENGTH_LONG).show()
                if (currentLocation != null) {
                    Log.d("Last Location", "Location Found $lastLocation")
                    currentLocation
                    locationUpdates()
//                    mMap.isMyLocationEnabled = true
//                    mMap.uiSettings.isMyLocationButtonEnabled = true
                    moveCamera(latLng = LatLng(currentLocation!!.latitude, currentLocation!!.longitude), zoom = DEFAULT_ZOOM, title = "My Car's Location")
                }else if (currentLocation == null){
                    locationUpdates()
                }



            }


        }


    }


    fun moveCamera(latLng: LatLng, zoom: Float, title: String) {

        Log.d("Move Camera", "Moving Camera To: ${latLng.latitude}  ${latLng.longitude}")
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom))
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom))

        var mycarMarker = mMap.addMarker(MarkerOptions().position(latLng).title(title).icon(BitmapDescriptorFactory.fromResource(R.drawable.car_marker)).draggable(true))
        mycarMarker.isVisible = false
        mMap.clear()


        carIcon.setOnClickListener {

            if (!mycarMarker.isVisible) {

            AlertDialog.Builder(this)
                    .setTitle("Adds A Marker To Your Current Location")
                    .setMessage("Is This Where You Have Parked?")
                    .setPositiveButton("Yes", DialogInterface.OnClickListener { dialogInterface, i ->



                           mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom))

                           mycarMarker = mMap.addMarker(MarkerOptions().position(latLng).title(title).icon(BitmapDescriptorFactory.fromResource(R.drawable.car_marker)).draggable(true))

                            Toast.makeText(this, "Car Parked!", Toast.LENGTH_LONG).show()

                            Log.d("Runtime", "Parked up")



                    })
                    .setNegativeButton("No", DialogInterface.OnClickListener { dialogInterface, i ->


                    })
                    .show()
        } else if (mycarMarker.isVisible){

                    Toast.makeText(this, "You have already parked", Toast.LENGTH_LONG).show()

            }


        }


        removeIcon.setOnClickListener {
            if (mycarMarker.isVisible) {


                AlertDialog.Builder(this)
                        .setTitle("Removes Your Current Location Marker")
                        .setMessage("Are You Sure You Want To Remove The Current Location Marker?")
                        .setPositiveButton("Yes", DialogInterface.OnClickListener { dialogInterface, i ->

                            mycarMarker.remove()
                            mycarMarker.isVisible = false
                            mMap.clear()



                        })
                        .setNegativeButton("No", DialogInterface.OnClickListener { dialogInterface, i ->

                        })
                        .show()

            } else if (!mycarMarker.isVisible){
                Toast.makeText(this, "You haven't parked yet", Toast.LENGTH_LONG).show()

            }

        }


    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */

    override fun onMapReady(googleMap: GoogleMap) {

        mMap = googleMap

    }

    private fun locationUpdates() {


        locationRequest = LocationRequest()
        locationRequest.interval = 4000
        locationRequest.fastestInterval = 2000
        locationRequest.priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                //Location Permission already granted
                val updateRequest = fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
                mMap.isMyLocationEnabled = true
                mMap.uiSettings.isMyLocationButtonEnabled = true
                Log.d("MMap2", updateRequest.toString())
            } else {
                //Request Location Permission
//                runtimePermissions()
            }
        } else {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
            mMap.isMyLocationEnabled = true
            mMap.uiSettings.isMyLocationButtonEnabled = true
            Log.d("MMap", "This works from the else statement")

        }


    }


}