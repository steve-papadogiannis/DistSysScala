package gr.papadogiannis.stefanos.messages

import gr.papadogiannis.stefanos.models.LatLng

final case class FinalResponse(request: CalculateReduction, results: Option[List[LatLng]])
