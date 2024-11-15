package org.folio.dew.batch.bulkedit.jobs.permissions.check;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;

import java.util.ArrayList;
import java.util.List;

@Data
@With
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class UserPermissions {

  @JsonProperty("permissionNames")
  private List<String> permissionNames = new ArrayList<>();

  @JsonProperty("permissions")
  private List<String> permissions = new ArrayList<>();
}
