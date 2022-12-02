package org.folio.dew.client;

import org.folio.dew.config.feign.FeignClientConfiguration;
import org.folio.dew.domain.dto.Department;
import org.folio.dew.domain.dto.DepartmentCollection;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "departments", configuration = FeignClientConfiguration.class)
public interface DepartmentClient {

  @GetMapping(value = "/{deptId}", produces = MediaType.APPLICATION_JSON_VALUE)
  Department getDepartmentById(@PathVariable String deptId);

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  DepartmentCollection getDepartmentByQuery(@RequestParam String query);
}
