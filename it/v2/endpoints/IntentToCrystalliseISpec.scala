/*
 * Copyright 2018 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIED OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package v2.endpoints

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status
import play.api.libs.json.Json
import play.api.libs.ws.{ EmptyBody, WSRequest, WSResponse }
import support.IntegrationBaseSpec
import v2.models.errors._
import v2.models.requestData.DesTaxYear
import v2.stubs.{ AuditStub, AuthStub, DesStub, MtdIdLookupStub }

class IntentToCrystalliseISpec extends IntegrationBaseSpec {

  private trait Test {

    val nino          = "AA123456A"
    val taxYear       = "2017-18"
    val calcId        = "041f7e4d-87b9-4d4a-a296-3cfbdf92f7e2"
    val correlationId = "X-123"

    def setupStubs(): StubMapping

    def uri: String

    def request(): WSRequest = {
      setupStubs()
      buildRequest(uri)
    }
  }

  "Calling the intent to crystallise endpoint" should {

    trait IntentToCrystalliseTest extends Test {
      def uri: String = s"/2.0/ni/$nino/$taxYear/intent-to-crystallise"
    }

    "return a 303 status code" when {

      "any valid request is made" in new IntentToCrystalliseTest {

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DesStub.intentToSuccess(nino, DesTaxYear.fromMtd(taxYear).toString)
        }

        val response: WSResponse = await(request().post(EmptyBody))
        response.status shouldBe Status.SEE_OTHER
        response.header("Location") shouldBe Some(s"/self-assessment/ni/$nino/calculations/$calcId")
        response.header("Content-Type") shouldBe Some("application/json")
      }
    }

    "return 500 (Internal Server Error)" when {

      createErrorTest(Status.BAD_REQUEST, "INVALID_REQUEST", Status.INTERNAL_SERVER_ERROR, DownstreamError)
      createErrorTest(Status.BAD_REQUEST, "INVALID_TAX_CRYSTALLISE", Status.INTERNAL_SERVER_ERROR, DownstreamError)
      createErrorTest(Status.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", Status.INTERNAL_SERVER_ERROR, DownstreamError)
      createErrorTest(Status.INTERNAL_SERVER_ERROR, "SERVER_ERROR", Status.INTERNAL_SERVER_ERROR, DownstreamError)
    }

    "return 400 (Bad Request)" when {
      createErrorTest(Status.BAD_REQUEST, "INVALID_NINO", Status.BAD_REQUEST, NinoFormatError)
      createErrorTest(Status.BAD_REQUEST, "INVALID_TAX_YEAR", Status.BAD_REQUEST, TaxYearFormatError)
    }

    "return 403 (Forbidden)" when {
      createErrorTest(Status.FORBIDDEN, "NO_SUBMISSION_EXIST", Status.FORBIDDEN, NoSubmissionsExistError)
      createErrorTest(Status.CONFLICT, "CONFLICT", Status.FORBIDDEN, FinalDeclarationReceivedError)
    }

    def createErrorTest(desStatus: Int, desCode: String, expectedStatus: Int, expectedBody: Error): Unit = {
      s"des returns an $desCode error" in new IntentToCrystalliseTest {

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DesStub.intentToError(nino, DesTaxYear.fromMtd(taxYear).toString, desStatus, errorBody(desCode))
        }

        val response: WSResponse = await(request().post(EmptyBody))
        response.status shouldBe expectedStatus
        response.json shouldBe Json.toJson(expectedBody)
        response.header("Content-Type") shouldBe Some("application/json")
      }
    }

    "return 400 (Bad Request)" when {
      createRequestValidationErrorTest("AA1123A", "2017-18", Status.BAD_REQUEST, NinoFormatError)
      createRequestValidationErrorTest("AA123456A", "20177", Status.BAD_REQUEST, TaxYearFormatError)
      createRequestValidationErrorTest("AA123456A", "2015-16", Status.BAD_REQUEST, RuleTaxYearNotSupportedError)
    }

    def createRequestValidationErrorTest(requestNino: String, requestTaxYear: String, expectedStatus: Int, expectedBody: Error): Unit = {
      s"validation fails with ${expectedBody.code} error" in new IntentToCrystalliseTest {

        override val nino: String    = requestNino
        override val taxYear: String = requestTaxYear

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
        }

        val response: WSResponse = await(request().post(EmptyBody))
        response.status shouldBe expectedStatus
        response.json shouldBe Json.toJson(expectedBody)
        response.header("Content-Type") shouldBe Some("application/json")
      }
    }
  }

  def errorBody(code: String): String =
    s"""
       |      {
       |        "code": "$code",
       |        "reason": "des message"
       |      }
      """.stripMargin

}
