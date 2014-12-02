import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import play.api.libs.json.{JsValue, JsString, Json}

import play.api.test._
import play.api.test.Helpers._

@RunWith(classOf[JUnitRunner])
class ExperimentsApiSpec extends Specification {

  "Experiments API" should {

    "return an experimentId when creating a new experiment" in new WithApplication {

      val json: JsValue = Json.parse("""
                                       |{
                                       |  "name": "Checkout page buttons",
                                       |  "scope": 100.0,
                                       |  "variations": [
                                       |    {
                                       |      "name": "Group A",
                                       |      "weight": 70.0,
                                       |      "params": [
                                       |        {
                                       |          "name": "foo",
                                       |          "value": "bar"
                                       |        }
                                       |      ]
                                       |    },
                                       |    {
                                       |      "name": "Group B",
                                       |      "weight": 30.0,
                                       |      "params": [
                                       |        {
                                       |          "name": "foo",
                                       |          "value": "baz"
                                       |        }
                                       |      ]
                                       |    }
                                       |  ]
                                       |}
                                     """.stripMargin)

      val response = route(
        FakeRequest(
          Helpers.POST,
          "/api/experiments",
          FakeHeaders(),
          json)).get

      status(response) must equalTo(OK)
      contentType(response) must beSome("application/json")
      charset(response) must beSome("utf-8")

      val responseBody = Json.parse(contentAsString(response))
      (responseBody \ "success").as[Boolean] must equalTo(true)
      (responseBody \ "experimentId").as[Int] must equalTo(1)
    }

    "not allow to add two experiments with the same name" in new WithApplication {

      val json: JsValue = Json.parse("""
                                       |{
                                       |  "name": "Checkout page buttons",
                                       |  "scope": 100.0,
                                       |  "variations": [
                                       |    {
                                       |      "name": "Group A",
                                       |      "weight": 70.0,
                                       |      "params": [
                                       |        {
                                       |          "name": "foo",
                                       |          "value": "bar"
                                       |        }
                                       |      ]
                                       |    },
                                       |    {
                                       |      "name": "Group B",
                                       |      "weight": 30.0,
                                       |      "params": [
                                       |        {
                                       |          "name": "foo",
                                       |          "value": "baz"
                                       |        }
                                       |      ]
                                       |    }
                                       |  ]
                                       |}
                                     """.stripMargin)

      route(
        FakeRequest(
          Helpers.POST,
          "/api/experiments",
          FakeHeaders(),
          json)).get

      val response = route(
        FakeRequest(
          Helpers.POST,
          "/api/experiments",
          FakeHeaders(),
          json)).get

      status(response) must equalTo(400)
      contentType(response) must beSome("application/json")
      charset(response) must beSome("utf-8")

      val responseBody = Json.parse(contentAsString(response))
      (responseBody \ "success").as[Boolean] must equalTo(false)
      (responseBody \ "error" \ "message").as[String] must equalTo("Bad Request")
      (responseBody \ "error" \ "detail").as[String] must equalTo("An experiment with this name already exists")
    }

    "not allow to add an experiment whose variations weights do not sum up to 100.0" in new WithApplication {

      val json: JsValue = Json.parse("""
                                       |{
                                       |  "name": "Checkout page buttons",
                                       |  "scope": 100.0,
                                       |  "variations": [
                                       |    {
                                       |      "name": "Group A",
                                       |      "weight": 70.1,
                                       |      "params": [
                                       |        {
                                       |          "name": "foo",
                                       |          "value": "bar"
                                       |        }
                                       |      ]
                                       |    },
                                       |    {
                                       |      "name": "Group B",
                                       |      "weight": 30.0,
                                       |      "params": [
                                       |        {
                                       |          "name": "foo",
                                       |          "value": "baz"
                                       |        }
                                       |      ]
                                       |    }
                                       |  ]
                                       |}
                                     """.stripMargin)

      val response = route(
        FakeRequest(
          Helpers.POST,
          "/api/experiments",
          FakeHeaders(),
          json)).get

      status(response) must equalTo(400)
      contentType(response) must beSome("application/json")
      charset(response) must beSome("utf-8")

      val responseBody = Json.parse(contentAsString(response))
      (responseBody \ "success").as[Boolean] must equalTo(false)
      (responseBody \ "error" \ "message").as[String] must equalTo("Bad Request")
      (responseBody \ "error" \ "detail").as[String] must equalTo("The sum of the variation weights must be 100.0")
    }

  }

}
