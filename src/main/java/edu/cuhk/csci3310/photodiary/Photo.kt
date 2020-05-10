package edu.cuhk.csci3310.photodiary

import android.location.Location
import com.google.android.gms.maps.model.LatLng
import java.util.*

class Photo(picture: String, location: Location?, datetime:Date, description:String) {
    var picture: String = picture
        get() = field
        set(value) {
            field = value
        }
    var latitude: Double = location?.latitude ?: 0.0
        get() = field
        set(value) {
            field = value
        }
    var longitude: Double = location?.longitude ?: 0.0
        get() = field
        set(value) {
            field = value
        }
    var datetime: Date = datetime
        get() = field
        set(value) {
            field = value
        }
    var description: String = description
        get() = field
        set(value) {
            field = value
        }

    var location:Any
        get(){
            return LatLng(latitude,longitude)
        }
        set(location: Any) {
            if (location != null) {
                helperSet(location)
            }
        }

    private fun <T> helperSet(t: T) = when (t) {
        is Location -> {
                latitude = t.latitude
                longitude = t.longitude
        }
        is LatLng -> {
                latitude = t.latitude
                longitude = t.longitude
        }
        else -> throw IllegalArgumentException()
    }

}