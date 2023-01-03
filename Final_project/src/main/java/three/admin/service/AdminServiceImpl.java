package three.admin.service;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import three.admin.mapper.AdminMapper;
import three.donation.model.DonationOrgVO;
import three.product.model.ProductVO;
import three.user.model.UserVO;

@Service
public class AdminServiceImpl implements AdminService {
	
	@Inject
	private AdminMapper adminMapper;
	
	@Override
	public List<DonationOrgVO> dOrgList() {
		return adminMapper.dOrgList();
	}

	@Override
	public List<UserVO> userList() {
		return adminMapper.userList();
	}

	@Override
	public List<ProductVO> productList() {
		return adminMapper.productList();
	}

	@Override
	public DonationOrgVO mainOrg() {
		return adminMapper.mainOrg();
	}

	@Override
	public int addOrg(DonationOrgVO vo) {
		return adminMapper.addOrg(vo);
	}

	@Override
	public int dondelete(int donOrgNum) {
		return adminMapper.dondelete(donOrgNum);
	}

	@Override
	public int changeDon(int donOrgNum) {
		return adminMapper.changeDon(donOrgNum);
	}

}
