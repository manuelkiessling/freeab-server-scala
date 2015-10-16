import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import org.specs2.specification.BeforeExample
import play.api.db.DB
import anorm.SQL
import play.api.libs.json.{JsValue, Json}
import play.api.test._
import play.api.test.Helpers._

@RunWith(classOf[JUnitRunner])
class ExperimentsApiSpec extends Specification with BeforeExample {

  def before = new WithApplication {
    DB.withConnection("api") { implicit connection =>
      SQL("DELETE FROM experiments").execute()
      SQL("DELETE FROM variations").execute()
    }
  }

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
      (responseBody \ "success").as[Boolean] must beTrue

      val uuidRegex = """[a-f0-9]{8}-[a-f0-9]{4}-4[a-f0-9]{3}-[89aAbB][a-f0-9]{3}-[a-f0-9]{12}""".r
      val doesMatch = (responseBody \ "experimentId").as[String] match {
        case uuidRegex(_*) => true
        case _ => false
      }
      doesMatch must beTrue
    }

    // TODO: This spec sometimes fails for no obvious reason
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
      (responseBody \ "success").as[Boolean] must beFalse
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
      (responseBody \ "success").as[Boolean] must beFalse
      (responseBody \ "error" \ "message").as[String] must equalTo("Bad Request")
      (responseBody \ "error" \ "detail").as[String] must equalTo("The sum of the variation weights must be 100.0")
    }

    "not allow to add an experiment whose variations names are not unique" in new WithApplication {
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
                                       |      "name": "Group A",
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
      (responseBody \ "success").as[Boolean] must beFalse
      (responseBody \ "error" \ "message").as[String] must equalTo("Bad Request")
      (responseBody \ "error" \ "detail").as[String] must equalTo("Variation names must be unique")
    }

    "not allow to add an experiment with less than 2 variations" in new WithApplication {
      val json: JsValue = Json.parse("""
                                       |{
                                       |  "name": "Checkout page buttons",
                                       |  "scope": 100.0,
                                       |  "variations": [
                                       |    {
                                       |      "name": "Group A",
                                       |      "weight": 100.0,
                                       |      "params": [
                                       |        {
                                       |          "name": "foo",
                                       |          "value": "bar"
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
      (responseBody \ "success").as[Boolean] must beFalse
      (responseBody \ "error" \ "message").as[String] must equalTo("Bad Request")
      (responseBody \ "error" \ "detail").as[String] must equalTo("An experiment needs at least 2 variations")
    }

    "should not allow to add an experiment without a name" in new WithApplication {
      val json: JsValue = Json.parse("""
                                       |{
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

      status(response) must equalTo(400)
      contentType(response) must beSome("application/json")
      charset(response) must beSome("utf-8")

      val responseBody = Json.parse(contentAsString(response))
      (responseBody \ "success").as[Boolean] must beFalse
      (responseBody \ "error" \ "message").as[String] must equalTo("Bad Request")
      (responseBody \ "error" \ "detail").as[String] must equalTo("An experiment needs a name")
    }

  }

}
