lightning-browse
================

Lightning Browse, in its complete form, will be a Clojurescript web app for quickly browsing SoundCloud uploads. By querying SoundCloud for streamable tracks, the app is able to continuously provide music, even if the listener elects to skip several tracks in a row. 

The project is very much in its infancy. :)

##Potential features

A playlist will be dynamically generated for the user based on the feedback they provide as tracks are played. While many music recommendation services' algorithms are relatively opaque, I hope to be as transparent as possible about the way that Lightning Browse chooses the next track to serve. 

##Status

###Functioning
- Retrieving the most recent track from Soundcloud and playing it via the HTML5 audio element

###Not Yet Implemented
- Firefox compatibility
- A custom audio player
- The ability to seamlessly retrieve another track
- Custom recommendations
- Any kind of persistence or state with regard to tracks already browsed

##License

Lightning Browse is released under the MIT License.
