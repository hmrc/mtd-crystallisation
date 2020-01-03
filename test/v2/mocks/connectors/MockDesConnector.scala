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

package v2.mocks.connectors

import org.scalamock.handlers.CallHandler
import org.scalamock.scalatest.MockFactory
import uk.gov.hmrc.http.HeaderCarrier
import v2.connectors.{CreateCrystallisationConnectorOutcome, DesConnector, IntentToCrystalliseConnectorOutcome, RetrieveObligationsConnectorOutcome}
import v2.models.requestData.{ RetrieveObligationsRequestData, CrystallisationRequestData, IntentToCrystalliseRequestData}

import scala.concurrent.{ExecutionContext, Future}

trait MockDesConnector extends MockFactory {
  val connector: DesConnector = mock[DesConnector]

  object MockedDesConnector {

    def performIntentToCrystallise(
        intentToCrystalliseRequestData: IntentToCrystalliseRequestData): CallHandler[Future[IntentToCrystalliseConnectorOutcome]] = {
      (connector
        .performIntentToCrystallise(_: IntentToCrystalliseRequestData)(_: HeaderCarrier, _: ExecutionContext))
        .expects(intentToCrystalliseRequestData, *, *)
    }

    def createCrystallisation(crystallisationRequestData: CrystallisationRequestData): CallHandler[Future[CreateCrystallisationConnectorOutcome]] = {
      (connector
        .createCrystallisation(_: CrystallisationRequestData)(_: HeaderCarrier, _: ExecutionContext))
        .expects(crystallisationRequestData, *, *)
    }

    def retrieveObligations(request: RetrieveObligationsRequestData): CallHandler[Future[RetrieveObligationsConnectorOutcome]] = {
      (connector
        .retrieveObligations(_: RetrieveObligationsRequestData)(_: HeaderCarrier, _: ExecutionContext))
        .expects(request, *, *)
    }
  }

}
