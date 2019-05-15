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

package v2.services
import javax.inject.Inject
import uk.gov.hmrc.http.HeaderCarrier
import v2.connectors.httpparsers.StandardDesHttpParser._
import v2.connectors.{ DesConnector, DesConnectorOutcome }
import v2.models.des.DesCalculationIdResponse
import v2.models.domain.EmptyJsonBody
import v2.models.requestData.{ CrystallisationRequestData, IntentToCrystalliseRequestData }

import scala.concurrent.{ ExecutionContext, Future }

class CrystallisationService @Inject()(connector: DesConnector) extends DesServiceSupport {

  /**
    * Service name for logging
    */
  override val serviceName: String = this.getClass.getSimpleName

  def performIntentToCrystallise(request: IntentToCrystalliseRequestData)(
      implicit hc: HeaderCarrier,
      ec: ExecutionContext): Future[DesConnectorOutcome[DesCalculationIdResponse]] = {

    connector.post(
      body = EmptyJsonBody,
      DesUri[DesCalculationIdResponse](s"income-tax/nino/${request.nino}/taxYear/${request.desTaxYear}/tax-calculation?crystallise=true")
    )
  }

  def createCrystallisation(request: CrystallisationRequestData)(implicit hc: HeaderCarrier,
                                                                 ec: ExecutionContext): Future[DesConnectorOutcome[Unit]] = {

    connector.post(
      body = EmptyJsonBody,
      DesUri[Unit](s"income-tax/calculation/nino/${request.nino}/${request.desTaxYear}/${request.crystallisation.calculationId}/crystallise")
    )
  }
}
