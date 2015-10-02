package com.github.javasrc.activedirectory;

import java.util.ArrayList;
import java.util.List;

public class ActiveDirectoryUser {

	private List<String> organizations = new ArrayList<String>();
	private String commonName;
	private String company;
	private String department;
	private String DN;
	private String email;
	private String familyName;
	private String givenName;
	private String manager;
	private String phone;
	private String userId;

	public List<String> getOrganizations() {
		return organizations;
	}

	public void setOrganizations(List<String> list) {
		this.organizations = list;
	}

	public String getCommonName() {
		return commonName;
	}

	public void setCommonName(String commonName) {
		this.commonName = commonName;
	}

	public String getCompany() {
		return company;
	}

	public void setCompany(String company) {
		this.company = company;
	}

	public String getDepartment() {
		return department;
	}

	public void setDepartment(String department) {
		this.department = department;
	}

	public String getDN() {
		return DN;
	}

	public void setDN(String dN) {
		DN = dN;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	@Override
	public String toString() {
		return "ActiveDirectoryUser [organizations=" + organizations + ", commonName=" + commonName + ", company=" + company + ", department=" + department
				+ ", DN=" + DN + ", email=" + email + ", familyName=" + familyName + ", givenName=" + givenName + ", manager=" + manager + ", phone=" + phone
				+ ", userId=" + userId + "]";
	}

	public String getFamilyName() {
		return familyName;
	}

	public void setFamilyName(String familyName) {
		this.familyName = familyName;
	}

	public String getGivenName() {
		return givenName;
	}

	public void setGivenName(String givenName) {
		this.givenName = givenName;
	}

	public String getManager() {
		return manager;
	}

	public void setManager(String manager) {
		this.manager = manager;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

}
