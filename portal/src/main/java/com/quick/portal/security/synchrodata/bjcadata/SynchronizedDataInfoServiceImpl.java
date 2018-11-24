package com.quick.portal.security.synchrodata.bjcadata;
import java.net.MalformedURLException;
import javax.xml.rpc.ServiceException;
import javax.xml.rpc.ServiceException;
import org.springframework.context.ApplicationContext;
import org.springframework.remoting.jaxrpc.ServletEndpointSupport;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.UUID;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bjca.uums.client.bean.DepartmentInformation;
import com.bjca.uums.client.bean.LoginInformation;
import com.bjca.uums.client.bean.PersonInformation;
import com.bjca.uums.client.bean.RoleInformation;
import com.quick.core.base.ISysBaseDao;
import com.quick.portal.security.synchrodata.bjcadata.ISynchronizedDataService;

/*
 * 接收公服系统数据实现类
 * 
 */
@Transactional

public class SynchronizedDataInfoServiceImpl extends ServletEndpointSupport implements ISynchronizedDataInfoService {
	
//	private final java.lang.String USER_WSDL = "http://172.26.64.109:7001/uumsinterface/services/User?wsdl"; //测试地址
	private final java.lang.String USER_WSDL = "http://192.168.161.192:7001/uumsinterface/services/User?wsdl";
	
//	private final java.lang.String DEPARTMENT_WSDL = "http://172.26.64.109:7001/uumsinterface/services/Department?wsdl";//测试地址
	
	
	private final java.lang.String DEPARTMENT_WSDL = "http://192.168.161.192:7001/uumsinterface/services/Department?wsdl";
	

	private ISynchronizedDataService syncDataService;
	
	
	/** spring初始化 **/
    private ApplicationContext applicationContext;  
    
    
    
    
    @Override  
    protected void onInit() throws ServiceException {  
        // 初始化Spirng 配置  
        applicationContext = super.getApplicationContext();  
        //实例化DataSend_PortType
        syncDataService  = (ISynchronizedDataService) applicationContext.getBean("syncDataService"); 
    }



	/*
	 * OperateID:操作类型  用户11、12、13， 角色21、22、23，机构 41、42、43  
	 * OperateCode:操作码，同步用户时为用户32位码,同步机构时为机构编码,同步角色时为系统编码
	 * OperateType:此参数仅用于区分个人用户和单位用户（此参数基本不用管）,个人用户 1，单位用户 2，其它内容（角色、部门，用户角色关系） 0
	 */
	@Override
	public boolean SynchronizedUserInfo(int OperateID, String OperateCode,
			String OperateType) throws Exception {
		boolean bol = false;
		System.out.println("OperateID=======" + OperateID);
		System.out.println("OperateCode=======" + OperateCode);
		System.out.println("OperateType=======" + OperateType);
		PersonInformation person = null;
		LoginInformation loginInformation = null;
		DepartmentInformation depart = null;
		synchronizedDataInfo2(OperateID,OperateCode,OperateType);
		bol = syncDataService.synchronizedPersonData(person,loginInformation,11);
		com.bjca.uumsinterface.services.User.UserSoapBindingStub binding = null;
		com.bjca.uumsinterface.services.Department.DepartmentSoapBindingStub binding1 = null;
		java.net.URL endpoint_User = null;
		java.net.URL endpoint_Department = null;
		try {
			endpoint_User = new java.net.URL(USER_WSDL);
			endpoint_Department = new java.net.URL(DEPARTMENT_WSDL);
		} catch (MalformedURLException e1) {
			// TODO Auto-generated catch block
			throw new Exception ("访问公服WSDL异常："+e1.getMessage());
		}
		try {
			binding = (com.bjca.uumsinterface.services.User.UserSoapBindingStub) new com.bjca.uumsinterface.services.User.UserServiceLocator()
					.getUser(endpoint_User);
			binding1 = (com.bjca.uumsinterface.services.Department.DepartmentSoapBindingStub) new com.bjca.uumsinterface.services.Department.DepartmentServiceLocator()
					.getDepartment(endpoint_Department);
		} catch (javax.xml.rpc.ServiceException jre) {
		/*	if (jre.getLinkedCause() != null)
				jre.getLinkedCause().printStackTrace();
			throw new junit.framework.AssertionFailedError(
					"JAX-RPC ServiceException caught: " + jre);*/
		}
		binding.setTimeout(60000);
		binding1.setTimeout(60000);
		
		/*OperateID 
		 * 11 新增用户 、12 修改用户、 13 删除用户
		 * 21 新增角色、 22 修改角色、 23 删除角色
		 * 41 新增机构、 42 修改机构、 43 删除机构
		 * */
		 
		//同步个人用户信息
		if (OperateID == 11 || OperateID == 12 || OperateID == 13) {
//			PersonInformation person = null;
//			LoginInformation loginInformation = null;//（单独证书用户没有此对象）
			try {	
				person = binding.findPersonInfosByUserIDForDC(OperateCode);
				//需要用户的登录信息(口令用户)
				loginInformation = binding
						.getLoginInformationByUserID(OperateCode);
	
			} catch (RemoteException e) {
				bol = false;
				throw new Exception ("调用个人用户信息远程接口异常："+e.getMessage());
			}		
			Collection collection = person.getDeparts();
			Iterator it = collection.iterator();
			while (it.hasNext()) {
				 depart = (DepartmentInformation) it
						.next(); 
				System.out.println("DepartCode=" + depart.getDepartCode());
				System.out.println("Default=" + depart.getDepartDefault());
				System.out.println("DepartUpcode=" + depart.getDepartUpcode());
			}
			System.out.println("用户32位码===" + person.getUserIdcode());
			System.out.println("用户姓名===" + person.getUserName());
			System.out.println("用户身份证号码===" + person.getUserIdcardNum());
			try {
				bol = syncDataService.synchronizedPersonData(person,loginInformation,OperateID);
			} catch (Exception e) {
				bol = false;
				throw new Exception ("解析个人用户信息报文异常："+e.getMessage());
			}
		}
	
		//同步角色信息
		if (OperateID == 21 || OperateID == 22 || OperateID == 23) {
			RoleInformation roleInformation = null;
			try {
				roleInformation = binding.findRoleInfoByRoleCode(OperateCode);
				System.out.println("角色流水号"+roleInformation.getUrFlowno());
				System.out.println("角色编码"+roleInformation.getUserRoleCode());
				System.out.println("角色描述"+roleInformation.getUserRoleDescribe());
				System.out.println("角色名称"+roleInformation.getUserRoleName());
				try {
					bol = syncDataService.synchronizedRoleData(roleInformation);
				} catch (Exception e) {
					bol = false;
					throw new Exception ("解析角色信息报文异常："+e.getMessage());
				}
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				bol = false;
				throw new Exception ("调用角色信息远程接口异常："+e.getMessage());
			}
			bol = true;
		}
		
		//41 新增机构、 42 修改机构、 43 删除机构
		//同步部门信息
		if (OperateID == 41 || OperateID == 42 || OperateID == 43) {
			DepartmentInformation department = null;
			boolean isFlag = true;
			Collection collection = null;
			try {
				//个人部门信息
				department = binding1.findDepartByDepartCodeForDC(OperateCode);
				//批量部门信息
				collection = binding1.getAllDepart();
			} catch (RemoteException e) {
				bol = false;
				throw new Exception ("调用部门信息远程接口异常："+e.getMessage());
			}
			String departupcode = department.getDepartUpcode();
			String departcode = department.getDepartCode();
			String departname = department.getDepartName();
			System.out.println("部门上级编码====" + departupcode);
			System.out.println("部门编码为=====" + departcode);
			System.out.println("部门名称=====" + departname);

			Iterator it = collection.iterator();
			while (it.hasNext()) {
				 depart = (DepartmentInformation) it
						.next();
				System.out.println("DepartCode=" + depart.getDepartCode());
				System.out.println("Default=" + depart.getDepartDefault());
				System.out.println("DepartUpcode=" + depart.getDepartUpcode());
			}
			try {
				bol = syncDataService.synchronizedDeptData(department,OperateID);
			//	bol = syncDataService.synchronizedBatchDeptData(collection);
			} catch (Exception e) {
				bol = false;
				throw new Exception ("解析部门信息报文异常："+e.getMessage());
			}
		}
		return bol;
	}
	
	/*测试部门
	 * (non-Javadoc)
	 * @see com.quick.portal.security.synchrodata.bjcadata.ISynchronizedDataInfoService#synchronizedDataInfo2(int, java.lang.String, java.lang.String)
	 */
/*	@Override
	public boolean synchronizedDataInfo2(int OperateID, String OperateCode,
			String OperateType) throws Exception {
		boolean bol = false;
		DepartmentInformation department = new DepartmentInformation();
		String uuid = UUID.randomUUID().toString().replace("-", "");
		department.setDepartDefault(uuid);
		department.setDepartCode("dept_6");
		department.setDepartDescript("detp_desc1");;
		department.setDepartName("dept_name111");
		department.setDepartUpcode("dept_1");
		OperateID = 41;
		try {
			bol = syncDataService.synchronizedDeptData(department,OperateID);
		} catch (Exception e) {
			bol = false;
			throw new Exception ("解析部门信息报文异常："+e.getMessage());
		}
		return bol;
	}*/
	
	/* 测试用户信息
	 * (non-Javadoc)
	 * @see com.quick.portal.security.synchrodata.bjcadata.ISynchronizedDataInfoService#synchronizedDataInfo2(int, java.lang.String, java.lang.String)
	 */
	/*@Override
	 */
	public boolean synchronizedDataInfo2(int OperateID, String OperateCode,
			String OperateType) throws Exception {
		boolean bol = false;
		PersonInformation person = new PersonInformation();
		person.setUniqueid("test11");
		person.setUserAddress("北京1");
		person.setUserPhone("01010001000");
		person.setUserDuty("处长");
		person.setUserType("01");
        person.setUserIdcardNum("1101011990121103X");
        person.setUserNation("");
        person.setUserDegree("硕士");
        person.setUserTitle("经理2");
        person.setUserPostcode("110101");
        person.setUserMobile("12300008888");
        person.setUserEmail("chenxh@163.com");
        person.setDefault1("1");
        person.setUserDefault2("1");

        Collection collection = new ArrayList();
		DepartmentInformation departs = new DepartmentInformation();
		departs.setDepartCode("dept_2");
		departs.setDepartUpcode("dept_0");
		departs.setDepartName("dept_desc2");
		collection.add(departs);
		DepartmentInformation depart = new DepartmentInformation();
		depart.setDepartCode("dept_1");
		depart.setDepartUpcode("dept_0");
		departs.setDepartName("dept_desc1");
		collection.add(depart);
		person.setDeparts(collection);
		LoginInformation loginInformation = new LoginInformation();
		loginInformation.setLoginName("test");
		loginInformation.setLoginNickName("张三1");
		loginInformation.setLoginPwd("111111");
		OperateID = 11;
		//11 新增用户 、12 修改用户、 13 删除用户
		//41 新增机构、 42 修改机构、 43 删除机构
		try {
			bol = syncDataService.synchronizedPersonData(person,loginInformation,OperateID);
		} catch (Exception e) {
			bol = false;
			throw new Exception ("解析角色信息报文异常："+e.getMessage());
		}
		return bol;
	}


/*	public static void main(String[] args) {
		synchronizedDataInfo demoWebService = new synchronizedDataInfo();
		try {
			demoWebService.SynchronizedUserInfo(11, "270ac0884f4459c702139d77bec93e2a", "1");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}*/
	
	


}
