/*
 * Copyright 2021 HM Revenue & Customs
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

    val start: LocalDate = LocalDate.parse("2018-04-06")
    val end: LocalDate = LocalDate.parse("2019-04-05")
    val due: LocalDate = LocalDate.parse("2020-01-31")
    val statusFulfilled: ObligationStatus = FulfilledObligation
    val statusOpen: ObligationStatus = OpenObligation
    val processed = LocalDate.parse("2020-01-25")
    val incomeSourceType = "ITSA"
    val referenceNumber = "AB123456A"
    val referenceType = "NINO"

    val fulfilledCrystallisationObligationsResponseDes = DesObligationsResponse(
      Seq(
        DesObligation(
          Identification(
            incomeSourceType,
            referenceNumber,
            referenceType),
        Seq(
          DesObligationDetail(
          status = "F",
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
          Identification(
            incomeSourceType,
            referenceNumber,
            referenceType),
        Seq(
          DesObligationDetail(
          status = "O",
          inboundCorrespondenceFromDate = start,
          inboundCorrespondenceToDate = end,
          inboundCorrespondenceDueDate = due,
          inboundCorrespondenceDateReceived = None
        ))
      ))
    )

    val fulfilledObligationsDes = DesObligation(
      Identification(
        incomeSourceType,
        referenceNumber,
        referenceType),
      Seq(
        DesObligationDetail(
          status = "F",
          inboundCorrespondenceFromDate = start,
          inboundCorrespondenceToDate = end,
          inboundCorrespondenceDueDate = due,
          inboundCorrespondenceDateReceived = Some(processed)
        ))

    )

    val openObligationsDes = DesObligation(
      Identification(
        incomeSourceType,
        referenceNumber,
        referenceType),
      Seq(
        DesObligationDetail(
          status = "O",
          inboundCorrespondenceFromDate = start,
          inboundCorrespondenceToDate = end,
          inboundCorrespondenceDueDate = due,
          inboundCorrespondenceDateReceived = None
        ))
    )

    val fulfilledObligationDes = DesObligationDetail(
          status = "F",
          inboundCorrespondenceFromDate = start,
          inboundCorrespondenceToDate = end,
          inboundCorrespondenceDueDate = due,
          inboundCorrespondenceDateReceived = Some(processed)
    )

    val openObligationDes = DesObligationDetail(
          status = "O",
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
        |   "inboundCorrespondenceFromDate": "2018-04-06",
        |   "inboundCorrespondenceToDate": "2019-04-05",
        |   "inboundCorrespondenceDateReceived": "2020-01-25",
        |   "inboundCorrespondenceDueDate": "2020-01-31"
        |}
      """.stripMargin)

    val openJsonObligationDes = Json.parse(
      """
        |{
        |   "status": "O",
        |   "inboundCorrespondenceFromDate": "2018-04-06",
        |   "inboundCorrespondenceToDate": "2019-04-05",
        |   "inboundCorrespondenceDueDate": "2020-01-31"
        |}
      """.stripMargin)

    val openCrystallisationObligationJsonDes = Json.parse(
      """
        |{
        |  "obligations": [
        |    {
        |    	"identification": {
        |				"incomeSourceType": "ITSA",
        |				"referenceNumber": "AB123456A",
        |				"referenceType": "NINO"
        |			},
        |    "obligationDetails": [
        |      {
        |        "status": "O",
        |        "inboundCorrespondenceFromDate": "2018-04-06",
        |        "inboundCorrespondenceToDate": "2019-04-05",
        |        "inboundCorrespondenceDateReceived": "2020-01-25",
        |        "inboundCorrespondenceDueDate": "2020-01-31"
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
        |			"identification": {
        |				"incomeSourceType": "ITSA",
        |				"referenceNumber": "AB123456A",
        |				"referenceType": "NINO"
        |			},
        |    "obligationDetails": [
        |      {
        |        "status": "F",
        |        "inboundCorrespondenceFromDate": "2018-04-06",
        |        "inboundCorrespondenceToDate": "2019-04-05",
        |        "inboundCorrespondenceDateReceived": "2020-01-25",
        |        "inboundCorrespondenceDueDate": "2020-01-31"
        |      }
        |    ]
        |    }
        |  ]
        |}
      """.stripMargin)

    val notCrystallisationObligationsJsonDes = Json.parse(
      """
        |{
        |  "obligations": [
        |    {
        |			"identification": {
        |				"incomeSourceType": "ITSP",
        |				"referenceNumber": "AB123456A",
        |				"referenceType": "NINO"
        |			},
        |    "obligationDetails": [
        |      {
        |        "status": "F",
        |        "inboundCorrespondenceFromDate": "2018-04-06",
        |        "inboundCorrespondenceToDate": "2019-04-05",
        |        "inboundCorrespondenceDateReceived": "2020-01-25",
        |        "inboundCorrespondenceDueDate": "2020-01-31"
        |      }
        |    ]
        |    }
        |  ]
        |}
      """.stripMargin)

    val fulfilledObligationsJsonDes = Json.parse(
      """
        |{
        |	 "identification": {
        |				"incomeSourceType": "ITSA",
        |				"referenceNumber": "AB123456A",
        |				"referenceType": "NINO"
        |			},
        |  "obligationDetails": [
        |    {
        |      "status": "F",
        |      "inboundCorrespondenceFromDate": "2018-04-06",
        |      "inboundCorrespondenceToDate": "2019-04-05",
        |      "inboundCorrespondenceDateReceived": "2020-01-25",
        |      "inboundCorrespondenceDueDate": "2020-01-31"
        |    }
        |  ]
        |}
      """.stripMargin)

    val fulfilledObligationJsonMtd =Json.parse(
      """
        |{
        |  "status": "Fulfilled",
        |  "start": "2018-04-06",
        |  "end": "2019-04-05",
        |  "processed": "2020-01-25",
        |  "due": "2020-01-31"
        |}
      """.stripMargin)

    val openObligationJsonMtd =Json.parse(
      """
        |{
        |  "status": "Open",
        |  "start": "2018-04-06",
        |  "end": "2019-04-05",
        |  "due": "2020-01-31"
        |}
      """.stripMargin)

    val fulfilledObligationsJsonArray =Json.parse(
      """
        |{
        | "obligations" : [
        | {
        |  "status": "Fulfilled",
        |  "start": "2018-04-06",
        |  "end": "2019-04-05",
        |  "processed": "2020-01-25",
        |  "due": "2020-01-31"
        | }
        | ]
        |}
      """.stripMargin)

  }
}
