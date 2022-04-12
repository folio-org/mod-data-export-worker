package org.folio.dew.utils;

import static org.apache.commons.lang3.StringUtils.EMPTY;

import com.opencsv.bean.BeanField;
import com.opencsv.bean.ColumnPositionMappingStrategy;
import com.opencsv.bean.CsvBindByName;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;

public class RecordColumnMappingStrategy<T> extends ColumnPositionMappingStrategy<T> {
  @Override
  public String[] generateHeader(T bean) throws CsvRequiredFieldEmptyException {
    super.generateHeader(bean);

    return getFieldMap().values().stream()
      .map(this::extractHeaderName)
      .toArray(String[]::new);
  }

  private String extractHeaderName(BeanField<T, ? extends Object> beanField) {
    return  beanField == null || beanField.getField() == null || beanField.getField().getDeclaredAnnotationsByType(CsvBindByName.class).length == 0 ?
      EMPTY :
      beanField.getField().getDeclaredAnnotationsByType(CsvBindByName.class)[0].column();
  }
}
