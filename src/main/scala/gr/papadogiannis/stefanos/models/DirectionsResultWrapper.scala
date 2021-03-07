package gr.papadogiannis.stefanos.models

import com.google.maps.model.DirectionsResult

case class DirectionsResultWrapper(startPoint: GeoPoint, endPoint: GeoPoint, directionsResult: DirectionsResult)
