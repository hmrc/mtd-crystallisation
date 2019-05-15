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

package v2.connectors

import javax.inject.{Inject, Singleton}
import play.api.Logger
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.logging.Authorization
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import v2.config.AppConfig
import v2.connectors.httpparsers.StandardDesHttpParser
import v2.models.des.{DesCalculationIdResponse, DesObligationsResponse}
import v2.models.domain.EmptyJsonBody
import v2.models.requestData.{ RetrieveObligationsRequestData, CrystallisationRequestData, IntentToCrystalliseRequestData}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DesConnector @Inject()(http: HttpClient, appConfig: AppConfig) {

  val logger = Logger(this.getClass)

  private[connectors] def desHeaderCarrier(implicit hc: HeaderCarrier): HeaderCarrier =
    hc.copy(authorization = Some(Authorization(s"Bearer ${appConfig.desToken}")))
      .withExtraHeaders("Environment" -> appConfig.desEnv)

  def performIntentToCrystallise(requestData: IntentToCrystalliseRequestData)(implicit hc: HeaderCarrier,
                                                                              ec: ExecutionContext): Future[IntentToCrystalliseConnectorOutcome] = {

    val nino    = requestData.nino.nino
    val taxYear = requestData.desTaxYear

    val url = s"${appConfig.desBaseUrl}/income-tax/nino/$nino/taxYear/$taxYear/tax-calculation?crystallise=true"

    http.POST(url, EmptyJsonBody)(EmptyJsonBody.writes, StandardDesHttpParser.reads[DesCalculationIdResponse], desHeaderCarrier, implicitly)
  }

  def createCrystallisation(crystallisationRequestData: CrystallisationRequestData)(
      implicit hc: HeaderCarrier,
      ec: ExecutionContext): Future[CreateCrystallisationConnectorOutcome] = {

    val nino    = crystallisationRequestData.nino.nino
    val taxYear = crystallisationRequestData.desTaxYear
    val calcId  = crystallisationRequestData.crystallisation.calculationId

    val url = s"${appConfig.desBaseUrl}/income-tax/calculation/nino/$nino/$taxYear/$calcId/crystallise"

    http.POST(url, EmptyJsonBody)(EmptyJsonBody.writes, StandardDesHttpParser.readsEmpty, desHeaderCarrier, implicitly)
  }

  def retrieveObligations(request: RetrieveObligationsRequestData)(
    implicit hc: HeaderCarrier,
    ec: ExecutionContext): Future[RetrieveCrystallisationConnectorOutcome] = {

    val nino = request.nino.nino
    val from = request.from
    val to   = request.to
    val url = s"${appConfig.desBaseUrl}/enterprise/obligation-data/nino/$nino/ITSA?from=$from&to=$to"

    http.POST(url, EmptyJsonBody)(EmptyJsonBody.writes, StandardDesHttpParser.reads[DesObligationsResponse], desHeaderCarrier, implicitly)
  }
}
