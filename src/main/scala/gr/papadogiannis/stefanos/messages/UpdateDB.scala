package gr.papadogiannis.stefanos.messages

import gr.papadogiannis.stefanos.models.{DirectionsResult, GeoPointPair}

case class UpdateDB(geoPointPair: GeoPointPair, directionsResult: DirectionsResult)
