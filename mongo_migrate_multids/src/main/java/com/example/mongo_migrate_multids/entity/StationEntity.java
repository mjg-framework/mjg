package com.example.mongo_migrate_multids.entity;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Document(value = "stations")
@Getter
@Setter
public class StationEntity extends BaseEntity {

    @Id
    public String id;

    @Field(value = "address")
    public String address;

    @Field(value = "agents_id")
    public String agentsId;

    @Field(value = "area_id")
    public String areaId;

    @Field(value = "career")
    public String career;

    @Field(value = "career_group")
    public String careerGroup;

    @Field(value = "contact_info")
    public String contactInfo;

    @Field(value = "contact_point")
    public String contactPoint;

    @Field(value = "country_code")
    public String countryCode;

    @Field(value = "data_folder")
    public String dataFolder;

    @Field(value = "data_server")
    public String dataServer;

    @Field(value = "data_server_port")
    public String dataServerPort;

    @Field(value = "data_server_public")
    public String dataServerPublic;

    @Field(value = "data_source")
    public String dataSource;

    @Field(value = "description")
    public String description;

    @Field(value = "email")
    public String email;

    @Field(value = "file_mapping")
    public String fileMapping;

    @Field(value = "file_mapping_desc")
    public String fileMappingDesc;

    @Field(value = "frequency_receiving_data")
    public Integer frequencyReceivingData;

    @Field(value = "ftp_connection_status")
    public Boolean ftpConnectionStatus;

    @Field(value = "ftp_id")
    public String ftpId;

    @Field(value = "implement_agency_ra")
    public String implementAgencyRa;

    @Field(value = "interval_scan")
    public String intervalScan;

    //trạm công bố
    @Field(value = "is_public")
    public Boolean isPublic;

    @Field(value = "is_public_data_type")
    public Integer isPublicDataType;

    @Field(value = "is_qi")
    public Boolean isQi;

    @Field(value = "is_synced")
    public Boolean isSynced;

    @Field(value = "last_file_content")
    public String lastFileContent;

    @Field(value = "last_file_name")
    public String lastFileName;

    @Field(value = "last_file_path")
    public String lastFilePath;

    @Field(value = "last_qty_adjusting")
    public String lastQtyAdjusting;

    @Field(value = "last_qty_error")
    public String lastQtyError;

    @Field(value = "last_qty_exceed")
    public String lastQtyExceed;

    @Field(value = "last_qty_good")
    public String lastQtyGood;

    @Field(value = "last_time")
    public LocalDateTime lastTime;

    @Field(value = "latitude")
    public Double latitude;

    @Field(value = "logger_id")
    public String loggerId;

    @Field(value = "longitude")
    public Double longitude;

    @Field(value = "mqtt_client_id")
    public String mqttClientId;

    @Field(value = "mqtt_pwd")
    public String mqttPwd;

    @Field(value = "mqtt_usr")
    public String mqttUsr;

    @Field(value = "off_time")
    public Instant offTime;

    @Field(value = "order_in_area")
    public Integer orderInArea;

    @Field(value = "order_no")
    public Integer orderNo;

    @Field(value = "path_format")
    public Integer pathFormat;

    @Field(value = "period_ra")
    public String periodRa;

    @Field(value = "phone")
    public String phone;

    @Field(value = "province_id")
    public String provinceId;

    @Field(value = "public_time")
    public LocalDateTime publicTime;

    @Field(value = "pwd")
    public String pwd;

    @Field(value = "qi")
    public String qi;

    @Field(value = "qi_adjsut_time")
    public LocalDateTime qiAdjsutTime;

    @Field(value = "qi_adjust")
    public Integer qiAdjust;

    @Field(value = "qty_adjusting")
    public Integer qtyAdjusting;

    @Field(value = "qty_error")
    public Integer qtyError;

    @Field(value = "qty_exceed")
    public Integer qtyExceed;

    @Field(value = "qty_good")
    public Integer qtyGood;

    @Field(value = "retry")
    public Integer retry;

    @Field(value = "scan_failed")
    public Integer scanFailed;

    @Field(value = "station_code")
    public String stationCode;

    @Field(value = "station_id")
    public String stationId;

    @Field(value = "station_name")
    public String stationName;

    @Field(value = "station_type")
    public Integer stationType;

    @Field(value = "status")
    public Long status;

    @Field(value = "time_count_offline")
    public Integer timeCountOffline;

    @Field(value = "transfer_type")
    public String transferType;

    @Field(value = "tw_request_sync_station_id")
    public String twRequestSyncStationId;

    @Field(value = "username")
    public String username;

    @Field(value = "using_status")
    public Long usingStatus;

    @Field(value = "area_ids")
    public List<String> areaIds;

    @Field(value = "station_pictures")
    public List<String> station_pictures;

    @Field(value = "is_preferred")
    public Boolean isPreferred;

    @Field(value = "verification_deadline")
    public LocalDateTime verificationDeadline;

    @Field(value = "read_from_date")
    public LocalDateTime readFromDate;

    @Field(value = "data_indicators")
    public String dataIndicators;

    @Field(value = "organization_id")
    public String organizationId;

}

