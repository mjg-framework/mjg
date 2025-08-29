package com.example.mongo_migrate_multids.entity;

import com.example.mjg.data.MigratableEntity;
import com.example.mongo_migrate_multids.helpers.ObjectIdHelpers;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(value = "station_indicator")
@Getter
@Setter
public class StationIndicatorEntity implements MigratableEntity {
    @Override
    public Object getMigratableId() {
        return id;
    }

    @Override
    public String getMigratableDescription() {
        return "StationIndicatorEntity(id=" + id + ", largeInteger=" + (
            id == null ? "null" : ObjectIdHelpers.convertObjectIdToLargeInteger(id)
        ) + ")";
    }

    @Id
    public String id;

    @Field(value = "continous_equal")
    public Boolean continousEqual;

    @Field(value = "continous_equal_value")
    public Integer continousEqualValue;

    @Field(value = "continous_times")
    public Object continousTimes;

    @Field(value = "convert_rate")
    public Double convertRate;

    @Field(value = "equal0")
    public Boolean equal0;

    @Field(value = "equipment_adjust")
    public Boolean equipmentAdjust;

    @Field(value = "equipment_id")
    public String equipmentId;

    @Field(value = "equipment_lrv")
    public String equipmentLrv;

    @Field(value = "equipment_name")
    public String equipmentName;

    @Field(value = "equipment_status")
    public Boolean equipmentStatus;

    @Field(value = "equipment_urv")
    public String equipmentUrv;

    @Field(value = "exceed_value")
    public Double exceedValue;

    @Field(value = "indicator_id")
    public String indicatorId;

    @Field(value = "is_calc_qi")
    public Boolean isCalcQi;

    @Field(value = "is_public")
    public Boolean isPublic;

    @Field(value = "mapping_name")
    public String mappingName;

    @Field(value = "negative_value")
    public Boolean negativeValue;

    @Field(value = "out_of_range")
    public Boolean outOfRange;

    @Field(value = "out_of_range_max")
    public Double outOfRangeMax;

    @Field(value = "out_of_range_min")
    public Double outOfRangeMin;

    @Field(value = "preparing_value")
    public Double preparingValue;

    @Field(value = "qcvn_code")
    public String qcvnCode;

    @Field(value = "qcvn_detail_const_area_value")
    public String qcvnDetailConstAreaValue;

    @Field(value = "qcvn_detail_id")
    public String qcvnDetailId;

    @Field(value = "qcvn_detail_max_value")
    public Double qcvnDetailMaxValue;

    @Field(value = "qcvn_detail_min_value")
    public Double qcvnDetailMinValue;

    @Field(value = "qcvn_detail_type_code")
    public String qcvnDetailTypeCode;

    @Field(value = "qcvn_id")
    public String qcvnId;

    @Field(value = "qcvn_kind_id")
    public String qcvnKindId;

    @Field(value = "station_id")
    public String stationId;

    @Field(value = "station_name")
    public String stationName;

    @Field(value = "station_type")
    public Object stationType;

    @Field(value = "status")
    public Integer status;

    @Field(value = "tendency_value")
    public Double tendencyValue;

    @Field(value = "unit")
    public String unit;

    @Field(value = "remove_with_indicator_check")
    public Boolean removeWithIndicatorCheck;

    @Field(value = "remove_with_indicator")
    public String removeWithIndicator;

    @Field(value = "remove_with_indicator_id")
    public String removeWithIndicatorId;

    // Giá Trị bất thường
    @Field(value = "extraordinary_value_check")
    public Boolean extraordinaryValueCheck;

    @Field(value = "extraordinary_value")
    public Long extraordinaryValue;

    @Field(value = "compare_value_check")
    public Boolean compareValueCheck;

    @Field(value = "compare_value")
    public String compareValue;

    @Field(value = "coefficient_data")
    public Double coefficientData;

    //thong so
    @Field(value = "parameter_value")
    public String parameterValue;

    @Field(value = "parameter_value_id")
    public String parameterValueId;

    @Field(value="k1")
    public Double k1;

    @Field(value="k2")
    public Double k2;

    @Field(value="k3")
    public Double k3;
}
