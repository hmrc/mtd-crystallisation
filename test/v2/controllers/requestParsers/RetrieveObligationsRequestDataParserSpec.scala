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

import java.time.LocalDate

import support.UnitSpec
import uk.gov.hmrc.domain.Nino
import v2.mocks.validators.MockRetrieveObligationsValidator
import v2.models.errors.{BadRequestError, ErrorWrapper, InvalidToDateError, NinoFormatError}
import v2.models.requestData.{RetrieveObligationsRawData, RetrieveObligationsRequestData}

class RetrieveObligationsRequestDataParserSpec extends UnitSpec {
  val nino = "AA123456B"
  val from = "2018-04-06"
  val to   = "2019-04-05"
  implicit val correlationId: String = "a1e8057e-fbbc-47a8-a8b4-78d9f015c253"

  val inputData =
    RetrieveObligationsRawData(nino, from, to)

  trait Test extends MockRetrieveObligationsValidator {
    lazy val parser = new RetrieveObligationsRequestDataParser(mockValidator)
  }

  "parse" should {

    "return a retrieve intent to crystallise request object" when {
      "valid request data is supplied" in new Test {
        MockGetObligationsValidator.validate(inputData).returns(Nil)

        parser.parseRequest(inputData) shouldBe
          Right(RetrieveObligationsRequestData(Nino(nino), LocalDate.parse(from), LocalDate.parse(to)))
      }
    }

    "return an ErrorWrapper" when {

      "a single validation error occurs" in new Test {
        MockGetObligationsValidator
          .validate(inputData)
          .returns(List(NinoFormatError))

        parser.parseRequest(inputData) shouldBe
          Left(ErrorWrapper(correlationId, NinoFormatError, None))
      }

      "multiple validation errors occur" in new Test {
        val errors = List(NinoFormatError, InvalidToDateError)

        MockGetObligationsValidator
          .validate(inputData)
          .returns(errors)

        parser.parseRequest(inputData) shouldBe
          Left(ErrorWrapper(correlationId, BadRequestError, Some(errors)))
      }
    }
  }
}
