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

package v2.controllers.requestParsers

import play.api.libs.json.Json
import play.api.mvc.AnyContentAsJson
import support.UnitSpec
import uk.gov.hmrc.domain.Nino
import v2.mocks.validators.MockCrystallisationValidator
import v2.models.domain.CrystallisationRequest
import v2.models.errors._
import v2.models.requestData.{CrystallisationRawData, CrystallisationRequestData, DesTaxYear}

class CrystallisationRequestDataParserSpec extends UnitSpec {
  val nino = "AA123456B"
  val taxYear = "2017-18"
  val calcId = "someCalcId"

  val json =
    s"""
      |{
      |    "calculationId": "$calcId"
      |}
    """.stripMargin
  val validJsonBody = AnyContentAsJson(Json.parse(json))

  val inputData =
    CrystallisationRawData(nino, taxYear, validJsonBody)

  trait Test extends MockCrystallisationValidator {
    lazy val parser = new CrystallisationRequestDataParser(mockValidator)
  }

  "parse" should {

    "return a retrieve crystallisation request object" when {
      "valid request data is supplied" in new Test {
        MockCrystallisationValidator.validate(inputData).returns(Nil)

        parser.parseRequest(inputData) shouldBe
          Right(CrystallisationRequestData(Nino(nino), DesTaxYear("2018"), CrystallisationRequest(calcId)))
      }
    }

    "return an ErrorWrapper" when {

      "a single validation error occurs" in new Test {
        MockCrystallisationValidator.validate(inputData)
          .returns(List(NinoFormatError))

        parser.parseRequest(inputData) shouldBe
          Left(ErrorWrapper(None, NinoFormatError, None))
      }

      "multiple validation errors occur" in new Test {
        MockCrystallisationValidator.validate(inputData)
          .returns(List(NinoFormatError, InvalidCalcIdError))

        parser.parseRequest(inputData) shouldBe
          Left(ErrorWrapper(None, BadRequestError, Some(Seq(NinoFormatError, InvalidCalcIdError))))
      }
    }
  }
}
