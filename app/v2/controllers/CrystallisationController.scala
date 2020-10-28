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
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContentAsJson, ControllerComponents}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import v2.controllers.requestParsers.CrystallisationRequestDataParser
import v2.models.audit._
import v2.models.auth.UserDetails
import v2.models.errors._
import v2.models.requestData.CrystallisationRawData
import v2.services.{AuditService, CrystallisationService, EnrolmentsAuthService, MtdIdLookupService}
import v2.utils.IdGenerator

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CrystallisationController @Inject()(val authService: EnrolmentsAuthService,
                                          val lookupService: MtdIdLookupService,
                                          crystallisationRequestDataParser: CrystallisationRequestDataParser,
                                          crystallisationService: CrystallisationService,
                                          auditService: AuditService,
                                          cc: ControllerComponents,
                                          val idGenerator: IdGenerator)(implicit ec: ExecutionContext)
  extends AuthorisedController(cc) with BaseController {

  implicit val endpointLogContext: EndpointLogContext =
    EndpointLogContext(controllerName = "CrystallisationController", endpointName = "create")

  def create(nino: String, taxYear: String): Action[JsValue] =
    authorisedAction(nino).async(parse.json) { implicit request =>

      implicit val correlationId: String = idGenerator.generateCorrelationId
      logger.info(
        s"[${endpointLogContext.controllerName}][${endpointLogContext.endpointName}] " +
          s"with CorrelationId: $correlationId")

      val rawData = CrystallisationRawData(nino, taxYear, AnyContentAsJson(request.body))
      val result =
        for {
          parsedRequest <- EitherT.fromEither[Future](crystallisationRequestDataParser.parseRequest(rawData))
          vendorResponse <- EitherT(crystallisationService.createCrystallisation(parsedRequest))
        } yield {
          logger.info(
            s"[${endpointLogContext.controllerName}][${endpointLogContext.endpointName}] - " +
              s"Success response received with CorrelationId: ${vendorResponse.correlationId}")
          auditSubmission(createAuditDetails(nino, taxYear, CREATED, request.request.body, vendorResponse.correlationId, request.userDetails))
          Created
            .withApiHeaders(vendorResponse.correlationId)
            .as(MimeTypes.JSON)
        }

      result.leftMap { errorWrapper =>

        val resCorrelationId = errorWrapper.correlationId
        val result = errorResult(errorWrapper).withApiHeaders(resCorrelationId)
        logger.info(
          s"[${endpointLogContext.controllerName}][${endpointLogContext.endpointName}] - " +
            s"Error response received with CorrelationId: $resCorrelationId")

        auditSubmission(createAuditDetails(nino, taxYear, result.header.status, request.request.body, correlationId, request.userDetails, Some(errorWrapper)))
        result
      }.merge
    }

  private def errorResult(errorWrapper: ErrorWrapper) = {
    (errorWrapper.error: @unchecked) match {
      case BadRequestError
           | NinoFormatError
           | TaxYearFormatError
           | RuleTaxYearNotSupportedError
           | RuleIncorrectOrEmptyBodyError
           | RuleTaxYearRangeExceededError
           | InvalidCalcIdError => BadRequest(Json.toJson(errorWrapper))
      case IncomeSourcesChangedError | RecentSubmissionsExistError | ResidencyChangedError | FinalDeclarationReceivedError =>
        Forbidden(Json.toJson(errorWrapper))
      case NotFoundError   => NotFound(Json.toJson(errorWrapper))
      case DownstreamError => InternalServerError(Json.toJson(errorWrapper))
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
