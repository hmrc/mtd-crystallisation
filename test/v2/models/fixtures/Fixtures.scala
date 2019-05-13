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

object Fixtures {

  object CrystallisationObligationFixture {

    val start: LocalDate = LocalDate.parse("2018-02-01")
    val end: LocalDate = LocalDate.parse("2018-02-28")
    val due: LocalDate = LocalDate.parse("2018-05-28")
    val statusFulfilled: ObligationStatus = FulfilledObligation
    val statusOpen: ObligationStatus = OpenObligation
    val processed = Some(LocalDate.parse("2018-04-01"))

    val fulfilledCrystallisationObligationsResponseMtd = CrystallisationObligationsResponse(
      Seq(Obligations(
        Seq(Obligation(
          status = statusFulfilled,
          startDate = start,
          endDate = end,
          dueDate = due,
          processedDate = processed
        ))
      ))
    )

    val openCrystallisationObligationsResponseMtd = CrystallisationObligationsResponse(
      Seq(Obligations(
        Seq(Obligation(
          status = statusOpen,
          startDate = start,
          endDate = end,
          dueDate = due,
          processedDate = None
        ))
      ))
    )

    val fulfilledObligationsMtd = Obligations(
        Seq(Obligation(
          status = statusFulfilled,
          startDate = start,
          endDate = end,
          dueDate = due,
          processedDate = processed
        ))

    )

    val openObligationsMtd = Obligations(
        Seq(Obligation(
          status = statusOpen,
          startDate = start,
          endDate = end,
          dueDate = due,
          processedDate = None
        ))
    )

    val fulfilledObligationMtd = Obligation(
          status = statusFulfilled,
          startDate = start,
          endDate = end,
          dueDate = due,
          processedDate = processed
    )

    val openObligationMtd = Obligation(
          status = statusOpen,
          startDate = start,
          endDate = end,
          dueDate = due,
          processedDate = None
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

    /*

        val fulfilledObligationJsonMtd =
          """
            |{
            |  "status": "Fulfilled",
            |  "start": "2018-02-01",
            |  "end": "2018-02-28",
            |  "processed": "2018-04-01",
            |  "due": "2018-05-28"
            |}
          """.stripMargin

        val openObligationJsonMtd =
          """
            |{
            |  "status": "Open",
            |  "start": "2018-02-01",
            |  "end": "2018-02-28",
            |  "due": "2018-05-28"
            |}
          """.stripMargin


        val fulfilledJsonObligationMissingProcessedDate = Json.parse(
          """
            |{
            |  "status": "F",
            |  "inboundCorrespondenceFromDate": "2018-01-01",
            |  "inboundCorrespondenceToDate": "2018-01-01",
            |  "inboundCorrespondenceDueDate": "2018-01-01",
            |  "periodKey": ""
            |}
          """.stripMargin
        )

        val openJsonObligationWithProcessedDate = Json.parse(
          """
            |{
            |  "status": "O",
            |  "inboundCorrespondenceFromDate": "2018-01-01",
            |  "inboundCorrespondenceToDate": "2018-01-01",
            |  "inboundCorrespondenceDueDate": "2018-01-01",
            |  "inboundCorrespondenceDateReceived": "2018-01-01",
            |  "periodKey": ""
            |}
          """.stripMargin
        )
    */

    val fulfilledCrystallisationObligationDes = Json.parse(
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

    val fulfilledObligationsDes = Json.parse(
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
  }
}
