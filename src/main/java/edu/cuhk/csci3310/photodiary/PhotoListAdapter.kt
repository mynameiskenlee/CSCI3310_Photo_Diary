package edu.cuhk.csci3310.photodiary

import android.R.attr.bitmap
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.icu.text.SimpleDateFormat
import android.location.Address
import android.location.Geocoder
import android.media.ExifInterface
import android.net.Uri
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_new_photo.*
import kotlinx.android.synthetic.main.photolist_item.view.*
import java.io.File
import java.io.IOException
import java.util.*


class PhotoListAdapter(
    context: Context?,
    photoList: LinkedList<Photo>
) :
    RecyclerView.Adapter<PhotoListAdapter.PhotoViewHolder>() {
    private var mInflater: LayoutInflater
    private var photoList: LinkedList<Photo> = photoList
    private var sharedPreferences: SharedPreferences? = null
    private var editor: SharedPreferences.Editor? = null
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): PhotoViewHolder {
        val mItemView: View = mInflater.inflate(R.layout.photolist_item, parent, false)
        return PhotoViewHolder(mItemView, this)
    }

    override fun onBindViewHolder(
        holder: PhotoViewHolder,
        position: Int
    ) {
        val photo: Photo = photoList[position]
        val path: String = photo.picture
        val file: File? = mInflater.context.getExternalFilesDir(path)
        var uri: Uri? = null
        if (file != null) {
            uri = FileProvider.getUriForFile(mInflater.context,"com.example.android.fileprovider", file)
        }
        val bitmapImage = BitmapFactory.decodeFile(file?.absolutePath)
        val nh = (bitmapImage.height * (256.0 / bitmapImage.width)).toInt()
        val scaled = Bitmap.createScaledBitmap(bitmapImage, 256, nh, true)
        var exif: ExifInterface? = null
        try {
            exif = ExifInterface(file?.absolutePath)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        val orientation = exif!!.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_UNDEFINED
        )
        val bmRotated = rotateBitmap(scaled, orientation)

//        holder.photoItemView.setImageURI(uri)
        holder.photoItemView.setImageBitmap(bmRotated)
        holder.picDescription.setText(photo.description)
        holder.picTime.setText(SimpleDateFormat("E dd/MM/yyyy HH:mm:ss").format(photo.datetime))
        val geocoder = Geocoder(mInflater.context, Locale.getDefault())

        val listAddress: List<Address> = geocoder.getFromLocation(photo.latitude, photo.longitude, 1)
        if (listAddress.isNotEmpty()){
            val adr = listAddress[0].getAddressLine(0)
            holder.picLocation.setText(adr)
        }
//        holder.sweetRestaurant.setText(sweet.getRestaurant())
//        holder.sweetRanking.text = mInflater.context.getString(R.string.star) + sweet.getRating()
    }

    override fun getItemCount(): Int {
        return photoList.size
    }

    inner class PhotoViewHolder(
        itemView: View,
        adapter: PhotoListAdapter
    ) :
        RecyclerView.ViewHolder(itemView), View.OnClickListener {
        var photoItemView: ImageView
        var picTime: TextView
        var picLocation: TextView
        var picDescription: TextView
        private var mAdapter: PhotoListAdapter
        override fun onClick(v: View) {
            val mPosition = layoutPosition
            mInflater.context
            sharedPreferences = mInflater.context.getSharedPreferences(
                "edu.cuhk.csci3310.photodiary",
                Context.MODE_PRIVATE
            )
            editor = sharedPreferences!!.edit()
            editor!!.putInt("location", mPosition)
            editor!!.apply()
//            val intent = Intent(mInflater.context, NewPhotoActivity::class.java)
//            mInflater.context.startActivity(intent)
            v.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING // Ignore device's setting. Otherwise, you can use FLAG_IGNORE_VIEW_SETTING to ignore view's setting.
            )
        }

        init {
            photoItemView = itemView.pic
            picTime = itemView.pictime
            picLocation = itemView.piclocation
            picDescription = itemView.picdescription
            mAdapter = adapter
            itemView.setOnClickListener(this)
        }
    }

    init {
        mInflater = LayoutInflater.from(context)
        this.photoList = photoList
    }

    fun rotateBitmap(bitmap: Bitmap, orientation: Int): Bitmap? {
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
}


