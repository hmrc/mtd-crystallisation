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
import play.api.libs.json.{ JsValue, Json }
import play.api.libs.ws.{ WSRequest, WSResponse }
import support.IntegrationBaseSpec
import v2.models.errors._
import v2.models.requestData.DesTaxYear
import v2.stubs.{ AuditStub, AuthStub, DesStub, MtdIdLookupStub }

class CrystallisationISpec extends IntegrationBaseSpec {

  private trait Test {

    val nino          = "AA123456A"
    val taxYear       = "2017-18"
    val calcId        = "041f7e4d-87b9-4d4a-a296-3cfbdf92f7e2"
    val correlationId = "X-123"

    val requestJson: String =
      s"""
         |{
         |"calculationId": "$calcId"
         |}
    """.stripMargin

    def setupStubs(): StubMapping

    def uri: String

    def request(): WSRequest = {
      setupStubs()
      buildRequest(uri)
    }
  }

  "Calling the create crystallisation endpoint" should {

    trait CreateTest extends Test {
      def uri: String = s"/2.0/ni/$nino/$taxYear/crystallisation"
    }

    "return a 201 status code" when {

      "any valid request is made" in new CreateTest {

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DesStub.createSuccess(nino, DesTaxYear.fromMtd(taxYear).toString, calcId)
        }

        val response: WSResponse = await(request().post(Json.parse(requestJson)))
        response.status shouldBe Status.CREATED
      }
    }

    "return 500 (Internal Server Error)" when {

      createErrorTest(Status.BAD_REQUEST, "INVALID_IDTYPE", Status.INTERNAL_SERVER_ERROR, DownstreamError)
      createErrorTest(Status.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", Status.INTERNAL_SERVER_ERROR, DownstreamError)
      createErrorTest(Status.INTERNAL_SERVER_ERROR, "SERVER_ERROR", Status.INTERNAL_SERVER_ERROR, DownstreamError)
    }

    "return 400 (Bad Request)" when {
      createErrorTest(Status.BAD_REQUEST, "INVALID_IDVALUE", Status.BAD_REQUEST, NinoFormatError)
      createErrorTest(Status.BAD_REQUEST, "INVALID_TAXYEAR", Status.BAD_REQUEST, TaxYearFormatError)
      createErrorTest(Status.BAD_REQUEST, "INVALID_CALCID", Status.BAD_REQUEST, InvalidCalcIdError)
    }

    "return 403 (Forbidden)" when {
      createErrorTest(Status.CONFLICT, "INCOME_SOURCES_CHANGED", Status.FORBIDDEN, IncomeSourcesChangedError)
      createErrorTest(Status.CONFLICT, "RECENT_SUBMISSIONS_EXIST", Status.FORBIDDEN, RecentSubmissionsExistError)
      createErrorTest(Status.CONFLICT, "RESIDENCY_CHANGED", Status.FORBIDDEN, ResidencyChangedError)
      createErrorTest(Status.CONFLICT, "FINAL_DECLARATION_RECEIVED", Status.FORBIDDEN, FinalDeclarationReceivedError)
    }

    def createErrorTest(desStatus: Int, desCode: String, expectedStatus: Int, expectedBody: Error): Unit = {
      s"des returns an $desCode error" in new CreateTest {

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DesStub.createError(nino, DesTaxYear.fromMtd(taxYear).toString, calcId, desStatus, errorBody(desCode))
        }

        val response: WSResponse = await(request().post(Json.parse(requestJson)))
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
      s"validation fails with ${expectedBody.code} error" in new CreateTest {

        override val nino: String    = requestNino
        override val taxYear: String = requestTaxYear

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
        }

        val response: WSResponse = await(request().post(Json.parse(requestJson)))
        response.status shouldBe expectedStatus
        response.json shouldBe Json.toJson(expectedBody)
        response.header("Content-Type") shouldBe Some("application/json")
      }
    }

    s"incorrect body is supplied" in new CreateTest {
      val requestBody: JsValue = Json.parse(
        s"""{
           | "calculationId": "1234567"
           |}""".stripMargin
      )

      override def setupStubs(): StubMapping = {
        AuditStub.audit()
        AuthStub.authorised()
        MtdIdLookupStub.ninoFound(nino)
      }

      val response: WSResponse = await(request().post(requestBody))
      response.status shouldBe Status.BAD_REQUEST
      response.json shouldBe Json.toJson(ErrorWrapper(None, InvalidCalcIdError, None))
      response.header("Content-Type") shouldBe Some("application/json")
    }

    s"empty body is supplied" in new CreateTest {
      val requestBody: JsValue = Json.parse(
        s"""{
           |
           |}""".stripMargin
      )

      override def setupStubs(): StubMapping = {
        AuditStub.audit()
        AuthStub.authorised()
        MtdIdLookupStub.ninoFound(nino)
      }

      val response: WSResponse = await(request().post(requestBody))
      response.status shouldBe Status.BAD_REQUEST
      response.json shouldBe Json.toJson(ErrorWrapper(None, RuleIncorrectOrEmptyBodyError, None))
      response.header("Content-Type") shouldBe Some("application/json")
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
