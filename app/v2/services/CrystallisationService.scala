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
import v2.connectors.DesConnector
import v2.models.errors._
import v2.models.requestData.CrystallisationRequestData

import scala.concurrent.{ExecutionContext, Future}

class CrystallisationService @Inject()(connector: DesConnector) extends DesServiceSupport {

  /**
    * Service name for logging
    */
  override val serviceName: String = this.getClass.getSimpleName

  def createCrystallisation(request: CrystallisationRequestData)
                           (implicit hc: HeaderCarrier,
                           ec: ExecutionContext): Future[CrystallisationOutcome] = {
    connector.createCrystallisation(request).map {
      mapToVendorDirect("createCrystallisation", desErrorToMtdErrorCreate)
    }
  }

  private def desErrorToMtdErrorCreate: Map[String, Error] = Map(
    "INVALID_IDTYPE" -> DownstreamError,
    "INVALID_IDVALUE" -> NinoFormatError,
    "INVALID_TAXYEAR" -> InvalidTaxYearError,
    "INVALID_CALCID" -> InvalidCalcIdError,
    "NOT_FOUND" -> NotFoundError,
    "INCOME_SOURCES_CHANGED" -> IncomeSourcesChangedError,
    "RECENT_SUBMISSIONS_EXIST" -> RecentSubmissionsExistError,
    "RESIDENCY_CHANGED" -> ResidencyChangedError,
    "FINAL_DECLARATION_RECEIVED" -> FinalDeclarationReceivedError,
    "SERVER_ERROR" -> DownstreamError,
    "SERVICE_UNAVAILABLE" -> DownstreamError
  ).withDefault { error =>
    logger.info(s"[SavingsAccountsService] [create] - No mapping found for error code $error")
    DownstreamError
  }
}
