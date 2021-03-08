package gr.papadogiannis.stefanos.messages

final case class RespondAllReduceResults(request: CalculateReduction, results: Map[String, ReducerResult])
