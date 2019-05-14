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

import java.time.LocalDate

import support.UnitSpec
import v2.config.FixedConfig
import v2.models.errors.{ RangeDateTooLongError, RangeEndDateBeforeStartDateError }

class DateRangeValidationSpec extends UnitSpec with FixedConfig {

  "validate" should {
    "return no errors" when {
      "when a from date that is before the to date is supplied " in {
        val fromDate = LocalDate.parse("2018-02-01")
        val toDate   = LocalDate.parse("2019-02-01")

        DateRangeValidation.validate(fromDate, toDate) shouldBe Nil
      }
    }

    "return an error" when {
      "when the from date is after the to date" in {
        val fromDate = LocalDate.parse("2019-02-01")
        val toDate   = LocalDate.parse("2018-02-01")

        DateRangeValidation.validate(fromDate, toDate) shouldBe Seq(RangeEndDateBeforeStartDateError)
      }
    }

    "return no error" when {
      "when the from date is the same as the to date" in {
        val date = LocalDate.parse("2019-02-01")

        DateRangeValidation.validate(date, date) shouldBe Nil
      }
    }

    "return an error" when {
      "when the date range is more than 366 days" in {
        val fromDate = LocalDate.parse("2020-01-01")
        val toDate   = LocalDate.parse("2021-01-02")

        DateRangeValidation.validate(fromDate, toDate) shouldBe Seq(RangeDateTooLongError)
      }
    }

    "return no error" when {
      "when the date range is 366 days" in {
        val fromDate = LocalDate.parse("2020-01-01")
        val toDate   = LocalDate.parse("2021-01-01")

        DateRangeValidation.validate(fromDate, toDate) shouldBe Nil
      }
    }
  }

}
