package edu.cuhk.csci3310.photodiary

import android.content.Context
import android.content.Intent
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.google.android.gms.location.*
import kotlinx.android.synthetic.main.activity_new_photo.*
import java.io.File
import java.lang.Exception
import java.util.*


class NewPhotoActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var location:Location? = null
    private var path: String? = null
    private lateinit var locationCallback: LocationCallback


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_photo)
        val intent: Intent = getIntent()
        path = intent.getStringExtra("path")
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return
                for (loc in locationResult.locations){
                    System.out.println("Success last location")
                    if (loc != null) {
                        location = loc
                    }
                    val locationManager =
                        getSystemService(Context.LOCATION_SERVICE) as LocationManager

                    val providerList:List<String> = locationManager.allProviders
                    if (location!=null && providerList.isNotEmpty()){
                        val longitude = location!!.longitude
                        val latitude = location!!.latitude
                        val geocoder = Geocoder(applicationContext, Locale.getDefault())
                        try {
                            val listAddress: List<Address> = geocoder.getFromLocation(latitude, longitude, 1);
                            if (listAddress.isNotEmpty()){
                                val adr = listAddress[0].getAddressLine(0)
                                textView.setText(adr)
                            }
                        } catch(e:Exception) {

                        }
                    }
                }
            }
        }
        startLocationUpdates()
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location : Location? ->
                // Got last known location. In some rare situations this can be null.
                System.out.println("Success last location")
                if (location != null) {
                    this.location = location
                }
                val locationManager =
                    getSystemService(Context.LOCATION_SERVICE) as LocationManager

                val providerList:List<String> = locationManager.allProviders
                if (location!=null && providerList.isNotEmpty()){
                    val longitude = location.longitude
                    val latitude = location.latitude
                    val geocoder = Geocoder(applicationContext, Locale.getDefault())

                    val listAddress: List<Address> = geocoder.getFromLocation(latitude, longitude, 1);
                    if (listAddress.isNotEmpty()){
                        val adr = listAddress[0].getAddressLine(0)
                        textView.setText(adr)
                    }
                    fusedLocationClient.removeLocationUpdates(locationCallback)

                }
            }
        val file: File? = getExternalFilesDir(path)

        var uri: Uri? = null
        if (file != null) {
            System.out.println(file.absolutePath)
            uri = FileProvider.getUriForFile(this,"com.example.android.fileprovider", file)
        }
        if (uri != null){
            imageView.setImageURI(uri)
        }

    }

    private fun startLocationUpdates() {
        fusedLocationClient.requestLocationUpdates(
            LocationRequest(),
            locationCallback,
            Looper.getMainLooper())
    }

}
