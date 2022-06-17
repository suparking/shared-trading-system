package cn.suparking.data.dao.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiscountInfoDO implements Serializable {

    private static final long serialVersionUID = -2859712365121070033L;

    private String discountNo;

    private ValueType valueType;

    private Integer value;

    private Integer quantity;

    private String usedStartTime;

    private String usedEndTime;
}
