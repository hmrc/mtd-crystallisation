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

package v2.models.fixtures
import java.time.LocalDate

import play.api.libs.json.Json
import v2.models.des._
import v2.models.domain.Obligation

object Fixtures {

  object CrystallisationObligationFixture {

    val start: LocalDate = LocalDate.parse("2018-02-01")
    val end: LocalDate = LocalDate.parse("2018-02-28")
    val due: LocalDate = LocalDate.parse("2018-05-28")
    val statusFulfilled: ObligationStatus = FulfilledObligation
    val statusOpen: ObligationStatus = OpenObligation
    val processed = LocalDate.parse("2018-04-01")

    val fulfilledCrystallisationObligationsResponseDes = DesObligationsResponse(
      Seq(
        DesObligation(
        Seq(
          DesObligationDetail(
          status = statusFulfilled,
          inboundCorrespondenceFromDate = start,
          inboundCorrespondenceToDate = end,
          inboundCorrespondenceDueDate = due,
          inboundCorrespondenceDateReceived = Some(processed)
        ))
      ))
    )

    val openCrystallisationObligationsResponseDes = DesObligationsResponse(
      Seq(
        DesObligation(
        Seq(
          DesObligationDetail(
          status = statusOpen,
          inboundCorrespondenceFromDate = start,
          inboundCorrespondenceToDate = end,
          inboundCorrespondenceDueDate = due,
          inboundCorrespondenceDateReceived = None
        ))
      ))
    )

    val fulfilledObligationsDes = DesObligation(
      Seq(
        DesObligationDetail(
          status = statusFulfilled,
          inboundCorrespondenceFromDate = start,
          inboundCorrespondenceToDate = end,
          inboundCorrespondenceDueDate = due,
          inboundCorrespondenceDateReceived = Some(processed)
        ))

    )

    val openObligationsDes = DesObligation(
      Seq(
        DesObligationDetail(
          status = statusOpen,
          inboundCorrespondenceFromDate = start,
          inboundCorrespondenceToDate = end,
          inboundCorrespondenceDueDate = due,
          inboundCorrespondenceDateReceived = None
        ))
    )

    val fulfilledObligationDes = DesObligationDetail(
          status = statusFulfilled,
          inboundCorrespondenceFromDate = start,
          inboundCorrespondenceToDate = end,
          inboundCorrespondenceDueDate = due,
          inboundCorrespondenceDateReceived = Some(processed)
    )

    val openObligationDes = DesObligationDetail(
          status = statusOpen,
          inboundCorrespondenceFromDate = start,
          inboundCorrespondenceToDate = end,
          inboundCorrespondenceDueDate = due,
          inboundCorrespondenceDateReceived = None
    )

    val fulfilledObligationMtd = Obligation(
      status = statusFulfilled,
      start = start,
      end = end,
      due = due,
      processed = Some(processed)
    )

    val openObligationMtd = Obligation(
      status = statusOpen,
      start = start,
      end = end,
      due = due,
      processed = None
    )



    val fulfilledJsonObligationDes = Json.parse(
      """
        |{
        |   "status": "F",
        |   "inboundCorrespondenceFromDate": "2018-02-01",
        |   "inboundCorrespondenceToDate": "2018-02-28",
        |   "inboundCorrespondenceDateReceived": "2018-04-01",
        |   "inboundCorrespondenceDueDate": "2018-05-28"
        |}
      """.stripMargin)

    val openJsonObligationDes = Json.parse(
      """
        |{
        |   "status": "O",
        |   "inboundCorrespondenceFromDate": "2018-02-01",
        |   "inboundCorrespondenceToDate": "2018-02-28",
        |   "inboundCorrespondenceDueDate": "2018-05-28"
        |}
      """.stripMargin)

    val openCrystallisationObligationJsonDes = Json.parse(
      """
        |{
        |  "obligations": [
        |    {
        |    "obligationDetails": [
        |      {
        |        "status": "O",
        |        "inboundCorrespondenceFromDate": "2018-02-01",
        |        "inboundCorrespondenceToDate": "2018-02-28",
        |        "inboundCorrespondenceDateReceived": "2018-04-01",
        |        "inboundCorrespondenceDueDate": "2018-05-28"
        |      }
        |    ]
        |    }
        |  ]
        |}
      """.stripMargin)

    val fulfilledCrystallisationObligationJsonDes = Json.parse(
      """
        |{
        |  "obligations": [
        |    {
        |    "obligationDetails": [
        |      {
        |        "status": "F",
        |        "inboundCorrespondenceFromDate": "2018-02-01",
        |        "inboundCorrespondenceToDate": "2018-02-28",
        |        "inboundCorrespondenceDateReceived": "2018-04-01",
        |        "inboundCorrespondenceDueDate": "2018-05-28"
        |      }
        |    ]
        |    }
        |  ]
        |}
      """.stripMargin)

    val fulfilledObligationsJsonDes = Json.parse(
      """
        |{
        |  "obligationDetails": [
        |    {
        |      "status": "F",
        |      "inboundCorrespondenceFromDate": "2018-02-01",
        |      "inboundCorrespondenceToDate": "2018-02-28",
        |      "inboundCorrespondenceDateReceived": "2018-04-01",
        |      "inboundCorrespondenceDueDate": "2018-05-28"
        |    }
        |  ]
        |}
      """.stripMargin)

    val fulfilledObligationJsonMtd =Json.parse(
      """
        |{
        |  "status": "Fulfilled",
        |  "start": "2018-02-01",
        |  "end": "2018-02-28",
        |  "processed": "2018-04-01",
        |  "due": "2018-05-28"
        |}
      """.stripMargin)

    val openObligationJsonMtd =Json.parse(
      """
        |{
        |  "status": "Open",
        |  "start": "2018-02-01",
        |  "end": "2018-02-28",
        |  "due": "2018-05-28"
        |}
      """.stripMargin)

  }
}
