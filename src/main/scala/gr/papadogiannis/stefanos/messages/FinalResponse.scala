package gr.papadogiannis.stefanos.messages

import gr.papadogiannis.stefanos.models.DirectionsResult

final case class FinalResponse(request: CalculateReduction, results: Option[DirectionsResult])
