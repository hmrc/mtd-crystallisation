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

package v2.stubs

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status.{NO_CONTENT, OK}
import play.api.libs.json.Json
import support.WireMockMethods
import v2.models.fixtures.Fixtures

object DesStub extends WireMockMethods {

  private def crystallisationUrl(nino: String, taxYear: String, calcId: String): String =
    s"/income-tax/calculation/nino/$nino/$taxYear/$calcId/crystallise"

  def createSuccess(nino: String, taxYear: String, calcId: String): StubMapping = {
    when(method = POST, uri = crystallisationUrl(nino, taxYear, calcId))
      .thenReturn(status = NO_CONTENT)
  }

  def createError(nino: String, taxYear: String, calcId: String, errorStatus: Int, errorBody: String): StubMapping = {
    when(method = POST, uri = crystallisationUrl(nino, taxYear, calcId))
      .thenReturn(status = errorStatus, errorBody)
  }

  private val responseBody = Json.parse(
    """
      | {
      | "id" : "041f7e4d-87b9-4d4a-a296-3cfbdf92f7e2"
      | }
    """.stripMargin)

  private def intentToCrystalliseUrl(nino: String, taxYear: String): String =
    s"/income-tax/nino/$nino/taxYear/$taxYear/tax-calculation"

  def intentToSuccess(nino: String, taxYear: String): StubMapping = {
    when(method = POST, uri = intentToCrystalliseUrl(nino, taxYear), queryParams = Map("crystallise" -> "true"))
      .thenReturn(status = OK, responseBody)
  }

  def intentToError(nino: String, taxYear: String, errorStatus: Int, errorBody: String): StubMapping = {
    when(method = POST, uri = intentToCrystalliseUrl(nino, taxYear), queryParams = Map("crystallise" -> "true"))
      .thenReturn(status = errorStatus, errorBody)
  }

  private val retrieveResponseJson = Fixtures.CrystallisationObligationFixture.fulfilledCrystallisationObligationJsonDes

  private def retrieveObligationsUrl(nino: String): String =
    s"/enterprise/obligation-data/nino/$nino/ITSA"

  def retrieveObligationsSuccess(nino: String, from: String, to: String): StubMapping = {
    when(method = GET, uri = retrieveObligationsUrl(nino), queryParams = Map("from" -> from, "to" -> to))
      .thenReturn(status = OK, retrieveResponseJson)
  }

  def retrieveNonCrystallisationObligationsSuccess(nino: String, from: String, to: String): StubMapping = {
    when(method = GET, uri = retrieveObligationsUrl(nino), queryParams = Map("from" -> from, "to" -> to))
      .thenReturn(status = OK, Fixtures.CrystallisationObligationFixture.notCrystallisationObligationsJsonDes)
  }

  def retrieveObligationsError(nino: String, from: String, to: String, errorStatus: Int, errorBody: String): StubMapping = {
    when(method = GET, uri = retrieveObligationsUrl(nino), queryParams = Map("from" -> from, "to" -> to))
      .thenReturn(status = errorStatus, errorBody)
  }
}
