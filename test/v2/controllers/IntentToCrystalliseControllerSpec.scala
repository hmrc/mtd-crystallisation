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
import play.api.mvc.Result
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import v2.mocks.requestParsers.MockIntentToCrystalliseRequestDataParser
import v2.mocks.services.{MockEnrolmentsAuthService, MockMtdIdLookupService}
import v2.models.errors._
import v2.models.requestData.{DesTaxYear, IntentToCrystalliseRawData, IntentToCrystalliseRequestData}

import scala.concurrent.Future

class IntentToCrystalliseControllerSpec extends ControllerBaseSpec
  with MockEnrolmentsAuthService
  with MockMtdIdLookupService
  with MockIntentToCrystalliseRequestDataParser {

  trait Test {
    val hc = HeaderCarrier()

    val controller = new IntentToCrystalliseController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      intentToCrystalliseRequestDataParser = mockIntentToCrystalliseRequestDataParser,
      cc = cc
    )

    MockedMtdIdLookupService.lookup(nino).returns(Future.successful(Right("test-mtd-id")))
    MockedEnrolmentsAuthService.authoriseUser()
  }

  private val nino = "AA123456A"
  private val taxYear = "2017-18"
  private val correlationId = "X-123"

  val intentToCrystalliseRequestData = IntentToCrystalliseRequestData(Nino(nino), DesTaxYear.fromMtd(taxYear))

  val intentToCrystalliseRawData = IntentToCrystalliseRawData(nino, taxYear)

  "create" should {
    "return 303" when {
      "a valid details is supplied" in new Test {

        MockIntentToCrystalliseRequestDataParser.parse(intentToCrystalliseRawData)
          .returns(Right(intentToCrystalliseRequestData))

        val result: Future[Result] = controller.create(nino, taxYear)(fakeRequest)
        status(result) shouldBe SEE_OTHER
      }
    }

    "return single error" when {
      "a invalid nino is supplied" in new Test {

        MockIntentToCrystalliseRequestDataParser.parse(intentToCrystalliseRawData)
          .returns(Left(ErrorWrapper(None, NinoFormatError, None)))

        val result: Future[Result] = controller.create(nino, taxYear)(fakeRequest)
        status(result) shouldBe BAD_REQUEST
        contentAsJson(result) shouldBe Json.toJson(ErrorWrapper(None, NinoFormatError, None))
      }
    }

    "return multiple errors response" when {
      "more than one validations exist" in new Test() {
        MockIntentToCrystalliseRequestDataParser.parse(intentToCrystalliseRawData)
          .returns(Left(ErrorWrapper(None, BadRequestError, Some(Seq(NinoFormatError, TaxYearFormatError)))))

        val result: Future[Result] = controller.create(nino, taxYear)(fakeRequest)

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
        RuleTaxYearNotSupportedError
      )

      badRequestErrorsFromParser.foreach(errorsFromCreateParserTester(_, BAD_REQUEST))
    }

    "return a 500 Internal Server Error with a single error" when {

      val internalServerErrorErrors = List(
        DownstreamError
      )

      internalServerErrorErrors.foreach(errorsFromCreateParserTester(_, INTERNAL_SERVER_ERROR))
    }

    def errorsFromCreateParserTester(error: Error, expectedStatus: Int): Unit = {
      s"a ${error.code} error is returned from the parser" in new Test {

        MockIntentToCrystalliseRequestDataParser.parse(intentToCrystalliseRawData)
          .returns(Left(ErrorWrapper(Some(correlationId), error, None)))

        val result: Future[Result] = controller.create(nino, taxYear)(fakeRequest)

        status(result) shouldBe expectedStatus
        contentAsJson(result) shouldBe Json.toJson(error)
        header("X-CorrelationId", result) shouldBe Some(correlationId)
      }
    }
  }
}
