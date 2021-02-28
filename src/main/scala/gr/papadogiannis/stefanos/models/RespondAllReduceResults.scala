package gr.papadogiannis.stefanos.models

final case class RespondAllReduceResults(request: CalculateReduction, results: Map[String, ReducerResult])
