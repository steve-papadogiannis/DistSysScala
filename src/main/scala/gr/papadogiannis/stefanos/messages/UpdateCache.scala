package gr.papadogiannis.stefanos.messages

import gr.papadogiannis.stefanos.models.{DirectionsResult, GeoPointPair}

case class UpdateCache(geoPointPair: GeoPointPair, value: DirectionsResult)
