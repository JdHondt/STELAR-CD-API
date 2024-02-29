package io.github.cdapi;

import io.swagger.annotations.Api;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/cd")
@Api(value = "Correlation Detective API", description = "Main API for running Correlation Detective jobs")
public class CdController {


}
