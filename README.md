# lisa
Library Inventory and Statistics Application.

Scientific article describing the system in German: https://www.o-bib.de/bib/article/view/5774
## backend
The backend of the server connects the LBS4 database (via JDBC parameterized in retrieve.DBConnection) as well as the central catalogue (via SRU interface parameterized via retrieve.QueryFactory) and retrieves data either based on a DB or an SRU query using retrieve.DBConnection and retrieve.XMLReader. These capabilities are used in services.* for various (generic) use cases. For the frontend connection, the class SRU_Engine uses services.DropbillRetriever to define a service endpoint which is used by jetty service.

## frontend
