package gr.papadogiannis.stefanos.models

import com.google.maps.model.DirectionsResult

case class FinalResponse(request: CalculateReduction, results: DirectionsResult)

