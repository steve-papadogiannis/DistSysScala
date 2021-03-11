package gr.papadogiannis.stefanos.validators

import akka.http.scaladsl.unmarshalling.{FromRequestUnmarshaller, Unmarshaller}
import gr.papadogiannis.stefanos.models.{GeoPoint, GeoPointPair}
import cats.data.Validated.{Invalid, Valid}
import akka.http.scaladsl.model.HttpRequest
import cats.implicits._

import scala.concurrent.Future

trait JSONBodyValidator extends FieldValidator {

  type JSONBodyValidation[F] = F => ValidationResult[F]

  def validateForm[F, A](form: F)(f: ValidationResult[F] => A)
                        (implicit formValidation: JSONBodyValidation[F]): A =
    f(formValidation(form))

  implicit lazy val jsonBodyValidation: JSONBodyValidation[GeoPointPair] = {
    case GeoPointPair(GeoPoint(startLat, startLng), GeoPoint(endLat, endLng)) => (
      validateLatitude(startLat, "Start Point Latitude"),
      validateLongitude(startLng, "Start Point Longitude"),
      validateLatitude(endLat, "End Point Latitude"),
      validateLongitude(endLng, "End Point Longitude"))
      .map4((a, b, c, d) => GeoPointPair(GeoPoint(a, b), GeoPoint(c, d)))
  }

  implicit class ValidationRequestMarshaller[A](um: FromRequestUnmarshaller[A]) {
    def validate(implicit validation: JSONBodyValidation[A]): Unmarshaller[HttpRequest, A] =
      um.flatMap { _ =>
        _ =>
          entity =>
            validateForm(entity) {
              case Valid(_) => Future.successful(entity)
              case Invalid(failures) =>
                Future.failed(new IllegalArgumentException(
                  failures.map(_.errorMessage).toList.mkString("\n")))
            }
      }
  }

}
