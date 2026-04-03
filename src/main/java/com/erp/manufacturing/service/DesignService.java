package com.erp.manufacturing.service;

import com.erp.manufacturing.entity.Design;

public interface DesignService {
    void submitDesign(Design design, Long orderId, String username);

    java.util.List<String> getDistinctDieIds();

    java.util.List<String> getDistinctPalleteIds();
}
