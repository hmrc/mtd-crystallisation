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

import play.api.libs.functional.syntax._
import play.api.libs.json.{Json, Reads, __}

case class CrystallisationObligationsResponse(obligations: Seq[Obligations])

object CrystallisationObligationsResponse {
  implicit val reads: Reads[CrystallisationObligationsResponse] = Json.reads[CrystallisationObligationsResponse]
  }

case class Obligations(obligationDetails: Seq[Obligation])

object Obligations {
  implicit val reads: Reads[Obligations] = Json.reads[Obligations]
}

case class Obligation(startDate: LocalDate,
                       endDate: LocalDate,
                       dueDate: LocalDate,
                       status: ObligationStatus,
                       processedDate: Option[LocalDate]
                      )

object Obligation {
  implicit val reads: Reads[Obligation] = (
      (__ \ "inboundCorrespondenceFromDate").read[LocalDate] and
        (__ \ "inboundCorrespondenceToDate").read[LocalDate] and
        (__ \ "inboundCorrespondenceDueDate").read[LocalDate] and
        (__ \ "status").read[ObligationStatus] and
        (__ \ "inboundCorrespondenceDateReceived").readNullable[LocalDate]
    )(Obligation.apply _)
  }
