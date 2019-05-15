import java.time.LocalDate

import v2.models.des._
import v2.models.domain.Obligation


val obligations = Seq(DesObligation(
  Identification(
    "ITSP",
    "",
    ""),
  Seq(
    DesObligationDetail(
      status = "F",
      inboundCorrespondenceFromDate = LocalDate.now(),
      inboundCorrespondenceToDate = LocalDate.now(),
      inboundCorrespondenceDueDate = LocalDate.now(),
      inboundCorrespondenceDateReceived = Some(LocalDate.now())
    ))

))


  val result = for {
      obligation <- obligations
      obligationDetail <- obligation.obligationDetails
      if obligation.identification.incomeSourceType == "ITSA"
    } yield
      Obligation(
        start = obligationDetail.inboundCorrespondenceFromDate,
        end = obligationDetail.inboundCorrespondenceToDate,
        due = obligationDetail.inboundCorrespondenceDueDate,
        status = FulfilledObligation,
        processed = obligationDetail.inboundCorrespondenceDateReceived
      )
