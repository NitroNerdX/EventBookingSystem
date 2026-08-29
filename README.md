"# EventBookingSystem"
Endpoint summary — worth keeping as a reference for your Postman collection and README
Method	Path	Role	Purpose
POST	/api/auth/register	Public	Register organizer or customer
POST	/api/auth/login	Public	Login, returns JWT
GET	/api/events	Authenticated	Browse all events
GET	/api/events/{id}	Authenticated	Event details
POST	/api/events	Organizer	Create event
PUT	/api/events/{id}	Organizer (owner only)	Update event
GET	/api/events/mine	Organizer	My events
POST	/api/events/{eventId}/bookings	Customer	Book tickets
GET	/api/bookings/mine	Customer	My bookings