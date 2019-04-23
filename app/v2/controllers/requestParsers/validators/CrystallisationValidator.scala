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

import v2.controllers.requestParsers.validators.validations._
import v2.models.domain.CrystallisationRequest
import v2.models.errors.{ Error, RuleTaxYearNotSupportedError }
import v2.models.requestData.CrystallisationRawData

class CrystallisationValidator extends Validator[CrystallisationRawData] {

  private val validationSet = List(parameterFormatValidation, bodyFormatValidator, parameterRuleValidation, bodyFieldsValidation)

  private def parameterFormatValidation: CrystallisationRawData => List[List[Error]] = (data: CrystallisationRawData) => {
    List(
      NinoValidation.validate(data.nino),
      TaxYearValidation.validate(data.taxYear)
    )
  }

  private def parameterRuleValidation: CrystallisationRawData => List[List[Error]] = { data =>
    List(
      MtdTaxYearValidation.validate(data.taxYear, RuleTaxYearNotSupportedError)
    )
  }

  private def bodyFormatValidator: CrystallisationRawData => List[List[Error]] = { data =>
    List(
      JsonFormatValidation.validate[CrystallisationRequest](data.body)
    )
  }

  private def bodyFieldsValidation: CrystallisationRawData => List[List[Error]] = (data: CrystallisationRawData) => {
    val req = data.body.json.as[CrystallisationRequest]

    List(
      CalculationIdValidation.validate(req.calculationId)
    )
  }

  override def validate(data: CrystallisationRawData): List[Error] = {
    run(validationSet, data).distinct
  }
}
