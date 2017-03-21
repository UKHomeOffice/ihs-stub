package model

import java.util.Date

import org.joda.time.DateTime

case class LeadApplicant(
                          Title: Option[String],
                          GivenNames: Option[String],
                          FamilyName: Option[String],
                          FullName: Option[String],
                          Nationality: Option[String],
                          VisaType: Option[String],
                          PassportNumber: Option[String],
                          DateOfBirth: Option[String],
                          LengthOfStay: Option[Int],
                          CosStartDate: Option[String] = None,
                          CosEndDate: Option[String] = None,
                          CasStartDate: Option[String] = None,
                          CasEndDate: Option[String] = None)

case class MainApplicant(
                          Title: Option[String],
                          GivenNames: Option[String],
                          FamilyName: Option[String],
                          FullName: Option[String],
                          Nationality: Option[String],
                          VisaType: Option[String],
                          PassportNumber: Option[String],
                          DateOfBirth: Option[String],
                          LengthOfStay: Option[Int],
                          CosStartDate: Option[String] = None,
                          CasStartDate: Option[String] = None,
                          CasEndDate: Option[String] = None,
                          Relationship: Option[String] = None)

case class Dependant(
                      Order: Int,
                      Title: Option[String],
                      GivenNames: Option[String],
                      FamilyName: Option[String],
                      FullName: Option[String],
                      Nationality: Option[String],
                      PassportNumber: Option[String],
                      DateOfBirth: Option[String])

case class IhsRequestData(ApplicationData: ApplicationData)

case class ApplicationData(
                            VisaApplicationNumber: String,
                            InCountry: Boolean,
                            VAC: Option[String],
                            EmailAddress: Option[String],
                            LeadApplicant: LeadApplicant,
                            MainApplicant: MainApplicant,
                            Dependants: List[Dependant])

object ApplicationData {

  def apply(visaApplicationNumber: String,
            inCountry: Boolean,
            vac: Option[String],
            emailAddress: Option[String],
            leadApplicant: LeadApplicant,
            mainApplicant: Option[MainApplicant],
            dependants: List[Dependant]): ApplicationData =

  // we have to set mainApplicant and dependants list to null because of IHS requirement
    ApplicationData(visaApplicationNumber, inCountry, vac, emailAddress, leadApplicant, mainApplicant.getOrElse(null), dependants)//if (dependants.isEmpty) null else dependants)
}

case class StandardClaimSet(iss: String,
                            sub: String,
                            exp: Date = DateTime.now.plusSeconds(60).toDate,
                            nbf: Date = DateTime.now.minusSeconds(5).toDate,
                            iat: Date = DateTime.now.toDate)

case class IhsResponse(visaApplicationNumber: String, ihsReferenceNumber: String)

case class CustomClaimSet(claims: Map[String, AnyRef])

case class EncryptedMessage(iv: Array[Byte], message: Array[Byte])
