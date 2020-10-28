/*
 * Copyright 2020 HM Revenue & Customs
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

import cats.data.EitherT
import cats.implicits._
import javax.inject.{Inject, Singleton}
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
import v2.utils.IdGenerator

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class IntentToCrystalliseController @Inject()(val authService: EnrolmentsAuthService,
                                              val lookupService: MtdIdLookupService,
                                              intentToCrystalliseRequestDataParser: IntentToCrystalliseRequestDataParser,
                                              crystallisationService: CrystallisationService,
                                              auditService: AuditService,
                                              cc: ControllerComponents,
                                              val idGenerator: IdGenerator)(implicit ec: ExecutionContext)
  extends AuthorisedController(cc) with BaseController {

  implicit val endpointLogContext: EndpointLogContext =
    EndpointLogContext(controllerName = "IntentToCrystalliseController", endpointName = "intentToCrystallise")


  def intentToCrystallise(nino: String, taxYear: String): Action[AnyContent] =
    authorisedAction(nino).async { implicit request =>

      implicit val correlationId: String = idGenerator.generateCorrelationId
      logger.info(
        s"[${endpointLogContext.controllerName}][${endpointLogContext.endpointName}] " +
          s"with CorrelationId: $correlationId")

      val rawData = IntentToCrystalliseRawData(nino, taxYear)
      val result =
        for {
          parsedRequest <- EitherT.fromEither[Future](intentToCrystalliseRequestDataParser.parseRequest(rawData))
          vendorResponse <- EitherT(crystallisationService.performIntentToCrystallise(parsedRequest))
        } yield {
          logger.info(
            s"[${endpointLogContext.controllerName}][${endpointLogContext.endpointName}] - " +
              s"Success response received with CorrelationId: ${vendorResponse.correlationId}")
          auditSubmission(createAuditDetails(nino, taxYear, SEE_OTHER, vendorResponse.correlationId, request.userDetails, Some(vendorResponse.responseData)))
          val url = s"/self-assessment/ni/$nino/calculations/${vendorResponse.responseData}"
          SeeOther(url).withApiHeaders(vendorResponse.correlationId, LOCATION -> url).as(MimeTypes.JSON)
        }

      result.leftMap { errorWrapper =>

        val resCorrelationId = errorWrapper.correlationId
        val result = errorResult(errorWrapper).withApiHeaders(resCorrelationId)
        logger.info(
          s"[${endpointLogContext.controllerName}][${endpointLogContext.endpointName}] - " +
            s"Error response received with CorrelationId: $resCorrelationId")


        auditSubmission(createAuditDetails(nino, taxYear, result.header.status, correlationId, request.userDetails, None, Some(errorWrapper)))
        result
      }.merge
    }


  private def errorResult(errorWrapper: ErrorWrapper) = {
    (errorWrapper.error: @unchecked) match {
      case BadRequestError
           | NinoFormatError
           | TaxYearFormatError
           | RuleTaxYearNotSupportedError
           | RuleTaxYearRangeExceededError => BadRequest(Json.toJson(errorWrapper))
      case NoSubmissionsExistError | FinalDeclarationReceivedError => Forbidden(Json.toJson(errorWrapper))
      case NotFoundError => NotFound(Json.toJson(errorWrapper))
      case DownstreamError => InternalServerError(Json.toJson(errorWrapper))
    }
  }

  private def createAuditDetails(nino: String,
                                 taxYear: String,
                                 statusCode: Int,
                                 correlationId: String,
                                 userDetails: UserDetails,
                                 calculationId: Option[String],
                                 errorWrapper: Option[ErrorWrapper] = None): IntentToCrystalliseAuditDetail = {
    val response = errorWrapper
      .map { wrapper =>
        IntentToCrystalliseAuditResponse(statusCode, Some(wrapper.allErrors.map(error => AuditError(error.code))))
      }
      .getOrElse(IntentToCrystalliseAuditResponse(statusCode, None))

    IntentToCrystalliseAuditDetail(userDetails.userType, userDetails.agentReferenceNumber, nino, taxYear, correlationId, calculationId, response)
  }

  private def auditSubmission(details: IntentToCrystalliseAuditDetail)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[AuditResult] = {
    val event = AuditEvent("submitIntentToCrystallise", "intent-to-crystallise", details)
    auditService.auditEvent(event)
  }
}
