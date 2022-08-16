package org.folio.dew.service;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.With;

import java.util.List;

@Setter
@Getter
@With
@AllArgsConstructor
@NoArgsConstructor
public class UpdatesResult<T> {
  private int total;
  private List<T> itemsForPreview;
  private List<T> usersForPreview;
}
