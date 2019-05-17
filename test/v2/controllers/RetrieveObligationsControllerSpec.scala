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

import java.time.LocalDate

import play.api.libs.json.Json
import play.api.mvc.Result
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import v2.mocks.requestParsers.MockRetrieveObligationsRequestDataParser
import v2.mocks.services.{ MockCrystallisationService, MockEnrolmentsAuthService, MockMtdIdLookupService }
import v2.models.errors._
import v2.models.fixtures.Fixtures
import v2.models.outcomes.DesResponse
import v2.models.requestData._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RetrieveObligationsControllerSpec
    extends ControllerBaseSpec
    with MockEnrolmentsAuthService
    with MockMtdIdLookupService
    with MockRetrieveObligationsRequestDataParser
    with MockCrystallisationService {

  trait Test {
    val hc = HeaderCarrier()

    val controller = new RetrieveObligationsController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      retrieveObligationsRequestDataParser = mockRetrieveObligationsRequestDataParser,
      crystallisationService = mockCrystallisationService,
      cc = cc
    )

    MockedMtdIdLookupService.lookup(nino).returns(Future.successful(Right("test-mtd-id")))
    MockedEnrolmentsAuthService.authoriseUser()
  }

  private val nino          = "AA123456A"
  private val from          = "2018-04-06"
  private val to            = "2019-04-05"
  private val correlationId = "X-123"

  private val retrieveObligationsRequestData = RetrieveObligationsRequestData(Nino(nino), LocalDate.parse(from), LocalDate.parse(to))

  private val retrieveObligationsRawData = RetrieveObligationsRawData(nino, from, to)

  "retrieve Obligations" should {
    "return 200" when {
      "a valid details is supplied" in new Test {

        MockRetrieveObligationsRequestDataParser
          .parse(retrieveObligationsRawData)
          .returns(Right(retrieveObligationsRequestData))

        MockCrystallisationService
          .retrieve(retrieveObligationsRequestData)
          .returns(Future.successful(Right(DesResponse(correlationId, Seq(Fixtures.CrystallisationObligationFixture.fulfilledObligationMtd)))))

        val result: Future[Result] = controller.retrieveObligations(nino, from, to)(fakeRequest)
        status(result) shouldBe OK
        contentAsJson(result) shouldBe Fixtures.CrystallisationObligationFixture.fulfilledObligationsJsonArray
        header("X-CorrelationId", result) shouldBe Some(correlationId)
      }
    }

    "return 404" when {
      "no crystallisation obligations found" in new Test {

        MockRetrieveObligationsRequestDataParser
          .parse(retrieveObligationsRawData)
          .returns(Right(retrieveObligationsRequestData))

        MockCrystallisationService
          .retrieve(retrieveObligationsRequestData)
          .returns(Future.successful(Right(DesResponse(correlationId, Seq()))))

        val result: Future[Result] = controller.retrieveObligations(nino, from, to)(fakeRequest)
        status(result) shouldBe NOT_FOUND
        contentAsJson(result) shouldBe Json.toJson(NotFoundError)
        header("X-CorrelationId", result) shouldBe Some(correlationId)
      }
    }

    "return single error" when {
      "a invalid nino is supplied" in new Test {

        MockRetrieveObligationsRequestDataParser
          .parse(retrieveObligationsRawData)
          .returns(Left(ErrorWrapper(None, NinoFormatError, None)))

        val result: Future[Result] = controller.retrieveObligations(nino, from, to)(fakeRequest)
        status(result) shouldBe BAD_REQUEST
        contentAsJson(result) shouldBe Json.toJson(ErrorWrapper(None, NinoFormatError, None))
        header("X-CorrelationId", result).nonEmpty shouldBe true
      }
    }

    "return multiple errors response" when {
      "more than one validations exist" in new Test() {
        MockRetrieveObligationsRequestDataParser
          .parse(retrieveObligationsRawData)
          .returns(Left(ErrorWrapper(None, BadRequestError, Some(Seq(NinoFormatError, InvalidFromDateError)))))

        val result: Future[Result] = controller.retrieveObligations(nino, from, to)(fakeRequest)

        status(result) shouldBe BAD_REQUEST
        contentAsJson(result) shouldBe Json.toJson(ErrorWrapper(None, BadRequestError, Some(Seq(NinoFormatError, InvalidFromDateError))))
        header("X-CorrelationId", result).nonEmpty shouldBe true
      }
    }

    "return 400 Bad Request with a single error" when {

      val badRequestErrorsFromParser = List(
        BadRequestError,
        NinoFormatError,
        InvalidFromDateError,
        InvalidToDateError,
        RangeDateTooLongError
      )

      val badRequestErrorsFromService = List(
        BadRequestError,
        NinoFormatError,
        InvalidFromDateError,
        InvalidToDateError,
        RangeDateTooLongError
      )

      badRequestErrorsFromParser.foreach(errorsFromCreateParserTester(_, BAD_REQUEST))
      badRequestErrorsFromService.foreach(errorsFromCreateServiceTester(_, BAD_REQUEST))
    }

    "return a 500 Internal Server Error with a single error" when {

      val internalServerErrorErrors = List(
        DownstreamError
      )

      internalServerErrorErrors.foreach(errorsFromCreateParserTester(_, INTERNAL_SERVER_ERROR))
      internalServerErrorErrors.foreach(errorsFromCreateServiceTester(_, INTERNAL_SERVER_ERROR))
    }

    "return a 404 Not Found Error" when {

      val notFoundErrors = List(
        NotFoundError
      )

      notFoundErrors.foreach(errorsFromCreateServiceTester(_, NOT_FOUND))

    }

    def errorsFromCreateParserTester(error: Error, expectedStatus: Int): Unit = {
      s"a ${error.code} error is returned from the parser" in new Test {

        MockRetrieveObligationsRequestDataParser
          .parse(retrieveObligationsRawData)
          .returns(Left(ErrorWrapper(Some(correlationId), error, None)))

        val result: Future[Result] = controller.retrieveObligations(nino, from, to)(fakeRequest)

        status(result) shouldBe expectedStatus
        contentAsJson(result) shouldBe Json.toJson(error)
        header("X-CorrelationId", result) shouldBe Some(correlationId)
      }
    }

    def errorsFromCreateServiceTester(error: Error, expectedStatus: Int): Unit = {
      s"a ${error.code} error is returned from the service" in new Test {

        MockRetrieveObligationsRequestDataParser
          .parse(retrieveObligationsRawData)
          .returns(Right(retrieveObligationsRequestData))

        MockCrystallisationService
          .retrieve(retrieveObligationsRequestData)
          .returns(Future.successful(Left(ErrorWrapper(Some(correlationId), error, None))))

        val result: Future[Result] = controller.retrieveObligations(nino, from, to)(fakeRequest)

        status(result) shouldBe expectedStatus
        contentAsJson(result) shouldBe Json.toJson(error)
        header("X-CorrelationId", result) shouldBe Some(correlationId)
      }
    }
  }
}
