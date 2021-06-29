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

package v2.connectors

import java.time.LocalDate

import uk.gov.hmrc.http.HeaderCarrier
import v2.mocks.{MockAppConfig, MockHttpClient}
import v2.models.des.{DesCalculationIdResponse, DesObligationsResponse}
import v2.models.domain.{CrystallisationRequest, Nino}
import v2.models.errors._
import v2.models.fixtures.Fixtures.CrystallisationObligationFixture._
import v2.models.outcomes.DesResponse
import v2.models.requestData.{CrystallisationRequestData, DesTaxYear, IntentToCrystalliseRequestData, RetrieveObligationsRequestData}

import scala.concurrent.Future

class DesConnectorSpec extends ConnectorSpec {

  val taxYear: DesTaxYear = DesTaxYear("2018")
  val nino: Nino    = Nino("AA123456A")
  val calcId  = "041f7e4d-87b9-4d4a-a296-3cfbdf92f7e2"

  class Test(desEnvironmentHeaders: Option[Seq[String]]) extends MockHttpClient with MockAppConfig {
    val connector: DesConnector = new DesConnector(http = mockHttpClient, appConfig = mockAppConfig)

    MockedAppConfig.desBaseUrl returns baseUrl
    MockedAppConfig.desToken returns "des-token"
    MockedAppConfig.desEnvironment returns "des-environment"
    MockedAppConfig.desEnvironmentHeaders returns desEnvironmentHeaders
  }

  "intent to crystallise" when {
    implicit val hc: HeaderCarrier = HeaderCarrier(otherHeaders = otherHeaders ++ Seq("Content-Type" -> "application/json"))
    "a valid request is supplied" should {
      "return a successful response with the correct correlationId" in new Test(Some(allowedDesHeaders)) {
        val expected = Right(DesResponse(correlationId, DesCalculationIdResponse(calcId)))

        MockedHttpClient
          .postEmpty(
            url = s"$baseUrl/income-tax/nino/${nino.nino}/taxYear/$taxYear/tax-calculation?crystallise=true")
          .returns(Future.successful(expected))

        performIntentToCrystalliseResult(connector) shouldBe expected
      }
    }

    "a request returning a single error" should {
      "return an unsuccessful response with the correct correlationId and a single error" in new Test(Some(allowedDesHeaders)) {
        val expected = Left(DesResponse(correlationId, SingleError(NoSubmissionsExistError)))

        MockedHttpClient
          .postEmpty(
            url = s"$baseUrl/income-tax/nino/${nino.nino}/taxYear/$taxYear/tax-calculation?crystallise=true")
          .returns(Future.successful(expected))

        performIntentToCrystalliseResult(connector) shouldBe expected
      }
    }

    "a request returning multiple errors" should {
      "return an unsuccessful response with the correct correlationId and multiple errors" in new Test(Some(allowedDesHeaders)) {
        val expected = Left(DesResponse(correlationId, MultipleErrors(Seq(RecentSubmissionsExistError, ResidencyChangedError))))

        MockedHttpClient
          .postEmpty(
            url = s"$baseUrl/income-tax/nino/${nino.nino}/taxYear/$taxYear/tax-calculation?crystallise=true")
          .returns(Future.successful(expected))

        performIntentToCrystalliseResult(connector) shouldBe expected
      }
    }

    def performIntentToCrystalliseResult(connector: DesConnector): IntentToCrystalliseConnectorOutcome =
      await(
        connector.performIntentToCrystallise(
          IntentToCrystalliseRequestData(
            nino = nino,
            desTaxYear = taxYear
          )))
  }

  "createCrystallisation" when {
    implicit val hc: HeaderCarrier = HeaderCarrier(otherHeaders = otherHeaders ++ Seq("Content-Type" -> "application/json"))
    "a valid request is supplied" should {
      "return a successful response with the correct correlationId" in new Test(Some(allowedDesHeaders)) {
        val expected = Right(DesResponse(correlationId, ()))

        MockedHttpClient
          .postEmpty(
            url = s"$baseUrl/income-tax/calculation/nino/${nino.nino}/$taxYear/$calcId/crystallise")
          .returns(Future.successful(expected))

        val result: CreateCrystallisationConnectorOutcome = createCrystallisationResult(connector)

        result shouldBe expected
      }
    }

    "a request returning a single error" should {
      "return an unsuccessful response with the correct correlationId and a single error" in new Test(Some(allowedDesHeaders)) {
        val expected = Left(DesResponse(correlationId, SingleError(RecentSubmissionsExistError)))

        MockedHttpClient
          .postEmpty(
            url = s"$baseUrl/income-tax/calculation/nino/${nino.nino}/$taxYear/$calcId/crystallise")
          .returns(Future.successful(expected))

        val result: CreateCrystallisationConnectorOutcome = createCrystallisationResult(connector)

        result shouldBe expected
      }
    }

    "a request returning multiple errors" should {
      "return an unsuccessful response with the correct correlationId and multiple errors" in new Test(Some(allowedDesHeaders)) {
        val expected = Left(DesResponse(correlationId, MultipleErrors(Seq(RecentSubmissionsExistError, ResidencyChangedError))))

        MockedHttpClient
          .postEmpty(
            url = s"$baseUrl/income-tax/calculation/nino/${nino.nino}/$taxYear/$calcId/crystallise")
          .returns(Future.successful(expected))

        val result: CreateCrystallisationConnectorOutcome = createCrystallisationResult(connector)

        result shouldBe expected
      }
    }

    def createCrystallisationResult(connector: DesConnector): CreateCrystallisationConnectorOutcome =
      await(
        connector.createCrystallisation(
          CrystallisationRequestData(
            nino = nino,
            desTaxYear = taxYear,
            crystallisation = CrystallisationRequest(calcId)
          )))
  }

  "retrieve crystallisation obligations" when {

    val from = "2018-02-01"
    val to = "2018-02-28"

    "a valid request is supplied" should {
      "return a successful response" in new Test(Some(allowedDesHeaders)) {

        val expected = Right(DesResponse(correlationId, DesObligationsResponse.reads.reads(openCrystallisationObligationJsonDes).get))

        MockedHttpClient
          .get(
            url = s"$baseUrl/enterprise/obligation-data/nino/${nino.nino}/ITSA?from=$from&to=$to",
            config = dummyDesHeaderCarrierConfig,
            requiredHeaders = requiredDesHeaders,
            excludedHeaders = Seq("AnotherHeader" -> "HeaderValue"))
          .returns(Future.successful(expected))

        retrieveObligationsResult(connector) shouldBe expected
      }
    }

    "a request returning a single error" should {
      "return an unsuccessful response with the correct correlationId and a single error" in new Test(Some(allowedDesHeaders)) {
        val expected = Left(DesResponse(correlationId, SingleError(NinoFormatError)))

        MockedHttpClient
          .get(
            url = s"$baseUrl/enterprise/obligation-data/nino/${nino.nino}/ITSA?from=$from&to=$to",
            config = dummyDesHeaderCarrierConfig,
            requiredHeaders = requiredDesHeaders,
            excludedHeaders = Seq("AnotherHeader" -> "HeaderValue"))
          .returns(Future.successful(expected))


        retrieveObligationsResult(connector) shouldBe expected
      }
    }

    "a request returning multiple errors" should {
      "return an unsuccessful response with the correct correlationId and multiple errors" in new Test(Some(allowedDesHeaders)) {
        val expected = Left(DesResponse(correlationId, MultipleErrors(Seq(NinoFormatError, InvalidFromDateError))))

        MockedHttpClient
          .get(
            url = s"$baseUrl/enterprise/obligation-data/nino/${nino.nino}/ITSA?from=$from&to=$to",
            config = dummyDesHeaderCarrierConfig,
            requiredHeaders = requiredDesHeaders,
            excludedHeaders = Seq("AnotherHeader" -> "HeaderValue"))
          .returns(Future.successful(expected))

        retrieveObligationsResult(connector) shouldBe expected
      }
    }

    def retrieveObligationsResult(connector: DesConnector): RetrieveObligationsConnectorOutcome =
      await(
        connector.retrieveObligations(RetrieveObligationsRequestData(nino, LocalDate.parse(from), LocalDate.parse(to))))
  }

}
