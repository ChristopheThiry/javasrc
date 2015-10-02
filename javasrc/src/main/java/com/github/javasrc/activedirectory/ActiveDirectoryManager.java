// This class was first the class ActiveDirectoryBrowser from terrancesnyder, I've modified it to get user groups
// and I also don't need to specify a ldap server as I connect on the DNS to get url of the Active Directory servers
package com.github.javasrc.activedirectory;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// TODO: if connection to the server fails use an alternative url, alternative urls are filled but for now only the first one is used when connecting on server
public class ActiveDirectoryManager {

	private ArrayList<String> activeDirectoryUrls;
	private String principle;
	private String password;
	private String organizationalUnit;

	private static Logger LOGGER = LogManager.getLogger(ActiveDirectoryManager.class);

	/**
	 * Construct an ActiveDirectoryManager using 3 parameters : username, password and organization the URL of the Active Directory is constructed automatically
	 * by retrieving informations from the DNS
	 * 
	 * @param activeDirectoryUsername
	 *            (example : CN=MyUser,OU=Miscellaneous Users,DC=github,DC=com) be best to my mind is to use the distinguishedname of the user that should
	 *            connect to the active directory
	 * @param password
	 *            from the activeDirectoryUsername
	 * @param organizationalUnit
	 *            (example : DC=github,DC=com)
	 */
	public ActiveDirectoryManager(String activeDirectoryUsername, String password, String organizationalUnit) {
		// First lookup in the DNS to get the SRV record that lists the active directory servers
		Hashtable<String, String> env = new Hashtable<String, String>();
		activeDirectoryUrls = new ArrayList<String>();
		env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
		env.put("java.naming.provider.url", "dns:");
		DirContext ctx = null;
		try {
			ctx = new InitialDirContext(env);
			Attributes attrs = ctx.getAttributes("_ldap._tcp.luxbourse.local", new String[] { "SRV" });
			Attribute srvs = attrs.get("SRV");
			// We iterate on each server to retrieve the server name and port
			// Attributes are 1/-priority 2/-weight 3/-port number 4/-server name
			for (int i = 0; i < srvs.size(); i++) {
				// Remove the . at the end of the servername, we don't need it
				String ldapServerName = ((String) srvs.get(i)).split(" ")[3].replaceAll("\\.$", "");
				String ldapPort = ((String) srvs.get(i)).split(" ")[2];
				activeDirectoryUrls.add("ldap://" + ldapServerName + ":" + ldapPort);
			}
		} catch (NamingException e) {
			LOGGER.error("Erreur de detection du serveur Active directory", e);
		}
		this.principle = activeDirectoryUsername;
		this.password = password;
		this.organizationalUnit = organizationalUnit;
	}

	/**
	 * Get an
	 * 
	 * @param query
	 * @return
	 * @throws Exception
	 */
	public List<ActiveDirectoryUser> getUser(String userAccount) throws Exception {
		String returnedAtts[] = { "distinguishedName", "sAMAccountName", "userPrincipalName", "displayName", "cn", "sn", "givenName", "mail", "department",
				"company", "manager", "telephoneNumber" };
		SearchControls searchContext = new SearchControls(SearchControls.SUBTREE_SCOPE, 0, 0, returnedAtts, false, false);

		List<ActiveDirectoryUser> users = new ArrayList<ActiveDirectoryUser>();

		LdapContext ctx = null;
		try {
			ctx = new InitialLdapContext(getConnectionSettings(), null);
			NamingEnumeration<SearchResult> results = ctx.search(this.organizationalUnit, "samaccountname=" + userAccount, searchContext);
			while (results.hasMoreElements()) {
				SearchResult item = results.next();
				Attributes metadata = item.getAttributes();
				NamingEnumeration<String> attributes = metadata.getIDs();
				List<String> availableValues = new ArrayList<String>();
				while (attributes.hasMoreElements()) {
					availableValues.add(attributes.next());
				}

				ActiveDirectoryUser u = new ActiveDirectoryUser();
				u.setCommonName(availableValues.contains("cn") ? String.valueOf(metadata.get("cn").get()) : "");
				u.setCompany(availableValues.contains("company") ? String.valueOf(metadata.get("company").get()) : "");
				u.setDepartment(availableValues.contains("department") ? String.valueOf(metadata.get("department").get()) : "");
				u.setDN(availableValues.contains("distinguishedName") ? String.valueOf(metadata.get("distinguishedName").get()) : "");
				u.setEmail(availableValues.contains("mail") ? String.valueOf(metadata.get("mail").get()) : "");
				u.setFamilyName(availableValues.contains("sn") ? String.valueOf(metadata.get("sn").get()) : "");
				u.setGivenName(availableValues.contains("givenName") ? String.valueOf(metadata.get("givenName").get()) : "");
				u.setManager(availableValues.contains("manager") ? String.valueOf(metadata.get("manager").get()) : "");
				u.setPhone(availableValues.contains("telephoneNumber") ? String.valueOf(metadata.get("telephoneNumber").get()) : "");
				u.setUserId(availableValues.contains("sAMAccountName") ? String.valueOf(metadata.get("sAMAccountName").get()) : "");

				String[] strings = u.getDN().split(",");
				for (String string : strings) {
					u.getOrganizations().add(string);
				}
				u.setOrganizations(getUserGroups(u));
				users.add(u);

			}
		} finally {
			if (ctx != null) {
				ctx.close();
			}
		}

		return users;
	}

	private List<String> getUserGroups(ActiveDirectoryUser user) throws Exception {
		List<String> groups = new ArrayList<String>();
		String returnedAtts[] = { "tokenGroups" };
		SearchControls searchContext = new SearchControls(SearchControls.OBJECT_SCOPE, 0, 0, returnedAtts, false, false);
		StringBuffer groupsSearchFilter = new StringBuffer();
		groupsSearchFilter.append("(|");

		LdapContext ctx = null;
		try {
			ctx = new InitialLdapContext(getConnectionSettings(), null);
			NamingEnumeration<SearchResult> results = ctx.search(user.getDN(), "(&(objectClass=user))", searchContext);
			while (results.hasMoreElements()) {
				SearchResult item = results.next();
				Attributes metadata = item.getAttributes();
				Attribute attribute = metadata.get("tokenGroups");
				NamingEnumeration<?> tokens = attribute.getAll();
				while (tokens.hasMore()) {
					byte[] sid = (byte[]) tokens.next();
					groupsSearchFilter.append("(objectSid=" + binarySidToStringSid(sid) + ")");
				}
			}
			groupsSearchFilter.append(")");
			// get names of the groups
			SearchControls groupsSearchCtls = new SearchControls();
			groupsSearchCtls.setSearchScope(SearchControls.SUBTREE_SCOPE);
			String groupsReturnedAtts[] = { "sAMAccountName" };
			groupsSearchCtls.setReturningAttributes(groupsReturnedAtts);
			NamingEnumeration<?> groupsAnswer = ctx.search(organizationalUnit, groupsSearchFilter.toString(), groupsSearchCtls);
			while (groupsAnswer.hasMoreElements()) {
				SearchResult sr = (SearchResult) groupsAnswer.next();
				Attributes attrs = sr.getAttributes();
				if (attrs != null) {
					groups.add(String.valueOf(attrs.get("sAMAccountName").get()));
				}
			}
		} finally {
			if (ctx != null) {
				ctx.close();
			}
		}
		return groups;
	}

	private Hashtable<String, String> getConnectionSettings() {
		Hashtable<String, String> env = new Hashtable<String, String>();
		env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
		env.put(Context.PROVIDER_URL, activeDirectoryUrls.get(0));
		env.put(Context.SECURITY_AUTHENTICATION, "simple");
		env.put(Context.SECURITY_PRINCIPAL, principle);
		env.put(Context.SECURITY_CREDENTIALS, password);
		env.put("java.naming.ldap.attributes.binary", "tokenGroups");
		return env;
	}

	/**
	 * Build a string from the SID provided
	 * 
	 * @param SID
	 * @return
	 */
	private static String binarySidToStringSid(byte[] SID) {
		String strSID = "";
		// convert the SID into string format
		long version;
		long authority;
		long count;
		long rid;
		strSID = "S";
		version = SID[0];
		strSID = strSID + "-" + Long.toString(version);
		authority = SID[4];
		for (int i = 0; i < 4; i++) {
			authority <<= 8;
			authority += SID[4 + i] & 0xFF;
		}
		strSID = strSID + "-" + Long.toString(authority);
		count = SID[2];
		count <<= 8;
		count += SID[1] & 0xFF;
		for (int j = 0; j < count; j++) {
			rid = SID[11 + (j * 4)] & 0xFF;
			for (int k = 1; k < 4; k++) {
				rid <<= 8;
				rid += SID[11 - k + (j * 4)] & 0xFF;
			}
			strSID = strSID + "-" + Long.toString(rid);
		}
		return strSID;
	}
}