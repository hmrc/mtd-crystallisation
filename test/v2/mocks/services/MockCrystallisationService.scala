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

package v2.mocks.services

import org.scalamock.handlers.CallHandler
import org.scalamock.scalatest.MockFactory
import uk.gov.hmrc.http.HeaderCarrier
import v2.models.requestData.{CrystallisationRequestData, IntentToCrystalliseRequestData}
import v2.services.{CrystallisationOutcome, CrystallisationService, IntentToCrystalliseOutcome}

import scala.concurrent.{ExecutionContext, Future}

trait MockCrystallisationService extends MockFactory {

  val mockCrystallisationService: CrystallisationService = mock[CrystallisationService]

  object MockCrystallisationService {
    def create(crystallisationRequestData: CrystallisationRequestData): CallHandler[Future[CrystallisationOutcome]] = {
      (mockCrystallisationService.createCrystallisation(_: CrystallisationRequestData)(_: HeaderCarrier, _: ExecutionContext))
        .expects(crystallisationRequestData, *, *)
    }

    def intent(intentToCrystalliseRequestData: IntentToCrystalliseRequestData): CallHandler[Future[IntentToCrystalliseOutcome]] = {
      (mockCrystallisationService.performIntentToCrystallise(_: IntentToCrystalliseRequestData)(_: HeaderCarrier, _: ExecutionContext))
        .expects(intentToCrystalliseRequestData, *, *)
    }
  }

}
