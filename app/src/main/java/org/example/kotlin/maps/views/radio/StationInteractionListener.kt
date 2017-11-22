package org.example.kotlin.maps.views.radio

import org.example.kotlin.maps.model.Station

/**
 * Created by jmtt_ on 11/19/2017.
 *
 */
interface StationInteractionListener {
    fun goDetails(station: Station)
    fun play(station: Station, callback: (valid:Boolean) -> Unit)
    fun pause()
    fun share(station: Station)
}