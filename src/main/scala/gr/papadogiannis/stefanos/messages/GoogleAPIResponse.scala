package gr.papadogiannis.stefanos.messages

import gr.papadogiannis.stefanos.models.DirectionsResult

case class GoogleAPIResponse(calculateReduction: CalculateReduction, maybeResult: Option[DirectionsResult])
