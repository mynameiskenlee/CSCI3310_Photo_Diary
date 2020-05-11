package edu.cuhk.csci3310.photodiary

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.icu.text.SimpleDateFormat
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import com.google.android.gms.location.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.activity_new_photo.*
import kotlinx.android.synthetic.main.activity_new_photo.editText2
import kotlinx.android.synthetic.main.activity_new_photo.fab
import kotlinx.android.synthetic.main.activity_new_photo.imageView
import kotlinx.android.synthetic.main.activity_new_photo.textView
import java.io.File
import java.util.*


class NewPhotoActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var location:Location? = null
    private var path: String? = ""
    private lateinit var locationCallback: LocationCallback
    private lateinit var date: Date
    private lateinit var photoList: LinkedList<Photo>
    private lateinit var photo: Photo
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    var PERMISSION_ID = 44


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_photo)
        sharedPreferences =
            getSharedPreferences("edu.cuhk.csci3310.photodiary", Context.MODE_PRIVATE)
        editor = sharedPreferences.edit()
        val intent: Intent = getIntent()
        path = intent.getStringExtra("path")
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return
                for (loc in locationResult.locations){
                    System.out.println("Success current location")
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
                            val listAddress: List<Address> = geocoder.getFromLocation(latitude, longitude, 1)
                            if (listAddress.isNotEmpty()){
                                val adr = listAddress[0].getAddressLine(0)
                                textView.setText(adr)
                                fusedLocationClient.removeLocationUpdates(locationCallback)
                            }
                        } catch(e:Exception) {
                            System.out.println(e.toString())
                        }
                    }
                }
            }
        }
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

                    val listAddress: List<Address> = geocoder.getFromLocation(latitude, longitude, 1)
                    if (listAddress.isNotEmpty()){
                        val adr = listAddress[0].getAddressLine(0)
                        textView.setText(adr)
                    }

                }
            }
        startLocationUpdates()//update location with finer location if avaliable
        val file: File? = getExternalFilesDir(path)

        var uri: Uri? = null
        if (file != null) {
            System.out.println(file.absolutePath)
            uri = FileProvider.getUriForFile(this,"com.example.android.fileprovider", file)
        }
        if (uri != null){
            imageView.setImageURI(uri)
        }
        date = Date()
        textView2.setText(SimpleDateFormat("E dd/MM/yyyy HH:mm:ss").format(date))
        photo = Photo(path!!,null,date,"")
        fab.setOnClickListener {
                view ->
            if (checkPermissions()){
                val sharingIntent = Intent(Intent.ACTION_SEND)
                sharingIntent.type = "image/jpeg"
                sharingIntent.putExtra(Intent.EXTRA_STREAM, uri)
                startActivity(Intent.createChooser(sharingIntent, "Share image using"))
            } else {
                requestPermissions()
            }

        }
    }

    private fun startLocationUpdates() {
        fusedLocationClient.requestLocationUpdates(
            LocationRequest(),
            locationCallback,
            Looper.getMainLooper())
    }

    fun save(view: View?) {
        photo.description = editText2.text.toString()
        photo.location = location!!
        readSharedPreferences()
        photoList.add(photo)
        val gson = Gson()
        val json = gson.toJson(photoList)
        editor.putString("photos", json)
        editor.apply()
        close(view!!)
    }

    fun del(view: View?) {
        val file: File? = getExternalFilesDir(photo.picture)
        file!!.delete()
        close(view!!)
    }

    fun close(view: View) {
        view.performHapticFeedback( //perform haptic feedback on real device
            HapticFeedbackConstants.VIRTUAL_KEY,
            HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING // Ignore device's setting. Otherwise, you can use FLAG_IGNORE_VIEW_SETTING to ignore view's setting.
        )
        finish()
    }

    private fun readSharedPreferences() { //read json string and convert it to linked list of sweet
        val gson = Gson()
        val json = sharedPreferences.getString("photos", null)
        val type =
            object : TypeToken<LinkedList<Photo?>?>() {}.type
        photoList = gson.fromJson<LinkedList<Photo>>(json, type)
    }

    private fun checkPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ),
            PERMISSION_ID
        )
    }
}
