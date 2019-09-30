package com.cotrafficpics.rest;

import org.apache.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.cotrafficpics.caches.GeoSortedCameras;
import com.cotrafficpics.caches.GeoSortedUtils;
import com.cotrafficpics.dtos.CamData;
import com.cotrafficpics.hibernate.dao.CameraPropsV2DAO;
import com.cotrafficpics.hibernate.factory.DAOFactory;
import com.cotrafficpics.hibernate.factory.HibernateDAOFactory;
import com.cotrafficpics.utils.AggregateCamDataUtil;
import com.cotrafficpics.xhibernate.pojos.CameraPropsV2;
import com.cotrafficpics.xhibernate.util.HibernateUtil;

/**
 * 
 * @author jlatsko
 *
 */
@RestController
public class NearestNeighborController {
	private static final Logger log = Logger
			.getLogger(NearestNeighborController.class);

	// TODO - autowire this so that its never null
//	private Map<String, TreeSet<CameraPropsV2>> geoSortedCamMap;
	private DAOFactory daoFactory = new HibernateDAOFactory();

	// 404
	@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "Invalid URL parameters")
	public class InvalidParamException extends RuntimeException {
		InvalidParamException(String camid) {};
	}
	
	// 204
	@ResponseStatus(value=HttpStatus.NO_CONTENT, reason="No Camera found for id")
	public class CameraNotFoundException extends RuntimeException {
		CameraNotFoundException(String camid) {}
	}
	
	/**
	 * 
	 * @param camera
	 *            - user is currently viewing
	 * @param direction
	 *            - users selection, either NORTH, SOUTH, EAST or WEST
	 * @return
	 */
	@RequestMapping(value = "getNearestNeighbor/{camId}/{direction}", method = RequestMethod.GET, produces = "application/json")
	public @ResponseBody
	CamData getNearestNeighbhor(
			@PathVariable("camId") String camId,
			@PathVariable("direction") String direction) 
	{
		log.debug("entered getNearestNeighbor for camId: " + camId + " direction: " + direction);
		
		if (direction == null) {
			log.error("NULL direction passed into getNearestNeighbor");
			throw new InvalidParamException(camId);
		}
		if (camId == null) {
			log.error("NULL camId passed into getNearestNeighbor");
			throw new InvalidParamException(camId);
		}

		// determine if we have an entry for this camera's highway in the map
		CameraPropsV2DAO cdao = daoFactory.getCameraPropsV2DAO();
		CamData jsonResponse = null;
		try {
			HibernateUtil.getSessionFactory().getCurrentSession()
					.beginTransaction();

			CameraPropsV2 camera = cdao.findById(new Integer(camId));

			if (camera == null) {
				log.error("NULL camId passed into getNearestNeighbor");
				throw new CameraNotFoundException(camId);
			}

			// check the cache of sorted cameras
			String hwy = camera.getHighways().getHighwayName();
			if (GeoSortedCameras.getSortedCamMap() == null) {
				GeoSortedCameras.createTreeSetAndAdd(hwy);
			}

			if (GeoSortedCameras.getSortedCamMap().containsKey(hwy)) 
			{
				CameraPropsV2 cp = GeoSortedUtils.retrieveNextCamera(camera, direction);
				jsonResponse = AggregateCamDataUtil.convertToCamData(cp);
				if (jsonResponse == null)
					throw new CameraNotFoundException(camId);
				HibernateUtil.getSessionFactory().getCurrentSession()
						.getTransaction().commit();
			} 
			else 
			{
				GeoSortedCameras.createTreeSetAndAdd(hwy);

				CameraPropsV2 cp = GeoSortedUtils.retrieveNextCamera(camera, direction);
				jsonResponse = AggregateCamDataUtil.convertToCamData(cp);
				if (jsonResponse == null)
					throw new CameraNotFoundException(camId);
				HibernateUtil.getSessionFactory().getCurrentSession()
						.getTransaction().commit();
			}
		} catch (Exception ex) {
			log.error(ex);
			throw new CameraNotFoundException(camId);
		} finally {
			HibernateUtil.getSessionFactory().getCurrentSession().close();
		}

		return jsonResponse;
	}
	
	// TODO getNearestNeighbor lat/long and direction of travel
}
