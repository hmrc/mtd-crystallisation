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

package v2.models.errors

// Nino Errors
object NinoFormatError extends Error("FORMAT_NINO", "The provided NINO is invalid")

// MTD Errors
object TaxYearFormatError extends Error("FORMAT_TAX_YEAR", "The provided tax year is invalid")

object InvalidCalcIdError extends Error("FORMAT_CALCID", "The provided calculationId is invalid")

object InvalidFromDateError extends Error("FORMAT_FROM_DATE", "The provided from date is invalid")

object InvalidToDateError extends Error("FORMAT_TO_DATE", "The provided to date is invalid")

object MissingFromDateError extends Error("MISSING_FROM_DATE", "The from date parameter is missing")

object MissingToDateError extends Error("MISSING_TO_DATE", "The to date parameter is missing")

object RangeEndDateBeforeStartDateError extends Error("RANGE_TO_DATE_BEFORE_FROM_DATE", "The To date cannot be before the From date")

object RangeDateTooLongError extends Error("RANGE_DATE_TOO_LONG", "The specified date range is too big")

object RuleFromDateNotSupported extends Error("RULE_FROM_DATE_NOT_SUPPORTED", "The specified from date is not supported as too early")

// Rule Errors
object RuleTaxYearNotSupportedError
    extends Error("RULE_TAX_YEAR_NOT_SUPPORTED", "Tax year not supported, because it precedes the earliest allowable tax year")

object RuleIncorrectOrEmptyBodyError extends Error("RULE_INCORRECT_OR_EMPTY_BODY_SUBMITTED", "An empty or non-matching body was submitted")

object RuleTaxYearRangeExceededError
    extends Error("RULE_TAX_YEAR_RANGE_EXCEEDED", "Tax year range exceeded. A tax year range of one year is required.")

object IncomeSourcesChangedError extends Error("RULE_INCOME_SOURCES_CHANGED", "Income sources data has changed. Perform intent to crystallise")

object RecentSubmissionsExistError extends Error("RULE_RECENT_SUBMISSIONS_EXIST", "More recent submissions exist. Perform intent to crystallise")

object ResidencyChangedError extends Error("RULE_RESIDENCY_CHANGED", "Residency has changed. Perform intent to crystallise")

object FinalDeclarationReceivedError extends Error("RULE_FINAL_DECLARATION_RECEIVED", "Crystallisation declaration has already been received")

object NoSubmissionsExistError extends Error("RULE_NO_SUBMISSIONS_EXIST", "No income submissions exist")

//Standard Errors
object NotFoundError extends Error("MATCHING_RESOURCE_NOT_FOUND", "Matching resource not found")

object DownstreamError extends Error("INTERNAL_SERVER_ERROR", "An internal server error occurred")

object BadRequestError extends Error("INVALID_REQUEST", "Invalid request")

object ServiceUnavailableError extends Error("SERVICE_UNAVAILABLE", "Internal server error")

//Authorisation Errors
object UnauthorisedError extends Error("CLIENT_OR_AGENT_NOT_AUTHORISED", "The client and/or agent is not authorised.")
