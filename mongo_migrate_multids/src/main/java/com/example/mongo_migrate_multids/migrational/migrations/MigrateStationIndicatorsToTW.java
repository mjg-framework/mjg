package com.example.mongo_migrate_multids.migrational.migrations;

import com.example.mjg.annotations.ForEachRecordFrom;
import com.example.mjg.annotations.MatchWith;
import com.example.mjg.annotations.Migration;
import com.example.mjg.annotations.TransformAndSaveTo;
import com.example.mjg.config.Cardinality;
import com.example.mjg.config.ErrorResolution;
import com.example.mjg.spring.filtering.SpringRepositoryFilterSet;
import com.example.mjg.storage.DataStoreRegistry;
import com.example.mongo_migrate_multids.entity.IndicatorEntity;
import com.example.mongo_migrate_multids.entity.StationEntity;
import com.example.mongo_migrate_multids.entity.StationIndicatorEntity;
import com.example.mongo_migrate_multids.helpers.ObjectIdHelpers;
import com.example.mongo_migrate_multids.migrational.datastores.dest.DestIndicatorStore;
import com.example.mongo_migrate_multids.migrational.datastores.dest.DestStationIndicatorStore;
import com.example.mongo_migrate_multids.migrational.datastores.dest.DestStationStore;
import com.example.mongo_migrate_multids.migrational.datastores.src.SrcIndicatorStore;
import com.example.mongo_migrate_multids.migrational.datastores.src.SrcStationIndicatorStore;
import com.example.mongo_migrate_multids.migrational.datastores.src.SrcStationStore;
import com.example.mongo_migrate_multids.repository.dest.DestIndicatorRepository;
import com.example.mongo_migrate_multids.repository.dest.DestStationRepository;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Migration
@ForEachRecordFrom(
    value = SrcStationIndicatorStore.class,
    inCaseOfError = @ErrorResolution(retryTimes = 3, retryDelayInSeconds = 2)
)
@MatchWith(
    // lấy station code từ station id cũ
    value = SrcStationStore.class,
    inCaseOfError = @ErrorResolution(retryTimes = 3, retryDelayInSeconds = 2),
    order = 0,
    cardinality = Cardinality.ZERO_OR_ONE
)
@MatchWith(
    // lấy indicator code từ indicator id cũ
    value = SrcIndicatorStore.class,
    inCaseOfError = @ErrorResolution(retryTimes = 3, retryDelayInSeconds = 2),
    order = 0,
    cardinality = Cardinality.ZERO_OR_ONE
)
@MatchWith(
    // tra station code lấy được station id mới
    value = DestStationStore.class,
    inCaseOfError = @ErrorResolution(retryTimes = 3, retryDelayInSeconds = 2),
    order = 1,
    cardinality = Cardinality.ZERO_OR_ONE
)
@MatchWith(
    // tra indicator code lấy được indicator id mới
    value = DestIndicatorStore.class,
    inCaseOfError = @ErrorResolution(retryTimes = 3, retryDelayInSeconds = 2),
    order = 1,
    cardinality = Cardinality.ZERO_OR_ONE
)
@TransformAndSaveTo(
    // lưu station indicator với (station id) và (indicator id) mới
    value = DestStationIndicatorStore.class,
    cardinality = Cardinality.EXACTLY_ONE,
    inCaseOfError = @ErrorResolution(retryTimes = 3, retryDelayInSeconds = 2)
)
public class MigrateStationIndicatorsToTW {
    public void startReduction(
        StationIndicatorEntity stationIndicator,
        Map<String, Object> aggregates
    ) {
        aggregates.put("stationCode", (String) null);
        aggregates.put("newStationId", (String) null);
        aggregates.put("newStationName", (String) null);
        aggregates.put("newStationType", (Integer) null);

        aggregates.put("indicatorCode", (String) null);
        aggregates.put("indicatorType", (Integer) null);
        aggregates.put("newIndicatorId", (String) null);
    }

    // lấy station code từ station id cũ,
    // tức là phải lấy được station ra trước
    public SpringRepositoryFilterSet<StationEntity, String> matchWithSrcStationStore(
        StationIndicatorEntity stationIndicator,
        Map<String, Object> aggregates,
        SrcStationStore srcStationStore
    ) {
        String oldStationId = stationIndicator.getStationId();
        if (oldStationId == null || oldStationId.isEmpty()) {
            // Không gọi repo filter, và cũng không gọi reduce... ở dưới
            return null;
        }

        return SpringRepositoryFilterSet.findAllByIdIn(
            Set.of(ObjectIdHelpers.convertLargeIntegerToStringId(oldStationId))
        );
    }
    public void reduceFromSrcStationStore(
        Map<String, Object> aggregates,
        List<StationEntity> moreMatchingSrcStations
    ) {
        StationEntity first = moreMatchingSrcStations.get(0);
        aggregates.put("stationCode", first.getStationCode());
    }


    // lấy indicator code từ indicator id cũ,
    // tức là phải lấy được các IndicatorEntity cũ đó trước
    public SpringRepositoryFilterSet<IndicatorEntity, String> matchWithSrcIndicatorStore(
        StationIndicatorEntity stationIndicator,
        Map<String, Object> aggregates,
        SrcIndicatorStore srcIndicatorStore
    ) {
        String oldIndicatorId = stationIndicator.getIndicatorId();
        if (oldIndicatorId == null || oldIndicatorId.isEmpty()) {
            // Không gọi repo filter, và cũng không gọi reduce... ỏ dưới
            return null;
        }
        return SpringRepositoryFilterSet.findAllByIdIn(
            Set.of(ObjectIdHelpers.convertLargeIntegerToStringId(oldIndicatorId))
        );
    }
    public void reduceFromSrcIndicatorStore(
        Map<String, Object> aggregates,
        List<IndicatorEntity> moreMatchingSrcIndicators
    ) {
        IndicatorEntity first = moreMatchingSrcIndicators.get(0);
        aggregates.put("indicatorCode", first.getIndicator());
        aggregates.put("indicatorType", first.getIndicatorType());
    }


    ////////////////////////
    // sau đó (order = 1) //
    ////////////////////////


    // tra station code lấy được station id mới
    public SpringRepositoryFilterSet<StationEntity, String> matchWithDestStationStore(
        StationIndicatorEntity stationIndicator,
        Map<String, Object> aggregates,
        DestStationStore destStationStore
    ) {
        String stationCode = (String) aggregates.get("stationCode");
        if (stationCode == null || stationCode.isEmpty()) {
            // Không có stationCode, không tra được station id mới...
            return null;
        }
        return SpringRepositoryFilterSet.of(
            DestStationRepository::findAllByStationCodeIn,
            Set.of(stationCode)
        );
    }
    public void reduceFromDestStationStore(
        Map<String, Object> aggregates,
        List<StationEntity> moreMatchingDestStations
    ) {
        StationEntity first = moreMatchingDestStations.get(0);
        if (first.getId() != null && !first.getId().isEmpty()) {
            aggregates.put("newStationId", ObjectIdHelpers.convertObjectIdToLargeInteger(first.getId()));
            aggregates.put("newStationName", first.getStationName());
            aggregates.put("newStationType", first.getStationType());
        }
    }


    // tra indicator code lấy được indicator id mới
    public SpringRepositoryFilterSet<IndicatorEntity, String> matchWithDestIndicatorStore(
        StationIndicatorEntity stationIndicator,
        Map<String, Object> aggregates,
        DestIndicatorStore destIndicatorStore
    ) {
        String indicatorCode = (String) aggregates.get("indicatorCode");
        Integer indicatorType = (Integer) aggregates.get("indicatorType");
        if (indicatorCode == null || indicatorCode.isEmpty() || indicatorType == null) {
            // Không có indicatorCode, nên không tra được indicator id mới...
            return null;
        }

        return SpringRepositoryFilterSet.of(
            DestIndicatorRepository::findAllByIndicatorInAndIndicatorTypeIn,
            Set.of(indicatorCode),
            Set.of(indicatorType)
        );
    }
    public void reduceFromDestIndicatorStore(
        Map<String, Object> aggregates,
        List<IndicatorEntity> moreMatchingDestIndicators
    ) {
        IndicatorEntity first = moreMatchingDestIndicators.get(0);
        if (first.getId() != null && !first.getId().isEmpty()) {
            aggregates.put("newIndicatorId", ObjectIdHelpers.convertObjectIdToLargeInteger(first.getId()));
        }
    }




    // Phew! Transform from all those aggregates we have formed so far
    public List<StationIndicatorEntity> transform(
        Map<String, Object> aggregates,
        StationIndicatorEntity oldStationIndicator
    ) {
        StationIndicatorEntity newStationIndicator = copyAllExceptIdAndIndicatorIdAndStationIdAndStationNameAndStationType(
            oldStationIndicator
        );

        String newStationId = (String) aggregates.get("newStationId");
        String newStationName = (String) aggregates.get("newStationName");
        Integer newStationType = (Integer) aggregates.get("newStationType");
        String newIndicatorId = (String) aggregates.get("newIndicatorId");

        if (newStationId != null && !newStationId.isEmpty()) {
            newStationIndicator.setStationId(newStationId);
            newStationIndicator.setStationName(newStationName);
            newStationIndicator.setStationType(newStationType);
        }

        if (newIndicatorId != null && !newIndicatorId.isEmpty()) {
            newStationIndicator.setIndicatorId(newIndicatorId);
        }

        return List.of(newStationIndicator);
    }

    public List<StationIndicatorEntity> handleDuplicate(
        StationIndicatorEntity inputRecord,
        List<StationIndicatorEntity> outputRecords,
        SrcStationIndicatorStore srcStationIndicatorStore,
        DestStationIndicatorStore destStationIndicatorStore,
        DataStoreRegistry dataStoreRegistry
    ) {
        // Do not handle duplicate error
        return null;
    }





    private static StationIndicatorEntity copyAllExceptIdAndIndicatorIdAndStationIdAndStationNameAndStationType(
        StationIndicatorEntity oldStationIndicator
    ) {
        StationIndicatorEntity newStationIndicator = new StationIndicatorEntity();

        newStationIndicator.setId(null);

        newStationIndicator.setContinousEqual(oldStationIndicator.getContinousEqual());
        newStationIndicator.setContinousEqualValue(oldStationIndicator.getContinousEqualValue());
        newStationIndicator.setContinousTimes(oldStationIndicator.getContinousTimes());
        newStationIndicator.setConvertRate(oldStationIndicator.getConvertRate());
        newStationIndicator.setEqual0(oldStationIndicator.getEqual0());
        newStationIndicator.setEquipmentAdjust(oldStationIndicator.getEquipmentAdjust());
        newStationIndicator.setEquipmentId(oldStationIndicator.getEquipmentId());
        newStationIndicator.setEquipmentLrv(oldStationIndicator.getEquipmentLrv());
        newStationIndicator.setEquipmentName(oldStationIndicator.getEquipmentName());
        newStationIndicator.setEquipmentStatus(oldStationIndicator.getEquipmentStatus());
        newStationIndicator.setEquipmentUrv(oldStationIndicator.getEquipmentUrv());
        newStationIndicator.setExceedValue(oldStationIndicator.getExceedValue());
        // newStationIndicator.setIndicatorId(oldStationIndicator.getIndicatorId());
        newStationIndicator.setIsCalcQi(oldStationIndicator.getIsCalcQi());
        newStationIndicator.setIsPublic(oldStationIndicator.getIsPublic());
        newStationIndicator.setMappingName(oldStationIndicator.getMappingName());
        newStationIndicator.setNegativeValue(oldStationIndicator.getNegativeValue());
        newStationIndicator.setOutOfRange(oldStationIndicator.getOutOfRange());
        newStationIndicator.setOutOfRangeMax(oldStationIndicator.getOutOfRangeMax());
        newStationIndicator.setOutOfRangeMin(oldStationIndicator.getOutOfRangeMin());
        newStationIndicator.setPreparingValue(oldStationIndicator.getPreparingValue());
        newStationIndicator.setQcvnCode(oldStationIndicator.getQcvnCode());
        newStationIndicator.setQcvnDetailConstAreaValue(oldStationIndicator.getQcvnDetailConstAreaValue());
        newStationIndicator.setQcvnDetailId(oldStationIndicator.getQcvnDetailId());
        newStationIndicator.setQcvnDetailMaxValue(oldStationIndicator.getQcvnDetailMaxValue());
        newStationIndicator.setQcvnDetailMinValue(oldStationIndicator.getQcvnDetailMinValue());
        newStationIndicator.setQcvnDetailTypeCode(oldStationIndicator.getQcvnDetailTypeCode());
        newStationIndicator.setQcvnId(oldStationIndicator.getQcvnId());
        newStationIndicator.setQcvnKindId(oldStationIndicator.getQcvnKindId());

        // newStationIndicator.setStationId(oldStationIndicator.getStationId());
        // newStationIndicator.setStationName(oldStationIndicator.getStationName());
        // newStationIndicator.setStationType(oldStationIndicator.getStationType());

        newStationIndicator.setStatus(oldStationIndicator.getStatus());
        newStationIndicator.setTendencyValue(oldStationIndicator.getTendencyValue());
        newStationIndicator.setUnit(oldStationIndicator.getUnit());
        newStationIndicator.setRemoveWithIndicatorCheck(oldStationIndicator.getRemoveWithIndicatorCheck());
        newStationIndicator.setRemoveWithIndicator(oldStationIndicator.getRemoveWithIndicator());
        newStationIndicator.setRemoveWithIndicatorId(oldStationIndicator.getRemoveWithIndicatorId());
        newStationIndicator.setExtraordinaryValueCheck(oldStationIndicator.getExtraordinaryValueCheck());
        newStationIndicator.setExtraordinaryValue(oldStationIndicator.getExtraordinaryValue());
        newStationIndicator.setCompareValueCheck(oldStationIndicator.getCompareValueCheck());
        newStationIndicator.setCompareValue(oldStationIndicator.getCompareValue());
        newStationIndicator.setCoefficientData(oldStationIndicator.getCoefficientData());
        newStationIndicator.setParameterValue(oldStationIndicator.getParameterValue());
        newStationIndicator.setParameterValueId(oldStationIndicator.getParameterValueId());
        newStationIndicator.setK1(oldStationIndicator.getK1());
        newStationIndicator.setK2(oldStationIndicator.getK2());
        newStationIndicator.setK3(oldStationIndicator.getK3());


        return newStationIndicator;
    }
}
