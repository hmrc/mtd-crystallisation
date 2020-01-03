/*
 * Copyright 2020 HM Revenue & Customs
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

import java.time.LocalDate

import support.UnitSpec
import v2.models.errors.Error
import v2.models.utils.JsonErrorValidators

class MtdDateValidationSpec extends UnitSpec with JsonErrorValidators {

  "validate" should {

    object SomeError extends Error("ERROR_CODE", "error message")

    "return no errors" when {
      "a date year after the minimum is supplied" in {
        MtdDateValidation.validate(LocalDate.parse("2018-04-06"), SomeError) shouldBe Nil
      }

      "the minimum allowed date is supplied" in {
        MtdDateValidation.validate(LocalDate.parse("2018-04-06"), SomeError) shouldBe Nil
      }
    }

    "return the given error" when {
      "a date before the minimuim is supplied" in {
        MtdDateValidation.validate(LocalDate.parse("2018-04-05"), SomeError) shouldBe Seq(SomeError)
      }
    }
  }
}
