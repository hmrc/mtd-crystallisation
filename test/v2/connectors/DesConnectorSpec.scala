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

package v2.connectors

import uk.gov.hmrc.domain.Nino
import v2.mocks.{MockAppConfig, MockHttpClient}
import v2.models.domain.CrystallisationRequest
import v2.models.errors._
import v2.models.outcomes.DesResponse
import v2.models.requestData.{CrystallisationRequestData, DesTaxYear}

import scala.concurrent.Future

class DesConnectorSpec extends ConnectorSpec {

  lazy val baseUrl = "test-BaseUrl"
  val correlationId = "a1e8057e-fbbc-47a8-a8b4-78d9f015c253"

  val taxYear = DesTaxYear("2017-18")
  val nino = Nino("AA123456A")
  val calcId = "041f7e4d-87b9-4d4a-a296-3cfbdf92f7e2"

  class Test extends MockHttpClient with MockAppConfig {
    val connector: DesConnector = new DesConnector(http = mockHttpClient, appConfig = mockAppConfig)
    MockedAppConfig.desBaseUrl returns baseUrl
    MockedAppConfig.desToken returns "des-token"
    MockedAppConfig.desEnvironment returns "des-environment"
  }

  "createCrystallisation" when {
    "a valid request is supplied" should {
      "return a successful response with the correct correlationId" in new Test {
        val expected = Right(DesResponse(correlationId, None))

        MockedHttpClient.postEmpty(s"$baseUrl/income-tax/calculation/nino/$nino/$taxYear/$calcId/crystallise")
          .returns(Future.successful(expected))

        val result: CreateCrystallisationConnectorOutcome = getResult(connector)

        result shouldBe expected
      }
    }

    "a request returning a single error" should {
      "return an unsuccessful response with the correct correlationId and a single error" in new Test {
        val expected = Left(DesResponse(correlationId, Some(SingleError(RecentSubmissionsExistError))))

        MockedHttpClient.postEmpty(s"$baseUrl/income-tax/calculation/nino/$nino/$taxYear/$calcId/crystallise")
          .returns(Future.successful(expected))

        val result: CreateCrystallisationConnectorOutcome = getResult(connector)

        result shouldBe expected
      }
    }

    "a request returning multiple errors" should {
      "return an unsuccessful response with the correct correlationId and multiple errors" in new Test {
        val expected = Left(DesResponse(correlationId, Some(MultipleErrors(Seq(RecentSubmissionsExistError, ResidencyChangedError)))))

        MockedHttpClient.postEmpty(s"$baseUrl/income-tax/calculation/nino/$nino/$taxYear/$calcId/crystallise")
          .returns(Future.successful(expected))

        val result: CreateCrystallisationConnectorOutcome = getResult(connector)

        result shouldBe expected
      }
    }
  }

  private lazy val getResult: DesConnector => CreateCrystallisationConnectorOutcome = connector => {
    await(connector.createCrystallisation(CrystallisationRequestData(
      nino = nino,
      desTaxYear = taxYear,
      crystallisation = CrystallisationRequest(calcId)
    )))
  }

}
