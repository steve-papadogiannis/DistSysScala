package gr.papadogiannis.stefanos.messages

final case class FinalResponse(request: CalculateReduction, results: Option[List[Double]])
