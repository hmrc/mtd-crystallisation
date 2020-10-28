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

package v2.services

import javax.inject.Inject
import uk.gov.hmrc.http.HeaderCarrier
import v2.connectors.DesConnector
import v2.models.errors._
import v2.models.outcomes.DesResponse
import v2.models.requestData.{ RetrieveObligationsRequestData, CrystallisationRequestData, IntentToCrystalliseRequestData}

import scala.concurrent.{ExecutionContext, Future}

class CrystallisationService @Inject()(connector: DesConnector) extends DesServiceSupport {

  /**
    * Service name for logging
    */
  override val serviceName: String = this.getClass.getSimpleName

  def performIntentToCrystallise(request: IntentToCrystalliseRequestData)(implicit hc: HeaderCarrier,
                                                                          ec: ExecutionContext,
                                                                          correlationId: String): Future[IntentToCrystalliseOutcome] = {
    connector.performIntentToCrystallise(request).map {
      mapToVendor("intentToCrystallise", desErrorToMtdErrorIntent) { desResponse =>
        Right(DesResponse(desResponse.correlationId, desResponse.responseData.id))
      }
    }
  }

  def createCrystallisation(request: CrystallisationRequestData)(implicit hc: HeaderCarrier,
                                                                 ec: ExecutionContext,
                                                                 correlationId: String): Future[CrystallisationOutcome] = {
    connector.createCrystallisation(request).map {
      mapToVendorDirect("createCrystallisation", desErrorToMtdErrorCreate)
    }
  }

  def retrieveObligations(request: RetrieveObligationsRequestData)
                         (implicit hc: HeaderCarrier,
                          ec: ExecutionContext,
                          correlationId: String): Future[RetrieveObligationsOutcome] = {
    connector.retrieveObligations(request).map {
      mapToVendor("retrieveObligations", desErrorToMtdErrorRetrieve) { desResponse =>
        Right(DesResponse(desResponse.correlationId, desResponse.responseData.toMtd))
      }
    }
  }

  private def desErrorToMtdErrorIntent: Map[String, Error] =
    Map(
      "INVALID_NINO"               -> NinoFormatError,
      "INVALID_TAX_YEAR"           -> TaxYearFormatError,
      "INVALID_TAX_CRYSTALLISE"    -> DownstreamError,
      "INVALID_REQUEST"            -> DownstreamError,
      "NO_SUBMISSION_EXIST"        -> NoSubmissionsExistError,
      "CONFLICT"                   -> FinalDeclarationReceivedError,
      "SERVER_ERROR"               -> DownstreamError,
      "SERVICE_UNAVAILABLE"        -> DownstreamError
    )

  private def desErrorToMtdErrorCreate: Map[String, Error] =
    Map(
      "INVALID_IDTYPE"             -> DownstreamError,
      "INVALID_IDVALUE"            -> NinoFormatError,
      "INVALID_TAXYEAR"            -> TaxYearFormatError,
      "INVALID_CALCID"             -> InvalidCalcIdError,
      "NOT_FOUND"                  -> NotFoundError,
      "INCOME_SOURCES_CHANGED"     -> IncomeSourcesChangedError,
      "RECENT_SUBMISSIONS_EXIST"   -> RecentSubmissionsExistError,
      "RESIDENCY_CHANGED"          -> ResidencyChangedError,
      "FINAL_DECLARATION_RECEIVED" -> FinalDeclarationReceivedError,
      "SERVER_ERROR"               -> DownstreamError,
      "SERVICE_UNAVAILABLE"        -> DownstreamError
    )

  private def desErrorToMtdErrorRetrieve: Map[String, Error] =
    Map(
      "INVALID_IDTYPE"             -> DownstreamError,
      "INVALID_IDNUMBER"           -> NinoFormatError,
      "INVALID_STATUS"             -> DownstreamError,
      "INVALID_REGIME"             -> DownstreamError,
      "NOT_FOUND"                  -> NotFoundError,
      "INVALID_DATE_TO"            -> InvalidToDateError,
      "INVALID_DATE_FROM"          -> InvalidFromDateError,
      "NOT_FOUND_BPKEY"            -> DownstreamError,
      "INVALID_DATE_RANGE"         -> RangeDateTooLongError,
      "SERVER_ERROR"               -> DownstreamError,
      "SERVICE_UNAVAILABLE"        -> DownstreamError
    )
}
