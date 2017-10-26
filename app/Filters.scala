import javax.inject._
import play.api._
import play.api.http.HttpFilters
import play.api.mvc._
import play.api.http.HttpFilters
import play.api.mvc.EssentialFilter
import play.filters.csrf.CSRFFilter
import play.filters.headers.SecurityHeadersFilter
import utils.LoggingFilter


@Singleton
class Filters @Inject() (
	csrfFilter: CSRFFilter, 
	securityHeadersFilter: SecurityHeadersFilter,
	loggingFilter: LoggingFilter) extends HttpFilters {
  override def filters: Seq[EssentialFilter] = Seq(csrfFilter, securityHeadersFilter, loggingFilter)
}

