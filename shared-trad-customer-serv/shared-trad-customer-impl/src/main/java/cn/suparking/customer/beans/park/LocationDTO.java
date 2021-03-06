package cn.suparking.customer.beans.park;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationDTO {

    /**
     * 维度.
     */
    @NotNull
    private Double latitude;

    /**
     * 经度.
     */
    @NotNull
    private Double longitude;

    /**
     * 获取场库个数.
     */
    @NotNull
    @Max(10)
    @Min(1)
    private Integer number;

    @NotNull
    @Min(5000)
    private Integer radius;
}
