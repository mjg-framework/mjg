# Metadata Tables

Following is the list of metadata tables
and their respective columns (**keys**) for
linking constraints. That means, to ensure
integrity, one must connect data points
from different tables with foreign keys [1]
based on the mentioned **keys**, NOT using
their IDs (since different databases have
inconsistent IDs but always the same **key**
for those entries).

```plain
    "stations" => station_code,
    "indicators" => indicator,
    "station_types" => ,
    "station_indicator",
    "qcvn",
    "qcvn_detail",
    "qcvn_kind",
    "qcvn_station_kind",
    "datalogger",
    "datalogger_command",
    "datalogger_command_execute_history", # TODO: WARNING: this is not present in DbProxy but PydbProxy
    "datalogger_status", # TODO: WARNING: this is not present in DbProxy but PydbProxy
    "areas",
    "equipments",
    "manager_stations",
    "sensor_trouble_history",
    "manager_stations_history",
    "camera_tokens",
    "agents",
    "ftp_management",
    "operating_chemical", # New table so not present in db.define_table('...')
    "standard_chemical", # New table so not present in db.define_table('...')
    # "provinces",
    "manager_areas",
    "agent_station",
    "agent_details",
    "manager_careers",
    "employees",
    "station_off_log",
    "area_station",
```
