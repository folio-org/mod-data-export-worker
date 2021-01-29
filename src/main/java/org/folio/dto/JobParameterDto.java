package org.folio.dto;

import lombok.Data;
import org.folio.model.entities.enums.ParameterType;

import java.util.Date;

@Data
public class JobParameterDto {

    private Object parameter;

    private ParameterType parameterType;

    private boolean identifying;

    public JobParameterDto() {
    }

    public JobParameterDto(String parameter, boolean identifying) {
        this(parameter);
        this.identifying = identifying;
    }

    public JobParameterDto(Long parameter, boolean identifying) {
        this(parameter);
        this.identifying = identifying;

    }

    public JobParameterDto(Date parameter, boolean identifying) {
        this(parameter);
        this.identifying = identifying;
    }

    public JobParameterDto(Double parameter, boolean identifying) {
        this(parameter);
        this.identifying = identifying;
    }

    public JobParameterDto(String parameter) {
        this.parameter = parameter;
        this.parameterType = ParameterType.STRING;
    }

    public JobParameterDto(Long parameter) {
        this.parameter = parameter;
        this.parameterType = ParameterType.LONG;
    }

    public JobParameterDto(Date parameter) {
        this.parameter = parameter;
        this.parameterType = ParameterType.DATE;
    }

    public JobParameterDto(Double parameter) {
        this.parameter = parameter;
        this.parameterType = ParameterType.DOUBLE;
    }
}
