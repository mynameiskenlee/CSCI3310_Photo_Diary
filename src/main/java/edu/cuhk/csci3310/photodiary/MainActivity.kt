package edu.cuhk.csci3310.photodiary

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.icu.text.SimpleDateFormat
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.IOException
import java.util.*


class MainActivity : AppCompatActivity() {

    lateinit var currentPhotoPath: String
    var PERMISSION_ID = 44
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var photoList: LinkedList<Photo>
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private var mAdapter: PhotoListAdapter? = null
    private lateinit var mRecyclerView: RecyclerView


    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = absolutePath
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        sharedPreferences = getSharedPreferences("edu.cuhk.csci3310.photodiary", Context.MODE_PRIVATE)
        editor = sharedPreferences.edit()
        photoList = LinkedList<Photo>()
        if (!sharedPreferences.contains("photos")) { //check if the shared preferences exist or not, if no, read default value from assets
            //convert the linked list of sweet to json string
            val gson = Gson()
            val json = gson.toJson(photoList)
            set("photos", json)
        } else {
            //As the linked list created by gson has a different memory address, we need to read from the new list and apply the change to the original list
            readSharedPreferences()
        }
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        fab.setOnClickListener { view ->
//            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                    .setAction("Action", null).show()
            if (checkPermissions()) {
                val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
                var gps_enabled = false
                var network_enabled = false
                try {
                    gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
                } catch (ex: Exception) {
                }

                try {
                    network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
                } catch (ex: Exception) {
                }
                if (gps_enabled || network_enabled){
                    dispatchTakePictureIntent(view)
                } else { //either fine and course location cannot be used
                    Snackbar.make(view, "Please enable location service", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
                }

            } else {
                requestPermissions() //the app cannot be run without permission
            }

        }
        mRecyclerView = findViewById<RecyclerView>(R.id.photolist)
        mAdapter = PhotoListAdapter(this, photoList)
        mRecyclerView.adapter = mAdapter
        mRecyclerView.layoutManager = LinearLayoutManager(this)
        (mRecyclerView.layoutManager as LinearLayoutManager).reverseLayout = true;
        (mRecyclerView.layoutManager as LinearLayoutManager).stackFromEnd = true
    }

    val REQUEST_TAKE_PHOTO = 1
    lateinit var mView: View

    private fun dispatchTakePictureIntent(view: View) {
        mView = view
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            // Ensure that there's a camera activity to handle the intent
            takePictureIntent.resolveActivity(packageManager)?.also {
                // Create the File where the photo should go
                val photoFile: File? = try {
                    createImageFile()
                } catch (ex: IOException) {
                    Snackbar.make(view,"Error when create photo file",Snackbar.LENGTH_LONG).setAction("Action",null).show()
                    null
                }
                System.out.println(currentPhotoPath)
                // Continue only if the File was successfully created
                photoFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(
                        this,
                        "com.example.android.fileprovider",
                        it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO)
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {
//            val imageBitmap = data.extras.get("data") as Bitmap
//            imageView.setImageBitmap(imageBitmap)
            var path = currentPhotoPath
            path = path.removePrefix("/storage/emulated/0/Android/data/edu.cuhk.csci3310.photodiary/files/")
            val intent = Intent(this,NewPhotoActivity::class.java).apply {
                putExtra("path",path)
            }
            startActivity(intent)
        } else {
            Snackbar.make(mView,"No photo taken",Snackbar.LENGTH_LONG).setAction("Action",null).show()
        }
    }

    private fun checkPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            ),
            PERMISSION_ID
        )
    }

    private fun set(
        key: String,
        value: String
    ) { // store the json string to shared preference
        editor.putString(key, value)
        editor.apply()
    }

    private fun readSharedPreferences() { //read json string and convert it to linked list of sweet
        val gson = Gson()
        val json = sharedPreferences.getString("photos", null)
        System.out.println(json)
        val type =
            object : TypeToken<LinkedList<Photo?>?>() {}.type
        val temp = gson.fromJson<LinkedList<Photo>>(json, type)
        photoList.clear()
        for (photo in temp){//keep same memory address
            photoList.add(photo)
        }
    }

    override fun onResume() {
        super.onResume()
        readSharedPreferences()
//        mAdapter?.notifyDataSetChanged()
        if (!photoList.isEmpty()){
            mRecyclerView.smoothScrollToPosition(photoList.size-1) //scroll back to top
        }

    }

}
