package us.penrose.calmerge

import java.net.URL
import net.fortuna.ical4j.data.CalendarBuilder
import org.httpobjects.client.ApacheCommons4xHttpClient
import org.httpobjects.util.HttpObjectUtil
import java.io.ByteArrayInputStream
import net.fortuna.ical4j.model.Calendar
import net.fortuna.ical4j.model.property.ProdId
import net.fortuna.ical4j.model.property.Version
import net.fortuna.ical4j.model.property.CalScale
import net.fortuna.ical4j.data.CalendarOutputter
import net.fortuna.ical4j.model.Property
import net.fortuna.ical4j.model.property.Organizer
import org.httpobjects.jetty.HttpObjectsJettyHandler
import org.httpobjects.HttpObject
import org.httpobjects.Request
import org.httpobjects.DSL._
import org.httpobjects.Representation
import java.io.OutputStream
import scala.collection.JavaConversions._
import net.fortuna.ical4j.model.Component
import scala.util.Try
import net.fortuna.ical4j.model.component.VEvent
import net.fortuna.ical4j.model.Date
import net.fortuna.ical4j.model.parameter.Value
import org.httpobjects.util.ClasspathResourceObject
import net.fortuna.ical4j.model.property.Uid
import java.util.UUID

object CalMerge {
	val calendars =
    """
http://www.meetup.com/codecraftgroup/events/ical/
http://www.meetup.com/santa-monica-haskell/events/ical/
""".lines.toList.map(_.trim).filterNot(_.isEmpty).map(_.replaceAllLiterally("webcal://", "http://"))

  
      
  def emptyCalendar() = {
    val c = new Calendar();
    c.getProperties().add(new ProdId("-//Ben Fortuna//iCal4j 1.0//EN"));
    c.getProperties().add(Version.VERSION_2_0);
    c.getProperties().add(CalScale.GREGORIAN);
    c
  }
  def defaultCalendar() = {
    val c = emptyCalendar()
    
    val cal = java.util.Calendar.getInstance();
    val christmas = new VEvent(new Date(cal.getTime()),new Date(cal.getTime()), "Please wait - calendar data is loading");
    // initialise as an all-day event..
//    christmas.getProperties().getProperty(Property.DTSTART).getParameters().add(Value.DATE);
    christmas.getProperties.add(new Uid(UUID.randomUUID().toString()))
    c.getComponents.add(christmas)
    
    c
  }
  def fetchCombinedCalendar() = {
    println(s"Fetching ${calendars.size} calendars")
  		val allComponents = calendars.flatMap{url=>
          println(s"Fetching $url")
      		val httpClient = new ApacheCommons4xHttpClient()
      		val u = httpClient.resource(url).get().representation()
          val stream = new ByteArrayInputStream(HttpObjectUtil.toByteArray(u))
      		val builder = new CalendarBuilder()
      		val calendar = builder.build(stream);
      		val components = calendar.getComponents().asInstanceOf[java.util.List[Component]].toList
  				components
  		}
  
  		val c = emptyCalendar()
  		val f= asJavaCollection(allComponents.toList)
  
      println(f.size() + " entries")
  
      if(f.size() == 0){
        val cal = java.util.Calendar.getInstance();
        cal.set(java.util.Calendar.MONTH, java.util.Calendar.DECEMBER);
        cal.set(java.util.Calendar.DAY_OF_MONTH, 25);
        val christmas = new VEvent(new Date(cal.getTime()), "Christmas Day");
        // initialise as an all-day event..
        christmas.getProperties().getProperty(Property.DTSTART).getParameters().add(Value.DATE);
        c.getComponents.add(christmas)
      }else{
        
        c.getComponents.asInstanceOf[java.util.List[Component]].addAll(f)
      }
      
      
			c
  	}  
    
    var calendar = defaultCalendar()
    
    new Thread(){
      override def run(){
        while(true){
          println("Refreshing")
          Try(fetchCombinedCalendar).foreach{c=>
            calendar = c
          }
          Thread.sleep(10000)
        }
      }
    }.start();
    
    val iCalResponse = OK(new Representation(){
      val contentType = "text/calendar"
      def write(out:OutputStream){
        new CalendarOutputter().output(calendar, out);  
      }
    })
    
    def main(args:Array[String]) {
      
      val port = System.getenv.getOrDefault("PORT", "8080").toInt
      HttpObjectsJettyHandler.launchServer(port,
          new ClasspathResourceObject("/",  "text/html", "/public_content/index.html", getClass),
          
          classpathResourcesAt("/public_content/").servedAt("/"),
          
          new HttpObject("/calendars"){
            override def get(r:Request) = OK(Text(calendars.mkString("\n")))
          },
          
          new HttpObject("/ical"){
            override def get(r:Request) = iCalResponse
            override def post(r:Request) = iCalResponse
          }
      )
      
      
    }
}