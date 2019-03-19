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

package v2.connectors.httpparsers

import play.api.Logger
import play.api.http.Status._
import uk.gov.hmrc.http.{HttpReads, HttpResponse}
import v2.connectors.DesConnectorOutcome
import v2.models.errors.{DownstreamError, OutboundError}
import v2.models.outcomes.DesResponse


object StandardDesHttpParser extends HttpParser {

  val logger = Logger(getClass)

  // Return Right[DesResponse[Unit]] as success response has no body - no need to assign it a value
  implicit val readsEmpty: HttpReads[DesConnectorOutcome[Unit]] =
    new HttpReads[DesConnectorOutcome[Unit]] {
      override def read(method: String, url: String, response: HttpResponse): DesConnectorOutcome[Unit] = {
        val correlationId = retrieveCorrelationId(response)

        if (response.status != NO_CONTENT) {
          logger.info("[StandardDesHttpParser][read] - " +
            s"Error response received from DES with status: ${response.status} and body\n" +
            s"${response.body} and correlationId: $correlationId when calling $url")
        }

        response.status match {
          case NO_CONTENT =>
            logger.info("[StandardDesHttpParser][read] - " +
              s"Success response received from DES with correlationId: $correlationId when calling $url")
            Right(DesResponse(correlationId, ()))
          case BAD_REQUEST | NOT_FOUND | FORBIDDEN => Left(DesResponse(correlationId, parseErrors(response)))
          case INTERNAL_SERVER_ERROR | SERVICE_UNAVAILABLE => Left(DesResponse(correlationId, OutboundError(DownstreamError)))
          case _ => Left(DesResponse(correlationId, OutboundError(DownstreamError)))
        }
      }
    }
}
