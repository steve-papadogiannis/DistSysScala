package gr.papadogiannis.stefanos.validators

sealed trait RequestValidation {
  def errorMessage: String
}

object RequestValidation {

  final case class EmptyField(fieldName: String) extends RequestValidation {
    override def errorMessage = s"$fieldName is empty"
  }

  final case class BelowMinimum(fieldName: String, limit: Number) extends RequestValidation {
    override def errorMessage = s"$fieldName is below the minimum of $limit"
  }

  final case class AboveMaximum(fieldName: String, limit: Number) extends RequestValidation {
    override def errorMessage = s"$fieldName is above the maximum of $limit"
  }

}
