package com.example.e_commerce.Model

import android.os.Parcel
import android.os.Parcelable
import kotlinx.serialization.Serializable
import java.util.ArrayList

@Serializable
data class Product(
    val id: Int,
    val title: String?,
    val description: String,
    var picUrl: ArrayList<String> = ArrayList(),
    val brand: Int,
    val price: Double = 0.0,
    val rating: Double? = 0.0,
    val size: ArrayList<String> = ArrayList()
) : Parcelable {

    // Constructor Secundario para leer desde Parcel
    constructor(parcel: Parcel) : this(
        // 1. LEER ID (Int)
        parcel.readInt(),
        // 2. LEER title (String) - Usar readString()
        parcel.readString(),
        // 3. LEER description (String)
        parcel.readString().toString(),
        // 4. LEER picUrl (ArrayList<String>) - Usar createStringArrayList()
        parcel.createStringArrayList() ?: ArrayList(),
        // 5. LEER brand (Int)
        parcel.readInt(),
        // 6. LEER price (Double)
        parcel.readDouble(),
        // 7. LEER rating (Double?)
        parcel.readDouble(),
        // 9. LEER size (ArrayList<String>)
        parcel.createStringArrayList() ?: ArrayList()
    )

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        // 1. ESCRIBIR id (Int)
        dest.writeInt(id)
        // 2. ESCRIBIR title (String)
        dest.writeString(title)
        // 3. ESCRIBIR description (String)
        dest.writeString(description)
        // 4. ESCRIBIR picUrl (ArrayList<String>)
        dest.writeStringList(picUrl)
        // 5. ESCRIBIR brand (Int)
        dest.writeInt(brand)
        // 6. ESCRIBIR price (Double)
        dest.writeDouble(price)
        // 7. ESCRIBIR rating (Double?)
        dest.writeDouble(rating ?: 0.0)
        // 9. ESCRIBIR size (ArrayList<String>)
        dest.writeStringList(size)
    }

    companion object CREATOR : Parcelable.Creator<Product> {
        override fun createFromParcel(source: Parcel): Product {
            // Llama al constructor secundario corregido
            return Product(source)
        }

        override fun newArray(size: Int): Array<Product?> {
            return arrayOfNulls(size)
        }
    }
}