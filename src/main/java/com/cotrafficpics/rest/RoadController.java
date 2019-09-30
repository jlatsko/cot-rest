package com.cotrafficpics.rest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriTemplate;

import com.cotrafficpics.dtos.AggregateCamData;
import com.cotrafficpics.dtos.CamData;
import com.cotrafficpics.hibernate.dao.CameraPropsV2DAO;
import com.cotrafficpics.hibernate.dao.HighwaysDAO;
import com.cotrafficpics.hibernate.factory.DAOFactory;
import com.cotrafficpics.hibernate.factory.HibernateDAOFactory;
import com.cotrafficpics.impl.RoadService;
import com.cotrafficpics.impl.RoadServiceIf;
import com.cotrafficpics.utils.AggregateCamDataUtil;
import com.cotrafficpics.xhibernate.pojos.CameraPropsV2;
import com.cotrafficpics.xhibernate.pojos.Highways;
import com.cotrafficpics.xhibernate.util.HibernateUtil;
import com.hephaestus.infratypes.data.Pair;




@RestController
public class RoadController {
	private static final Logger log = LogManager.getLogger(RoadController.class);
	
	private DAOFactory daoFactory = new HibernateDAOFactory();

	/**
	 * return cameras for the region and highway. We use look ahead distance (how
	 * far to look for data in miles) and direction of travel to limit the result
	 * set.
	 * 
	 * @param 
	 * @return a map of 
	 */
	@RequestMapping(value = "getCameras/highway/{hwy}", method = RequestMethod.GET, produces="application/json")
	public @ResponseBody List<CamData> getCamerasForHwy(@PathVariable("hwy") String hwy)
	{
		List<CamData> response = new ArrayList<CamData>(); 
		try
		{	
			RoadServiceIf midtier = new RoadService();
			AggregateCamData aggData = midtier.retrieveDetailsForHwy(hwy, false);
			
			for (Pair<Integer, CamData> data : aggData.getCamDatums())
				response.add(data.getSecond());
			
		}
		catch (Exception ex)
		{
			log.error(ex);
		}
		return response;
	}
	
	/**
	 * getCamera
	 * @param camId
	 * @return
	 */
	@RequestMapping(value = "/camera/{camId}", method = RequestMethod.GET, produces="application/json")
	public @ResponseBody CamData getCamara(@PathVariable("camId") String camId)
	{
		log.debug("entered getCameras for camId: " + camId);
		CameraPropsV2DAO dao = daoFactory.getCameraPropsV2DAO();
		HibernateUtil.getSessionFactory().getCurrentSession().beginTransaction();
				
		Integer id = null;
		try {
		  id = new Integer(camId);
		} catch (NumberFormatException nfe) {
			log.error("Cannot Cast " + camId + " into Integer");
			return null;
		}
		
		CameraPropsV2 cp = dao.findById(id);
		CamData response = AggregateCamDataUtil.convertToCamData(cp);
		HibernateUtil.getSessionFactory().getCurrentSession().getTransaction().commit();
		return response;
	}
	
	@RequestMapping(value = "/getHighways", method = RequestMethod.GET, produces="application/json")
	public @ResponseBody List<Highways> getHighways()
	{
		HighwaysDAO dao = daoFactory.getHighwaysDAO();
		HibernateUtil.getSessionFactory().getCurrentSession().beginTransaction();
		
		List<Highways> response = dao.findAll();
		HibernateUtil.getSessionFactory().getCurrentSession().getTransaction().commit();
		return response;
	}
	
	/**
	 * updateCamera
	 * @param camId
	 * @param camera
	 * @param url
	 * @return
	 */
//    @RequestMapping(value="updatecamera", method=RequestMethod.POST)
//    @ResponseStatus(HttpStatus.OK)
//    public HttpEntity<?> updateCamera(@RequestBody CameraPropsV2 camera,
//            						  @Value("#{request.requestURL}") StringBuffer url) 
//    {
//    	CameraPropsV2HibernateDAO dao = (CameraPropsV2HibernateDAO)daoFactory.getCameraPropsV2DAO();
//    	dao.makePersistent(camera);
//    	
//        HttpHeaders headers = new HttpHeaders();
//        headers.add("Location", getLocationForChildResource(url, camera.getCamId().toString()));
//        return new HttpEntity<String>(headers);
//    }

    /**
     * Determines URL of child resource based on the full URL of the given request,
     * appending the path info with the given childIdentifier using a UriTemplate.
     */
    protected static String getLocationForChildResource(StringBuffer url, Object childIdentifier) {
            UriTemplate template = new UriTemplate(url.append("/{childId}").toString());
            return template.expand(childIdentifier).toASCIIString();
    }

}
