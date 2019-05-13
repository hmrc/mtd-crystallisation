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

package v2.controllers.requestParsers.validators

import support.UnitSpec
import v2.models.errors._
import v2.models.requestData.GetObligationsRawData

class GetObligationsValidatorSpec extends UnitSpec {
  val validator = new GetObligationsValidator()

  private val validNino     = "AA123456A"
  private val validFromDate = "2018-04-06"
  private val validToDate   = "2019-04-05"

  "running a validator" should {
    "return FORMAT_NINO" when {
      "invalid nino" in {
        validator.validate(GetObligationsRawData("A12344A", validFromDate, validToDate)) shouldBe
          List(NinoFormatError)
      }
    }

    "return FORMAT_FROM_DATE" when {
      "invalid from date invalid" in {
        validator.validate(GetObligationsRawData(validNino, "xxxx", validToDate)) shouldBe
          List(InvalidFromDateError)
      }
    }

    "return FORMAT_TO_DATE" when {
      "invalid to date invalid" in {
        validator.validate(GetObligationsRawData(validNino, validFromDate, "xxxx")) shouldBe
          List(InvalidToDateError)
      }
    }

    "return MISSING_FROM_DATE" when {
      "from date missing" in {
        validator.validate(GetObligationsRawData(validNino, "", "2019-01-01")) shouldBe
          List(MissingFromDateError)
      }
    }

    "return MISSING_TO_DATE" when {
      "to date missing" in {
        validator.validate(GetObligationsRawData(validNino, "2019-01-01", "")) shouldBe
          List(MissingToDateError)
      }
    }

    "return RANGE_TO_DATE_BEFORE_FROM_DATE" when {
      "to is before from" in {
        validator.validate(GetObligationsRawData(validNino, "2019-01-01", "2018-01-01")) shouldBe
          List(RangeEndDateBeforeStartDateError)
      }
    }

    "return RANGE_DATE_TOO_LONG" when {
      "to date range is more than 366 days" in {
        validator.validate(GetObligationsRawData(validNino, "2019-01-01", "2020-01-03")) shouldBe
          List(RangeDateToLongError)
      }
    }

    "return RULE_FROM_DATE_NOT_SUPPORTED" when {
      "from date is too early" in {
        validator.validate(GetObligationsRawData(validNino, "2016-01-01", "2016-04-01")) shouldBe
          List(RuleFromDateNotSupported)
      }
    }

    "return multiple errors" when {
      "multiple errors are detected" in {
        validator.validate(GetObligationsRawData(validNino, "xxxx", "xxxx")) shouldBe
          List(InvalidFromDateError, InvalidToDateError)
      }
    }
  }
}
