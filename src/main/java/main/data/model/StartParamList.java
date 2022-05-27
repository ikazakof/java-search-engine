package main.data.model;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
@Getter
@Component
@ConfigurationProperties(prefix = "started")
public class StartParamList {

    private List<Site> sites = new ArrayList<>();

    private List<Field> field = new ArrayList<>();




}
