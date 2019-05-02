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
import play.api.libs.json.{ Reads, Writes }
import uk.gov.hmrc.http.logging.Authorization
import uk.gov.hmrc.http.{ HeaderCarrier, HttpReads }
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import v2.config.AppConfig
import v2.connectors.DesConnectorOutcome
import v2.connectors.httpparsers.StandardDesHttpParser

import scala.concurrent.{ ExecutionContext, Future }

case class DesUri[Resp](uri: String)(implicit val responseReads: Reads[Resp])

class DesService @Inject()(http: HttpClient, appConfig: AppConfig) {

  private def desHeaderCarrier(implicit hc: HeaderCarrier): HeaderCarrier =
    hc.copy(authorization = Some(Authorization(s"Bearer ${appConfig.desToken}")))
      .withExtraHeaders("Environment" -> appConfig.desEnv)

  def post[Body: Writes, Resp](body: Body, cmd: DesUri[Resp])(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[DesConnectorOutcome[Resp]] = {
    implicit val parserReads: HttpReads[DesConnectorOutcome[Resp]] = StandardDesHttpParser.reads(cmd.responseReads)

    def doPost(implicit hc: HeaderCarrier) =
      http.POST(s"${appConfig.desBaseUrl}/${cmd.uri}", body)

    doPost(desHeaderCarrier(hc))
  }

  def get[Resp](cmd: DesUri[Resp])(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[DesConnectorOutcome[Resp]] = {
    implicit val parserReads: HttpReads[DesConnectorOutcome[Resp]] = StandardDesHttpParser.reads(cmd.responseReads)

    def doGet(implicit hc: HeaderCarrier) =
      http.GET(s"${appConfig.desBaseUrl}/${cmd.uri}")

    doGet(desHeaderCarrier(hc))
  }
}
