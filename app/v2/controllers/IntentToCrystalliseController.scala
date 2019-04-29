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

import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.http.MimeTypes
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import v2.controllers.requestParsers.IntentToCrystalliseRequestDataParser
import v2.models.audit.{AuditError, AuditEvent, IntentToCrystalliseAuditDetail, IntentToCrystalliseAuditResponse}
import v2.models.auth.UserDetails
import v2.models.errors._
import v2.models.requestData.IntentToCrystalliseRawData
import v2.services.{AuditService, CrystallisationService, EnrolmentsAuthService, MtdIdLookupService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class IntentToCrystalliseController @Inject()(val authService: EnrolmentsAuthService,
                                              val lookupService: MtdIdLookupService,
                                              intentToCrystalliseRequestDataParser: IntentToCrystalliseRequestDataParser,
                                              crystallisationService: CrystallisationService,
                                              auditService: AuditService,
                                              cc: ControllerComponents)
    extends AuthorisedController(cc) {

  val logger: Logger = Logger(this.getClass)

  def intentToCrystallise(nino: String, taxYear: String): Action[AnyContent] =
    authorisedAction(nino).async { implicit request =>
      intentToCrystalliseRequestDataParser.parseRequest(IntentToCrystalliseRawData(nino, taxYear)) match {
        case Right(intentToCrystalliseRequestData) =>
          crystallisationService.performIntentToCrystallise(intentToCrystalliseRequestData).map {
            case Right(desResponse) =>
              logger.info(
                s"[IntentToCrystalliseController][intentToCrystallise] - Success response received with CorrelationId: ${desResponse.correlationId}")
              val url = s"/self-assessment/ni/$nino/calculations/${desResponse.responseData}"
              auditSubmission(createAuditDetails(nino, taxYear, SEE_OTHER, desResponse.correlationId, request.userDetails))
              SeeOther(url).withHeaders(LOCATION -> url, "X-CorrelationId" -> desResponse.correlationId).as(MimeTypes.JSON)
            case Left(errorWrapper) =>
              val correlationId = getCorrelationId(errorWrapper)
              val result        = processError(errorWrapper).withHeaders("X-CorrelationId" -> correlationId)
              auditSubmission(createAuditDetails(nino, taxYear, result.header.status, correlationId, request.userDetails, Some(errorWrapper)))
              result
          }
        case Left(errorWrapper) =>
          val correlationId = getCorrelationId(errorWrapper)
          val result        = processError(errorWrapper).withHeaders("X-CorrelationId" -> correlationId)
          auditSubmission(createAuditDetails(nino, taxYear, result.header.status, correlationId, request.userDetails, Some(errorWrapper)))
          Future.successful(result)
      }
    }

  private def processError(errorWrapper: ErrorWrapper) = {
    errorWrapper.error match {
      case BadRequestError | NinoFormatError | TaxYearFormatError | RuleTaxYearNotSupportedError | RuleTaxYearRangeExceededError =>
        BadRequest(Json.toJson(errorWrapper))
      case NoSubmissionsExistError | FinalDeclarationReceivedError => Forbidden(Json.toJson(errorWrapper))
      case NotFoundError                                           => NotFound(Json.toJson(errorWrapper))
      case DownstreamError                                         => InternalServerError(Json.toJson(errorWrapper))
    }
  }

  private def getCorrelationId(errorWrapper: ErrorWrapper): String = {
    errorWrapper.correlationId match {
      case Some(correlationId) =>
        logger.info(
          "[IntentToCrystalliseController][getCorrelationId] - " +
            s"Error received from DES ${Json.toJson(errorWrapper)} with CorrelationId: $correlationId")
        correlationId
      case None =>
        val correlationId = UUID.randomUUID().toString
        logger.info(
          "[IntentToCrystalliseController][getCorrelationId] - " +
            s"Validation error: ${Json.toJson(errorWrapper)} with CorrelationId: $correlationId")
        correlationId
    }
  }

  private def createAuditDetails(nino: String,
                                 taxYear: String,
                                 statusCode: Int,
                                 correlationId: String,
                                 userDetails: UserDetails,
                                 errorWrapper: Option[ErrorWrapper] = None
                                ): IntentToCrystalliseAuditDetail = {
    val response = errorWrapper.map {
      wrapper =>
        IntentToCrystalliseAuditResponse(statusCode, Some(wrapper.allErrors.map(error => AuditError(error.code))))
    }.getOrElse(IntentToCrystalliseAuditResponse(statusCode, None))

    IntentToCrystalliseAuditDetail(userDetails.userType, userDetails.agentReferenceNumber, nino, taxYear, correlationId, response)
  }

  private def auditSubmission(details: IntentToCrystalliseAuditDetail)
                             (implicit hc: HeaderCarrier,
                              ec: ExecutionContext): Future[AuditResult] = {
    val event = AuditEvent("submitIntentToCrystallise", "intent-to-crystallise", details)
    auditService.auditEvent(event)
  }
}
