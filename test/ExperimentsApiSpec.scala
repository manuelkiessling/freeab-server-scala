import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import play.api.libs.json.Json

import play.api.test._
import play.api.test.Helpers._

@RunWith(classOf[JUnitRunner])
class ExperimentsApiSpec extends Specification {

  "Experiments API" should {

    "return an experimentId when creating a new experiment" in new WithApplication {
      val response = route(
        FakeRequest(
          Helpers.POST,
          "/api/experiments",
          FakeHeaders(),
          """
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
          """.stripMargin)).get

      status(response) must equalTo(OK)
      contentType(response) must beSome("application/json")
      charset(response) must beSome("utf-8")

      val responseBody = Json.parse(contentAsString(response))
      (responseBody \ "success").as[Boolean] must equalTo(true)
      (responseBody \ "experimentId").as[Int] must equalTo(1)
    }

    "should not allow to add two experiments with the same name" in new WithApplication {
      route(
        FakeRequest(
          Helpers.POST,
          "/api/experiments",
          FakeHeaders(),
          """
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
          """.stripMargin)).get

      val response = route(
        FakeRequest(
          Helpers.POST,
          "/api/experiments",
          FakeHeaders(),
          """
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
          """.stripMargin)).get

      status(response) must equalTo(400)
      contentType(response) must beSome("application/json")
      charset(response) must beSome("utf-8")

      val responseBody = Json.parse(contentAsString(response))
      (responseBody \ "success").as[Boolean] must equalTo(false)
      (responseBody \ "error" \ "message").as[String] must equalTo("Bad Request")
      (responseBody \ "error" \ "detail").as[String] must equalTo("An experiment with this name already exists")
    }

  }

}
