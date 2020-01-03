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

package v2.controllers

import play.api.libs.json.Json
import play.api.mvc.Result
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import v2.mocks.requestParsers.MockIntentToCrystalliseRequestDataParser
import v2.mocks.services.{MockAuditService, MockCrystallisationService, MockEnrolmentsAuthService, MockMtdIdLookupService}
import v2.models.audit.{AuditError, AuditEvent, IntentToCrystalliseAuditDetail, IntentToCrystalliseAuditResponse}
import v2.models.errors._
import v2.models.outcomes.DesResponse
import v2.models.requestData.{DesTaxYear, IntentToCrystalliseRawData, IntentToCrystalliseRequestData}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class IntentToCrystalliseControllerSpec
    extends ControllerBaseSpec
    with MockEnrolmentsAuthService
    with MockMtdIdLookupService
    with MockIntentToCrystalliseRequestDataParser
    with MockCrystallisationService
    with MockAuditService {

  trait Test {
    val hc = HeaderCarrier()

    val controller = new IntentToCrystalliseController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      intentToCrystalliseRequestDataParser = mockIntentToCrystalliseRequestDataParser,
      crystallisationService = mockCrystallisationService,
      auditService = mockAuditService,
      cc = cc
    )

    MockedMtdIdLookupService.lookup(nino).returns(Future.successful(Right("test-mtd-id")))
    MockedEnrolmentsAuthService.authoriseUser()
  }

  private val nino          = "AA123456A"
  private val taxYear       = "2017-18"
  private val correlationId = "X-123"
  private val calculationId = "041f7e4d-87b9-4d4a-a296-3cfbdf92f7e2"

  private val intentToCrystalliseRequestData = IntentToCrystalliseRequestData(Nino(nino), DesTaxYear.fromMtd(taxYear))

  private val intentToCrystalliseRawData = IntentToCrystalliseRawData(nino, taxYear)

  "intentToCrystallise" should {
    "return 303" when {
      "a valid details is supplied" in new Test {

        MockIntentToCrystalliseRequestDataParser
          .parse(intentToCrystalliseRawData)
          .returns(Right(intentToCrystalliseRequestData))

        MockCrystallisationService
          .intent(intentToCrystalliseRequestData)
          .returns(Future.successful(Right(DesResponse(correlationId, calculationId))))

        val result: Future[Result] = controller.intentToCrystallise(nino, taxYear)(fakeRequest)
        status(result) shouldBe SEE_OTHER
        header("Location", result).isEmpty shouldBe false
        header("Location", result) shouldBe Some(s"/self-assessment/ni/$nino/calculations/$calculationId")

        val detail = IntentToCrystalliseAuditDetail("Individual", None, nino, taxYear, correlationId, Some(calculationId),
          IntentToCrystalliseAuditResponse(SEE_OTHER, None))
        val event: AuditEvent[IntentToCrystalliseAuditDetail] = AuditEvent[IntentToCrystalliseAuditDetail]("submitIntentToCrystallise",
          "intent-to-crystallise", detail)
        MockedAuditService.verifyAuditEvent(event).once
      }
    }

    "return single error" when {
      "a invalid nino is supplied" in new Test {

        MockIntentToCrystalliseRequestDataParser
          .parse(intentToCrystalliseRawData)
          .returns(Left(ErrorWrapper(None, NinoFormatError, None)))

        val result: Future[Result] = controller.intentToCrystallise(nino, taxYear)(fakeRequest)
        status(result) shouldBe BAD_REQUEST
        contentAsJson(result) shouldBe Json.toJson(ErrorWrapper(None, NinoFormatError, None))

        val detail = IntentToCrystalliseAuditDetail("Individual", None, nino, taxYear, header("X-CorrelationId", result).get, None,
          IntentToCrystalliseAuditResponse(BAD_REQUEST, Some(Seq(AuditError(NinoFormatError.code)))))
        val event: AuditEvent[IntentToCrystalliseAuditDetail] = AuditEvent[IntentToCrystalliseAuditDetail]("submitIntentToCrystallise",
          "intent-to-crystallise", detail)
        MockedAuditService.verifyAuditEvent(event).once
      }
    }

    "return multiple errors response" when {
      "more than one validations exist" in new Test() {
        MockIntentToCrystalliseRequestDataParser
          .parse(intentToCrystalliseRawData)
          .returns(Left(ErrorWrapper(None, BadRequestError, Some(Seq(NinoFormatError, TaxYearFormatError)))))

        val result: Future[Result] = controller.intentToCrystallise(nino, taxYear)(fakeRequest)

        status(result) shouldBe BAD_REQUEST
        contentAsJson(result) shouldBe Json.toJson(ErrorWrapper(None, BadRequestError, Some(Seq(NinoFormatError, TaxYearFormatError))))
        header("X-CorrelationId", result).nonEmpty shouldBe true

        val detail = IntentToCrystalliseAuditDetail("Individual", None, nino, taxYear, header("X-CorrelationId", result).get, None,
          IntentToCrystalliseAuditResponse(BAD_REQUEST, Some(Seq(AuditError(NinoFormatError.code), AuditError(TaxYearFormatError.code)))))
        val event: AuditEvent[IntentToCrystalliseAuditDetail] = AuditEvent[IntentToCrystalliseAuditDetail]("submitIntentToCrystallise",
          "intent-to-crystallise", detail)
        MockedAuditService.verifyAuditEvent(event).once
      }
    }

    "return 400 Bad Request with a single error" when {

      val badRequestErrorsFromParser = List(
        BadRequestError,
        NinoFormatError,
        TaxYearFormatError,
        RuleTaxYearNotSupportedError,
        RuleTaxYearRangeExceededError
      )

      val badRequestErrorsFromService = List(
        NinoFormatError,
        TaxYearFormatError,
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
        NoSubmissionsExistError,
        FinalDeclarationReceivedError
      )

      forbiddenErrors.foreach(errorsFromCreateServiceTester(_, FORBIDDEN))

    }

    def errorsFromCreateParserTester(error: Error, expectedStatus: Int): Unit = {
      s"a ${error.code} error is returned from the parser" in new Test {

        MockIntentToCrystalliseRequestDataParser
          .parse(intentToCrystalliseRawData)
          .returns(Left(ErrorWrapper(Some(correlationId), error, None)))

        val result: Future[Result] = controller.intentToCrystallise(nino, taxYear)(fakeRequest)

        status(result) shouldBe expectedStatus
        contentAsJson(result) shouldBe Json.toJson(error)
        header("X-CorrelationId", result) shouldBe Some(correlationId)

        val detail = IntentToCrystalliseAuditDetail("Individual", None, nino, taxYear, header("X-CorrelationId", result).get, None,
          IntentToCrystalliseAuditResponse(expectedStatus, Some(Seq(AuditError(error.code)))))
        val event: AuditEvent[IntentToCrystalliseAuditDetail] = AuditEvent[IntentToCrystalliseAuditDetail]("submitIntentToCrystallise",
          "intent-to-crystallise", detail)
        MockedAuditService.verifyAuditEvent(event).once
      }
    }

    def errorsFromCreateServiceTester(error: Error, expectedStatus: Int): Unit = {
      s"a ${error.code} error is returned from the service" in new Test {

        MockIntentToCrystalliseRequestDataParser
          .parse(intentToCrystalliseRawData)
          .returns(Right(intentToCrystalliseRequestData))

        MockCrystallisationService
          .intent(intentToCrystalliseRequestData)
          .returns(Future.successful(Left(ErrorWrapper(Some(correlationId), error, None))))

        val result: Future[Result] = controller.intentToCrystallise(nino, taxYear)(fakeRequest)

        status(result) shouldBe expectedStatus
        contentAsJson(result) shouldBe Json.toJson(error)
        header("X-CorrelationId", result) shouldBe Some(correlationId)

        val detail = IntentToCrystalliseAuditDetail("Individual", None, nino, taxYear, header("X-CorrelationId", result).get, None,
          IntentToCrystalliseAuditResponse(expectedStatus, Some(Seq(AuditError(error.code)))))
        val event: AuditEvent[IntentToCrystalliseAuditDetail] = AuditEvent[IntentToCrystalliseAuditDetail]("submitIntentToCrystallise",
          "intent-to-crystallise", detail)
        MockedAuditService.verifyAuditEvent(event).once
      }
    }
  }
}
