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

import play.api.libs.json.Json
import play.api.mvc.{ AnyContentAsJson, Result }
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import v2.mocks.requestParsers.MockCrystallisationRequestDataParser
import v2.mocks.services.{ MockCrystallisationService, MockEnrolmentsAuthService, MockMtdIdLookupService }
import v2.models.domain.CrystallisationRequest
import v2.models.errors._
import v2.models.outcomes.DesResponse
import v2.models.requestData.{ CrystallisationRawData, CrystallisationRequestData, DesTaxYear }

import scala.concurrent.Future

class CrystallisationControllerSpec
    extends ControllerBaseSpec
    with MockEnrolmentsAuthService
    with MockMtdIdLookupService
    with MockCrystallisationRequestDataParser
    with MockCrystallisationService {

  trait Test {
    val hc = HeaderCarrier()

    val controller = new CrystallisationController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      crystallisationRequestDataParser = mockCrystallisationRequestDataParser,
      crystallisationService = mockCrystallisationService,
      cc = cc
    )

    MockedMtdIdLookupService.lookup(nino).returns(Future.successful(Right("test-mtd-id")))
    MockedEnrolmentsAuthService.authoriseUser()
  }

  val nino          = "AA123456A"
  val taxYear       = "2017-18"
  val calculationId = "041f7e4d-87b9-4d4a-a296-3cfbdf92f7e2"
  val correlationId = "X-123"

  val request: String =
    s"""
      |{
      |"calculationId": "$calculationId"
      |}
    """.stripMargin

  val crystallisationRequest = CrystallisationRequest(calculationId)

  val crystallisationRequestData = CrystallisationRequestData(Nino(nino), DesTaxYear.fromMtd(taxYear), crystallisationRequest)

  val crystallisationRawData = CrystallisationRawData(nino, taxYear, AnyContentAsJson(Json.parse(request)))

  "create" should {
    "return 201" when {
      "a valid details is supplied" in new Test {

        MockCrystallisationRequestDataParser
          .parse(crystallisationRawData)
          .returns(Right(crystallisationRequestData))

        MockCrystallisationService
          .create(crystallisationRequestData)
          .returns(Future.successful(Right(DesResponse(correlationId, ()))))

        val result: Future[Result] = controller.create(nino, taxYear)(fakePostRequest(Json.parse(request)))
        status(result) shouldBe CREATED
      }
    }

    "return single error" when {
      "a invalid nino is supplied" in new Test {

        MockCrystallisationRequestDataParser
          .parse(crystallisationRawData)
          .returns(Left(ErrorWrapper(None, NinoFormatError, None)))

        val result: Future[Result] = controller.create(nino, taxYear)(fakePostRequest(Json.parse(request)))
        status(result) shouldBe BAD_REQUEST
        contentAsJson(result) shouldBe Json.toJson(ErrorWrapper(None, NinoFormatError, None))
      }
    }

    "return multiple errors response" when {
      "more than one validations exist" in new Test() {
        MockCrystallisationRequestDataParser
          .parse(crystallisationRawData)
          .returns(Left(ErrorWrapper(None, BadRequestError, Some(Seq(NinoFormatError, TaxYearFormatError)))))

        val result: Future[Result] = controller.create(nino, taxYear)(fakePostRequest(Json.parse(request)))

        status(result) shouldBe BAD_REQUEST
        contentAsJson(result) shouldBe Json.toJson(ErrorWrapper(None, BadRequestError, Some(Seq(NinoFormatError, TaxYearFormatError))))
        header("X-CorrelationId", result).nonEmpty shouldBe true
      }
    }

    "return a 400 Bad Request with a single error" when {

      val badRequestErrorsFromParser = List(
        BadRequestError,
        NinoFormatError,
        TaxYearFormatError,
        RuleTaxYearNotSupportedError,
        RuleIncorrectOrEmptyBodyError,
        InvalidCalcIdError
      )

      val badRequestErrorsFromService = List(
        NinoFormatError,
        TaxYearFormatError,
        InvalidCalcIdError,
        BadRequestError
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

    "return a 403 Forbidden Error" when {

      val forbiddenErrors = List(
        IncomeSourcesChangedError,
        RecentSubmissionsExistError,
        ResidencyChangedError,
        FinalDeclarationReceivedError
      )

      forbiddenErrors.foreach(errorsFromCreateServiceTester(_, FORBIDDEN))

    }

    def errorsFromCreateParserTester(error: Error, expectedStatus: Int): Unit = {
      s"a ${error.code} error is returned from the parser" in new Test {

        MockCrystallisationRequestDataParser
          .parse(crystallisationRawData)
          .returns(Left(ErrorWrapper(Some(correlationId), error, None)))

        val result: Future[Result] = controller.create(nino, taxYear)(fakePostRequest(Json.parse(request)))

        status(result) shouldBe expectedStatus
        contentAsJson(result) shouldBe Json.toJson(error)
        header("X-CorrelationId", result) shouldBe Some(correlationId)
      }
    }

    def errorsFromCreateServiceTester(error: Error, expectedStatus: Int): Unit = {
      s"a ${error.code} error is returned from the service" in new Test {

        MockCrystallisationRequestDataParser
          .parse(crystallisationRawData)
          .returns(Right(crystallisationRequestData))

        MockCrystallisationService
          .create(crystallisationRequestData)
          .returns(Future.successful(Left(ErrorWrapper(Some(correlationId), error, None))))

        val result: Future[Result] = controller.create(nino, taxYear)(fakePostRequest(Json.parse(request)))

        status(result) shouldBe expectedStatus
        contentAsJson(result) shouldBe Json.toJson(error)
        header("X-CorrelationId", result) shouldBe Some(correlationId)
      }
    }
  }
}
