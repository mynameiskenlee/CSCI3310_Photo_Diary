package edu.cuhk.csci3310.photodiary

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.icu.text.SimpleDateFormat
import android.location.Address
import android.location.Geocoder
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.core.content.FileProvider
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.activity_photo.*
import java.io.File
import java.util.*

class PhotoActivity : AppCompatActivity() {

    private lateinit var photoList: LinkedList<Photo>
    private lateinit var photo: Photo
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo)
        sharedPreferences =
            getSharedPreferences("edu.cuhk.csci3310.photodiary", Context.MODE_PRIVATE)
        editor = sharedPreferences.edit()
        readSharedPreferences()
        val intent: Intent = getIntent()
        val idx = intent.getIntExtra("index",0)
        photo = photoList[idx]
        editText2.setText(photo.description)
        textView3.setText(SimpleDateFormat("E dd/MM/yyyy HH:mm:ss").format(photo.datetime))
        val longitude = photo.longitude
        val latitude = photo.latitude
        val geocoder = Geocoder(applicationContext, Locale.getDefault())
        try {
            val listAddress: List<Address> = geocoder.getFromLocation(latitude, longitude, 1)
            if (listAddress.isNotEmpty()){
                val adr = listAddress[0].getAddressLine(0)
                textView.setText(adr)
            }
        } catch(e:Exception) {
            System.out.println(e.toString())
        }
        val file: File? = getExternalFilesDir(photo.picture)

        var uri: Uri? = null
        if (file != null) {
            System.out.println(file.absolutePath)
            uri = FileProvider.getUriForFile(this,"com.example.android.fileprovider", file)
        }
        if (uri != null){
            imageView.setImageURI(uri)
        }
    }

    private fun readSharedPreferences() { //read json string and convert it to linked list of sweet
        val gson = Gson()
        val json = sharedPreferences.getString("photos", null)
        val type =
            object : TypeToken<LinkedList<Photo?>?>() {}.type
        photoList = gson.fromJson(json, type)
    }

    fun del(view: View?) {
        photoList.remove(photo)
        val gson = Gson()
        val json = gson.toJson(photoList)
        editor.putString("photos", json)
        editor.apply()
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
}
