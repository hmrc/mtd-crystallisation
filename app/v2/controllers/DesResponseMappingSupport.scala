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

import play.api.Logger
import v2.models.errors.{ BadRequestError, DesError, DesErrors, DownstreamError, MtdError, ErrorWrapper, OutboundError }
import v2.models.outcomes.DesResponse

trait DesResponseMappingSupport {

  /**
    * Controller name for logging
    */
  val controllerName: String

  protected val logger: Logger = Logger(this.getClass)

  final def mapDesErrors[D](endpointName: String, errorMap: PartialFunction[String, MtdError])(desOutcome: DesResponse[DesError]): ErrorWrapper = {

    lazy val defaultErrorMapping: String => MtdError = { code =>
      logger.info(s"[$controllerName] [$endpointName] - No mapping found for error code $code")
      DownstreamError
    }

    desOutcome match {
      case DesResponse(correlationId, DesErrors(error :: Nil)) =>
        ErrorWrapper(Some(correlationId), errorMap.applyOrElse(error.code, defaultErrorMapping), None)

      case DesResponse(correlationId, DesErrors(errors)) =>
        val mtdErrors = errors.map(error => errorMap.applyOrElse(error.code, defaultErrorMapping))

        if (mtdErrors.contains(DownstreamError)) {
          logger.info(
            s"[$controllerName] [$endpointName] [CorrelationId - $correlationId]" +
              s" - downstream returned ${errors.map(_.code).mkString(",")}. Revert to ISE")
          ErrorWrapper(Some(correlationId), DownstreamError, None)
        } else {
          ErrorWrapper(Some(correlationId), BadRequestError, Some(mtdErrors))
        }

      case DesResponse(correlationId, OutboundError(error, errors)) =>
        ErrorWrapper(Some(correlationId), error, errors)
    }
  }
}
