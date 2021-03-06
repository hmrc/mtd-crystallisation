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

package v2.controllers.requestParsers.validators.validations

import java.time.LocalDate

import v2.models.errors.Error

import scala.util.Try

object DateFormatValidation extends Validation {

  private val dateRegex = "([0-9]{4}\\-[0-9]{2}\\-[0-9]{2})"

  def validate(date: String, dateError: Error): List[Error] = {
    if (date.matches(dateRegex) && Try(LocalDate.parse(date)).isSuccess) noValidationErrors else List(dateError)
  }

}
