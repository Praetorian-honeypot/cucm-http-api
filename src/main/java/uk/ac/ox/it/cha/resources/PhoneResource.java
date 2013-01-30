package uk.ac.ox.it.cha.resources;

import com.cisco.axl.api._8.ExecuteSQLQueryReq;
import com.cisco.axl.api._8.ExecuteSQLQueryRes;
import com.cisco.axl.api._8.GetPhoneReq;
import com.cisco.axl.api._8.GetPhoneRes;
import com.cisco.axl.api._8.StandardResponse;
import com.cisco.axl.api._8.UpdatePhoneReq;
import com.cisco.axl.api._8.XSpeeddial;
import com.cisco.axlapiservice.AXLPort;
import com.yammer.dropwizard.jersey.params.IntParam;
import java.util.ArrayList;
import java.util.List;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.ws.soap.SOAPFaultException;
import org.w3c.dom.Node;
import uk.ac.ox.it.cha.representations.Phone;
import uk.ac.ox.it.cha.representations.Speeddial;

/**
 *
 * @author martinfilliau
 */
@Path("/phone")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PhoneResource {
    
    private final AXLPort axlService;
    
    public PhoneResource(AXLPort axlService) {
        this.axlService = axlService;
    }
    
    @GET
    /**
     * Get phone information from a directory number
     * @param dirn directory number
     * @return 
     */
    public List<Phone> get(@QueryParam("dirn") IntParam dirn) {
        List<String> phoneNames = this.findPhonesByDirN(dirn.toString());
        if(phoneNames.isEmpty()) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        List<Phone> phones = new ArrayList<Phone>();
        for(String phoneName : phoneNames) {
            phones.add(getPhoneInfo(phoneName));
        }
        return phones;
    }
    
    /**
     * Update the speeddials for the given directory number
     * @param dirn query parameter: dir number
     * @param speeddials list of Speeddial
     * @return response code from the SOAP web service
     */
    @POST
    @Path("/speeddials")
    public Response updateSpeeddials(@QueryParam("dirn") IntParam dirn,
            @QueryParam("phone") String phone,
            @Valid List<Speeddial> speeddials) {
        if(phone != null && dirn != null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("You have to specify either the 'phone' or 'dirn' param, not both of them.")
                    .build();
        }
        if(phone != null) {
            String response = updateSpeeddialyByPhone(phone, speeddials);
            return Response.ok(response).build();
        } else {
            List<String> phoneNames = this.findPhonesByDirN(dirn.toString());
            if(phoneNames.isEmpty()) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }
            StringBuilder sb = new StringBuilder();
            for(String name : phoneNames) {
                sb.append(updateSpeeddialyByPhone(name, speeddials));
            }
            return Response.ok(sb.toString()).build();
        }
    }
    
    /**
     * Do an update phone request to the given phone name to update speeddials
     * @param phoneName name of the phone
     * @param speeddials list of speeddials
     * @return String representing the UUID of this action
     */
    private String updateSpeeddialyByPhone(String phoneName, List<Speeddial> speeddials) {
        UpdatePhoneReq upr = new UpdatePhoneReq();
        upr.setName(phoneName);
        UpdatePhoneReq.Speeddials sds = new UpdatePhoneReq.Speeddials();
        for(Speeddial sd : speeddials) {
            sds.getSpeeddial().add(sd.getXSpeeddial());
        }
        upr.setSpeeddials(sds);
        StandardResponse response = this.axlService.updatePhone(upr);
        return response.getReturn();
    }

    /**
     * Find the name of a phone by its directory number
     * @param dirn directory number to search for
     * @return list of phone name as a String, null if it couldn't be found
     */
    private List<String> findPhonesByDirN(String dirn) {
        List<String> phones = new ArrayList<String>();
        try {
            ExecuteSQLQueryReq sql = new ExecuteSQLQueryReq();
            sql.setSql("SELECT D.Name FROM NumPlan NP, DeviceNumPlanMap DNPMap, Device D WHERE NP.DNorPattern = '" + dirn + "' AND DNPMap.fkNumPlan = NP.pkid AND D.pkid = DNPMap.fkDevice");

            ExecuteSQLQueryRes res = this.axlService.executeSQLQuery(sql);
            for(Object o : res.getReturn().getRow()) {
                com.sun.org.apache.xerces.internal.dom.ElementNSImpl element = (com.sun.org.apache.xerces.internal.dom.ElementNSImpl) o;
                Node n;
                for(int i = 0; i < element.getChildNodes().getLength(); i++) {
                    n = element.getChildNodes().item(i);
                    phones.add(n.getTextContent());
                }
            }
        } catch (SOAPFaultException sfe) {
        }
        return phones;
    }
    
    /**
     * Get phone information by phone name
     * @param phoneName name of the phone
     * @return Phone object
     */
    private Phone getPhoneInfo(String phoneName) {
        GetPhoneReq axlParams = new GetPhoneReq();
        axlParams.setName(phoneName);
        GetPhoneRes getPhoneResponse = this.axlService.getPhone(axlParams);
        return new Phone(getPhoneResponse);
    }
    
}
