/*
 * Copyright 2019 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package v2.controllers

import java.util.UUID

import cats.data.EitherT
import cats.implicits._
import javax.inject.{ Inject, Singleton }
import play.api.http.MimeTypes
import play.api.libs.json.{ JsValue, Json }
import play.api.mvc.{ Action, AnyContentAsJson, ControllerComponents }
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import v2.controllers.requestParsers.CrystallisationRequestDataParser
import v2.models.audit.{ AuditError, AuditEvent, _ }
import v2.models.auth.UserDetails
import v2.models.errors._
import v2.models.requestData.CrystallisationRawData
import v2.services.{ AuditService, CrystallisationService, EnrolmentsAuthService, MtdIdLookupService }

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ ExecutionContext, Future }

@Singleton
class CrystallisationController @Inject()(val authService: EnrolmentsAuthService,
                                          val lookupService: MtdIdLookupService,
                                          crystallisationRequestDataParser: CrystallisationRequestDataParser,
                                          crystallisationService: CrystallisationService,
                                          auditService: AuditService,
                                          cc: ControllerComponents)
    extends AuthorisedController(cc)
    with DesResponseMappingSupport {

  override val controllerName: String = "CrystallisationController"

  def create(nino: String, taxYear: String): Action[JsValue] = authorisedAction(nino).async(parse.json) { implicit request =>
    val result =
      for {
        parsedRequest <- EitherT.fromEither[Future](
          crystallisationRequestDataParser.parseRequest(CrystallisationRawData(nino, taxYear, AnyContentAsJson(request.body))))
        desResponse <- EitherT(crystallisationService.createCrystallisation(parsedRequest))
          .leftMap(mapDesErrors("createCrystalisation", desErrorMapCreate))
      } yield {
        val vendorResponse = desResponse
        logger.info(s"[$controllerName][createCrystalisation] - Success response received with CorrelationId: ${vendorResponse.correlationId}")
        auditSubmission(createAuditDetails(nino, taxYear, CREATED, request.request.body, desResponse.correlationId, request.userDetails))
        Created.withHeaders("X-CorrelationId" -> vendorResponse.correlationId).as(MimeTypes.JSON)
      }

    result.leftMap { errorWrapper =>
      val correlationId = getCorrelationId(errorWrapper)
      val result        = errorResult(errorWrapper).withHeaders("X-CorrelationId" -> correlationId)
      auditSubmission(
        createAuditDetails(nino, taxYear, result.header.status, request.request.body, correlationId, request.userDetails, Some(errorWrapper)))
      result
    }.merge
  }

  private def errorResult(errorWrapper: ErrorWrapper) = {
    errorWrapper.error match {
      case BadRequestError | NinoFormatError | TaxYearFormatError | RuleTaxYearNotSupportedError | RuleIncorrectOrEmptyBodyError |
          RuleTaxYearRangeExceededError | InvalidCalcIdError =>
        BadRequest(Json.toJson(errorWrapper))
      case IncomeSourcesChangedError | RecentSubmissionsExistError | ResidencyChangedError | FinalDeclarationReceivedError =>
        Forbidden(Json.toJson(errorWrapper))
      case NotFoundError   => NotFound(Json.toJson(errorWrapper))
      case DownstreamError => InternalServerError(Json.toJson(errorWrapper))
    }
  }

  private def desErrorMapCreate: Map[String, MtdError] =
    Map(
      "INVALID_IDTYPE"             -> DownstreamError,
      "INVALID_IDVALUE"            -> NinoFormatError,
      "INVALID_TAXYEAR"            -> TaxYearFormatError,
      "INVALID_CALCID"             -> InvalidCalcIdError,
      "NOT_FOUND"                  -> NotFoundError,
      "INCOME_SOURCES_CHANGED"     -> IncomeSourcesChangedError,
      "RECENT_SUBMISSIONS_EXIST"   -> RecentSubmissionsExistError,
      "RESIDENCY_CHANGED"          -> ResidencyChangedError,
      "FINAL_DECLARATION_RECEIVED" -> FinalDeclarationReceivedError,
      "SERVER_ERROR"               -> DownstreamError,
      "SERVICE_UNAVAILABLE"        -> DownstreamError
    )

  private def getCorrelationId(errorWrapper: ErrorWrapper): String = {
    errorWrapper.correlationId match {
      case Some(correlationId) =>
        logger.info(
          "[CrystallisationController][getCorrelationId] - " +
            s"Error received from DES ${Json.toJson(errorWrapper)} with CorrelationId: $correlationId")
        correlationId
      case None =>
        val correlationId = UUID.randomUUID().toString
        logger.info(
          "[CrystallisationController][getCorrelationId] - " +
            s"Validation error: ${Json.toJson(errorWrapper)} with CorrelationId: $correlationId")
        correlationId
    }
  }

  private def createAuditDetails(nino: String,
                                 taxYear: String,
                                 statusCode: Int,
                                 request: JsValue,
                                 correlationId: String,
                                 userDetails: UserDetails,
                                 errorWrapper: Option[ErrorWrapper] = None): CrystallisationAuditDetail = {
    val response = errorWrapper
      .map { wrapper =>
        CrystallisationAuditResponse(statusCode, Some(wrapper.allErrors.map(error => AuditError(error.code))))
      }
      .getOrElse(CrystallisationAuditResponse(statusCode, None))

    CrystallisationAuditDetail(userDetails.userType, userDetails.agentReferenceNumber, nino, taxYear, request, correlationId, response)
  }

  private def auditSubmission(details: CrystallisationAuditDetail)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[AuditResult] = {
    val event = AuditEvent("submitCrystallisation", "crystallisation", details)
    auditService.auditEvent(event)
  }
}
