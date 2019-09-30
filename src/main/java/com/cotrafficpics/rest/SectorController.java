package com.cotrafficpics.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.cotrafficpics.dtos.AggregateCamData;
import com.cotrafficpics.dtos.AggregateCamsForXRoad;
import com.cotrafficpics.dtos.CamData;
import com.cotrafficpics.dtos.SectorsDTO;
import com.cotrafficpics.hibernate.dao.CameraPropsV2HibernateDAO;
import com.cotrafficpics.hibernate.dao.SectorsHibernateDAO;
import com.cotrafficpics.hibernate.factory.DAOFactory;
import com.cotrafficpics.hibernate.factory.HibernateDAOFactory;
import com.cotrafficpics.utils.AggregateCamDataUtil;
import com.cotrafficpics.xhibernate.pojos.CameraPropsV2;
import com.cotrafficpics.xhibernate.pojos.Sectors;
import com.cotrafficpics.xhibernate.util.HibernateUtil;
import com.hephaestus.infratypes.data.Pair;

/**
 * Defines available regions for interacting with SectorController.
 * TODO provide a method to create and manage personal regions. 
 * 
 * @author jlatsko
 *
 */
@RestController
public class SectorController {
	private static final Logger log = LogManager.getLogger(SectorController.class);
	
	// 404
	@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "Invalid URL parameters")
	public class InvalidParamException extends RuntimeException {
		InvalidParamException(String sector) {};
	}
	
	// 204
	@ResponseStatus(value=HttpStatus.NO_CONTENT, reason="No Camera found for id")
	public class NotFoundException extends RuntimeException {
		NotFoundException() {}
	}
	
	/**
	 * return list of available sector names
	 * 
	 * @param 
	 * @return
	 */
	@RequestMapping(value = "getSectors", method = RequestMethod.GET, produces="application/json")
	public SectorsDTO getAllSectors()
	{
		log.info("Inside getAllSectors");
		SectorsHibernateDAO dao = new SectorsHibernateDAO();
		dao.setSession(HibernateUtil.getSessionFactory().getCurrentSession());
		HibernateUtil.getSessionFactory().getCurrentSession().beginTransaction();
		
		List<Sectors> sectors = dao.findAll();
		
		List<String> sectorNames = new ArrayList<String>();
		for (Sectors sector : sectors)
		{
			sectorNames.add(sector.getSectorName());
		}
		
		SectorsDTO dto = new SectorsDTO();
		dto.setSectorNames(sectorNames);
		HibernateUtil.getSessionFactory().getCurrentSession().getTransaction().commit();
		HibernateUtil.getSessionFactory().getCurrentSession().close();
		return dto;
	}
	
	/**
	 * return cameras for the sector. 
	 * @param 
	 * @return List<Map<>> is a Wavemaker json construct. Probably more json than WM.
	 * 
	 * @RequestMapping(value = "getContacts", method = RequestMethod.GET, produces = "application/json")
	 * public @ResponseBody
	 * List<Map<String, Object>> getContacts() 
	 */
	@RequestMapping(value = "getCameras/sector/{sector}", method = RequestMethod.GET, produces="application/json")
	public @ResponseBody List<CamData> getCamerasForSector(@PathVariable("sector") String sector)
	{	
		if (sector == null) {
			log.error("NULL sector passed into getCamerasForSector");
			throw new InvalidParamException(sector);
		}
		
		List<CamData> response = new ArrayList<CamData>();
		List<CameraPropsV2> cameras = null;
		Session session = HibernateUtil.getSessionFactory().getCurrentSession();
		try
		{	
			CameraPropsV2HibernateDAO dao = new CameraPropsV2HibernateDAO();
			dao.setSession(session);
			
			session.beginTransaction();
			System.out.println("invoking getCamerasForSector: " + sector);
			cameras = dao.getCamerasForSector(sector);
			if ((cameras==null) || (cameras.isEmpty()))
			{
				log.error("No cameras found for sector " + sector);
				throw new NotFoundException();
			}
			
			AggregateCamData acd = new AggregateCamData();

			acd = AggregateCamDataUtil.convertCameraPropsToAgData(cameras);
			if ((acd==null) || (acd.getCamDatums()==null) || acd.getCamDatums().isEmpty())
			{
				log.error("Conversion of cameras to AggregateCamData Failed " + sector);
				throw new NotFoundException();
			}
			
			for (Pair<Integer, CamData> data : acd.getCamDatums())
			{
//				Map<Integer, Object> map = new HashMap();
//				map.put(data.getFirst(), data.getSecond());
				response.add(data.getSecond());
			}
			
			session.getTransaction().commit();
		}
		catch (Exception ex) {
			log.error(ex);
			if (session.getTransaction()!=null) 
				session.getTransaction().rollback();
			
			throw new NotFoundException();
		} finally {
			if (session.isOpen())
				session.close();
		}
		return response;
	}
	
	
	/**
	 * return cameras for the sector formated as a function of xroad
	 * @param 
	 * @return List<Map<>> is a Wavemaker json construct. Probably more json than WM.
	 * 
	 * @RequestMapping(value = "getContacts", method = RequestMethod.GET, produces = "application/json")
	 * public @ResponseBody
	 * List<Map<String, Object>> getContacts() 
	 */
	@RequestMapping(value = "getCamerasXRoadFmt/sector/{sector}", method = RequestMethod.GET, produces="application/json")
	public @ResponseBody Map<String, List<CamData>> getCamerasXRoadFmtForSector(@PathVariable("sector") String sector)
	{	
		if (sector == null) {
			log.error("NULL sector passed into getCamerasForSector");
			throw new InvalidParamException(sector);
		}
		
		AggregateCamsForXRoad acd = new AggregateCamsForXRoad();
		List<CameraPropsV2> cameras = null;
		Session session = HibernateUtil.getSessionFactory().getCurrentSession();
		try
		{	
			DAOFactory daoFactory = new HibernateDAOFactory();
			CameraPropsV2HibernateDAO dao = (CameraPropsV2HibernateDAO)daoFactory.getCameraPropsV2DAO();

			dao.setSession(session);
			
			session.beginTransaction();
			
			cameras = dao.getCamerasForSector(sector);
			if ((cameras==null) || (cameras.isEmpty()))
			{
				log.error("No cameras found for sector " + sector);
				throw new NotFoundException();
			}

			acd = AggregateCamDataUtil.convertCameraPropsToXRoadFmt(cameras);
			if ((acd==null) || (acd.getCamsForXRoadMap()==null) || acd.getCamsForXRoadMap().isEmpty())
			{
				log.error("Conversion of cameras to AggregateCamsForXRoad Failed " + sector);
				throw new NotFoundException();
			}
			
			session.getTransaction().commit();
		}
		catch (Exception ex) {
			log.error(ex);
			if (session.getTransaction()!=null) 
				session.getTransaction().rollback();
			
			throw new NotFoundException();
		} finally {
			if (session.isOpen())
				session.close();
		}
		return acd.getCamsForXRoadMap();
	}
}
