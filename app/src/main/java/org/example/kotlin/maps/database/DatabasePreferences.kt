package org.example.kotlin.maps.database

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import com.google.gson.Gson
import org.example.kotlin.maps.model.Category
import org.example.kotlin.maps.model.Country
import org.example.kotlin.maps.model.Station

object DatabasePreferences : Database {

    private lateinit var dbCategories:SharedPreferences
    private lateinit var dbCountries:SharedPreferences
    private lateinit var dbStations:SharedPreferences
    private lateinit var gson:Gson

    fun instance(context: Context): DatabasePreferences{
        dbCategories = context.getSharedPreferences("LOCAL_DB_CATEGORIES", MODE_PRIVATE)
        dbCountries  = context.getSharedPreferences("LOCAL_DB_COUNTRIES", MODE_PRIVATE)
        dbStations   = context.getSharedPreferences("LOCAL_DB_STATIONS", MODE_PRIVATE)
        gson = Gson()
        return this
    }

    override fun store(category: Category) {
        dbCategories.edit().putString(""+category.id, gson.toJson(category)).apply()
    }

    override fun store(country: Country) {
        dbCountries.edit().putString(country.country_code, gson.toJson(country)).apply()
    }

    override fun store(station: Station) {
        dbStations.edit().putString(""+station.id, gson.toJson(station)).apply()
    }

    override fun store(objects: Collection<Any>) {
        val editCat = dbCategories.edit()
        val editCnt = dbCountries.edit()
        val editStn = dbStations.edit()
        for (obj in objects){
            when(obj){
                is Country -> editCnt.putString(obj.country_code, gson.toJson(obj))
                is Category -> editCat.putString("" + obj.id, gson.toJson(obj))
                is Station -> editStn.putString("" + obj.id, gson.toJson(obj))
            }
        }
        editCat.apply()
        editCnt.apply()
        editStn.apply()
    }

    override fun loadCategories(): Collection<Category> {
        val list = ArrayList<Category>()
        dbCategories.all.values
                .filterIsInstance<String>()
                .mapTo(list) { gson.fromJson(it, Category::class.java) }
        return list
    }

    override fun loadCategory(categoryId: Int): Category? {
        val json:String? = dbCategories.getString(""+categoryId,null)
        return if(json != null) gson.fromJson(json, Category::class.java) else null
    }

    override fun loadCountries(): Collection<Country> {
        val list = ArrayList<Country>()
        dbCountries.all.values
                .filterIsInstance<String>()
                .mapTo(list) { gson.fromJson(it, Country::class.java) }
        return list
    }

    override fun loadCountry(countryId: String): Country? {
        val json:String? = dbCountries.getString(countryId,null)
        return if(json != null) gson.fromJson(json, Country::class.java) else null
    }

    override fun loadStations(): Collection<Station> {
        val list = ArrayList<Station>()
        dbStations.all.values
                .filterIsInstance<String>()
                .mapTo(list) { gson.fromJson(it, Station::class.java) }
        return list
    }
}