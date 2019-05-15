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

import java.time.LocalDate

import uk.gov.hmrc.domain.Nino
import v2.mocks.connectors.MockDesConnector
import v2.models.des.{DesCalculationIdResponse, DesObligationsResponse, FulfilledObligation}
import v2.models.domain.{CrystallisationRequest, Obligation}
import v2.models.errors._
import v2.models.fixtures.Fixtures.CrystallisationObligationFixture._
import v2.models.outcomes.DesResponse
import v2.models.requestData.{ RetrieveObligationsRequestData, CrystallisationRequestData, DesTaxYear, IntentToCrystalliseRequestData}

import scala.concurrent.Future

class CrystallisationServiceSpec extends ServiceSpec {

  val correlationId = "a1e8057e-fbbc-47a8-a8b4-78d9f015c253"

  val taxYear = DesTaxYear("2018")
  val nino    = Nino("AA123456A")
  val calcId  = "041f7e4d-87b9-4d4a-a296-3cfbdf92f7e2"

  trait Test extends MockDesConnector {
    lazy val service = new CrystallisationService(connector)
  }

  "performIntentToCrystallise" when {
    lazy val request = IntentToCrystalliseRequestData(nino, taxYear)

    "valid data is passed" should {
      "return a successful response with the correct correlationId" in new Test {
        MockedDesConnector
          .performIntentToCrystallise(request)
          .returns(Future.successful(Right(DesResponse(correlationId, DesCalculationIdResponse(calcId)))))

        await(service.performIntentToCrystallise(request)) shouldBe Right(DesResponse(correlationId, calcId))
      }
    }

    Map(
      "INVALID_NINO"            -> NinoFormatError,
      "INVALID_TAX_YEAR"        -> TaxYearFormatError,
      "INVALID_TAX_CRYSTALLISE" -> DownstreamError,
      "NO_SUBMISSION_EXIST"     -> NoSubmissionsExistError,
      "CONFLICT"                -> FinalDeclarationReceivedError,
      "SERVER_ERROR"            -> DownstreamError,
      "SERVICE_UNAVAILABLE"     -> DownstreamError
    ).foreach {
      case (k, v) =>
        s"a $k error is received from the connector" should {
          s"return a $v MTD error" in new Test {
            MockedDesConnector
              .performIntentToCrystallise(request)
              .returns(Future.successful(Left(DesResponse(correlationId, SingleError(Error(k, "MESSAGE"))))))

            await(service.performIntentToCrystallise(request)) shouldBe Left(ErrorWrapper(Some(correlationId), v, None))
          }
        }
    }
  }

  "createCrystallisation" when {
    lazy val request = CrystallisationRequestData(nino, taxYear, CrystallisationRequest(calcId))

    "valid data is passed" should {
      "return a successful response with the correct correlationId" in new Test {
        val expected = Right(DesResponse(correlationId, ()))

        MockedDesConnector.createCrystallisation(request).returns(Future.successful(expected))

        val result: CrystallisationOutcome = await(service.createCrystallisation(request))

        result shouldBe expected
      }
    }

    Map(
      "INVALID_IDTYPE"             -> DownstreamError,
      "INVALID_IDVALUE"            -> NinoFormatError,
      "INVALID_TAXYEAR"            -> TaxYearFormatError,
      "INVALID_CALCID"             -> InvalidCalcIdError,
      "NOT_FOUND"                  -> NotFoundError,
      "INCOME_SOURCES_CHANGED"     -> IncomeSourcesChangedError,
      "RECENT_SUBMISSIONS_EXIST"   -> RecentSubmissionsExistError,
      "RESIDENCY_CHANGED"          -> ResidencyChangedError,
      "FINAL_DECLARATION_RECEIVED" -> FinalDeclarationReceivedError,
      "SERVER_ERROR"               -> DownstreamError,
      "SERVICE_UNAVAILABLE"        -> DownstreamError
    ).foreach {
      case (k, v) =>
        s"a $k error is received from the connector" should {
          s"return a $v MTD error" in new Test {
            val desResponse = DesResponse(correlationId, SingleError(Error(k, "MESSAGE")))
            val expected    = Left(ErrorWrapper(Some(correlationId), v, None))

            MockedDesConnector.createCrystallisation(request).returns(Future.successful(Left(desResponse)))

            val result: CrystallisationOutcome = await(service.createCrystallisation(request))

            result shouldBe expected
          }
        }
    }
  }

  "retrieveObligations" when {
    val from = LocalDate.parse("2018-02-01")
    val to = LocalDate.parse("2018-02-28")
    val due = LocalDate.parse("2018-05-28")
    val processed = LocalDate.parse("2018-04-01")

    lazy val request = RetrieveObligationsRequestData(nino, from, to)

    "valid data is passed" should {
      "return a sequence of obligations with the correct correlationId" in new Test {
        val expected = Right(DesResponse(correlationId,
          List(Obligation(from, to, due, FulfilledObligation, Some(processed)))))

        val desResponse = Right(DesResponse(correlationId, DesObligationsResponse.reads.reads(fulfilledCrystallisationObligationJsonDes).get))

        MockedDesConnector.retrieveObligations(request).returns(Future.successful(desResponse))

        val result: RetrieveObligationsOutcome = await(service.retrieveObligations(request))

        result shouldBe expected
      }
    }

    "data passed doesn't have any crystallisation obligations" should {
      "return a empty sequence with the correct correlationId" in new Test {
        val expected = Right(DesResponse(correlationId, List()))

        val desResponse = Right(DesResponse(correlationId, DesObligationsResponse.reads.reads(notCrystallisationObligationsJsonDes).get))

        MockedDesConnector.retrieveObligations(request).returns(Future.successful(desResponse))

        val result: RetrieveObligationsOutcome = await(service.retrieveObligations(request))

        result shouldBe expected
      }
    }

    Map(
      "INVALID_IDTYPE"             -> DownstreamError,
      "INVALID_IDNUMBER"           -> NinoFormatError,
      "INVALID_STATUS"             -> DownstreamError,
      "INVALID_REGIME"             -> DownstreamError,
      "NOT_FOUND"                  -> NotFoundError,
      "INVALID_DATE_TO"            -> InvalidToDateError,
      "INVALID_DATE_FROM"          -> InvalidFromDateError,
      "NOT_FOUND_BPKEY"            -> DownstreamError,
      "INVALID_DATE_RANGE"         -> RangeDateTooLongError,
      "SERVER_ERROR"               -> DownstreamError,
      "SERVICE_UNAVAILABLE"        -> DownstreamError
    ).foreach {
      case (k, v) =>
        s"a $k error is received from the connector" should {
          s"return a $v MTD error" in new Test {
            val desResponse = DesResponse(correlationId, SingleError(Error(k, "MESSAGE")))
            val expected    = Left(ErrorWrapper(Some(correlationId), v, None))

            MockedDesConnector.retrieveObligations(request).returns(Future.successful(Left(desResponse)))

            val result: RetrieveObligationsOutcome = await(service.retrieveObligations(request))

            result shouldBe expected
          }
        }
    }
  }
}
