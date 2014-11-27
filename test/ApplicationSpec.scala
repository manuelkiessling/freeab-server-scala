import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import play.api.libs.json.Json

import play.api.test._
import play.api.test.Helpers._

/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 * For more information, consult the wiki.
 */
@RunWith(classOf[JUnitRunner])
class ApplicationSpec extends Specification {

  "Application" should {

    "return an experimentId when creating a new experiment" in new WithApplication{
      val response = route(FakeRequest(Helpers.POST, "/api/experiments", FakeHeaders(), """ {"name": "New Group", "collabs": ["foo", "asdf"]} """)).get

      status(response) must equalTo(OK)
      contentType(response) must beSome("application/json")
      charset(response) must beSome("utf-8")

      val responseBody = Json.parse(contentAsString(response))
      (responseBody \ "success").as[Boolean] must equalTo(true)
    }
  }
}
