# calmerge
A simple webapp that merges multiple iCal feeds into one

# configuration

Edit the list of urls in [CalMerge.scala](src/main/scala/us/penrose/calmerge/CalMerge.scala)

# local deploy

    mvn clean install
    java -jar target/calmerge-latest-SNAPSHOT.one-jar.jar

open your browser to http://localhost:8080

# heroku deploy

    heroku apps:create my-ical-feed
    git push heroku master

open your browser to https://my-ical-feed.herokuapp.com/
