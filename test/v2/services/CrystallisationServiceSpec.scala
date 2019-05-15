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

package v2.services

import uk.gov.hmrc.domain.Nino
import v2.mocks.connectors.MockDesConnector
import v2.models.des.DesCalculationIdResponse
import v2.models.domain.{ CrystallisationRequest, EmptyJsonBody }
import v2.models.outcomes.DesResponse
import v2.models.requestData.{ CrystallisationRequestData, DesTaxYear, IntentToCrystalliseRequestData }

import scala.concurrent.Future

class CrystallisationServiceSpec extends ServiceSpec {

  val correlationId = "a1e8057e-fbbc-47a8-a8b4-78d9f015c253"

  val taxYear = DesTaxYear("2018")
  val nino    = Nino("AA123456A")
  val calcId  = "041f7e4d-87b9-4d4a-a296-3cfbdf92f7e2"

  trait Test extends MockDesConnector {
    lazy val service = new CrystallisationService(connector)
  }

  "performIntentToCrystallise" must {
    val request = IntentToCrystalliseRequestData(nino, taxYear)

    "post an empty body and retun the result" in new Test {

      val outcome = Right(DesResponse(correlationId, DesCalculationIdResponse(calcId)))

      MockedDesConnector
        .post(EmptyJsonBody,
              DesUri[DesCalculationIdResponse](s"income-tax/nino/${nino.nino}/taxYear/${taxYear.value}/tax-calculation?crystallise=true"))
        .returns(Future.successful(outcome))

      await(service.performIntentToCrystallise(request)) shouldBe outcome
    }
  }

  "createCrystallisation" must {
    val request = CrystallisationRequestData(nino, taxYear, CrystallisationRequest(calcId))

    "post an empty body and retun the result" in new Test {

      val outcome = Right(DesResponse(correlationId, ()))

      MockedDesConnector
        .post(EmptyJsonBody, DesUri[Unit](s"income-tax/calculation/nino/${nino.nino}/${taxYear.value}/$calcId/crystallise"))
        .returns(Future.successful(outcome))

      await(service.createCrystallisation(request)) shouldBe outcome
    }
  }
}
