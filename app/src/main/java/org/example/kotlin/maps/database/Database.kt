package org.example.kotlin.maps.database

import org.example.kotlin.maps.model.Category
import org.example.kotlin.maps.model.Country
import org.example.kotlin.maps.model.Station


/**
 * Created by jmtt_ on 11/18/2017.
 *
 */
interface Database {
    fun store(category:  Category)
    fun store(country:   Country)
    fun store(station:   Station)
    fun store(objects: Collection<Any>)

    fun loadCategories():Collection<Category>
    fun loadCategory(categoryId: Int): Category?
    fun loadCountries():Collection<Country>
    fun loadCountry(countryId: String): Country?
    fun loadStations():Collection<Station>
}