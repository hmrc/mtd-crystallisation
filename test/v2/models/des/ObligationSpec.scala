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

import play.api.libs.json.Json
import support.UnitSpec
import v2.models.fixtures.Fixtures.CrystallisationObligationFixture._
import scala.util.{Failure, Success, Try}


class ObligationSpec extends UnitSpec {

  "Crystallisation Obligations reads" should {
    "parse a fulfilled obligation from DES json.response" in {
      val model = Obligation.reads.reads(fulfilledJsonObligation).get
      model shouldBe fulfilledObligation
    }

    "parse an open obligation from DES json.response" in {
      val model = Obligation.reads.reads(openJsonObligationInput).get
      model shouldBe openObligation
    }


    "the JSON represents a fulfilled obligation with a missing processed data" should {
      "throw an error" in {
        Try(fulfilledJsonObligationMissingProcessedDate.as[Obligation]) match {
          case Success(_) => fail("An error was expected")
          case Failure(e) => e.getMessage shouldBe "Cannot create a fulfilled obligation without a processed date"
        }
      }
    }

    "the JSON represents an open obligation with a processed data" should {
      "throw an error" in {
        Try(openJsonObligationWithProcessedDate.as[Obligation]) match {
          case Success(_) => fail("An error was expected")
          case Failure(e) => e.getMessage shouldBe "Cannot create an open obligation with a processed date"
        }
      }
    }
  }

  "Crystallisation Obligations writes" should {
    "generate a correct fulfilled Json" in {
      val json = Json.toJson(openObligation)
      val expectedJson = Json.parse(openObligationOutputJson)
      json shouldBe expectedJson
    }

    "generate a correct open Json" in {
      val json = Json.toJson(fulfilledObligation)
      val expectedJson = Json.parse(fulfilledObligationOutputJson)
      json shouldBe expectedJson
    }
  }

  "Creating a valid obligation" when {
    "the details represent a fulfilled obligation" should {

      val obligation = fulfilledObligation
      "result in an obligation with the correct start date" in {
        obligation.startDate shouldBe start
      }
      "result in an obligation with the correct end date" in {
        obligation.endDate shouldBe end
      }
      "result in an obligation with the correct due date" in {
        obligation.dueDate shouldBe due
      }
      "result in an obligation with the correct status" in {
        obligation.status shouldBe statusFulfilled
      }
      "result in an obligation with the correct processed date" in {
        obligation.processedDate shouldBe processed
      }
    }

    "the details represent an open obligation" should {

      val obligation = openObligation

      "result in an obligation with the correct start date" in {
        obligation.startDate shouldBe start
      }
      "result in an obligation with the correct end date" in {
        obligation.endDate shouldBe end
      }
      "result in an obligation with the correct due date" in {
        obligation.dueDate shouldBe due
      }
      "result in an obligation with the correct status" in {
        obligation.status shouldBe statusOpen
      }
      "result in an obligation with the correct processed date" in {
        obligation.processedDate shouldBe None
      }
    }
  }

  "Creating an invalid obligation" when {
    "a fulfilled obligation is missing the processed date" should {
      "throw an exception" in {

        Try(
          Obligation(
            startDate = start,
            endDate = end,
            dueDate = due,
            status = statusFulfilled,
            processedDate = None
          )
        ) match {
          case Success(_) => fail("Fulfilled obligation must have a processed date")
          case Failure(e) => e.getMessage shouldBe "Cannot create a fulfilled obligation without a processed date"
        }

      }
    }

    "an open obligation has a processed date" should {
      "throw an exception" in {

        Try(
          Obligation(
            startDate = start,
            endDate = end,
            dueDate = due,
            status = statusOpen,
            processedDate = processed
          )
        ) match {
          case Success(_) => fail("Open obligation must NOT have a processed date")
          case Failure(e) => e.getMessage shouldBe "Cannot create an open obligation with a processed date"
        }

      }
    }
  }
}
