package controllers



import components.Configuration.dcjFailureUrl
import model.IhsRequestData
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc._
import services.{IhsRequestTokenParser, IhsResponseTokenGenerator}

import scala.util.Random

case class Ihs(visa_token: String)

object IhsForm {
  def apply(): Form[Ihs] = Form(
    mapping(
      "visa_token" -> text
    )(Ihs.apply)(Ihs.unapply)
  )
}

class IhsController(responseTokenGenerator: IhsResponseTokenGenerator, requestTokenParser: IhsRequestTokenParser) extends Controller {

  private def randomIhsNumber: String = s"IHS${"%09d".format(Random.nextInt(1000000000))}"

  def complete() = Action { implicit request =>

    IhsForm().bindFromRequest().fold(
      errors => {
        Found(dcjFailureUrl)
      },
      ihs => {
        val ihsData: IhsRequestData = requestTokenParser.parse(ihs.visa_token)
        val responseToken: String = responseTokenGenerator.generate(
          visaApplicationNumber = ihsData.ApplicationData.VisaApplicationNumber,
          ihsReferenceNumber =   randomIhsNumber
        )

        println("Response Token : " + responseToken)

        Ok(views.html.ihs(ihsData.ApplicationData, responseToken))
      }
    )
  }

  def ok() = Action {
    Ok("Ok")
  }
}