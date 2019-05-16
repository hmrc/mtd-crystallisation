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
import play.api.libs.ws.{WSRequest, WSResponse}
import support.IntegrationBaseSpec
import v2.models.errors._
import v2.models.fixtures.Fixtures
import v2.stubs.{AuditStub, AuthStub, DesStub, MtdIdLookupStub}

class RetrieveObligationsISpec extends IntegrationBaseSpec {

  private trait Test {

    val nino          = "AA123456A"
    val from       = "2018-04-06"
    val to       = "2019-04-05"
    val correlationId = "X-123"

    def setupStubs(): StubMapping

    def uri: String

    def request(): WSRequest = {
      setupStubs()
      buildRequest(uri)
    }
  }

  "Calling the retrieve obligations endpoint" should {

    trait RetrieveObligationsTest extends Test {
      def uri: String = s"/2.0/ni/$nino/crystallisation/obligations?from=$from&to=$to"
    }

    "return a 200 status code" when {

      "any valid request is made" in new RetrieveObligationsTest {

        override def setupStubs(): StubMapping = {
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DesStub.retrieveObligationsSuccess(nino, from, to)
        }

        val response: WSResponse = await(request().get())
        response.status shouldBe Status.OK
        response.json shouldBe Json.toJson(Fixtures.CrystallisationObligationFixture.fulfilledObligationsJsonArray)
      }
    }

    "return a 404 status code" when {

      "any valid request is made but no obligations found" in new RetrieveObligationsTest {

        override def setupStubs(): StubMapping = {
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DesStub.retrieveNonCrystallisationObligationsSuccess(nino, from, to)
        }

        val response: WSResponse = await(request().get())
        response.status shouldBe Status.NOT_FOUND
        response.json shouldBe Json.toJson(NotFoundError)
      }
    }

    "return 500 (Internal Server Error)" when {

      retrieveObligationsErrorTest(Status.BAD_REQUEST, "INVALID_IDTYPE", Status.INTERNAL_SERVER_ERROR, DownstreamError)
      retrieveObligationsErrorTest(Status.BAD_REQUEST, "INVALID_STATUS", Status.INTERNAL_SERVER_ERROR, DownstreamError)
      retrieveObligationsErrorTest(Status.BAD_REQUEST, "INVALID_REGIME", Status.INTERNAL_SERVER_ERROR, DownstreamError)
      retrieveObligationsErrorTest(Status.BAD_REQUEST, "NOT_FOUND_BPKEY", Status.INTERNAL_SERVER_ERROR, DownstreamError)
      retrieveObligationsErrorTest(Status.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", Status.INTERNAL_SERVER_ERROR, DownstreamError)
      retrieveObligationsErrorTest(Status.INTERNAL_SERVER_ERROR, "SERVER_ERROR", Status.INTERNAL_SERVER_ERROR, DownstreamError)
    }

    "return 400 (Bad Request)" when {
      retrieveObligationsErrorTest(Status.BAD_REQUEST, "INVALID_IDNUMBER", Status.BAD_REQUEST, NinoFormatError)
      retrieveObligationsErrorTest(Status.BAD_REQUEST, "INVALID_DATE_TO", Status.BAD_REQUEST, InvalidToDateError)
      retrieveObligationsErrorTest(Status.BAD_REQUEST, "INVALID_DATE_FROM", Status.BAD_REQUEST, InvalidFromDateError)
    }

    "return 404 NOT FOUND" when {
      retrieveObligationsErrorTest(Status.NOT_FOUND, "NOT_FOUND", Status.NOT_FOUND, NotFoundError)
    }

    def retrieveObligationsErrorTest(desStatus: Int, desCode: String, expectedStatus: Int, expectedBody: Error): Unit = {
      s"des returns an $desCode error" in new RetrieveObligationsTest {

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DesStub.retrieveObligationsError(nino, from, to, desStatus, errorBody(desCode))
        }

        val response: WSResponse = await(request().get())
        response.status shouldBe expectedStatus
        response.json shouldBe Json.toJson(expectedBody)
      }
    }

    "return 400 (Bad Request)" when {
      retrieveObligationsValidationErrorTest("AA1123A", "2018-04-06", "2019-04-05", Status.BAD_REQUEST, NinoFormatError)
      retrieveObligationsValidationErrorTest("AA123456A", "", "2019-04-05", Status.BAD_REQUEST, MissingFromDateError)
      retrieveObligationsValidationErrorTest("AA123456A", "2018-04-06", "", Status.BAD_REQUEST, MissingToDateError)
      retrieveObligationsValidationErrorTest("AA123456A", "20184-06", "2019-04-05", Status.BAD_REQUEST, InvalidFromDateError)
      retrieveObligationsValidationErrorTest("AA123456A", "2018-04-06", "20104-05", Status.BAD_REQUEST, InvalidToDateError)
      retrieveObligationsValidationErrorTest("AA123456A", "2018-04-06", "2018-04-05", Status.BAD_REQUEST, RangeEndDateBeforeStartDateError)
      retrieveObligationsValidationErrorTest("AA123456A", "2018-04-06", "2020-04-05", Status.BAD_REQUEST, RangeDateTooLongError)
      retrieveObligationsValidationErrorTest("AA123456A", "2016-04-06", "2017-04-05", Status.BAD_REQUEST, RuleFromDateNotSupported)
    }

    def retrieveObligationsValidationErrorTest(requestNino: String, fromDate: String, toDate: String, expectedStatus: Int, expectedBody: Error): Unit = {
      s"validation fails with ${expectedBody.code} error" in new RetrieveObligationsTest {

        override val nino: String    = requestNino
        override val from: String = fromDate
        override val to: String = toDate

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
        }

        val response: WSResponse = await(request().get())
        response.status shouldBe expectedStatus
        response.json shouldBe Json.toJson(expectedBody)
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
