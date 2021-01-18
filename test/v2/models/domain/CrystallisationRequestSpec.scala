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

package v2.models.domain

import play.api.libs.json._
import support.UnitSpec
import v2.models.utils.JsonErrorValidators

class CrystallisationRequestSpec extends UnitSpec with JsonErrorValidators {
  "reads" when {
    "passed valid JSON" should {
      val inputJson = Json.parse(
        """
          |{
          |   "calculationId": "041f7e4d-87b9-4d4a-a296-3cfbdf92f7e2"
          |}
        """.stripMargin
      )

      "return a valid CrystallisationRequest model" in {
        CrystallisationRequest("041f7e4d-87b9-4d4a-a296-3cfbdf92f7e2") shouldBe inputJson.as[CrystallisationRequest]
      }

      testMandatoryProperty[CrystallisationRequest](inputJson)("/calculationId")

      testPropertyType[CrystallisationRequest](inputJson)(
        path = "/calculationId",
        replacement = 12344.toJson,
        expectedError = JsonError.STRING_FORMAT_EXCEPTION
      )
    }
  }
}
