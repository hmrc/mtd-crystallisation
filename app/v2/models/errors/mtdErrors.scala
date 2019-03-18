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

package v2.models.errors

// Nino Errors
object NinoFormatError extends Error("FORMAT_NINO", "The NINO format is invalid")

// MTD Errors
object TaxYearFormatError extends Error("FORMAT_TAX_YEAR", "The provided tax year is invalid")
object InvalidCalcIdError extends Error("FORMAT_CALCID", "The provided calculationId is invalid")

// Rule Errors
object RuleTaxYearNotSupportedError extends Error("RULE_TAX_YEAR_NOT_SUPPORTED", "Tax year not supported, because it precedes the earliest allowable tax year")
object RuleIncorrectOrEmptyBodyError extends Error("RULE_INCORRECT_OR_EMPTY_BODY_SUBMITTED", "An empty or non-matching body was submitted")
object IncomeSourcesChangedError extends Error("RULE_INCOME_SOURCES_CHANGED", "The remote endpoint has indicated changed information for income sources.  Recalculate before crystallising")
object RecentSubmissionsExistError extends Error("RULE_RECENT_SUBMISSIONS_EXIST", "The remote endpoint has indicated more recent submissions exist.  Recalculate before crystallising")
object ResidencyChangedError extends Error("RULE_RESIDENCY_CHANGED", "The remote endpoint has indicated a change in residency.  Recalculate before crystallising")
object FinalDeclarationReceivedError extends Error("RULE_FINAL_DECLARATION_RECEIVED", "The remote endpoint has indicated that final declaration has already been received")

//Standard Errors
object NotFoundError extends Error("MATCHING_RESOURCE_NOT_FOUND", "The remote endpoint has indicated that no intent-to-crystallise calculation exists for the calculationId")
object DownstreamError extends Error("INTERNAL_SERVER_ERROR", "An internal server error occurred")
object BadRequestError extends Error("INVALID_REQUEST", "Invalid request")
object ServiceUnavailableError extends Error("SERVICE_UNAVAILABLE", "Internal server error")

//Authorisation Errors
object UnauthorisedError extends Error("CLIENT_OR_AGENT_NOT_AUTHORISED", "The client and/or agent is not authorised.")
