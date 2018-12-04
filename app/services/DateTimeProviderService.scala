package services


import javax.inject.Singleton
import org.joda.time.{DateTime, LocalDate}

@Singleton
class DateTimeProviderService {

  def now: DateTime = DateTime.now()
}
