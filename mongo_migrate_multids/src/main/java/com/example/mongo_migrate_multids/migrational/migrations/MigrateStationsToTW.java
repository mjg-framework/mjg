package com.example.mongo_migrate_multids.migrational.migrations;

import java.util.*;

import com.example.mjg.annotations.ForEachRecordFrom;
import com.example.mjg.annotations.MatchWith;
import com.example.mjg.annotations.Migration;
import com.example.mjg.annotations.TransformAndSaveTo;
import com.example.mjg.config.Cardinality;
import com.example.mjg.config.ErrorResolution;
import com.example.mjg.exceptions.DuplicateDataException;
import com.example.mjg.spring.filtering.SpringRepositoryFilterSet;
import com.example.mjg.storage.DataStoreRegistry;
import com.example.mongo_migrate_multids.entity.AreaEntity;
import com.example.mongo_migrate_multids.entity.StationEntity;
import com.example.mongo_migrate_multids.helpers.ObjectIdHelpers;
import com.example.mongo_migrate_multids.migrational.datastores.dest.DestAreaStore;
import com.example.mongo_migrate_multids.migrational.datastores.dest.DestStationStore;
import com.example.mongo_migrate_multids.migrational.datastores.src.SrcAreaStore;
import com.example.mongo_migrate_multids.migrational.datastores.src.SrcStationStore;
import com.example.mongo_migrate_multids.repository.dest.DestAreaRepository;

@Migration
@ForEachRecordFrom(
    value = SrcStationStore.class,
    batchSize = 512,
    inCaseOfError = @ErrorResolution(retryTimes = 3, retryDelayInSeconds = 2)
)
@MatchWith(
    value = SrcAreaStore.class,
    batchSize = 8,
    cardinality = Cardinality.ZERO_OR_MORE,
    order = 0
)
@MatchWith(
    value = DestAreaStore.class,
    batchSize = 8,
    cardinality = Cardinality.ZERO_OR_MORE,
    order = 1
)
@TransformAndSaveTo(
    value = DestStationStore.class,
    cardinality = Cardinality.EXACTLY_ONE,
    inCaseOfError = @ErrorResolution(retryTimes = 3, retryDelayInSeconds = 2)
)
public class MigrateStationsToTW {
    public void startReduction(
        StationEntity station,
        Map<String, Object> aggregates
    ) {
        aggregates.put("areaCodes", new HashSet<String>());
        aggregates.put("newAreaIds", new HashSet<String>());
    }

    public SpringRepositoryFilterSet<AreaEntity, String> matchWithSrcAreaStore(
        StationEntity station,
        Map<String, Object> aggregates,
        SrcAreaStore srcAreaStore
    ) {
        Set<String> areaIds = new HashSet<>();
        if (null != station.getAreaIds()) {
            areaIds.addAll(station.getAreaIds());
        }
        if (null != station.getAreaId()) {
            areaIds.add(station.getAreaId());
        }

        return SpringRepositoryFilterSet.findAllByIdIn(areaIds);
    }

    public void reduceFromSrcAreaStore(
        Map<String, Object> aggregates,
        List<AreaEntity> moreMatchingAreas
    ) {
        if (!moreMatchingAreas.isEmpty()) {
            @SuppressWarnings("unchecked")
            Set<String> matchingAreaCodes = (Set<String>) aggregates.get("areaCodes");
            matchingAreaCodes.addAll(
                moreMatchingAreas.stream()
                    .map(AreaEntity::getAreaCode)
                    .toList()
            );
        }
    }

    public SpringRepositoryFilterSet<AreaEntity, String> matchWithDestAreaStore(
        StationEntity station,
        Map<String, Object> aggregates,
        DestAreaStore destAreaStore
    ) {
        @SuppressWarnings("unchecked")
        Set<String> matchingAreaCodes = (Set<String>) aggregates.get("areaCodes");

        return SpringRepositoryFilterSet.of(DestAreaRepository::findAllByAreaCodeIn, matchingAreaCodes);
    }

    public void reduceFromDestAreaStore(
        Map<String, Object> aggregates,
        List<AreaEntity> moreMatchingAreasFromStore2
    ) {
        @SuppressWarnings("unchecked")
        Set<String> newAreaIds = (Set<String>) aggregates.get("newAreaIds");

        newAreaIds.addAll(
            moreMatchingAreasFromStore2
                .stream()
                .map(area -> ObjectIdHelpers.convertObjectIdToLargeInteger(
                    area.getId()
                ))
                .toList()
        );
    }

    public List<StationEntity> transform(
        Map<String, Object> aggregates,
        StationEntity oldStation
    ) {
        StationEntity newStation = copyAllExceptIdAndAreaId_s(oldStation);

        @SuppressWarnings("unchecked")
        Set<String> newAreaIds = (Set<String>) aggregates.get("newAreaIds");
        List<String> newAreaIdList = newAreaIds.stream().toList();
        newStation.setAreaIds(newAreaIdList);
        if (newAreaIds.isEmpty()) {
            newStation.setAreaIds(null);
        } else {
            newStation.setAreaId(newAreaIdList.get(0));
        }

        return List.of(newStation);
    }

    public List<StationEntity> handleDuplicate(
        DuplicateDataException exception,
        StationEntity inputRecord,
        List<StationEntity> outputRecords,
        SrcStationStore srcStationStore,
        DestStationStore destStationStore,
        DataStoreRegistry dataStoreRegistry
    ) {
        // Do not handle duplicate error
        return null;
    }



    private static StationEntity copyAllExceptIdAndAreaId_s(
        StationEntity oldStation
    ) {
        StationEntity newStation = new StationEntity();

        // newStation.setAreaIds(oldStation.getAreaIds());
        // newStation.setAreaId(oldStation.getAreaId());


        // Sets all fields from oldStation to newStation
        newStation.setAddress(oldStation.getAddress());
        newStation.setAgentsId(oldStation.getAgentsId());
        newStation.setCareer(oldStation.getCareer());
        newStation.setCareerGroup(oldStation.getCareerGroup());
        newStation.setContactInfo(oldStation.getContactInfo());
        newStation.setContactPoint(oldStation.getContactPoint());
        newStation.setCountryCode(oldStation.getCountryCode());
        newStation.setDataFolder(oldStation.getDataFolder());
        newStation.setDataServer(oldStation.getDataServer());
        newStation.setDataServerPort(oldStation.getDataServerPort());
        newStation.setDataServerPublic(oldStation.getDataServerPublic());
        newStation.setDataSource(oldStation.getDataSource());
        newStation.setDescription(oldStation.getDescription());
        newStation.setEmail(oldStation.getEmail());
        newStation.setFileMapping(oldStation.getFileMapping());
        newStation.setFileMappingDesc(oldStation.getFileMappingDesc());
        newStation.setFrequencyReceivingData(oldStation.getFrequencyReceivingData());
        newStation.setFtpConnectionStatus(oldStation.getFtpConnectionStatus());
        newStation.setFtpId(oldStation.getFtpId());
        newStation.setImplementAgencyRa(oldStation.getImplementAgencyRa());
        newStation.setIntervalScan(oldStation.getIntervalScan());
        newStation.setIsPublic(oldStation.getIsPublic());
        newStation.setIsPublicDataType(oldStation.getIsPublicDataType());
        newStation.setIsQi(oldStation.getIsQi());
        newStation.setIsSynced(oldStation.getIsSynced());
        newStation.setLastFileContent(oldStation.getLastFileContent());
        newStation.setLastFileName(oldStation.getLastFileName());
        newStation.setLastFilePath(oldStation.getLastFilePath());
        newStation.setLastQtyAdjusting(oldStation.getLastQtyAdjusting());
        newStation.setLastQtyError(oldStation.getLastQtyError());
        newStation.setLastQtyExceed(oldStation.getLastQtyExceed());
        newStation.setLastQtyGood(oldStation.getLastQtyGood());
        newStation.setLastTime(oldStation.getLastTime());
        newStation.setLatitude(oldStation.getLatitude());
        newStation.setLoggerId(oldStation.getLoggerId());
        newStation.setLongitude(oldStation.getLongitude());
        newStation.setMqttClientId(oldStation.getMqttClientId());
        newStation.setMqttPwd(oldStation.getMqttPwd());
        newStation.setMqttUsr(oldStation.getMqttUsr());
        newStation.setOffTime(oldStation.getOffTime());
        newStation.setOrderInArea(oldStation.getOrderInArea());
        newStation.setOrderNo(oldStation.getOrderNo());
        newStation.setPathFormat(oldStation.getPathFormat());
        newStation.setPeriodRa(oldStation.getPeriodRa());
        newStation.setPhone(oldStation.getPhone());
        newStation.setProvinceId(oldStation.getProvinceId());
        newStation.setPublicTime(oldStation.getPublicTime());
        newStation.setPwd(oldStation.getPwd());
        newStation.setQi(oldStation.getQi());
        newStation.setQiAdjsutTime(oldStation.getQiAdjsutTime());
        newStation.setQiAdjust(oldStation.getQiAdjust());
        newStation.setQtyAdjusting(oldStation.getQtyAdjusting());
        newStation.setQtyError(oldStation.getQtyError());
        newStation.setQtyExceed(oldStation.getQtyExceed());
        newStation.setQtyGood(oldStation.getQtyGood());
        newStation.setRetry(oldStation.getRetry());
        newStation.setScanFailed(oldStation.getScanFailed());
        newStation.setStationCode(oldStation.getStationCode());
        newStation.setStationId(oldStation.getStationId());
        newStation.setStationName(oldStation.getStationName());
        newStation.setStationType(oldStation.getStationType());
        newStation.setStatus(oldStation.getStatus());
        newStation.setTimeCountOffline(oldStation.getTimeCountOffline());
        newStation.setTransferType(oldStation.getTransferType());
        newStation.setTwRequestSyncStationId(oldStation.getTwRequestSyncStationId());
        newStation.setUsername(oldStation.getUsername());
        newStation.setUsingStatus(oldStation.getUsingStatus());
        newStation.setStation_pictures(oldStation.getStation_pictures());
        newStation.setIsPreferred(oldStation.getIsPreferred());
        newStation.setVerificationDeadline(oldStation.getVerificationDeadline());
        newStation.setReadFromDate(oldStation.getReadFromDate());
        newStation.setDataIndicators(oldStation.getDataIndicators());
        newStation.setOrganizationId(oldStation.getOrganizationId());

        return newStation;
    }
}
