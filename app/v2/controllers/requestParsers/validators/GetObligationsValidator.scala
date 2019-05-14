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

import java.time.LocalDate

import v2.controllers.requestParsers.validators.validations.{
  DateFormatValidation,
  DateRangeValidation,
  MtdDateValidation,
  NinoValidation,
  PredicateValidation
}
import v2.models.errors._
import v2.models.requestData.GetObligationsRawData

class GetObligationsValidator extends Validator[GetObligationsRawData] {

  private val validations = List(parameterPresenceValidation, parameterFormatValidation, parameterRuleValidation)

  private def parameterPresenceValidation: GetObligationsRawData => List[List[Error]] = (data: GetObligationsRawData) => {
    List(
      PredicateValidation.validate(data.from.isEmpty, MissingFromDateError),
      PredicateValidation.validate(data.to.isEmpty, MissingToDateError)
    )
  }

  private def parameterFormatValidation: GetObligationsRawData => List[List[Error]] = (data: GetObligationsRawData) => {
    List(
      NinoValidation.validate(data.nino),
      DateFormatValidation.validate(data.from, InvalidFromDateError),
      DateFormatValidation.validate(data.to, InvalidToDateError)
    )
  }

  private def parameterRuleValidation: GetObligationsRawData => List[List[Error]] = (data: GetObligationsRawData) => {

    val from = LocalDate.parse(data.from)
    val to   = LocalDate.parse(data.to)

    List(
      DateRangeValidation.validate(from, to),
      MtdDateValidation.validate(from, RuleFromDateNotSupported)
    )
  }

  def validate(data: GetObligationsRawData): List[Error] =
    run(validations, data).distinct
}
