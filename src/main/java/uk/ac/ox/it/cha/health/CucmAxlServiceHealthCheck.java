package uk.ac.ox.it.cha.health;

import com.cisco.axl.api._8.GetOSVersionReq;
import com.cisco.axl.api._8.GetOSVersionRes;
import com.cisco.axlapiservice.AXLPort;
import com.yammer.metrics.core.HealthCheck;

/**
 * Health check to CUCM AXL Service
 * @author martinfilliau
 */
public class CucmAxlServiceHealthCheck extends HealthCheck {

    private AXLPort service;
    
    public CucmAxlServiceHealthCheck(AXLPort service) {
        super("CucmAxlService");
        this.service = service;
    }

    /**
     * Make a query to get the OS version
     * @return healthy if there was no exception else unhealthy
     */
    @Override
    protected Result check() throws Exception {   
        try {
            GetOSVersionRes res = this.service.getOSVersion(new GetOSVersionReq());
            // only return the OS name to avoid any potential
            // security problem if the version was exposed...
            return Result.healthy(res.getReturn().getOs().getOsName());
        } catch(Exception e) {
            return Result.unhealthy(e.getMessage());
        }
    }
    
}
