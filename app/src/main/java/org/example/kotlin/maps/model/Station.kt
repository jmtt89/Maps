package org.example.kotlin.maps.model

import java.util.*
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import java.io.IOException
import java.net.URL


/**
 * Created by jmtt_ on 11/18/2017.
 */

data class Thumb(val url:String?)

data class Image(
        val url:String?,
        val thumb: Thumb?)

data class Stream(
        val stream:String,
        val bitrate: Int,
        val content_type:String,
        val listeners: Int,
        val status: Int)

data class Station (
        val id:Int,
        val name:String,
        val description:String?,
        val country: String,
        val image: Image?,
        val slug: String?,
        val website: String?,
        val twitter: String?,
        val facebook: String?,
        val total_listeners: Int?,
        val categories: List<Category>,
        val streams: List<Stream>,
        val create_at: Date?,
        val update_at: Date?)