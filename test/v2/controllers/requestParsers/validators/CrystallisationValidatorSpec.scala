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

package v2.controllers.requestParsers.validators

import play.api.libs.json.Json
import play.api.mvc.AnyContentAsJson
import support.UnitSpec
import v2.models.errors._
import v2.models.requestData.CrystallisationRawData

class CrystallisationValidatorSpec extends UnitSpec {

  private val validNino = "AA123456A"
  private val validTaxYear = "2018-19"

  private val validJson =
    """{
      |  "calculationId": "00000000-0000-1000-8000-000000000000"
      |}
    """.stripMargin

  private def body(json: String) = AnyContentAsJson(Json.parse(json))

  val validator = new CrystallisationValidator()

  "running a validation" should {
    "return no errors" when {
      "a valid request is supplied" in {
        validator.validate(CrystallisationRawData(validNino, validTaxYear, body(validJson))) shouldBe Nil
      }
    }

    "return NinoFormatError error" when {
      "an invalid nino is supplied" in {
        validator.validate(CrystallisationRawData("badNino", validTaxYear, body(validJson))) shouldBe
          List(NinoFormatError)
      }
    }

    "return TaxYearFormatError error" when {
      "an invalid tax year is supplied" in {
        validator.validate(CrystallisationRawData(validNino, "badTaxYear", body(validJson))) shouldBe
          List(TaxYearFormatError)
      }
    }

    "return RuleTaxYearNotSupportedError error" when {
      "an out of range tax year is supplied" in {
                        validator.validate(
                          CrystallisationRawData(validNino, "2016-17",  body(validJson))) shouldBe
                            List(RuleTaxYearNotSupportedError)
      }
    }

    "return InvalidCalcIdError error" when {
      "an invalid calculation id is supplied" in {
        val json =
          """
            |{
            |  "calculationId": "xxxxxxxx"
            |}
          """.stripMargin

        validator.validate(CrystallisationRawData(validNino, validTaxYear, body(json))) shouldBe
          List(InvalidCalcIdError)
      }
    }

    "return RuleIncorrectOrEmptyBodyError error" when {
      "an empty body is supplied" in {
        val json = "{}"

        validator.validate(
          CrystallisationRawData(validNino, validTaxYear, body(json))
        ) shouldBe List(RuleIncorrectOrEmptyBodyError)
      }

      "an the body does not contain a calculationId" in {
        val json = """{"someField": 1234}"""

        validator.validate(
          CrystallisationRawData(validNino, validTaxYear, body(json))
        ) shouldBe List(RuleIncorrectOrEmptyBodyError)
      }
    }

    "return multiple errors" when {
      "request supplied has multiple errors" in {
        validator.validate(CrystallisationRawData("badNino", "badTaxYear", body(validJson))) shouldBe
          List(NinoFormatError, TaxYearFormatError)
      }
    }
  }
}
