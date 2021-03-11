package gr.papadogiannis.stefanos.validators

import gr.papadogiannis.stefanos.constants.ApplicationConstants.{LATITUDE_LOWER_BOUND, LATITUDE_UPPER_BOUND, LONGITUDE_LOWER_BOUND, LONGITUDE_UPPER_BOUND}
import gr.papadogiannis.stefanos.validators.RequestValidation.{AboveMaximum, BelowMinimum, EmptyField}
import cats.data.ValidatedNel
import cats.implicits._

trait FieldValidator {

  trait Required[F] extends (F => Boolean)

  trait Minimum[F] extends ((F, Number) => Boolean)

  trait Maximum[F] extends ((F, Number) => Boolean)

  def required[F](field: F)(implicit req: Required[F]): Boolean =
    req(field)

  def minimum[F](field: F, limit: Number)(implicit min: Minimum[F]): Boolean =
    min(field, limit)

  def maximum[F](field: F, limit: Number)(implicit max: Maximum[F]): Boolean =
    max(field, limit)

  implicit val minimumDouble: Minimum[Double] = _ >= _.doubleValue()

  implicit val maximumDouble: Maximum[Double] = _ <= _.doubleValue()

  implicit val requiredDouble: Required[Double] = !_.isNaN

  type ValidationResult[A] = ValidatedNel[RequestValidation, A]

  def validateRequired[F: Required](field: F, fieldName: String): ValidationResult[F] =
    Either.cond(
      required(field),
      field,
      EmptyField(fieldName)).toValidatedNel

  def validateMinimum[F: Minimum](field: F, fieldName: String, limit: Double): ValidationResult[F] =
    Either.cond(
      minimum(field, limit),
      field,
      BelowMinimum(fieldName, limit)).toValidatedNel

  def validateMaximum[F: Maximum](field: F, fieldName: String, limit: Double): ValidationResult[F] =
    Either.cond(
      maximum(field, limit),
      field,
      AboveMaximum(fieldName, limit)).toValidatedNel

  def validateCoordinate(field: Double, fieldName: String, lowerBound: Double, upperBound: Double): ValidationResult[Double] =
    validateRequired(field, fieldName).combine(validateMinimum(field, fieldName, lowerBound))
      .combine(validateMaximum(field, fieldName, upperBound))

  def validateLatitude(field: Double, fieldName: String): ValidationResult[Double] =
    validateCoordinate(field, fieldName, LATITUDE_LOWER_BOUND, LATITUDE_UPPER_BOUND)

  def validateLongitude(field: Double, fieldName: String): ValidationResult[Double] =
    validateCoordinate(field, fieldName, LONGITUDE_LOWER_BOUND, LONGITUDE_UPPER_BOUND)

}
