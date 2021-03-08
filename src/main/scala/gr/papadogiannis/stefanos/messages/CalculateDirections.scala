package gr.papadogiannis.stefanos.messages

final case class CalculateDirections(requestId: Long, startLat: Double, startLong: Double, endLat: Double, endLong: Double)
