package com.cotrafficpics.rest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.cotrafficpics.dtos.AggregateCamData;
import com.cotrafficpics.dtos.CamData;
import com.cotrafficpics.impl.RoadService;
import com.hephaestus.infratypes.data.GeoLoc;
import com.hephaestus.infratypes.data.Pair;
/**
 * RegionController works with data in highly congested
 * areas where there are lots of cameras. Example region/Denver/highway/I-25
 * @author jlatsko
 *
 */
@RestController
public class GeoController
{
	private static final Logger log = LoggerFactory.getLogger(GeoController.class);

	@Autowired RoadService geoBO;


	
	/**
	 * return cameras for the region and highway. We use look ahead distance (how
	 * far to look for data in miles) and direction of travel to limit the result
	 * set. The image closet to the point of origin will be first one in the list
	 * Reference: http://stackoverflow.com/questions/16332092/spring-mvc-pathvariable-with-dot-is-getting-truncated
	 * regarding the regex expression on longitude
	 * @param 
	 * @return Wavemaker complianceRest return List<Map<String, Object>> for getContacts and returns Contact pojo for getContact
	 */
	@RequestMapping(value = "getCameras/{latitude}/{longitude:.+}", method = RequestMethod.GET, produces="application/json")
	public @ResponseBody List<Map<Integer, Object>> getCamerasForGeo(@PathVariable("latitude") String latitude, @PathVariable("longitude") String longitude)
	{
		List<Map<Integer, Object>> response = new ArrayList<Map<Integer, Object>>();
		System.out.println("enter getCamerasForGeo, lat/long: " + latitude + " " + longitude);
		try
		{
			GeoLoc location = new GeoLoc(new Float(latitude), new Float(longitude));
			
			AggregateCamData aggData = geoBO.retrieveDetailsForGeo(location, true);
			log.info("Number of cameras found: " + aggData.getCamDatums().size());
			for (Pair<Integer, CamData> data : aggData.getCamDatums())
			{
				Map<Integer, Object> map = new HashMap();
				map.put(data.getFirst(), data.getSecond());
				response.add(map);
			}
		}
		catch (Exception ex)
		{
			log.error("",ex);
		}
		return response;
	}
}
