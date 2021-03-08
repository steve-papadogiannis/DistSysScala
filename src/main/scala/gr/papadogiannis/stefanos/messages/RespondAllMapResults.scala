package gr.papadogiannis.stefanos.messages

final case class RespondAllMapResults(request: CalculateDirections, results: Map[String, MapperResult])
