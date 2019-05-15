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
package v2.controllers

import support.UnitSpec

class DesResponseMappingSupportSpec extends UnitSpec {

  val mapping = new DesResponseMappingSupport {
    override val controllerName: String = "somecontroller"
  }

  "mapping Des errors" when {
    "single error" when {
      "the error code is in the map provided" must {
        "use the mapping and wrap" in {
          fail
        }
      }

      "the error code is not in the map provided" must {
        "default to DownstreamError and wrap" in {
          fail
        }
      }
    }

    "multiple errors" when {
      "the error codes is in the map provided" must {
        "use the mapping and wrap with main error type of BadRequest" in {
          fail
        }
      }

      "the error code is not in the map provided" must {
        "default to DownstreamError and wrap with main error type of BadRequest" in {
          fail
        }
      }

      "one of the mapped errors is DownstreamError" must {
        "wrap the errors with main error type of DownstreamError" in {
          fail
        }
      }
    }

    "the error code is an OutboundError" must {
      "return the error as is (in an ErrorWrapper)" in {
        fail
      }
    }
  }

}
