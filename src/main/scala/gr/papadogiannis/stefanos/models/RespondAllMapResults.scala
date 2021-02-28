package gr.papadogiannis.stefanos.models

final case class RespondAllMapResults(request: CalculateDirections, results: Map[String, MapperResult])
