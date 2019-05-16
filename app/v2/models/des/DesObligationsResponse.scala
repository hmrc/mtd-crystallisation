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

package v2.models.des
import java.time.LocalDate

import play.api.libs.json.{Json, Reads}
import v2.models.domain.Obligation

case class DesObligationsResponse(obligations: Seq[DesObligation]) {

  def toMtd: Seq[Obligation] =
      for {
        obligation       <- obligations
        obligationDetail <- obligation.obligationDetails
      } yield
    Obligation(
      start = obligationDetail.inboundCorrespondenceFromDate,
      end = obligationDetail.inboundCorrespondenceToDate,
      due = obligationDetail.inboundCorrespondenceDueDate,
      status = ObligationStatus(obligationDetail.status),
      processed = obligationDetail.inboundCorrespondenceDateReceived
    )
}

object DesObligationsResponse {
  implicit val reads: Reads[DesObligationsResponse] = Json.reads[DesObligationsResponse]
}

case class DesObligation(identification: Identification, obligationDetails: Seq[DesObligationDetail])

object DesObligation {
  implicit val reads: Reads[DesObligation] = Json.reads[DesObligation]
}

case class DesObligationDetail(inboundCorrespondenceFromDate: LocalDate,
                               inboundCorrespondenceToDate: LocalDate,
                               inboundCorrespondenceDueDate: LocalDate,
                               status: String,
                               inboundCorrespondenceDateReceived: Option[LocalDate]
                              )
object DesObligationDetail {
  implicit val reads: Reads[DesObligationDetail] = Json.reads[DesObligationDetail]
}

case class Identification(incomeSourceType: String,
                          referenceNumber: String,
                          referenceType: String
                         )
object Identification {
    implicit val reads: Reads[Identification] = Json.reads[Identification]
}
