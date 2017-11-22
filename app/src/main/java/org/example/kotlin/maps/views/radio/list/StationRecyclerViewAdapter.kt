package org.example.kotlin.maps.views.radio.list

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.squareup.picasso.Picasso
import org.example.kotlin.maps.MediaPlayerNotification
import org.example.kotlin.maps.R

import org.example.kotlin.maps.model.Station
import org.example.kotlin.maps.views.radio.StationInteractionListener

class StationRecyclerViewAdapter(
        private val context:Context,
        initValues: List<Station>,
        private val listener: StationInteractionListener?
    ):RecyclerView.Adapter<StationRecyclerViewAdapter.ViewHolder>() {
    private var actualPlayingIndx: Int = -1
    private val isPlaying:ArrayList<Boolean> = ArrayList(initValues.map { _ -> false })
    private val mValues:ArrayList<Station> = ArrayList(initValues)
    private val picasso:Picasso = Picasso.with(context)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.fragment_station, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val station = mValues[position]
        holder.mItem = station
        holder.title.text = station.name

        holder.category.text = if(station.categories.isNotEmpty()) station.categories[0].title else ""
        holder.country.text = station.country

        if(station.total_listeners != null){
            holder.listeners.text = context.getString(R.string.lbl_listeners,station.total_listeners)
        }else{
            holder.listeners.text = context.getString(R.string.lbl_listeners,0)
        }

        if(station.image?.thumb?.url != null){
            holder.logo.visibility = View.VISIBLE
            picasso.load(station.image.thumb.url).into(holder.logo)
        }else{
            holder.logo.visibility = View.GONE
        }

        Log.d("ADAPTER", "" + position + ": " + isPlaying[position])


        holder.play.setText(if(isPlaying[position]) R.string.btn_stop else R.string.btn_play)

        holder.play.setOnClickListener {
            if(!isPlaying[position]){
                holder.play.setText(R.string.btn_buffering)
                holder.play.isEnabled = false

                // Se Detiene la reproduccion anterior de manera visual
                if(actualPlayingIndx != -1){
                    isPlaying[actualPlayingIndx] = false
                    notifyItemChanged(actualPlayingIndx)
                    actualPlayingIndx = -1
                }

                //Se detiene la reproduccion anterior y se ejecuta la nueva
                listener?.play(station, { valid: Boolean ->
                    holder.play.isEnabled = true
                    if(valid){
                        actualPlayingIndx = holder.adapterPosition
                        isPlaying[holder.adapterPosition] = true
                        holder.play.setText(R.string.btn_stop)
                    }else{
                        MediaPlayerNotification.cancel(context)
                        holder.play.setText(R.string.btn_play)
                    }
                })
            }else{
                actualPlayingIndx = -1
                isPlaying[position] = false
                MediaPlayerNotification.cancel(context)
                listener?.pause()
                holder.play.text = context.getString(R.string.btn_play)
            }
        }

        holder.share.setOnClickListener {
            listener?.share(station)
        }

        holder.itemView.setOnClickListener {
            listener?.goDetails(station)
        }
    }

    override fun getItemCount(): Int = mValues.size

    fun add(stations: List<Station>) {
        for(station in stations){
            val idx = mValues.indexOf(station)
            if(idx >= 0){
                mValues[idx] = station
            }else{
                mValues.add(station)
                isPlaying.add(false)
            }
        }
        notifyDataSetChanged()
    }

    inner class ViewHolder(mView: View) : RecyclerView.ViewHolder(mView) {

        val title: TextView
        val category: TextView
        val listeners: TextView
        val country: TextView
        val logo: ImageView

        val play: Button
        val share:Button

        var mItem: Station? = null

        init {
            title = mView.findViewById<View>(R.id.station_name) as TextView
            category = mView.findViewById<View>(R.id.category) as TextView
            listeners = mView.findViewById<View>(R.id.listeners) as TextView
            country = mView.findViewById<View>(R.id.country) as TextView
            logo = mView.findViewById<View>(R.id.logo) as ImageView
            play = mView.findViewById<View>(R.id.play) as Button
            share = mView.findViewById<View>(R.id.share) as Button
        }
    }
}
