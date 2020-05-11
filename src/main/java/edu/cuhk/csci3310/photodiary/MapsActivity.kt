package edu.cuhk.csci3310.photodiary

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.*
import android.icu.text.SimpleDateFormat
import android.location.Location
import android.location.LocationManager
import androidx.exifinterface.media.ExifInterface
import android.os.Bundle
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.IOException
import java.util.*

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var location: Location? = null
    private lateinit var photoList: LinkedList<Photo>
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var markerList: LinkedList<Marker>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        sharedPreferences =
            getSharedPreferences("edu.cuhk.csci3310.photodiary", Context.MODE_PRIVATE)
        editor = sharedPreferences.edit()
        readSharedPreferences()
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
        mMap.isMyLocationEnabled = true
        markerList = LinkedList()
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
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(latitude,longitude),
                            19.0F
                        ))
                        fusedLocationClient.removeLocationUpdates(locationCallback)
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
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(latitude,longitude),
                        19.0F
                    ))

                }
            }
        startLocationUpdates()//update location with finer location if avaliable
        for (photo in photoList){
            val file: File? = applicationContext.getExternalFilesDir(photo.picture)
            val bitmapImage = BitmapFactory.decodeFile(file?.absolutePath)
            val nh = (bitmapImage.height * (128.0 / bitmapImage.width)).toInt()
            val scaled = Bitmap.createScaledBitmap(bitmapImage, 128, nh, true)
            var exif: ExifInterface? = null
            try {
                exif = ExifInterface(file?.absolutePath!!)
            } catch (e: IOException) {
                e.printStackTrace()
            }
            val orientation = exif!!.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED
            )
            val bmRotated = rotateBitmap(scaled, orientation)
            val marker = mMap.addMarker(MarkerOptions().position(LatLng(photo.latitude,photo.longitude)).title(
                SimpleDateFormat("E dd/MM/yyyy HH:mm:ss").format(photo.datetime))
                .snippet(photo.description)
                .icon(BitmapDescriptorFactory.fromBitmap(bmRotated?.let { getCroppedBitmap(it) })))
            markerList.add(marker)
        }
        mMap.setOnInfoWindowClickListener {
            val idx = markerList.indexOf(it)
            val intent = Intent(this,PhotoActivity::class.java).apply {
                putExtra("index",idx)
            }
            startActivity(intent)
        }

    }
    private fun startLocationUpdates() {
        fusedLocationClient.requestLocationUpdates(
            LocationRequest(),
            locationCallback,
            Looper.getMainLooper())
    }

    private fun readSharedPreferences() { //read json string and convert it to linked list of sweet
        val gson = Gson()
        val json = sharedPreferences.getString("photos", null)
        val type =
            object : TypeToken<LinkedList<Photo?>?>() {}.type
        photoList = gson.fromJson<LinkedList<Photo>>(json, type)
    }

    private fun rotateBitmap(bitmap: Bitmap, orientation: Int): Bitmap? {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_NORMAL -> return bitmap
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1F, 1F)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180F)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
                matrix.setRotate(180F)
                matrix.postScale(-1F, 1F)
            }
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.setRotate(90F)
                matrix.postScale(-1F, 1F)
            }
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90F)
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.setRotate(-90F)
                matrix.postScale(-1F, 1F)
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(-90F)
            else -> return bitmap
        }
        return try {
            val bmRotated = Bitmap.createBitmap(
                bitmap,
                0,
                0,
                bitmap.width,
                bitmap.height,
                matrix,
                true
            )
            bitmap.recycle()
            bmRotated
        } catch (e: OutOfMemoryError) {
            e.printStackTrace()
            null
        }
    }

    fun getCroppedBitmap(bitmap: Bitmap): Bitmap? {
        val output = Bitmap.createBitmap(
            bitmap.width,
            bitmap.height, Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(output)
        val color = -0xbdbdbe
        val paint = Paint()
        val rect = Rect(0, 0, bitmap.width, bitmap.height)
        paint.setAntiAlias(true)
        canvas.drawARGB(0, 0, 0, 0)
        paint.setColor(color)
        canvas.drawCircle(
            (bitmap.width / 2.0).toFloat(), (bitmap.height / 2.0).toFloat(),
            (bitmap.width / 2.0).toFloat(), paint
        )
        paint.setXfermode(PorterDuffXfermode(PorterDuff.Mode.SRC_IN))
        canvas.drawBitmap(bitmap, rect, rect, paint)
        return output
    }

    override fun onResume() {
        super.onResume()
        if (this::mMap.isInitialized){
            mMap.clear()
            markerList.clear()
            readSharedPreferences()
            for (photo in photoList){//reset marker
                val file: File? = applicationContext.getExternalFilesDir(photo.picture)
                val bitmapImage = BitmapFactory.decodeFile(file?.absolutePath)
                val nh = (bitmapImage.height * (128.0 / bitmapImage.width)).toInt()
                val scaled = Bitmap.createScaledBitmap(bitmapImage, 128, nh, true)
                var exif: ExifInterface? = null
                try {
                    exif = ExifInterface(file?.absolutePath!!)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
                val orientation = exif!!.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_UNDEFINED
                )
                val bmRotated = rotateBitmap(scaled, orientation)
                val marker = mMap.addMarker(MarkerOptions().position(LatLng(photo.latitude,photo.longitude)).title(
                    SimpleDateFormat("E dd/MM/yyyy HH:mm:ss").format(photo.datetime))
                    .snippet(photo.description)
                    .icon(BitmapDescriptorFactory.fromBitmap(bmRotated?.let { getCroppedBitmap(it) })))
                markerList.add(marker)
            }
        }

    }
}
