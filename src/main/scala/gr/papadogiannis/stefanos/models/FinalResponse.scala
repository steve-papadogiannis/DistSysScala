package gr.papadogiannis.stefanos.models

import com.google.maps.model.DirectionsResult

final case class FinalResponse(request: CalculateReduction, results: DirectionsResult)

