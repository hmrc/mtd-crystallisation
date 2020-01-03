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

package v2.models.des

import support.UnitSpec
import v2.models.fixtures.Fixtures.CrystallisationObligationFixture._


class DesObligationDetailSpec extends UnitSpec {

  "Obligation reads" should {
    "parse a fulfilled obligation from DES json.response" in {
      val model = DesObligationDetail.reads.reads(fulfilledJsonObligationDes).get
      model shouldBe fulfilledObligationDes
    }

    "parse an open obligation from DES json.response" in {
      val model = DesObligationDetail.reads.reads(openJsonObligationDes).get
      model shouldBe openObligationDes
    }
  }

  "toMtd conversion" should {
    "convert the des result to the mtd result" in {
      val mtdResult = fulfilledCrystallisationObligationsResponseDes.toMtd
      mtdResult shouldBe Seq(fulfilledObligationMtd)

    }
  }
}
