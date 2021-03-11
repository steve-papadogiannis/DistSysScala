package gr.papadogiannis.stefanos.validators

import gr.papadogiannis.stefanos.constants.ApplicationConstants.{END_POINT_LATITUDE_FIELD_NAME, END_POINT_LONGITUDE_FIELD_NAME, NEW_LINE_SEPARATOR, START_POINT_LATITUDE_FIELD_NAME, START_POINT_LONGITUDE_FIELD_NAME}
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
    case GeoPointPair(GeoPoint(startLat, startLng), GeoPoint(endLat, endLng)) =>
      (validateLatitude(startLat, START_POINT_LATITUDE_FIELD_NAME),
        validateLongitude(startLng, START_POINT_LONGITUDE_FIELD_NAME),
        validateLatitude(endLat, END_POINT_LATITUDE_FIELD_NAME),
        validateLongitude(endLng, END_POINT_LONGITUDE_FIELD_NAME))
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
                  failures.map(_.errorMessage).toList.mkString(NEW_LINE_SEPARATOR)))
            }
      }
  }

}
