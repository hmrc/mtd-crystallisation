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

package v2.controllers.requestParsers.validators.validations

import support.UnitSpec
import v2.models.errors._

class DateFormatValidationSpec extends UnitSpec {

  object SomeError extends Error("ERROR_CODE", "error message")

  "validate" should {
    "return no errors" when {

      "a date with a valid format is supplied" in {
        DateFormatValidation.validate("2018-01-01", SomeError) shouldBe Nil
      }
    }

    "return the specific error supplied " when {

      "the format of the date is not correct" in {
        DateFormatValidation.validate("123ABC2018-01-01", SomeError) shouldBe Seq(SomeError)
      }

      "the date is not a valid date (although the numeric format is correct)" in {
        DateFormatValidation.validate("1234-56-78", SomeError) shouldBe Seq(SomeError)
      }
    }
  }
}
