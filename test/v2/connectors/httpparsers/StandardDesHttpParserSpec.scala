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

package v2.connectors.httpparsers

import play.api.http.Status._
import play.api.libs.json.Json
import support.UnitSpec
import uk.gov.hmrc.http.{HttpReads, HttpResponse}
import v2.connectors.DesConnectorOutcome
import v2.models.errors._
import v2.models.outcomes.DesResponse

class StandardDesHttpParserSpec extends UnitSpec {

  val method = "POST"
  val url = "test-url"

  val correlationId = "a1e8057e-fbbc-47a8-a8b4-78d9f015c253"

  import v2.connectors.httpparsers.StandardDesHttpParser._

  val httpReads: HttpReads[DesConnectorOutcome[String]] = implicitly

  "The generic HTTP parser" when {
    "receiving a 204 response" should {
      "return a Right DesResponse with the correct correlationId and no responseData" in {
        val httpResponse = HttpResponse(NO_CONTENT, responseHeaders = Map("CorrelationId" -> Seq(correlationId)))

        getResult(httpResponse) shouldBe Right(DesResponse(correlationId, None))
      }
    }

    Seq(BAD_REQUEST, NOT_FOUND, FORBIDDEN).foreach(
      responseCode =>
        s"receiving a $responseCode response" should {
          "be able to parse a single error" in {
            val singleErrorJson = Json.parse(
              """
                |{
                |   "code": "CODE",
                |   "reason": "MESSAGE"
                |}
              """.stripMargin
            )

            val httpResponse = HttpResponse(responseCode, Some(singleErrorJson), Map("CorrelationId" -> Seq(correlationId)))

            getResult(httpResponse) shouldBe Left(DesResponse(correlationId, Some(SingleError(Error("CODE", "MESSAGE")))))
          }

          "be able to parse multiple errors" in {
            val multipleErrorsJson = Json.parse(
              """
                |{
                |   "failures": [
                |       {
                |           "code": "CODE 1",
                |           "reason": "MESSAGE 1"
                |       },
                |       {
                |           "code": "CODE 2",
                |           "reason": "MESSAGE 2"
                |       }
                |   ]
                |}
              """.stripMargin
            )

            val httpResponse = HttpResponse(responseCode, Some(multipleErrorsJson), Map("CorrelationId" -> Seq(correlationId)))

            getResult(httpResponse) shouldBe {
              Left(DesResponse(correlationId, Some(MultipleErrors(Seq(Error("CODE 1", "MESSAGE 1"), Error("CODE 2", "MESSAGE 2"))))))
            }
          }

          "return an outbound error when the error returned doesn't match the Error model" in {
            val singleErrorJson = Json.parse(
              """
                |{
                |   "coed": "CODE",
                |   "resaon": "MESSAGE"
                |}
              """.stripMargin
            )

            val httpResponse = HttpResponse(responseCode, Some(singleErrorJson), Map("CorrelationId" -> Seq(correlationId)))

            getResult(httpResponse) shouldBe Left(DesResponse(correlationId, Some(OutboundError(DownstreamError))))
          }
        }
    )

    Seq(INTERNAL_SERVER_ERROR, SERVICE_UNAVAILABLE).foreach (
      responseCode =>
        s"receiving a $responseCode response" should {
          "return an outbound error when the error returned matches the Error model" in {
            val singleErrorJson = Json.parse(
              """
                |{
                |   "code": "CODE",
                |   "reason": "MESSAGE"
                |}
              """.stripMargin
            )

            val httpResponse = HttpResponse(responseCode, Some(singleErrorJson), Map("CorrelationId" -> Seq(correlationId)))

            getResult(httpResponse) shouldBe Left(DesResponse(correlationId, Some(OutboundError(DownstreamError))))
          }

          "return an outbound error when the error returned doesn't match the Error model" in {
            val singleErrorJson = Json.parse(
              """
                |{
                |   "coed": "CODE",
                |   "resaon": "MESSAGE"
                |}
              """.stripMargin
            )

            val httpResponse = HttpResponse(responseCode, Some(singleErrorJson), Map("CorrelationId" -> Seq(correlationId)))

            getResult(httpResponse) shouldBe Left(DesResponse(correlationId, Some(OutboundError(DownstreamError))))
          }
        }
    )

    "receiving an unexpected response" should {
      val responseCode = 499
      "return an outbound error when the error returned matches the Error model" in {
        val singleErrorJson = Json.parse(
          """
            |{
            |   "code": "CODE",
            |   "reason": "MESSAGE"
            |}
          """.stripMargin
        )

        val httpResponse = HttpResponse(responseCode, Some(singleErrorJson), Map("CorrelationId" -> Seq(correlationId)))

        getResult(httpResponse) shouldBe Left(DesResponse(correlationId, Some(OutboundError(DownstreamError))))
      }

      "return an outbound error when the error returned doesn't match the Error model" in {
        val singleErrorJson = Json.parse(
          """
            |{
            |   "coed": "CODE",
            |   "resaon": "MESSAGE"
            |}
          """.stripMargin
        )

        val httpResponse = HttpResponse(responseCode, Some(singleErrorJson), Map("CorrelationId" -> Seq(correlationId)))

        getResult(httpResponse) shouldBe Left(DesResponse(correlationId, Some(OutboundError(DownstreamError))))
      }
    }
  }

  private lazy val getResult: HttpResponse => DesConnectorOutcome[String] = httpResponse => httpReads.read(method, url, httpResponse)
}
