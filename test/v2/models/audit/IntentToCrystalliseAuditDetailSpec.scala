/*
 * Copyright 2021 HM Revenue & Customs
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

package v2.models.audit

import play.api.http.Status
import play.api.libs.json.Json
import support.UnitSpec

class IntentToCrystalliseAuditDetailSpec extends UnitSpec {

  private val userType = "Organisation"
  private val agentReferenceNumber = Some("012345678")
  private val nino = "AA123456A"
  private val taxYear = "2017-18"
  private val `X-CorrelationId` = "X-123"
  private val responseSuccess = IntentToCrystalliseAuditResponse(Status.SEE_OTHER, None)
  private val responseFail = IntentToCrystalliseAuditResponse(Status.BAD_REQUEST, Some(Seq(AuditError("FORMAT_NINO"))))
  private val calculationId = "041f7e4d-87b9-4d4a-a296-3cfbdf92f7e2"

  "writes" when {
    "passed an audit model with all fields provided" should {
      "produce valid json" in {
        val json = Json.parse(
          s"""
             |{
             |  "userType": "Organisation",
             |  "agentReferenceNumber": "012345678",
             |  "nino": "AA123456A",
             |  "taxYear": "2017-18",
             |  "X-CorrelationId": "X-123",
             |  "calculationId": "$calculationId",
             |  "response": {
             |    "httpStatus": 303
             |  }
             |}
           """.stripMargin)

        val model = IntentToCrystalliseAuditDetail(userType, agentReferenceNumber, nino, taxYear, `X-CorrelationId`, Some(calculationId), responseSuccess)

        Json.toJson(model) shouldBe json
      }
    }

    "passed an audit model with only mandatory fields provided" should {
      "produce valid json" in {
        val json = Json.parse(
          s"""
             |{
             |  "userType": "Organisation",
             |  "nino": "AA123456A",
             |  "taxYear": "2017-18",
             |  "X-CorrelationId": "X-123",
             |  "response": {
             |    "httpStatus": 400,
             |    "errors": [
             |      {
             |        "errorCode": "FORMAT_NINO"
             |      }
             |    ]
             |  }
             |}
           """.stripMargin)

        val model = IntentToCrystalliseAuditDetail(userType, None, nino, taxYear, `X-CorrelationId`, None, responseFail)

        Json.toJson(model) shouldBe json
      }
    }
  }
}
