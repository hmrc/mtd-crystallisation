/*
 * Copyright 2021 HM Revenue & Customs
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
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import v2.controllers.requestParsers.RetrieveObligationsRequestDataParser
import v2.models.errors._
import v2.models.requestData.RetrieveObligationsRawData
import v2.services.{CrystallisationService, EnrolmentsAuthService, MtdIdLookupService}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RetrieveObligationsController @Inject()(val authService: EnrolmentsAuthService,
                                              val lookupService: MtdIdLookupService,
                                              retrieveObligationsRequestDataParser: RetrieveObligationsRequestDataParser,
                                              crystallisationService: CrystallisationService,
                                              cc: ControllerComponents)(implicit ec: ExecutionContext)
  extends AuthorisedController(cc) with BaseController {

  implicit val endpointLogContext: EndpointLogContext =
    EndpointLogContext(controllerName = "RetrieveObligationsController", endpointName = "Retrieve Obligations")


  def retrieveObligations(nino: String, from: String, to: String): Action[AnyContent] =
    authorisedAction(nino).async { implicit request =>

      val rawData = RetrieveObligationsRawData(nino, from, to)
      val result =
        for {
          parsedRequest <- EitherT.fromEither[Future](retrieveObligationsRequestDataParser.parseRequest(rawData))
          vendorResponse <- EitherT(crystallisationService.retrieveObligations(parsedRequest))
        } yield {
          if (vendorResponse.responseData.isEmpty) {
            logger.info(
              s"[${endpointLogContext.controllerName}][${endpointLogContext.endpointName}] - " +
                s"Empty response received with correlationId: ${vendorResponse.correlationId}")
            NotFound(Json.toJson(NotFoundError))
              .withApiHeaders(vendorResponse.correlationId)
          }
          else {
            logger.info(
              s"[${endpointLogContext.controllerName}][${endpointLogContext.endpointName}] - " +
                s"Success response received with CorrelationId: ${vendorResponse.correlationId}")

            Ok(Json.obj("obligations" -> vendorResponse.responseData))
              .withApiHeaders(vendorResponse.correlationId)
          }
        }

      result.leftMap { errorWrapper =>
        val correlationId = getCorrelationId(errorWrapper)
        val result = errorResult(errorWrapper).withApiHeaders(correlationId)
        result
      }.merge
    }

  private def errorResult(errorWrapper: ErrorWrapper) = {
    (errorWrapper.error: @unchecked)  match {
      case BadRequestError
           | NinoFormatError
           | MissingFromDateError
           | MissingToDateError
           | InvalidFromDateError
           | InvalidToDateError
           | RangeDateTooLongError
           | RuleFromDateNotSupported
           | RangeEndDateBeforeStartDateError => BadRequest(Json.toJson(errorWrapper))
      case NotFoundError => NotFound(Json.toJson(errorWrapper))
      case DownstreamError => InternalServerError(Json.toJson(errorWrapper))
    }
  }
}
