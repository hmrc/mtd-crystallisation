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
import v2.models.domain.CrystallisationRequest
import v2.models.errors._
import v2.models.outcomes.DesResponse
import v2.models.requestData.{CrystallisationRequestData, DesTaxYear}

import scala.concurrent.Future

class CrystallisationServiceSpec extends ServiceSpec {

  val correlationId = "a1e8057e-fbbc-47a8-a8b4-78d9f015c253"

  val taxYear = DesTaxYear("2017-18")
  val nino = Nino("AA123456A")
  val calcId = "041f7e4d-87b9-4d4a-a296-3cfbdf92f7e2"

  trait Test extends MockDesConnector {
    lazy val request = CrystallisationRequestData(nino, taxYear, CrystallisationRequest(calcId))
    lazy val service = new CrystallisationService(connector)
  }

  "createCrystallisation" when {
    "valid data is passed" should {
      "return a successful response with the correct correlationId" in new Test {
        val expected = Right(DesResponse(correlationId, ()))

        MockedDesConnector.createCrystallisation(request).returns(Future.successful(expected))

        val result: CrystallisationOutcome = await(service.createCrystallisation(request))

        result shouldBe expected
      }
    }

    Map(
      "INVALID_IDTYPE" -> DownstreamError,
      "INVALID_IDVALUE" -> NinoFormatError,
      "INVALID_TAXYEAR" -> InvalidTaxYearError,
      "INVALID_CALCID" -> InvalidCalcIdError,
      "NOT_FOUND" -> NotFoundError,
      "INCOME_SOURCES_CHANGED" -> IncomeSourcesChangedError,
      "RECENT_SUBMISSIONS_EXIST" -> RecentSubmissionsExistError,
      "RESIDENCY_CHANGED" -> ResidencyChangedError,
      "FINAL_DECLARATION_RECEIVED" -> FinalDeclarationReceivedError,
      "SERVER_ERROR" -> DownstreamError,
      "SERVICE_UNAVAILABLE" -> DownstreamError
    ).foreach {
      case (k, v) =>
        s"a $k error is received from the connector" should {
          s"return a $v MTD error" in new Test {
            val desResponse = DesResponse(correlationId, SingleError(Error(k, "MESSAGE")))
            val expected = Left(ErrorWrapper(Some(correlationId), v, None))

            MockedDesConnector.createCrystallisation(request).returns(Future.successful(Left(desResponse)))

            val result: CrystallisationOutcome = await(service.createCrystallisation(request))

            result shouldBe expected
          }
        }
    }

    "multiple errors are received from the connector" should {
      "return multiple MTD errors" in new Test {
        val desResponse = DesResponse(correlationId, MultipleErrors(Seq(Error("INVALID_IDVALUE", "MESSAGE"), Error("INVALID_TAXYEAR", "MESSAGE"))))
        val expected = Left(ErrorWrapper(Some(correlationId), BadRequestError, Some(Seq(NinoFormatError, InvalidTaxYearError))))

        MockedDesConnector.createCrystallisation(request).returns(Future.successful(Left(desResponse)))

        val result: CrystallisationOutcome = await(service.createCrystallisation(request))

        result shouldBe expected
      }
    }

    "one of multiple errors received from the connector is mapped to a DownstreamError" should {
      "return a single DownstreamError" in new Test {
        val desResponse = DesResponse(correlationId, MultipleErrors(Seq(Error("INVALID_IDTYPE", "MESSAGE"), Error("INVALID_TAXYEAR", "MESSAGE"))))
        val expected = Left(ErrorWrapper(Some(correlationId), DownstreamError, None))

        MockedDesConnector.createCrystallisation(request).returns(Future.successful(Left(desResponse)))

        val result: CrystallisationOutcome = await(service.createCrystallisation(request))

        result shouldBe expected
      }
    }

    "the connector returns an OutboundError" should {
      "return an OutboundError" in new Test {
        val desResponse = DesResponse(correlationId, OutboundError(DownstreamError))
        val expected = Left(ErrorWrapper(Some(correlationId), DownstreamError, None))

        MockedDesConnector.createCrystallisation(request).returns(Future.successful(Left(desResponse)))

        val result: CrystallisationOutcome = await(service.createCrystallisation(request))

        result shouldBe expected
      }
    }

    "the connector returns an unexpected error" should {
      "return a single DownstreamError" in new Test {
        val desResponse = DesResponse(correlationId, SingleError(Error("INVALID EXAMPLE", "MESSAGE")))
        val expected = Left(ErrorWrapper(Some(correlationId), DownstreamError, None))

        MockedDesConnector.createCrystallisation(request).returns(Future.successful(Left(desResponse)))

        val result: CrystallisationOutcome = await(service.createCrystallisation(request))

        result shouldBe expected
      }
    }
  }
}
