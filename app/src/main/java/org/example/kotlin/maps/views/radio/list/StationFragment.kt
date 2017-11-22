package org.example.kotlin.maps.views.radio.list

import android.content.Context
import android.os.AsyncTask
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.gson.Gson
import org.example.kotlin.maps.R
import org.example.kotlin.maps.database.Database
import org.example.kotlin.maps.database.DatabasePreferences

import org.example.kotlin.maps.model.Category
import org.example.kotlin.maps.model.Country
import org.example.kotlin.maps.model.Station
import org.example.kotlin.maps.views.radio.StationInteractionListener
import java.net.URL

class StationFragment : Fragment() {
    private lateinit var database:Database
    private var mColumnCount = 1
    private var fCountry: Country? = null
    private var fCategory: Category? = null

    private var list:List<Station> = ArrayList()
    private lateinit var adapter: StationRecyclerViewAdapter
    private var mListener: StationInteractionListener? = null

    companion object {
        private val ARG_FILTER_COUNTRY = "filter_country"
        private val ARG_FILTER_CATEGORY = "filter_category"
        fun newInstance(filter:Any?): StationFragment {
            val fragment = StationFragment()
            val args = Bundle()
            when(filter){
                is Category -> args.putInt(ARG_FILTER_CATEGORY, filter.id)
                is Country -> args.putString(ARG_FILTER_COUNTRY, filter.country_code)
            }
            fragment.arguments = args
            return fragment
        }

    }

    /*
     * Lo ideal aqui fuese crear una clase que controlara la comunicacion con el api, pero por cuestiones
     * de tiempo lo dejo dentro de los AsyncTask directamente
     */
    class FetchStations(val context: Context, private val adapter: StationRecyclerViewAdapter): AsyncTask<Any, Void, List<Station>>(){
        private val apiKey: String = context.resources.getString(R.string.dirble_api_key)
        private val database: Database = DatabasePreferences.instance(context)

        private val gson: Gson = Gson()

        override fun doInBackground(vararg params: Any?): List<Station> {
            val stations: List<Station>

            var apiCall = "http://api.dirble.com/v2/stations/popular?token="+apiKey
            if(params.isNotEmpty() && params[0] != null){
                val obj = params[0]
                when(obj){
                    is Country -> apiCall = "http://api.dirble.com/v2/countries/"+obj.country_code+"/stations?token="+apiKey
                    is Category -> apiCall = "http://api.dirble.com/v2/category/"+obj.id+"/stations?token="+apiKey
                }
            }

            val result = URL(apiCall).readText()
            stations = gson.fromJson(result, Array<Station>::class.java).toList()
            database.store(stations)

            return stations
        }
        override fun onPostExecute(result: List<Station>?) {
            super.onPostExecute(result)
            if(result != null){
                adapter.add(result)
            }
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        database = DatabasePreferences.instance(context.applicationContext)

        if (arguments != null) {
            val categoryId:Int = arguments.getInt(ARG_FILTER_CATEGORY, -1)
            if(categoryId != -1){
                fCategory = database.loadCategory(categoryId)
            }

            val countryId:String? = arguments.getString(ARG_FILTER_COUNTRY, null)
            if(countryId != null){
                fCountry = database.loadCountry(countryId)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater!!.inflate(R.layout.fragment_station_list, container, false)

        // Set the adapter
        if (view is RecyclerView) {
            val context = view.getContext()
            if (mColumnCount <= 1) {
                view.layoutManager = LinearLayoutManager(context)
            } else {
                view.layoutManager = GridLayoutManager(context, mColumnCount)
            }
            adapter = StationRecyclerViewAdapter(getContext().applicationContext, list, mListener)
            view.adapter = adapter

            when {
                fCategory != null -> FetchStations(context.applicationContext, adapter).execute(fCategory)
                fCountry != null -> FetchStations(context.applicationContext, adapter).execute(fCountry)
                else -> FetchStations(context.applicationContext, adapter).execute()
            }

        }
        return view
    }


    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (context is StationInteractionListener) {
            mListener = context
        } else {
            throw RuntimeException(context!!.toString() + " must implement OnListFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
    }

}
